"""
SmartWaste AI Service — Waste Classifier API
=============================================
FastAPI service that classifies waste images.

Strategy (in order of preference):
  1. Custom trained model (model/waste_classifier.keras) — best accuracy if trained on real data
  2. MobileNetV2 + ImageNet label mapping — works immediately with no training required

Endpoints:
  GET  /health          — Health check
  POST /classify        — Classify waste from uploaded image file
  POST /classify-url    — Classify waste from an image URL
  GET  /classes         — List supported waste categories

Run:
  uvicorn main:app --host 0.0.0.0 --port 8000 --reload
"""

import io
import json
import logging
import os
import urllib.request
from pathlib import Path

import numpy as np
from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image
from pydantic import BaseModel

# ─── Logging ──────────────────────────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s — %(message)s",
)
logger = logging.getLogger("waste-classifier")

# ─── Paths ────────────────────────────────────────────────────────────────────
MODEL_DIR   = Path("model")
MODEL_PATH  = MODEL_DIR / "waste_classifier.keras"
LABELS_PATH = MODEL_DIR / "class_labels.json"

# ─── App ──────────────────────────────────────────────────────────────────────
app = FastAPI(
    title="SmartWaste AI — Waste Classifier",
    version="2.0.0",
)

ALLOWED_ORIGINS = os.getenv(
    "ALLOWED_ORIGINS",
    "http://localhost:3000,http://localhost:8080",
).split(",")

