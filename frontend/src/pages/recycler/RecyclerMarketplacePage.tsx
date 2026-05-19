import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import toast from 'react-hot-toast'
import {
  Plus, X, ChevronLeft, ChevronRight, Search,
  Trash2, CheckCircle, Package
} from 'lucide-react'
import { marketplaceApi, type CreateListingPayload } from '../../api/marketplace'
import { useAuthStore } from '../../store/authStore'
import type { MarketplaceListing, MaterialType, ListingStatus } from '../../types'

const MATERIAL_TYPES: MaterialType[] = [
  'PLASTIC', 'PAPER', 'METAL', 'GLASS', 'ELECTRONIC', 'RUBBER', 'TEXTILE', 'OTHER',
]

const MATERIAL_COLORS: Record<MaterialType, string> = {
  PLASTIC:    'bg-blue-100 text-blue-700',
  PAPER:      'bg-yellow-100 text-yellow-700',
  METAL:      'bg-gray-100 text-gray-700',
  GLASS:      'bg-cyan-100 text-cyan-700',
  ELECTRONIC: 'bg-purple-100 text-purple-700',
  RUBBER:     'bg-orange-100 text-orange-700',
  TEXTILE:    'bg-pink-100 text-pink-700',
  OTHER:      'bg-green-100 text-green-700',
}

const STATUS_COLORS: Record<ListingStatus, string> = {
  ACTIVE:    'bg-green-100 text-green-700',
  SOLD:      'bg-gray-100 text-gray-500',
  EXPIRED:   'bg-red-100 text-red-600',
  CANCELLED: 'bg-red-100 text-red-600',
}

interface FormData {
  title: string
  description: string
  materialType: MaterialType
  quantityKg: string
  pricePerKg: string
  city: string
  pincode: string
}

