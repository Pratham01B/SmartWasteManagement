"""
Sample Dataset Generator
=========================
Creates synthetic training images that mirror the folder structure of the
Kaggle "Garbage Classification" dataset (asdasdasasdas), so you can test
the full training pipeline without downloading the real dataset first.

Real dataset (recommended for production):
  https://www.kaggle.com/datasets/asdasdasasdas/garbage-classification
  Classes: cardboard, glass, metal, paper, plastic, trash (~2,467 images)

Synthetic images are solid-color + noise + random shapes — enough to verify
the pipeline runs end-to-end. Replace with real images for actual accuracy.

Usage:
  python generate_sample_data.py
"""

import random
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

# ─── Config ───────────────────────────────────────────────────────────────────

# Exact class names matching the Kaggle dataset folder names
CLASSES = ["cardboard", "glass", "metal", "paper", "plastic", "trash"]

# Approximate real dataset distribution (used as reference)
REAL_COUNTS = {
    "cardboard": 393,
    "glass":     491,
    "metal":     400,
    "paper":     584,
    "plastic":   472,
    "trash":     127,
}

# Synthetic counts — scaled down for quick testing
SYNTHETIC_TRAIN = 60   # images per class for training
SYNTHETIC_TEST  = 15   # images per class for testing

IMG_SIZE = (224, 224)

# Distinct background colors per class for visual differentiation
CLASS_COLORS = {
    "cardboard": [(139, 90,  43),  (160, 110, 60),  (180, 130, 80)],   # Browns
    "glass":     [(173, 216, 230), (135, 206, 235), (176, 224, 230)],  # Light blues
    "metal":     [(169, 169, 169), (192, 192, 192), (128, 128, 128)],  # Grays/silvers
    "paper":     [(255, 255, 224), (255, 250, 205), (240, 230, 140)],  # Yellows/creams
    "plastic":   [(30,  144, 255), (0,  191, 255),  (100, 149, 237)],  # Blues
    "trash":     [(85,  85,  85),  (64,  64,  64),  (105, 105, 105)],  # Dark grays
}

# Mirror the Kaggle structure: dataset/Garbage classification/<class>/
DATASET_DIR = Path("dataset") / "Garbage classification"


def create_synthetic_image(label: str, index: int) -> Image.Image:
    """
    Create a 224×224 synthetic image with:
    - Class-specific background color
    - Random pixel noise to simulate texture
    - Random geometric shapes for visual complexity
    - Text label overlay
    """
    base_color = random.choice(CLASS_COLORS[label])
    img = Image.new("RGB", IMG_SIZE, base_color)
    pixels = img.load()

    # Add per-pixel noise
    for y in range(IMG_SIZE[1]):
        for x in range(IMG_SIZE[0]):
            r, g, b = pixels[x, y]
            n = random.randint(-30, 30)
            pixels[x, y] = (
                max(0, min(255, r + n)),
                max(0, min(255, g + n)),
                max(0, min(255, b + n)),
            )

    # Draw random shapes to add visual complexity
    draw = ImageDraw.Draw(img)
    for _ in range(random.randint(3, 8)):
        x1 = random.randint(0, IMG_SIZE[0] - 50)
        y1 = random.randint(0, IMG_SIZE[1] - 50)
        x2 = x1 + random.randint(20, 80)
        y2 = y1 + random.randint(20, 80)
        color = tuple(random.randint(0, 255) for _ in range(3))
        if random.random() > 0.5:
            draw.rectangle([x1, y1, x2, y2], fill=color)
        else:
            draw.ellipse([x1, y1, x2, y2], fill=color)

    # Draw label text
    try:
        font = ImageFont.truetype("arial.ttf", 18)
    except (IOError, OSError):
        font = ImageFont.load_default()

    draw.text((8, 8), f"{label}\n#{index}", fill=(255, 255, 255), font=font)

    return img


def generate_split(split_name: str, count: int):
    """Generate `count` synthetic images per class into dataset/Garbage classification/<class>/."""
    print(f"\nGenerating {split_name} images ({count} per class)...")
    for label in CLASSES:
        class_dir = DATASET_DIR / label
        class_dir.mkdir(parents=True, exist_ok=True)

        # Use an offset for test images so filenames don't collide with train
        offset = SYNTHETIC_TRAIN if split_name == "test" else 0

        for i in range(count):
            img = create_synthetic_image(label, offset + i)
            img_path = class_dir / f"{label}_{offset + i:04d}.jpg"
            img.save(img_path, "JPEG", quality=85)

        print(f"  {label:12s}: {count} images → {class_dir}")


def main():
    print("=" * 60)
    print("SmartWaste — Synthetic Dataset Generator")
    print("Mirrors: Kaggle Garbage Classification dataset structure")
    print("=" * 60)
    print(f"\nOutput directory : {DATASET_DIR.resolve()}")
    print(f"Classes          : {CLASSES}")
    print(f"Train per class  : {SYNTHETIC_TRAIN}")
    print(f"Test per class   : {SYNTHETIC_TEST}")
    print(f"Total images     : {(SYNTHETIC_TRAIN + SYNTHETIC_TEST) * len(CLASSES)}")

    print("\nReal dataset distribution (for reference):")
    for cls, count in REAL_COUNTS.items():
        print(f"  {cls:12s}: {count} images")

    # Generate both splits into the same folder structure
    # (train_model.py uses validation_split to separate them)
    generate_split("train", SYNTHETIC_TRAIN)
    generate_split("test",  SYNTHETIC_TEST)

    total = (SYNTHETIC_TRAIN + SYNTHETIC_TEST) * len(CLASSES)
    print(f"\nDone! Generated {total} synthetic images.")
    print("\n" + "─" * 60)
    print("NOTE: These are synthetic images for pipeline testing only.")
    print("For real accuracy, download the actual dataset:")
    print("  https://www.kaggle.com/datasets/asdasdasasdas/garbage-classification")
    print("\nAfter downloading, extract so the structure is:")
    print(f"  {DATASET_DIR.resolve()}/")
    print("    cardboard/  (393 real images)")
    print("    glass/      (491 real images)")
    print("    metal/      (400 real images)")
    print("    paper/      (584 real images)")
    print("    plastic/    (472 real images)")
    print("    trash/      (127 real images)")


if __name__ == "__main__":
    main()