app.add_middleware(
    CORSMiddleware,
    allow_origins=ALLOWED_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ─── Model state ──────────────────────────────────────────────────────────────
custom_model = None
class_labels: dict = {}
index_to_label: dict = {}
mobilenet_model = None   # fallback MobileNetV2 for ImageNet-based classification

IMG_SIZE = (224, 224)

# ─── Waste type metadata ──────────────────────────────────────────────────────
WASTE_DESCRIPTIONS = {
    "cardboard":  "Cardboard boxes, packaging, corrugated sheets, and paper-based containers.",
    "glass":      "Glass bottles, jars, broken glass, and other glass containers.",
    "metal":      "Aluminum cans, steel containers, tin cans, screws, bolts, and other metallic items.",
    "paper":      "Newspapers, magazines, office paper, books, and paper packaging.",
    "plastic":    "Plastic bottles, bags, containers, wrappers, and other plastic materials.",
    "trash":      "General non-recyclable waste that doesn't fit other categories.",
}

DISPOSAL_TIPS = {
    "cardboard":  "Flatten boxes and place in the recycling bin. Keep dry — wet cardboard is not recyclable.",
    "glass":      "Rinse and place in a glass recycling bin. Do not mix with other recyclables.",
    "metal":      "Rinse cans and place in the recycling bin. Metal is infinitely recyclable.",
    "paper":      "Place in the paper recycling bin. Avoid soiled or greasy paper.",
    "plastic":    "Check the recycling number on the bottom. Rinse before placing in recycling bin.",
    "trash":      "Place in the general waste bin. Consider if any parts can be separated for recycling.",
}

# ─── ImageNet label → waste category mapping ─────────────────────────────────
# Maps ImageNet class names (from MobileNetV2) to our 6 waste categories.
# This allows accurate classification without any custom training.
IMAGENET_TO_WASTE: dict[str, str] = {
    # METAL — tools, hardware, containers
    "can_opener": "metal", "corkscrew": "metal", "hammer": "metal",
    "nail": "metal", "screw": "metal", "wrench": "metal",
    "chain": "metal", "padlock": "metal", "combination_lock": "metal",
    "steel_drum": "metal", "bucket": "metal", "pot": "metal",
    "frying_pan": "metal", "wok": "metal", "caldron": "metal",
    "tin_can": "metal", "beer_bottle": "metal",
    "safety_pin": "metal", "paper_clip": "metal", "mousetrap": "metal",
    "hatchet": "metal", "cleaver": "metal", "letter_opener": "metal",
    "spatula": "metal", "ladle": "metal", "strainer": "metal",
    "measuring_cup": "metal", "mixing_bowl": "metal",
    "file": "metal", "hand_blower": "metal",

    # PLASTIC — bottles, containers, bags
    "water_bottle": "plastic", "pop_bottle": "plastic",
    "plastic_bag": "plastic", "shopping_basket": "plastic",
    "milk_can": "plastic", "pill_bottle": "plastic",
    "lotion": "plastic", "soap_dispenser": "plastic",
    "shampoo": "plastic", "toothbrush": "plastic",
    "comb": "plastic", "hair_slide": "plastic",
    "Band_Aid": "plastic", "syringe": "plastic",
    "rubber_eraser": "plastic", "ballpoint": "plastic",
    "pencil_box": "plastic", "pencil_sharpener": "plastic",
    "ruler": "plastic", "scale": "plastic",
    "ladle": "plastic", "strainer": "plastic",
    "colander": "plastic", "funnel": "plastic",
    "ice_lolly": "plastic", "cup": "plastic",
    "pitcher": "plastic", "carton": "plastic",

    # GLASS — bottles, jars, lenses
    "wine_bottle": "glass", "beer_bottle": "glass",
    "whiskey_jug": "glass", "perfume": "glass",
    "lens_cap": "glass", "sunglasses": "glass",
    "magnifying_glass": "glass", "monocle": "glass",
    "goblet": "glass", "cocktail_shaker": "glass",
    "vase": "glass", "jar": "glass",

    # PAPER — books, documents, packaging
    "book_jacket": "paper", "comic_book": "paper",
    "envelope": "paper", "menu": "paper",
    "newspaper": "paper", "packet": "paper",
    "paper_towel": "paper", "toilet_tissue": "paper",
    "cardboard": "cardboard",

    # CARDBOARD — boxes, packaging
    "carton": "cardboard", "moving_van": "cardboard",
    "mailbox": "cardboard",

    # TRASH — food, organic, misc
    "banana": "trash", "orange": "trash", "lemon": "trash",
    "fig": "trash", "pineapple": "trash", "strawberry": "trash",
    "mushroom": "trash", "broccoli": "trash", "cauliflower": "trash",
    "zucchini": "trash", "spaghetti_squash": "trash",
    "head_cabbage": "trash", "artichoke": "trash",
    "corn": "trash", "acorn": "trash", "hip": "trash",
    "buckeye": "trash", "rapeseed": "trash",
    "diaper": "trash", "toilet_seat": "trash",
    "toilet": "trash", "bathtub": "trash",
    "ashcan": "trash", "wastebasket": "trash",
    "garbage_truck": "trash",
}

# Visual feature keywords for fallback heuristic classification
VISUAL_KEYWORDS = {
    "metal":     ["metal", "steel", "iron", "aluminum", "chrome", "silver", "bolt",
                  "screw", "nut", "nail", "wire", "chain", "can", "tin"],
    "plastic":   ["plastic", "bottle", "container", "bag", "wrap", "polymer",
                  "nylon", "vinyl", "pvc", "polyethylene"],
    "glass":     ["glass", "bottle", "jar", "window", "crystal", "transparent"],
    "paper":     ["paper", "newspaper", "magazine", "book", "document", "cardboard",
                  "carton", "box", "packaging"],
    "cardboard": ["cardboard", "box", "carton", "corrugated", "packaging"],
    "trash":     ["food", "organic", "waste", "garbage", "rubbish", "litter"],
}


# ─── Model Loading ────────────────────────────────────────────────────────────

@app.on_event("startup")
async def load_model():
    """Load models on startup. Always loads MobileNetV2 as reliable fallback."""
    global custom_model, class_labels, index_to_label, mobilenet_model

    try:
        import tensorflow as tf
        from tensorflow.keras.applications import MobileNetV2

        # Always load MobileNetV2 for ImageNet-based fallback classification
        logger.info("Loading MobileNetV2 (ImageNet weights) as base classifier...")
        mobilenet_model = MobileNetV2(weights="imagenet", include_top=True)
        logger.info("MobileNetV2 loaded successfully.")

        # Try to load custom trained model if it exists
        if MODEL_PATH.exists() and LABELS_PATH.exists():
            logger.info(f"Loading custom model from {MODEL_PATH}...")
            custom_model = tf.keras.models.load_model(str(MODEL_PATH))
            with open(LABELS_PATH) as f:
                class_labels = json.load(f)
            index_to_label = {v: k for k, v in class_labels.items()}
            logger.info(f"Custom model loaded. Classes: {list(class_labels.keys())}")
        else:
            logger.info(
                "No custom model found — using MobileNetV2 + ImageNet label mapping. "
                "Run python train_model.py with real dataset for better accuracy."
            )

    except Exception as e:
        logger.error(f"Failed to load model: {e}")


# ─── Schemas ──────────────────────────────────────────────────────────────────

class ClassifyUrlRequest(BaseModel):
    image_url: str


class ClassificationResult(BaseModel):
    waste_type: str
    confidence: float
    confidence_percent: str
    description: str
    disposal_tip: str
    all_predictions: dict[str, float]
    model_used: str = "custom"


# ─── Helpers ──────────────────────────────────────────────────────────────────

def preprocess_for_mobilenet(img: Image.Image) -> np.ndarray:
    """Preprocess image for MobileNetV2 — scales to [-1, 1]."""
    from tensorflow.keras.applications.mobilenet_v2 import preprocess_input
    img = img.convert("RGB")
    img = img.resize(IMG_SIZE, Image.LANCZOS)
    arr = np.array(img, dtype=np.float32)
    arr = preprocess_input(arr)
    return np.expand_dims(arr, axis=0)


def classify_with_custom_model(img: Image.Image) -> ClassificationResult:
    """Use the custom trained waste classifier."""
    input_arr = preprocess_for_mobilenet(img)
    predictions = custom_model.predict(input_arr, verbose=0)[0]

    top_idx = int(np.argmax(predictions))
    top_label = index_to_label.get(top_idx, "trash")
    top_confidence = float(predictions[top_idx])

    all_preds = {
        index_to_label.get(i, f"class_{i}"): round(float(p) * 100, 2)
        for i, p in enumerate(predictions)
    }

    return ClassificationResult(
        waste_type=top_label,
        confidence=round(top_confidence, 4),
        confidence_percent=f"{top_confidence * 100:.1f}%",
        description=WASTE_DESCRIPTIONS.get(top_label, ""),
        disposal_tip=DISPOSAL_TIPS.get(top_label, ""),
        all_predictions=all_preds,
        model_used="custom",
    )


def classify_with_imagenet(img: Image.Image) -> ClassificationResult:
    """
    Use MobileNetV2 ImageNet predictions mapped to waste categories.
    Aggregates confidence scores across all ImageNet classes that map
    to each waste category, giving robust multi-label classification.
    """
    from tensorflow.keras.applications.mobilenet_v2 import decode_predictions

    input_arr = preprocess_for_mobilenet(img)
    raw_preds = mobilenet_model.predict(input_arr, verbose=0)

    # Get top-50 ImageNet predictions for better coverage
    decoded = decode_predictions(raw_preds, top=50)[0]

    # Accumulate confidence per waste category
    waste_scores: dict[str, float] = {k: 0.0 for k in WASTE_DESCRIPTIONS}

    for _, label, confidence in decoded:
        label_lower = label.lower()

        # Direct mapping from known ImageNet labels
        if label_lower in IMAGENET_TO_WASTE:
            category = IMAGENET_TO_WASTE[label_lower]
            waste_scores[category] += float(confidence)
            continue

        # Keyword-based heuristic for unmapped labels
        for category, keywords in VISUAL_KEYWORDS.items():
            if any(kw in label_lower for kw in keywords):
                waste_scores[category] += float(confidence) * 0.5
                break

    # Normalize scores to percentages
    total = sum(waste_scores.values())
    if total == 0:
        # No matches — default to trash with low confidence
        waste_scores["trash"] = 1.0
        total = 1.0

    normalized = {k: round((v / total) * 100, 2) for k, v in waste_scores.items()}

    # Top prediction
    top_label = max(normalized, key=lambda k: normalized[k])
    top_pct = normalized[top_label]
    top_confidence = top_pct / 100.0

    return ClassificationResult(
        waste_type=top_label,
        confidence=round(top_confidence, 4),
        confidence_percent=f"{top_pct:.1f}%",
        description=WASTE_DESCRIPTIONS.get(top_label, ""),
        disposal_tip=DISPOSAL_TIPS.get(top_label, ""),
        all_predictions=normalized,
        model_used="imagenet_mapping",
    )


def run_inference(img: Image.Image) -> ClassificationResult:
    """Run inference — prefer custom model, fall back to ImageNet mapping."""
    if mobilenet_model is None:
        raise HTTPException(
            status_code=503,
            detail="Model not loaded. Please restart the AI service.",
        )

    if custom_model is not None:
        return classify_with_custom_model(img)
    else:
        return classify_with_imagenet(img)


# ─── Endpoints ────────────────────────────────────────────────────────────────

@app.get("/health")
async def health():
    return {
        "status": "ok",
        "custom_model_loaded": custom_model is not None,
        "imagenet_fallback_loaded": mobilenet_model is not None,
        "classes": list(class_labels.keys()) if class_labels else list(WASTE_DESCRIPTIONS.keys()),
    }


@app.get("/classes")
async def get_classes():
    return {
        "classes": [
            {
                "name": cls,
                "description": WASTE_DESCRIPTIONS.get(cls, ""),
                "disposal_tip": DISPOSAL_TIPS.get(cls, ""),
            }
            for cls in WASTE_DESCRIPTIONS.keys()
        ]
    }


@app.post("/classify", response_model=ClassificationResult)
async def classify_image(file: UploadFile = File(...)):
    """Classify waste from an uploaded image file."""
    if not file.content_type or not file.content_type.startswith("image/"):
        raise HTTPException(
            status_code=400,
            detail=f"Invalid file type '{file.content_type}'. Only image files are accepted.",
        )

    try:
        contents = await file.read()
        if len(contents) > 10 * 1024 * 1024:
            raise HTTPException(status_code=400, detail="File too large. Maximum size is 10MB.")

        img = Image.open(io.BytesIO(contents))
        logger.info(f"Classifying: {file.filename} ({img.size})")

        result = run_inference(img)
        logger.info(f"Result: {result.waste_type} ({result.confidence_percent}) via {result.model_used}")
        return result

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Classification error: {e}")
        raise HTTPException(status_code=500, detail=f"Classification failed: {str(e)}")


@app.post("/classify-url", response_model=ClassificationResult)
async def classify_image_url(request: ClassifyUrlRequest):
    """Classify waste from an image URL."""
    try:
        req = urllib.request.Request(
            request.image_url,
            headers={"User-Agent": "SmartWaste-AI/2.0"},
        )
        with urllib.request.urlopen(req, timeout=10) as response:
            contents = response.read()

        if len(contents) > 10 * 1024 * 1024:
            raise HTTPException(status_code=400, detail="Remote image too large.")

        img = Image.open(io.BytesIO(contents))
        result = run_inference(img)
        logger.info(f"URL result: {result.waste_type} ({result.confidence_percent})")
        return result

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"URL classification error: {e}")
        raise HTTPException(status_code=500, detail=f"Failed to classify image from URL: {str(e)}")