export default function RecyclerMarketplacePage() {
  const [searchParams] = useSearchParams()
  const defaultTab = searchParams.get('tab') === 'my' ? 'my' : 'browse'

  const [tab, setTab] = useState<'browse' | 'my'>(defaultTab)
  const [page, setPage] = useState(0)
  const [myPage, setMyPage] = useState(0)
  const [filterMaterial, setFilterMaterial] = useState<MaterialType | ''>('')
  const [search, setSearch] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [editListing, setEditListing] = useState<MarketplaceListing | null>(null)

  const user = useAuthStore((s) => s.user)
  const queryClient = useQueryClient()

  const { register, handleSubmit, reset, setValue, formState: { errors } } = useForm<FormData>()

  // Browse all active listings
  const { data: browseData, isLoading: browseLoading } = useQuery({
    queryKey: ['marketplace', page, filterMaterial],
    queryFn: () => marketplaceApi.getAll(page, 12, filterMaterial || undefined),
    enabled: tab === 'browse',
  })

  // My listings
  const { data: myData, isLoading: myLoading } = useQuery({
    queryKey: ['my-listings', myPage],
    queryFn: () => marketplaceApi.getMy(myPage),
    enabled: tab === 'my',
  })

  const createMutation = useMutation({
    mutationFn: (data: CreateListingPayload) =>
      editListing ? marketplaceApi.update(editListing.id, data) : marketplaceApi.create(data),
    onSuccess: () => {
      toast.success(editListing ? 'Listing updated!' : 'Listing created!')
      queryClient.invalidateQueries({ queryKey: ['marketplace'] })
      queryClient.invalidateQueries({ queryKey: ['my-listings'] })
      closeForm()
    },
    onError: (err: any) =>
      toast.error(err.response?.data?.message ?? 'Failed to save listing'),
  })

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: ListingStatus }) =>
      marketplaceApi.updateStatus(id, status),
    onSuccess: () => {
      toast.success('Listing updated')
      queryClient.invalidateQueries({ queryKey: ['my-listings'] })
      queryClient.invalidateQueries({ queryKey: ['marketplace'] })
    },
    onError: () => toast.error('Failed to update listing'),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => marketplaceApi.delete(id),
    onSuccess: () => {
      toast.success('Listing deleted')
      queryClient.invalidateQueries({ queryKey: ['my-listings'] })
      queryClient.invalidateQueries({ queryKey: ['marketplace'] })
    },
    onError: () => toast.error('Failed to delete listing'),
  })

  const onSubmit = (data: FormData) => {
    createMutation.mutate({
      title: data.title,
      description: data.description || undefined,
      materialType: data.materialType,
      quantityKg: Number(data.quantityKg),
      pricePerKg: Number(data.pricePerKg),
      city: data.city || undefined,
      pincode: data.pincode || undefined,
    })
  }

  const openEdit = (listing: MarketplaceListing) => {
    setEditListing(listing)
    setValue('title', listing.title)
    setValue('description', listing.description ?? '')
    setValue('materialType', listing.materialType)
    setValue('quantityKg', String(listing.quantityKg))
    setValue('pricePerKg', String(listing.pricePerKg))
    setValue('city', listing.city ?? '')
    setValue('pincode', listing.pincode ?? '')
    setShowForm(true)
  }

  const closeForm = () => {
    setShowForm(false)
    setEditListing(null)
    reset()
  }

  // Client-side search filter for browse tab
  const browseListings = (browseData?.content ?? []).filter((l) =>
    !search ||
    l.title.toLowerCase().includes(search.toLowerCase()) ||
    l.sellerName.toLowerCase().includes(search.toLowerCase()) ||
    (l.city ?? '').toLowerCase().includes(search.toLowerCase())
  )

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-gray-800">Marketplace</h1>
        <button
          onClick={() => { setShowForm(true); setEditListing(null); reset() }}
          className="flex items-center gap-2 bg-primary-600 hover:bg-primary-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition"
        >
          <Plus className="w-4 h-4" />
          Post Listing
        </button>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-gray-200">
        {(['browse', 'my'] as const).map((t) => (
          <button
            key={t}
            onClick={() => { setTab(t); setPage(0); setMyPage(0) }}
            className={`px-5 py-2.5 text-sm font-medium border-b-2 transition capitalize ${
              tab === t
                ? 'border-primary-600 text-primary-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            {t === 'browse' ? 'Browse All' : 'My Listings'}
          </button>
        ))}
      </div>

      {/* Browse tab */}
      {tab === 'browse' && (
        <>
          {/* Filters */}
          <div className="flex flex-wrap gap-3">
            <div className="relative flex-1 min-w-[200px] max-w-sm">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search listings..."
                className="w-full pl-9 pr-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
            </div>
            <select
              value={filterMaterial}
              onChange={(e) => { setFilterMaterial(e.target.value as MaterialType | ''); setPage(0) }}
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            >
              <option value="">All Materials</option>
              {MATERIAL_TYPES.map((m) => (
                <option key={m} value={m}>{m}</option>
              ))}
            </select>
          </div>

          {browseLoading ? (
            <div className="flex items-center justify-center h-48">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
            </div>
          ) : browseListings.length === 0 ? (
            <div className="bg-white rounded-xl shadow-sm p-12 text-center text-gray-400">
              <Package className="w-12 h-12 mx-auto mb-3 opacity-25" />
              <p className="font-medium">No listings found</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {browseListings.map((listing) => (
                <BrowseCard key={listing.id} listing={listing} />
              ))}
            </div>
          )}

          {browseData && browseData.totalPages > 1 && (
            <Pagination
              page={page}
              totalPages={browseData.totalPages}
              onPrev={() => setPage((p) => p - 1)}
              onNext={() => setPage((p) => p + 1)}
            />
          )}
        </>
      )}

      {/* My listings tab */}
      {tab === 'my' && (
        <>
          {myLoading ? (
            <div className="flex items-center justify-center h-48">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
            </div>
          ) : (myData?.content ?? []).length === 0 ? (
            <div className="bg-white rounded-xl shadow-sm p-12 text-center text-gray-400">
              <Package className="w-12 h-12 mx-auto mb-3 opacity-25" />
              <p className="font-medium">You haven't posted any listings yet</p>
              <button
                onClick={() => { setShowForm(true); reset() }}
                className="mt-4 text-sm text-primary-600 hover:underline"
              >
                Post your first listing
              </button>
            </div>
          ) : (
            <div className="bg-white rounded-xl shadow-sm overflow-hidden">
              <table className="w-full text-sm">
                <thead className="bg-gray-50 border-b border-gray-200">
                  <tr>
                    <th className="text-left px-4 py-3 font-medium text-gray-600">Title</th>
                    <th className="text-left px-4 py-3 font-medium text-gray-600">Material</th>
                    <th className="text-left px-4 py-3 font-medium text-gray-600">Qty (kg)</th>
                    <th className="text-left px-4 py-3 font-medium text-gray-600">Price/kg</th>
                    <th className="text-left px-4 py-3 font-medium text-gray-600">Total</th>
                    <th className="text-left px-4 py-3 font-medium text-gray-600">Status</th>
                    <th className="text-left px-4 py-3 font-medium text-gray-600">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {(myData?.content ?? []).map((listing) => (
                    <tr key={listing.id} className="hover:bg-gray-50 transition">
                      <td className="px-4 py-3 font-medium text-gray-800 max-w-[180px]">
                        <span className="truncate block">{listing.title}</span>
                      </td>
                      <td className="px-4 py-3">
                        <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${MATERIAL_COLORS[listing.materialType]}`}>
                          {listing.materialType}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-gray-600">{listing.quantityKg}</td>
                      <td className="px-4 py-3 text-gray-600">₹{listing.pricePerKg}</td>
                      <td className="px-4 py-3 font-medium text-green-700">₹{listing.totalPrice}</td>
                      <td className="px-4 py-3">
                        <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${STATUS_COLORS[listing.status]}`}>
                          {listing.status}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2">
                          {listing.status === 'ACTIVE' && (
                            <>
                              <button
                                onClick={() => openEdit(listing)}
                                className="text-xs text-blue-600 hover:text-blue-800 font-medium"
                              >
                                Edit
                              </button>
                              <button
                                onClick={() => statusMutation.mutate({ id: listing.id, status: 'SOLD' })}
                                disabled={statusMutation.isPending}
                                className="flex items-center gap-0.5 text-xs text-green-600 hover:text-green-800 font-medium"
                              >
                                <CheckCircle className="w-3.5 h-3.5" />
                                Sold
                              </button>
                            </>
                          )}
                          <button
                            onClick={() => {
                              if (confirm('Delete this listing?')) deleteMutation.mutate(listing.id)
                            }}
                            disabled={deleteMutation.isPending}
                            className="text-red-400 hover:text-red-600 transition"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {myData && myData.totalPages > 1 && (
            <Pagination
              page={myPage}
              totalPages={myData.totalPages}
              onPrev={() => setMyPage((p) => p - 1)}
              onNext={() => setMyPage((p) => p + 1)}
            />
          )}
        </>
      )}

      {/* Create / Edit Modal */}
      {showForm && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-xl p-6 w-full max-w-lg max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-5">
              <h2 className="text-base font-semibold text-gray-800">
                {editListing ? 'Edit Listing' : 'Post New Listing'}
              </h2>
              <button onClick={closeForm} className="p-1 rounded-lg hover:bg-gray-100 text-gray-500">
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
              {/* Title */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Title <span className="text-red-500">*</span>
                </label>
                <input
                  {...register('title', { required: 'Title is required' })}
                  placeholder="e.g. PET Plastic Bottles — 50kg"
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                />
                {errors.title && <p className="text-red-500 text-xs mt-1">{errors.title.message}</p>}
              </div>

              {/* Description */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                <textarea
                  {...register('description')}
                  rows={2}
                  placeholder="Describe the material condition, grade, etc."
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none"
                />
              </div>

              {/* Material type */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Material Type <span className="text-red-500">*</span>
                </label>
                <select
                  {...register('materialType', { required: 'Material type is required' })}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                >
                  <option value="">— Select material —</option>
                  {MATERIAL_TYPES.map((m) => (
                    <option key={m} value={m}>{m}</option>
                  ))}
                </select>
                {errors.materialType && <p className="text-red-500 text-xs mt-1">{errors.materialType.message}</p>}
              </div>

              {/* Quantity + Price */}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Quantity (kg) <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="number"
                    step="0.1"
                    {...register('quantityKg', { required: 'Required', min: { value: 0.1, message: 'Must be > 0' } })}
                    placeholder="e.g. 50"
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                  {errors.quantityKg && <p className="text-red-500 text-xs mt-1">{errors.quantityKg.message}</p>}
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Price per kg (₹) <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    {...register('pricePerKg', { required: 'Required', min: { value: 0.01, message: 'Must be > 0' } })}
                    placeholder="e.g. 12.50"
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                  {errors.pricePerKg && <p className="text-red-500 text-xs mt-1">{errors.pricePerKg.message}</p>}
                </div>
              </div>

              {/* City + Pincode */}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">City</label>
                  <input
                    {...register('city')}
                    placeholder="e.g. Bhopal"
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Pincode</label>
                  <input
                    {...register('pincode')}
                    placeholder="e.g. 462001"
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                </div>
              </div>

              <div className="flex gap-3 pt-2">
                <button
                  type="button"
                  onClick={closeForm}
                  className="flex-1 border border-gray-300 text-gray-600 py-2.5 rounded-lg text-sm hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={createMutation.isPending}
                  className="flex-1 bg-primary-600 text-white py-2.5 rounded-lg text-sm font-medium hover:bg-primary-700 disabled:opacity-60"
                >
                  {createMutation.isPending
                    ? 'Saving...'
                    : editListing ? 'Update Listing' : 'Post Listing'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

// ── Browse Card ───────────────────────────────────────────

function BrowseCard({ listing }: { listing: MarketplaceListing }) {
  return (
    <div className="bg-white border border-gray-100 rounded-xl p-4 hover:shadow-md transition flex flex-col gap-3">
      {listing.imageUrl && (
        <img
          src={listing.imageUrl}
          alt={listing.title}
          className="w-full h-36 object-cover rounded-lg"
        />
      )}
      <div className="flex items-start justify-between gap-2">
        <p className="font-semibold text-gray-800 text-sm leading-snug">{listing.title}</p>
        <span className={`text-xs px-2 py-0.5 rounded-full font-medium shrink-0 ${MATERIAL_COLORS[listing.materialType]}`}>
          {listing.materialType}
        </span>
      </div>
      {listing.description && (
        <p className="text-xs text-gray-500 line-clamp-2">{listing.description}</p>
      )}
      <div className="flex items-center justify-between text-sm">
        <span className="text-gray-500">{listing.quantityKg} kg available</span>
        <span className="font-bold text-green-700">₹{listing.pricePerKg}/kg</span>
      </div>
      <div className="flex items-center justify-between text-xs text-gray-400 border-t border-gray-50 pt-2">
        <span>Total: <strong className="text-gray-600">₹{listing.totalPrice}</strong></span>
        {listing.city && <span>📍 {listing.city}</span>}
      </div>
      <p className="text-xs text-gray-400">Seller: {listing.sellerName}</p>
    </div>
  )
}

// ── Pagination ────────────────────────────────────────────

function Pagination({
  page, totalPages, onPrev, onNext,
}: { page: number; totalPages: number; onPrev: () => void; onNext: () => void }) {
  return (
    <div className="flex items-center justify-between text-sm text-gray-600">
      <span>Page {page + 1} of {totalPages}</span>
      <div className="flex gap-2">
        <button
          disabled={page === 0}
          onClick={onPrev}
          className="p-1.5 rounded-lg border border-gray-200 disabled:opacity-40 hover:bg-gray-50"
        >
          <ChevronLeft className="w-4 h-4" />
        </button>
        <button
          disabled={page >= totalPages - 1}
          onClick={onNext}
          className="p-1.5 rounded-lg border border-gray-200 disabled:opacity-40 hover:bg-gray-50"
        >
          <ChevronRight className="w-4 h-4" />
        </button>
      </div>
    </div>
  )
}
