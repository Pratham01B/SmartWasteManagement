# SmartWaste AI Service — Waste Classifier

Python FastAPI service that classifies waste images into 6 categories using MobileNetV2 transfer learning,
trained on the **Kaggle Garbage Classification** dataset.

## Dataset

**Kaggle Garbage Classification** by asdasdasasdas  
URL: https://www.kaggle.com/datasets/asdasdasasdas/garbage-classification  
Total images: ~2,467

| Class | Images | Description |
|-------|--------|-------------|
| cardboard | 393 | Boxes, packaging, corrugated sheets |
| glass | 491 | Bottles, jars, broken glass |
| metal | 400 | Aluminum cans, steel containers, tin |
| paper | 584 | Newspapers, magazines, office paper |
| plastic | 472 | Bottles, bags, containers, wrappers |
| trash | 127 | General non-recyclable waste |

## Setup

### 1. Install dependencies
```bash
cd ai-service
pip install -r requirements.txt
```

### 2. Prepare the dataset

**Option A — Real Kaggle dataset (recommended):**
1. Go to https://www.kaggle.com/datasets/asdasdasasdas/garbage-classification
2. Click **Download** and extract the zip
3. Place the extracted folder so the structure is:
```
ai-service/
  dataset/
    Garbage classification/
      cardboard/   ← 393 images
      glass/       ← 491 images
      metal/       ← 400 images
      paper/       ← 584 images
      plastic/     ← 472 images
      trash/       ← 127 images
```

**Option B — Synthetic data (pipeline testing only):**
```bash
python generate_sample_data.py
```
This creates 60 synthetic images per class — enough to verify the training pipeline runs,
but accuracy will be meaningless. Use real data for actual deployment.

### 3. Train the model
```bash
python train_model.py
```
Training runs in two phases:
- **Phase 1** — MobileNetV2 base frozen, only the classification head trains (~5–10 epochs)
- **Phase 2** — Upper MobileNetV2 layers unfrozen, fine-tuned at a low learning rate

Outputs:
- `model/waste_classifier.keras` — trained model
- `model/class_labels.json` — class name → index mapping
- `model/training_curves.png` — accuracy/loss plots

Expected accuracy on real dataset: **~90–95%** (MobileNetV2 transfer learning)

### 4. Start the API server
```bash
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/health` | Health check + model status |
| GET | `/classes` | List all waste categories with tips |
| POST | `/classify` | Classify from uploaded image file |
| POST | `/classify-url` | Classify from image URL (Supabase) |

### Example: Classify uploaded file
```bash
curl -X POST http://localhost:8000/classify \
  -F "file=@/path/to/waste_image.jpg"
```

### Example: Classify from Supabase URL
```bash
curl -X POST http://localhost:8000/classify-url \
  -H "Content-Type: application/json" \
  -d '{"image_url": "https://your-supabase-url.com/storage/v1/object/public/..."}'
```

### Response format
```json
{
  "waste_type": "plastic",
  "confidence": 0.9234,
  "confidence_percent": "92.3%",
  "description": "Plastic bottles, bags, containers, wrappers, and other plastic materials.",
  "disposal_tip": "Check the recycling number on the bottom. Rinse before placing in recycling bin.",
  "all_predictions": {
    "cardboard": 0.5,
    "glass": 1.2,
    "metal": 0.8,
    "paper": 2.1,
    "plastic": 92.3,
    "trash": 3.1
  }
}
```

## Vite Proxy (Development)
The frontend Vite dev server proxies `/ai` → `http://localhost:8000`.  
Frontend calls `/ai/classify` → AI service at `http://localhost:8000/classify`.
