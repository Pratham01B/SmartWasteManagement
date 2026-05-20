import { useState, useRef, useEffect, useCallback } from 'react'
import { Upload, X, Loader2, Recycle, AlertTriangle, Zap, Leaf, Package, Layers, Info } from 'lucide-react'
import toast from 'react-hot-toast'

// ─── Types ────────────────────────────────────────────────────────────────────

interface ClassificationResult {
  waste_type: string
  confidence: number
  confidence_percent: string
  description: string
  disposal_tip: string
  all_predictions: Record<string, number>
}

// ─── Waste metadata ───────────────────────────────────────────────────────────

const WASTE_DESCRIPTIONS: Record<string, string> = {
  Cardboard: 'Cardboard boxes, packaging, corrugated sheets, and paper-based containers.',
  Glass:     'Glass bottles, jars, broken glass, and other glass containers.',
  Metal:     'Aluminum cans, steel containers, tin cans, screws, bolts, and other metallic items.',
  Paper:     'Newspapers, magazines, office paper, books, and paper packaging.',
  Plastic:   'Plastic bottles, bags, containers, wrappers, and other plastic materials.',
  Trash:     'General non-recyclable waste that does not fit other categories.',
}

const DISPOSAL_TIPS: Record<string, string> = {
  Cardboard: 'Flatten boxes and place in the recycling bin. Keep dry — wet cardboard is not recyclable.',
  Glass:     'Rinse and place in a glass recycling bin. Do not mix with other recyclables.',
  Metal:     'Rinse cans and place in the recycling bin. Metal is infinitely recyclable.',
  Paper:     'Place in the paper recycling bin. Avoid soiled or greasy paper.',
  Plastic:   'Check the recycling number on the bottom. Rinse before placing in recycling bin.',
  Trash:     'Place in the general waste bin. Consider if any parts can be separated for recycling.',
}

const WASTE_ICONS: Record<string, React.ReactNode> = {
  Cardboard: <Package className="w-6 h-6" />,
  Glass:     <Layers className="w-6 h-6" />,
  Metal:     <Zap className="w-6 h-6" />,
  Paper:     <Leaf className="w-6 h-6" />,
  Plastic:   <Recycle className="w-6 h-6" />,
  Trash:     <AlertTriangle className="w-6 h-6" />,
}

const WASTE_COLORS: Record<string, { bg: string; text: string; bar: string; badge: string }> = {
  Cardboard: { bg: 'bg-amber-50',  text: 'text-amber-700',  bar: 'bg-amber-500',  badge: 'bg-amber-100 text-amber-800' },
  Glass:     { bg: 'bg-cyan-50',   text: 'text-cyan-700',   bar: 'bg-cyan-500',   badge: 'bg-cyan-100 text-cyan-800' },
  Metal:     { bg: 'bg-slate-50',  text: 'text-slate-700',  bar: 'bg-slate-500',  badge: 'bg-slate-100 text-slate-800' },
  Paper:     { bg: 'bg-yellow-50', text: 'text-yellow-700', bar: 'bg-yellow-500', badge: 'bg-yellow-100 text-yellow-800' },
  Plastic:   { bg: 'bg-blue-50',   text: 'text-blue-700',   bar: 'bg-blue-500',   badge: 'bg-blue-100 text-blue-800' },
  Trash:     { bg: 'bg-red-50',    text: 'text-red-700',    bar: 'bg-red-500',    badge: 'bg-red-100 text-red-800' },
}

const DEFAULT_COLOR = { bg: 'bg-gray-50', text: 'text-gray-700', bar: 'bg-gray-500', badge: 'bg-gray-100 text-gray-800' }

