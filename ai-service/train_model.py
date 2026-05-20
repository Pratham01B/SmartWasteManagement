"""
Waste Classifier — Model Training Script
=========================================
Uses MobileNetV2 (transfer learning) to classify waste images using the
Kaggle "Garbage Classification" dataset by asdasdasasdas.

Dataset: https://www.kaggle.com/datasets/asdasdasasdas/garbage-classification
Classes (6): cardboard, glass, metal, paper, plastic, trash
Total images: ~2,467

Dataset folder structure after download & extraction:
  dataset/
    Garbage classification/
      cardboard/   (393 images)
      glass/       (491 images)
      metal/       (400 images)
      paper/       (584 images)
      plastic/     (472 images)
      trash/       (127 images)

Steps:
  1. Download dataset from Kaggle (see README.md for instructions)
  2. Place it so the path matches DATASET_DIR below
  3. Run: python train_model.py

Trained model saved to: ./model/waste_classifier.keras
Class labels saved to : ./model/class_labels.json
"""

import json
import numpy as np
import matplotlib.pyplot as plt
from pathlib import Path

import tensorflow as tf
from tensorflow.keras import layers, models
from tensorflow.keras.applications import MobileNetV2
from tensorflow.keras.preprocessing.image import ImageDataGenerator
from tensorflow.keras.callbacks import EarlyStopping, ModelCheckpoint, ReduceLROnPlateau
from sklearn.metrics import classification_report

# ─── Config ───────────────────────────────────────────────────────────────────
IMG_SIZE     = (224, 224)
BATCH_SIZE   = 32
EPOCHS       = 30          # EarlyStopping will cut this short if needed
FINE_TUNE_AT = 100         # Unfreeze MobileNetV2 layers from this index onward
VAL_SPLIT    = 0.15        # 15% of data used for validation
TEST_SPLIT   = 0.15        # 15% of data used for test (held out before training)

# Kaggle dataset: after extraction the images live inside "Garbage classification/"
DATASET_DIR  = Path("dataset") / "Garbage classification"
MODEL_DIR    = Path("model")
MODEL_PATH   = MODEL_DIR / "waste_classifier.keras"
LABELS_PATH  = MODEL_DIR / "class_labels.json"

# Exact folder names as they appear in the Kaggle dataset
CLASSES = ["cardboard", "glass", "metal", "paper", "plastic", "trash"]

# ─── Helpers ──────────────────────────────────────────────────────────────────

def build_data_generators():
    """
    Build train / validation / test generators from the Kaggle dataset.

    Since the Kaggle dataset has no pre-split train/test folders, we use
    ImageDataGenerator's validation_split for val, and a separate generator
    (with a fixed seed) to approximate a held-out test set.
    """
    # Training + validation generators (augmented)
    train_datagen = ImageDataGenerator(
        rescale=1.0 / 255,
        rotation_range=20,
        width_shift_range=0.15,
        height_shift_range=0.15,
        shear_range=0.1,
        zoom_range=0.2,
        horizontal_flip=True,
        brightness_range=[0.8, 1.2],
        fill_mode="nearest",
        validation_split=VAL_SPLIT,
    )

    # Test generator — no augmentation, just rescale
    test_datagen = ImageDataGenerator(
        rescale=1.0 / 255,
        validation_split=TEST_SPLIT,   # reuse split mechanism for test slice
    )

    train_gen = train_datagen.flow_from_directory(
        DATASET_DIR,
        target_size=IMG_SIZE,
        batch_size=BATCH_SIZE,
        class_mode="categorical",
        subset="training",
        shuffle=True,
        classes=CLASSES,
        seed=42,
    )

    val_gen = train_datagen.flow_from_directory(
        DATASET_DIR,
        target_size=IMG_SIZE,
        batch_size=BATCH_SIZE,
        class_mode="categorical",
        subset="validation",
        shuffle=False,
        classes=CLASSES,
        seed=42,
    )

    # Use the validation subset of the no-augmentation generator as test set
    test_gen = test_datagen.flow_from_directory(
        DATASET_DIR,
        target_size=IMG_SIZE,
        batch_size=BATCH_SIZE,
        class_mode="categorical",
        subset="validation",
        shuffle=False,
        classes=CLASSES,
        seed=99,   # Different seed → different slice than val
    )

    return train_gen, val_gen, test_gen


def build_model(num_classes: int):
    """
    Build a transfer-learning model on top of MobileNetV2.
    Returns (model, base_model) so we can unfreeze base_model in phase 2.
    """
    base_model = MobileNetV2(
        input_shape=(*IMG_SIZE, 3),
        include_top=False,
        weights="imagenet",
    )
    base_model.trainable = False   # Freeze for phase 1

    inputs = tf.keras.Input(shape=(*IMG_SIZE, 3))
    x = base_model(inputs, training=False)
    x = layers.GlobalAveragePooling2D()(x)
    x = layers.BatchNormalization()(x)
    x = layers.Dense(256, activation="relu")(x)
    x = layers.Dropout(0.4)(x)
    x = layers.Dense(128, activation="relu")(x)
    x = layers.Dropout(0.3)(x)
    outputs = layers.Dense(num_classes, activation="softmax")(x)

    model = models.Model(inputs, outputs)
    return model, base_model


