"""
SmartWaste AI Service — Waste Classifier API
=============================================
FastAPI service that classifies waste images using a trained MobileNetV2 model.

Trained on: Kaggle "Garbage Classification" dataset (asdasdasasdas)
  https://www.kaggle.com/datasets/asdasdasasdas/garbage-classification
  Classes: cardboard, glass, metal, paper, plastic, trash

Endpoints:
  GET  /health          — Health check
  POST /classify        — Classify waste from uploaded image file
  POST /classify-url    — Classify waste from an image URL (Supabase Storage)
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
from typing import Optional

import numpy as np
from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image
from pydantic import BaseModel, HttpUrl

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
    description="Classifies waste images into cardboard, glass, metal, paper, plastic, or trash using the Kaggle Garbage Classification dataset.",
    version="1.0.0",
)

# Allow requests from the React frontend and Spring Boot backend
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

# ─── Model Loading ────────────────────────────────────────────────────────────
model = None
class_labels: dict[str, int] = {}   # {"ORGANIC": 0, "PLASTIC": 1, ...}
index_to_label: dict[int, str] = {} # {0: "ORGANIC", 1: "PLASTIC", ...}

IMG_SIZE = (224, 224)

# Waste type descriptions shown to the citizen
WASTE_DESCRIPTIONS = {
    "cardboard":  "Cardboard boxes, packaging, corrugated sheets, and paper-based containers.",
    "glass":      "Glass bottles, jars, broken glass, and other glass containers.",
    "metal":      "Aluminum cans, steel containers, tin cans, and other metallic items.",
    "paper":      "Newspapers, magazines, office paper, books, and paper packaging.",
    "plastic":    "Plastic bottles, bags, containers, wrappers, and other plastic materials.",
    "trash":      "General non-recyclable waste that doesn't fit other categories.",
}

# Disposal tips per category
DISPOSAL_TIPS = {
    "cardboard":  "Flatten boxes and place in the recycling bin. Keep dry — wet cardboard is not recyclable.",
    "glass":      "Rinse and place in a glass recycling bin. Do not mix with other recyclables.",
    "metal":      "Rinse cans and place in the recycling bin. Metal is infinitely recyclable.",
    "paper":      "Place in the paper recycling bin. Avoid soiled or greasy paper.",
    "plastic":    "Check the recycling number on the bottom. Rinse before placing in recycling bin.",
    "trash":      "Place in the general waste bin. Consider if any parts can be separated for recycling.",
}


@app.on_event("startup")
async def load_model():
    """Load the trained model and class labels on startup."""
    global model, class_labels, index_to_label

    if not MODEL_PATH.exists():
        logger.warning(
            f"Model not found at {MODEL_PATH}. "
            "Run `python train_model.py` to train the model first. "
            "The /classify endpoint will return an error until the model is loaded."
        )
        return

    if not LABELS_PATH.exists():
        logger.warning(f"Class labels not found at {LABELS_PATH}.")
        return

    try:
        # Import TensorFlow here to avoid slow startup if model isn't ready
        import tensorflow as tf

        logger.info(f"Loading model from {MODEL_PATH}...")
        model = tf.keras.models.load_model(str(MODEL_PATH))

        with open(LABELS_PATH) as f:
            class_labels = json.load(f)

        index_to_label = {v: k for k, v in class_labels.items()}
        logger.info(f"Model loaded. Classes: {list(class_labels.keys())}")

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


# ─── Helpers ──────────────────────────────────────────────────────────────────

def preprocess_image(img: Image.Image) -> np.ndarray:
    """Resize and normalize image for MobileNetV2 input."""
    img = img.convert("RGB")
    img = img.resize(IMG_SIZE, Image.LANCZOS)
    arr = np.array(img, dtype=np.float32) / 255.0
    return np.expand_dims(arr, axis=0)  # Shape: (1, 224, 224, 3)


def run_inference(img: Image.Image) -> ClassificationResult:
    """Run model inference and return structured result."""
    if model is None:
        raise HTTPException(
            status_code=503,
            detail="Model not loaded. Run `python train_model.py` first.",
        )

    input_arr = preprocess_image(img)
    predictions = model.predict(input_arr, verbose=0)[0]  # Shape: (num_classes,)

    # Top prediction
    top_idx = int(np.argmax(predictions))
    top_label = index_to_label.get(top_idx, "MIXED")
    top_confidence = float(predictions[top_idx])

    # All predictions as a dict
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
    )


# ─── Endpoints ────────────────────────────────────────────────────────────────

@app.get("/health")
async def health():
    """Health check — returns model status."""
    return {
        "status": "ok",
        "model_loaded": model is not None,
        "classes": list(class_labels.keys()) if class_labels else [],
    }


@app.get("/classes")
async def get_classes():
    """Return all supported waste categories with descriptions and disposal tips."""
    return {
        "classes": [
            {
                "name": cls,
                "description": WASTE_DESCRIPTIONS.get(cls, ""),
                "disposal_tip": DISPOSAL_TIPS.get(cls, ""),
            }
            for cls in (class_labels.keys() if class_labels else WASTE_DESCRIPTIONS.keys())
        ]
    }


@app.post("/classify", response_model=ClassificationResult)
async def classify_image(file: UploadFile = File(...)):
    """
    Classify waste from an uploaded image file.

    Accepts: JPEG, PNG, WEBP, BMP (max ~10MB)
    Returns: waste_type, confidence, description, disposal_tip, all_predictions
    """
    # Validate content type
    if not file.content_type or not file.content_type.startswith("image/"):
        raise HTTPException(
            status_code=400,
            detail=f"Invalid file type '{file.content_type}'. Only image files are accepted.",
        )

    try:
        contents = await file.read()

        # Limit file size to 10MB
        if len(contents) > 10 * 1024 * 1024:
            raise HTTPException(status_code=400, detail="File too large. Maximum size is 10MB.")

        img = Image.open(io.BytesIO(contents))
        logger.info(f"Classifying uploaded image: {file.filename} ({img.size}, {img.mode})")

        result = run_inference(img)
        logger.info(f"Result: {result.waste_type} ({result.confidence_percent})")
        return result

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Classification error: {e}")
        raise HTTPException(status_code=500, detail=f"Classification failed: {str(e)}")


@app.post("/classify-url", response_model=ClassificationResult)
async def classify_image_url(request: ClassifyUrlRequest):
    """
    Classify waste from an image URL (e.g. Supabase Storage public URL).

    Useful when the image is already uploaded to Supabase and you have the URL.
    """
    try:
        logger.info(f"Fetching image from URL: {request.image_url}")

        # Fetch image from URL
        req = urllib.request.Request(
            request.image_url,
            headers={"User-Agent": "SmartWaste-AI/1.0"},
        )
        with urllib.request.urlopen(req, timeout=10) as response:
            contents = response.read()

        if len(contents) > 10 * 1024 * 1024:
            raise HTTPException(status_code=400, detail="Remote image too large. Maximum size is 10MB.")

        img = Image.open(io.BytesIO(contents))
        logger.info(f"Fetched image: {img.size}, {img.mode}")

        result = run_inference(img)
        logger.info(f"Result: {result.waste_type} ({result.confidence_percent})")
        return result

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"URL classification error: {e}")
        raise HTTPException(status_code=500, detail=f"Failed to classify image from URL: {str(e)}")
