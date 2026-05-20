import { useState, useRef } from 'react'
import { Upload, X, Loader2, Recycle, AlertTriangle, Zap, Leaf, Package, Layers } from 'lucide-react'
import toast from 'react-hot-toast'
import axios from 'axios'

// ─── Types ────────────────────────────────────────────────────────────────────

interface ClassificationResult {
  waste_type: string
  confidence: number
  confidence_percent: string
  description: string
  disposal_tip: string
  all_predictions: Record<string, number>
}

// ─── Constants ────────────────────────────────────────────────────────────────

const WASTE_ICONS: Record<string, React.ReactNode> = {
  cardboard:  <Package className="w-6 h-6" />,
  glass:      <Layers className="w-6 h-6" />,
  metal:      <Zap className="w-6 h-6" />,
  paper:      <Leaf className="w-6 h-6" />,
  plastic:    <Recycle className="w-6 h-6" />,
  trash:      <AlertTriangle className="w-6 h-6" />,
}

const WASTE_COLORS: Record<string, { bg: string; text: string; bar: string; badge: string }> = {
  cardboard:  { bg: 'bg-amber-50',   text: 'text-amber-700',   bar: 'bg-amber-500',   badge: 'bg-amber-100 text-amber-800' },
  glass:      { bg: 'bg-cyan-50',    text: 'text-cyan-700',    bar: 'bg-cyan-500',    badge: 'bg-cyan-100 text-cyan-800' },
  metal:      { bg: 'bg-gray-50',    text: 'text-gray-700',    bar: 'bg-gray-500',    badge: 'bg-gray-100 text-gray-800' },
  paper:      { bg: 'bg-yellow-50',  text: 'text-yellow-700',  bar: 'bg-yellow-500',  badge: 'bg-yellow-100 text-yellow-800' },
  plastic:    { bg: 'bg-blue-50',    text: 'text-blue-700',    bar: 'bg-blue-500',    badge: 'bg-blue-100 text-blue-800' },
  trash:      { bg: 'bg-red-50',     text: 'text-red-700',     bar: 'bg-red-500',     badge: 'bg-red-100 text-red-800' },
}

const DEFAULT_COLOR = { bg: 'bg-gray-50', text: 'text-gray-700', bar: 'bg-gray-500', badge: 'bg-gray-100 text-gray-800' }

// ─── Component ────────────────────────────────────────────────────────────────