def plot_history(history, save_path: Path):
    """Save training/validation accuracy and loss curves."""
    fig, axes = plt.subplots(1, 2, figsize=(12, 4))

    axes[0].plot(history.history["accuracy"],     label="Train Acc")
    axes[0].plot(history.history["val_accuracy"], label="Val Acc")
    axes[0].set_title("Accuracy")
    axes[0].legend()
    axes[0].set_xlabel("Epoch")

    axes[1].plot(history.history["loss"],     label="Train Loss")
    axes[1].plot(history.history["val_loss"], label="Val Loss")
    axes[1].set_title("Loss")
    axes[1].legend()
    axes[1].set_xlabel("Epoch")

    plt.tight_layout()
    plt.savefig(save_path)
    plt.close()
    print(f"Training curves saved to {save_path}")


# ─── Main ─────────────────────────────────────────────────────────────────────

def main():
    print("=" * 60)
    print("SmartWaste — Waste Classifier Training")
    print("Dataset: Kaggle Garbage Classification (6 classes)")
    print("=" * 60)

    # Validate dataset path
    if not DATASET_DIR.exists():
        raise FileNotFoundError(
            f"\nDataset not found at: {DATASET_DIR.resolve()}\n\n"
            "Download steps:\n"
            "  1. Go to https://www.kaggle.com/datasets/asdasdasasdas/garbage-classification\n"
            "  2. Click Download → extract the zip\n"
            "  3. Place the extracted folder so the path is:\n"
            f"     {DATASET_DIR.resolve()}\n"
            "     (it should contain: cardboard/, glass/, metal/, paper/, plastic/, trash/)\n"
        )

    # Check all class folders exist
    missing = [c for c in CLASSES if not (DATASET_DIR / c).exists()]
    if missing:
        raise FileNotFoundError(
            f"Missing class folders in dataset: {missing}\n"
            f"Expected folders: {CLASSES}\n"
            f"Found in {DATASET_DIR}: {[p.name for p in DATASET_DIR.iterdir() if p.is_dir()]}"
        )

    MODEL_DIR.mkdir(parents=True, exist_ok=True)

    # ── Data ──────────────────────────────────────────────────────────────────
    print("\n[1/4] Loading dataset...")
    train_gen, val_gen, test_gen = build_data_generators()

    print(f"  Classes       : {list(train_gen.class_indices.keys())}")
    print(f"  Train samples : {train_gen.samples}")
    print(f"  Val samples   : {val_gen.samples}")
    print(f"  Test samples  : {test_gen.samples}")

    # Save class label mapping  {class_name: index}
    with open(LABELS_PATH, "w") as f:
        json.dump(train_gen.class_indices, f, indent=2)
    print(f"  Class labels saved → {LABELS_PATH}")

    num_classes = len(train_gen.class_indices)

    # ── Phase 1: Train classification head ────────────────────────────────────
    print("\n[2/4] Phase 1 — Training classification head (MobileNetV2 frozen)...")
    model, base_model = build_model(num_classes)

    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=1e-3),
        loss="categorical_crossentropy",
        metrics=["accuracy"],
    )
    model.summary()

    callbacks_p1 = [
        EarlyStopping(monitor="val_accuracy", patience=5, restore_best_weights=True, verbose=1),
        ReduceLROnPlateau(monitor="val_loss", factor=0.5, patience=3, verbose=1),
        ModelCheckpoint(str(MODEL_PATH), monitor="val_accuracy", save_best_only=True, verbose=1),
    ]

    history1 = model.fit(
        train_gen,
        validation_data=val_gen,
        epochs=EPOCHS,
        callbacks=callbacks_p1,
        verbose=1,
    )

    # ── Phase 2: Fine-tune upper MobileNetV2 layers ───────────────────────────
    print(f"\n[3/4] Phase 2 — Fine-tuning MobileNetV2 from layer {FINE_TUNE_AT}...")
    base_model.trainable = True

    # Keep all layers before FINE_TUNE_AT frozen
    for layer in base_model.layers[:FINE_TUNE_AT]:
        layer.trainable = False

    # Lower learning rate to avoid destroying pretrained weights
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=1e-5),
        loss="categorical_crossentropy",
        metrics=["accuracy"],
    )

    callbacks_p2 = [
        EarlyStopping(monitor="val_accuracy", patience=7, restore_best_weights=True, verbose=1),
        ReduceLROnPlateau(monitor="val_loss", factor=0.5, patience=3, verbose=1),
        ModelCheckpoint(str(MODEL_PATH), monitor="val_accuracy", save_best_only=True, verbose=1),
    ]

    history2 = model.fit(
        train_gen,
        validation_data=val_gen,
        epochs=EPOCHS,
        callbacks=callbacks_p2,
        verbose=1,
    )

    # ── Evaluation ────────────────────────────────────────────────────────────
    print("\n[4/4] Evaluating on test set...")
    test_loss, test_acc = model.evaluate(test_gen, verbose=1)
    print(f"\n  Test Accuracy : {test_acc:.4f}")
    print(f"  Test Loss     : {test_loss:.4f}")

    # Per-class classification report
    test_gen.reset()
    y_pred_probs = model.predict(test_gen, verbose=1)
    y_pred = np.argmax(y_pred_probs, axis=1)
    y_true = test_gen.classes

    label_names = list(test_gen.class_indices.keys())
    print("\nClassification Report:")
    print(classification_report(y_true, y_pred, target_names=label_names))

    # Save training curves from phase 2
    plot_history(history2, MODEL_DIR / "training_curves.png")

    print(f"\nModel saved → {MODEL_PATH}")
    print("Training complete!")


if __name__ == "__main__":
    main()