// ─── ImageNet → Waste category mapping ───────────────────────────────────────
// Each entry is [keyword_in_imagenet_label, waste_category, weight]
// Higher weight = stronger signal for that category
const LABEL_RULES: Array<[string, string, number]> = [
  // METAL — strong signals
  ['can', 'Metal', 3], ['tin', 'Metal', 3], ['steel', 'Metal', 3],
  ['iron', 'Metal', 3], ['metal', 'Metal', 3], ['aluminum', 'Metal', 3],
  ['screw', 'Metal', 4], ['bolt', 'Metal', 4], ['nail', 'Metal', 4],
  ['nut', 'Metal', 3], ['wrench', 'Metal', 4], ['hammer', 'Metal', 3],
  ['chain', 'Metal', 3], ['padlock', 'Metal', 3], ['lock', 'Metal', 2],
  ['knife', 'Metal', 2], ['fork', 'Metal', 2], ['spoon', 'Metal', 2],
  ['ladle', 'Metal', 2], ['spatula', 'Metal', 2], ['pan', 'Metal', 2],
  ['pot', 'Metal', 2], ['wok', 'Metal', 2], ['bucket', 'Metal', 2],
  ['drum', 'Metal', 3], ['barrel', 'Metal', 3], ['pipe', 'Metal', 2],
  ['wire', 'Metal', 3], ['cable', 'Metal', 2], ['spring', 'Metal', 3],
  ['gear', 'Metal', 3], ['cog', 'Metal', 3], ['hinge', 'Metal', 3],
  ['staple', 'Metal', 3], ['clip', 'Metal', 2], ['pin', 'Metal', 2],
  ['razor', 'Metal', 3], ['scissors', 'Metal', 3], ['shears', 'Metal', 3],
  ['crowbar', 'Metal', 4], ['chisel', 'Metal', 3], ['drill', 'Metal', 3],
  ['saw', 'Metal', 3], ['file', 'Metal', 3], ['pliers', 'Metal', 4],

  // PLASTIC — strong signals
  ['plastic', 'Plastic', 4], ['bottle', 'Plastic', 3],
  ['water_bottle', 'Plastic', 5], ['pop_bottle', 'Plastic', 5],
  ['milk_can', 'Plastic', 3], ['jug', 'Plastic', 2],
  ['bag', 'Plastic', 2], ['wrapper', 'Plastic', 3],
  ['container', 'Plastic', 2], ['tub', 'Plastic', 2],
  ['cup', 'Plastic', 2], ['straw', 'Plastic', 3],
  ['syringe', 'Plastic', 3], ['pill_bottle', 'Plastic', 4],
  ['shampoo', 'Plastic', 4], ['lotion', 'Plastic', 3],
  ['detergent', 'Plastic', 3], ['soap_dispenser', 'Plastic', 3],
  ['toothbrush', 'Plastic', 3], ['comb', 'Plastic', 3],
  ['ruler', 'Plastic', 2], ['pen', 'Plastic', 2],
  ['ballpoint', 'Plastic', 3], ['eraser', 'Plastic', 2],
  ['toy', 'Plastic', 2], ['lego', 'Plastic', 3],
  ['funnel', 'Plastic', 3], ['colander', 'Plastic', 2],
  ['strainer', 'Plastic', 2], ['pitcher', 'Plastic', 2],

  // GLASS — strong signals
  ['glass', 'Glass', 4], ['bottle', 'Glass', 1],
  ['wine_bottle', 'Glass', 5], ['beer_bottle', 'Glass', 5],
  ['whiskey_jug', 'Glass', 4], ['jar', 'Glass', 3],
  ['vase', 'Glass', 3], ['goblet', 'Glass', 4],
  ['wineglass', 'Glass', 5], ['beaker', 'Glass', 3],
  ['flask', 'Glass', 3], ['test_tube', 'Glass', 3],
  ['lens', 'Glass', 2], ['mirror', 'Glass', 2],
  ['window', 'Glass', 2], ['crystal', 'Glass', 3],
  ['perfume', 'Glass', 3], ['cocktail_shaker', 'Glass', 3],

  // PAPER — strong signals
  ['paper', 'Paper', 4], ['newspaper', 'Paper', 5],
  ['magazine', 'Paper', 4], ['book', 'Paper', 3],
  ['envelope', 'Paper', 4], ['letter', 'Paper', 3],
  ['document', 'Paper', 3], ['menu', 'Paper', 3],
  ['tissue', 'Paper', 3], ['towel', 'Paper', 2],
  ['napkin', 'Paper', 3], ['receipt', 'Paper', 3],
  ['ticket', 'Paper', 2], ['label', 'Paper', 2],
  ['comic', 'Paper', 3], ['notebook', 'Paper', 3],

  // CARDBOARD — strong signals
  ['cardboard', 'Cardboard', 5], ['box', 'Cardboard', 3],
  ['carton', 'Cardboard', 4], ['crate', 'Cardboard', 3],
  ['package', 'Cardboard', 2], ['packaging', 'Cardboard', 3],
  ['corrugated', 'Cardboard', 5], ['moving_van', 'Cardboard', 2],

  // TRASH — general waste
  ['trash', 'Trash', 4], ['garbage', 'Trash', 4],
  ['waste', 'Trash', 4], ['rubbish', 'Trash', 4],
  ['litter', 'Trash', 3], ['diaper', 'Trash', 4],
  ['cigarette', 'Trash', 3], ['butt', 'Trash', 2],
  ['food', 'Trash', 2], ['banana', 'Trash', 2],
  ['orange', 'Trash', 2], ['apple', 'Trash', 2],
  ['peel', 'Trash', 3], ['rotten', 'Trash', 3],
  ['organic', 'Trash', 3], ['compost', 'Trash', 3],
]