export default function WasteClassifierPage() {
  const [imageFile, setImageFile]       = useState<File | null>(null)
  const [imagePreview, setImagePreview] = useState<string | null>(null)
  const [classifying, setClassifying]   = useState(false)
  const [result, setResult]             = useState<ClassificationResult | null>(null)
  const [dragOver, setDragOver]         = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  // ── File handling ──────────────────────────────────────────────────────────

  const handleFile = (file: File) => {
    if (!file.type.startsWith('image/')) {
      toast.error('Only image files are supported (JPG, PNG, WEBP)')
      return
    }
    if (file.size > 10 * 1024 * 1024) {
      toast.error('Image must be smaller than 10MB')
      return
    }

    setImageFile(file)
    setResult(null)

    const reader = new FileReader()
    reader.onload = (e) => setImagePreview(e.target?.result as string)
    reader.readAsDataURL(file)
  }

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) handleFile(file)
  }

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault()
    setDragOver(false)
    const file = e.dataTransfer.files?.[0]
    if (file) handleFile(file)
  }

  const handleRemove = () => {
    setImageFile(null)
    setImagePreview(null)
    setResult(null)
    if (fileInputRef.current) fileInputRef.current.value = ''
  }

  // ── Classification ─────────────────────────────────────────────────────────

  const classify = async () => {
    if (!imageFile) return

    setClassifying(true)
    setResult(null)

    try {
      const formData = new FormData()
      formData.append('file', imageFile)

      const response = await axios.post<ClassificationResult>('/ai/classify', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 30_000,
      })

      setResult(response.data)
      toast.success('Classification complete!')
    } catch (err: any) {
      if (err.code === 'ECONNREFUSED' || err.message?.includes('Network Error')) {
        toast.error('AI service is not running. Start it with: uvicorn main:app --port 8000')
      } else if (err.response?.status === 503) {
        toast.error('Model not loaded yet. Run python train_model.py first.')
      } else {
        toast.error(err.response?.data?.detail ?? 'Classification failed. Please try again.')
      }
    } finally {
      setClassifying(false)
    }
  }

  // ── Render ─────────────────────────────────────────────────────────────────

  const colors = result ? (WASTE_COLORS[result.waste_type] ?? DEFAULT_COLOR) : DEFAULT_COLOR

  // Sort predictions by confidence descending
  const sortedPredictions = result
    ? Object.entries(result.all_predictions).sort(([, a], [, b]) => b - a)
    : []

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-xl font-bold text-gray-800 flex items-center gap-2">
          <Recycle className="w-6 h-6 text-primary-600" />
          Waste Classifier
        </h1>
        <p className="text-sm text-gray-500 mt-1">
          Upload a photo of waste and our AI will identify what type it is and how to dispose of it.
        </p>
      </div>

      {/* Upload area */}
      <div className="bg-white rounded-xl shadow-sm p-6 space-y-4">
        <h2 className="font-semibold text-gray-700 text-sm">Upload Waste Image</h2>

        {imagePreview ? (
          /* Image preview */
          <div className="relative rounded-xl overflow-hidden border border-gray-200">
            <img
              src={imagePreview}
              alt="Waste preview"
              className="w-full max-h-72 object-contain bg-gray-50"
            />
            <button
              onClick={handleRemove}
              className="absolute top-2 right-2 bg-red-500 hover:bg-red-600 text-white rounded-full p-1.5 transition shadow"
              title="Remove image"
            >
              <X className="w-4 h-4" />
            </button>
            {imageFile && (
              <div className="absolute bottom-2 left-2 bg-black/50 text-white text-xs px-2 py-1 rounded-full">
                {imageFile.name} · {(imageFile.size / 1024).toFixed(0)} KB
              </div>
            )}
          </div>
        ) : (
          /* Drop zone */
          <label
            onDragOver={(e) => { e.preventDefault(); setDragOver(true) }}
            onDragLeave={() => setDragOver(false)}
            onDrop={handleDrop}
            className={`flex flex-col items-center justify-center w-full h-48 border-2 border-dashed rounded-xl cursor-pointer transition ${
              dragOver
                ? 'border-primary-500 bg-primary-50'
                : 'border-gray-300 hover:border-primary-400 hover:bg-gray-50'
            }`}
          >
            <Upload className={`w-10 h-10 mb-3 ${dragOver ? 'text-primary-500' : 'text-gray-400'}`} />
            <p className="text-sm text-gray-600">
              <span className="text-primary-600 font-medium">Click to upload</span> or drag & drop
            </p>
            <p className="text-xs text-gray-400 mt-1">JPG, PNG, WEBP — max 10MB</p>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              onChange={handleInputChange}
              className="hidden"
            />
          </label>
        )}

        {/* Classify button */}
        <button
          onClick={classify}
          disabled={!imageFile || classifying}
          className="w-full bg-primary-600 hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed text-white py-2.5 rounded-lg text-sm font-medium transition flex items-center justify-center gap-2"
        >
          {classifying ? (
            <>
              <Loader2 className="w-4 h-4 animate-spin" />
              Analyzing image...
            </>
          ) : (
            <>
              <Recycle className="w-4 h-4" />
              Classify Waste
            </>
          )}
        </button>
      </div>

      {/* Result card */}
      {result && (
        <div className={`rounded-xl shadow-sm overflow-hidden border border-gray-100`}>
          {/* Top result banner */}
          <div className={`${colors.bg} px-6 py-5`}>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className={`${colors.text} p-2 rounded-lg bg-white/60`}>
                  {WASTE_ICONS[result.waste_type] ?? <Recycle className="w-6 h-6" />}
                </div>
                <div>
                  <p className="text-xs font-medium text-gray-500 uppercase tracking-wide">Detected Waste Type</p>
                  <p className={`text-2xl font-bold ${colors.text}`}>{result.waste_type}</p>
                </div>
              </div>
              <div className="text-right">
                <p className="text-xs text-gray-500">Confidence</p>
                <p className={`text-3xl font-bold ${colors.text}`}>{result.confidence_percent}</p>
              </div>
            </div>
          </div>

          <div className="bg-white px-6 py-5 space-y-5">
            {/* Description */}
            <div>
              <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-1">About this waste</p>
              <p className="text-sm text-gray-700">{result.description}</p>
            </div>

            {/* Disposal tip */}
            <div className={`${colors.bg} rounded-lg p-4`}>
              <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-1">How to dispose</p>
              <p className={`text-sm font-medium ${colors.text}`}>{result.disposal_tip}</p>
            </div>

            {/* All predictions breakdown */}
            <div>
              <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-3">Confidence breakdown</p>
              <div className="space-y-2">
                {sortedPredictions.map(([label, pct]) => {
                  const c = WASTE_COLORS[label] ?? DEFAULT_COLOR
                  return (
                    <div key={label} className="flex items-center gap-3">
                      <span className="text-xs text-gray-600 w-28 shrink-0">{label}</span>
                      <div className="flex-1 bg-gray-100 rounded-full h-2 overflow-hidden">
                        <div
                          className={`h-2 rounded-full ${c.bar} transition-all duration-500`}
                          style={{ width: `${Math.min(pct, 100)}%` }}
                        />
                      </div>
                      <span className="text-xs text-gray-500 w-12 text-right shrink-0">{pct.toFixed(1)}%</span>
                    </div>
                  )
                })}
              </div>
            </div>

            {/* Try another */}
            <button
              onClick={handleRemove}
              className="w-full border border-gray-300 text-gray-600 hover:bg-gray-50 py-2 rounded-lg text-sm transition"
            >
              Classify another image
            </button>
          </div>
        </div>
      )}

      {/* Info card — shown before any result */}
      {!result && (
        <div className="bg-white rounded-xl shadow-sm p-5">
          <h3 className="font-semibold text-gray-700 text-sm mb-3">Supported Waste Categories</h3>
          <div className="grid grid-cols-2 gap-2">
            {Object.entries(WASTE_ICONS).map(([label, icon]) => {
              const c = WASTE_COLORS[label] ?? DEFAULT_COLOR
              return (
                <div key={label} className={`flex items-center gap-2 px-3 py-2 rounded-lg ${c.badge}`}>
                  <span className="shrink-0">{icon}</span>
                  <span className="text-xs font-medium">{label}</span>
                </div>
              )
            })}
          </div>
          <p className="text-xs text-gray-400 mt-3">
            Powered by MobileNetV2 transfer learning. For best results, use clear, well-lit photos.
          </p>
        </div>
      )}
    </div>
  )
}