function mapToWasteCategory(
  predictions: Array<{ className: string; probability: number }>
): Record<string, number> {
  const scores: Record<string, number> = {
    Cardboard: 0, Glass: 0, Metal: 0, Paper: 0, Plastic: 0, Trash: 0,
  }

  for (const { className, probability } of predictions) {
    const lower = className.toLowerCase().replace(/[,\s]+/g, '_')

    for (const [keyword, category, weight] of LABEL_RULES) {
      if (lower.includes(keyword.toLowerCase())) {
        scores[category] += probability * weight
      }
    }
  }

  // Normalize to percentages
  const total = Object.values(scores).reduce((a, b) => a + b, 0)
  if (total === 0) {
    // No match — distribute evenly with slight Trash bias
    return { Cardboard: 5, Glass: 5, Metal: 5, Paper: 5, Plastic: 5, Trash: 75 }
  }

  const normalized: Record<string, number> = {}
  for (const [k, v] of Object.entries(scores)) {
    normalized[k] = Math.round((v / total) * 1000) / 10
  }
  return normalized
}

// ─── Component ────────────────────────────────────────────────────────────────

export default function WasteClassifierPage() {
  const [imageFile, setImageFile]       = useState<File | null>(null)
  const [imagePreview, setImagePreview] = useState<string | null>(null)
  const [classifying, setClassifying]   = useState(false)
  const [modelLoading, setModelLoading] = useState(true)
  const [modelError, setModelError]     = useState(false)
  const [result, setResult]             = useState<ClassificationResult | null>(null)
  const [dragOver, setDragOver]         = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const imgRef       = useRef<HTMLImageElement>(null)
  const modelRef     = useRef<any>(null)

  // Load MobileNet on mount
  useEffect(() => {
    let cancelled = false

    const load = async () => {
      setModelLoading(true)
      setModelError(false)
      try {
        // Load TF.js backend first
        await import('@tensorflow/tfjs')
        const mobilenet = await import('@tensorflow-models/mobilenet')
        const model = await mobilenet.load({ version: 2, alpha: 1.0 })
        if (!cancelled) {
          modelRef.current = model
          setModelLoading(false)
        }
      } catch (err) {
        console.error('MobileNet load error:', err)
        if (!cancelled) {
          setModelLoading(false)
          setModelError(true)
        }
      }
    }

    load()
    return () => { cancelled = true }
  }, [])

  const retryLoad = useCallback(() => {
    modelRef.current = null
    setModelError(false)
    setModelLoading(true)

    const load = async () => {
      try {
        await import('@tensorflow/tfjs')
        const mobilenet = await import('@tensorflow-models/mobilenet')
        const model = await mobilenet.load({ version: 2, alpha: 1.0 })
        modelRef.current = model
        setModelLoading(false)
      } catch {
        setModelLoading(false)
        setModelError(true)
      }
    }
    load()
  }, [])

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
    if (!imageFile || !imgRef.current) return

    if (!modelRef.current) {
      toast.error(modelError
        ? 'Model failed to load. Click "Retry" to try again.'
        : 'Model is still loading. Please wait a moment.')
      return
    }

    setClassifying(true)
    setResult(null)

    try {
      // Get top-20 ImageNet predictions from MobileNet
      const rawPredictions = await modelRef.current.classify(imgRef.current, 20)

      // Map ImageNet labels → waste categories
      const wasteScores = mapToWasteCategory(rawPredictions)

      // Find top category
      const topLabel = Object.entries(wasteScores).reduce(
        (best, [k, v]) => (v > best[1] ? [k, v] : best),
        ['Trash', 0]
      )[0]

      const topPct = wasteScores[topLabel]

      setResult({
        waste_type:         topLabel,
        confidence:         topPct / 100,
        confidence_percent: `${topPct.toFixed(1)}%`,
        description:        WASTE_DESCRIPTIONS[topLabel] ?? '',
        disposal_tip:       DISPOSAL_TIPS[topLabel] ?? '',
        all_predictions:    wasteScores,
      })

      toast.success('Classification complete!')
    } catch (err) {
      console.error('Classification error:', err)
      toast.error('Classification failed. Please try a different image.')
    } finally {
      setClassifying(false)
    }
  }

  // ── Render ─────────────────────────────────────────────────────────────────

  const colors = result
    ? (WASTE_COLORS[result.waste_type] ?? DEFAULT_COLOR)
    : DEFAULT_COLOR

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

        {/* Model status */}
        {modelLoading && (
          <div className="flex items-center gap-2 mt-2 text-xs text-blue-600 bg-blue-50 px-3 py-2 rounded-lg">
            <Loader2 className="w-3.5 h-3.5 animate-spin shrink-0" />
            Loading AI model (MobileNet V2)... This takes ~10 seconds on first load.
          </div>
        )}
        {modelError && (
          <div className="flex items-center justify-between mt-2 text-xs text-red-600 bg-red-50 px-3 py-2 rounded-lg">
            <span>Failed to load AI model. Check your internet connection.</span>
            <button
              onClick={retryLoad}
              className="ml-3 font-medium underline hover:no-underline shrink-0"
            >
              Retry
            </button>
          </div>
        )}
        {!modelLoading && !modelError && modelRef.current && (
          <div className="flex items-center gap-2 mt-2 text-xs text-green-600 bg-green-50 px-3 py-2 rounded-lg">
            <span className="w-2 h-2 rounded-full bg-green-500 shrink-0" />
            AI model ready — MobileNet V2 (ImageNet, 1000 classes)
          </div>
        )}
      </div>

      {/* Upload area */}
      <div className="bg-white rounded-xl shadow-sm p-6 space-y-4">
        <h2 className="font-semibold text-gray-700 text-sm">Upload Waste Image</h2>

        {imagePreview ? (
          <div className="relative rounded-xl overflow-hidden border border-gray-200">
            <img
              ref={imgRef}
              src={imagePreview}
              alt="Waste preview"
              crossOrigin="anonymous"
              className="w-full max-h-72 object-contain bg-gray-50"
            />
            <button
              onClick={handleRemove}
              className="absolute top-2 right-2 bg-red-500 hover:bg-red-600 text-white rounded-full p-1.5 transition shadow"
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

        <button
          onClick={classify}
          disabled={!imageFile || classifying || modelLoading || modelError}
          className="w-full bg-primary-600 hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed text-white py-2.5 rounded-lg text-sm font-medium transition flex items-center justify-center gap-2"
        >
          {classifying ? (
            <><Loader2 className="w-4 h-4 animate-spin" />Analyzing image...</>
          ) : modelLoading ? (
            <><Loader2 className="w-4 h-4 animate-spin" />Loading model...</>
          ) : (
            <><Recycle className="w-4 h-4" />Classify Waste</>
          )}
        </button>
      </div>

      {/* Result card */}
      {result && (
        <div className="rounded-xl shadow-sm overflow-hidden border border-gray-100">
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
            <div>
              <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-1">About this waste</p>
              <p className="text-sm text-gray-700">{result.description}</p>
            </div>

            <div className={`${colors.bg} rounded-lg p-4`}>
              <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-1">How to dispose</p>
              <p className={`text-sm font-medium ${colors.text}`}>{result.disposal_tip}</p>
            </div>

            <div>
              <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-3">Confidence breakdown</p>
              <div className="space-y-2">
                {sortedPredictions.map(([label, pct]) => {
                  const c = WASTE_COLORS[label] ?? DEFAULT_COLOR
                  return (
                    <div key={label} className="flex items-center gap-3">
                      <span className="text-xs text-gray-600 w-24 shrink-0">{label}</span>
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

            <button
              onClick={handleRemove}
              className="w-full border border-gray-300 text-gray-600 hover:bg-gray-50 py-2 rounded-lg text-sm transition"
            >
              Classify another image
            </button>
          </div>
        </div>
      )}

      {/* Info card */}
      {!result && (
        <div className="bg-white rounded-xl shadow-sm p-5 space-y-4">
          <h3 className="font-semibold text-gray-700 text-sm">Supported Waste Categories</h3>
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
          <div className="flex items-start gap-2 text-xs text-gray-400 bg-gray-50 rounded-lg p-3">
            <Info className="w-3.5 h-3.5 shrink-0 mt-0.5" />
            <span>
              Powered by MobileNet V2 (Google) — runs entirely in your browser.
              No images are uploaded to any server. Best results with clear, well-lit photos.
            </span>
          </div>
        </div>
      )}
    </div>
  )
}
