import { useState, useEffect, useRef } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { ChevronLeft, ChevronRight, AlertCircle, MapPin, Navigation, X, Package } from 'lucide-react'
import { complaintsApi } from '../../api/complaints'
import { marketplaceApi } from '../../api/marketplace'
import { useAuthStore } from '../../store/authStore'
import { StatusBadge, PriorityBadge } from '../../components/StatusBadge'
import type { Complaint, ComplaintStatus, MaterialType } from '../../types'
import { useForm } from 'react-hook-form'

const WORKER_STATUS_OPTIONS: ComplaintStatus[] = ['IN_PROGRESS', 'RESOLVED']

const MATERIAL_TYPES: MaterialType[] = [
  'PLASTIC', 'PAPER', 'METAL', 'GLASS', 'ELECTRONIC', 'RUBBER', 'TEXTILE', 'OTHER',
]

interface WasteListingForm {
  title: string
  materialType: MaterialType
  quantityKg: string
  pricePerKg: string
  description: string
}

export default function WorkerTasksPage() {
  const [page, setPage] = useState(0)
  const [mapModal, setMapModal] = useState<Complaint | null>(null)
  const [listingModal, setListingModal] = useState<Complaint | null>(null)
  const mapRef = useRef<HTMLDivElement>(null)
  const mapInstanceRef = useRef<any>(null)
  const queryClient = useQueryClient()
  const user = useAuthStore((s) => s.user)

  const { data, isLoading } = useQuery({
    queryKey: ['worker-complaints', page],
    queryFn: () => complaintsApi.getWorkerComplaints(page),
  })

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) =>
      complaintsApi.updateStatus(id, status),
    onSuccess: (updated) => {
      if (updated.status === 'RESOLVED') {
        toast.success('Task resolved! Citizen has been awarded 10 reward points 🎉')
      } else {
        toast.success('Status updated')
      }
      queryClient.invalidateQueries({ queryKey: ['worker-complaints'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard-stats'] })
    },
    onError: (err: any) =>
      toast.error(err.response?.data?.message ?? 'Failed to update status'),
  })

  const { register, handleSubmit, reset, formState: { errors } } = useForm<WasteListingForm>()

  const listingMutation = useMutation({
    mutationFn: (data: WasteListingForm) =>
      marketplaceApi.create({
        title: data.title,
        description: data.description || undefined,
        materialType: data.materialType,
        quantityKg: Number(data.quantityKg),
        pricePerKg: Number(data.pricePerKg),
        city: user?.fullName,
      }),
    onSuccess: () => {
      toast.success('Waste listed on marketplace for recyclers!')
      queryClient.invalidateQueries({ queryKey: ['marketplace'] })
      setListingModal(null)
      reset()
    },
    onError: (err: any) =>
      toast.error(err.response?.data?.message ?? 'Failed to create listing'),
  })

  // Initialize map when modal opens
  useEffect(() => {
    if (!mapModal) {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove()
        mapInstanceRef.current = null
      }
      return
    }

    const initMap = async () => {
      if (!mapRef.current || mapInstanceRef.current) return

      const L = await import('leaflet')
      await import('leaflet/dist/leaflet.css' as any)

      delete (L.Icon.Default.prototype as any)._getIconUrl
      L.Icon.Default.mergeOptions({
        iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
        iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
        shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
      })

      const lat = mapModal.latitude ?? 20.5937
      const lng = mapModal.longitude ?? 78.9629
      const hasCoords = !!mapModal.latitude && !!mapModal.longitude

      const map = L.map(mapRef.current).setView([lat, lng], hasCoords ? 15 : 5)
      mapInstanceRef.current = map

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors',
      }).addTo(map)

      if (hasCoords) {
        // Destination marker (complaint location)
        const destMarker = L.marker([lat, lng], {
          icon: L.divIcon({
            html: `<div style="background:#ef4444;width:28px;height:28px;border-radius:50% 50% 50% 0;transform:rotate(-45deg);border:3px solid white;box-shadow:0 2px 6px rgba(0,0,0,0.3)"></div>`,
            iconSize: [28, 28],
            iconAnchor: [14, 28],
            className: '',
          }),
        }).addTo(map)
        destMarker.bindPopup(`
          <div style="font-size:13px;min-width:160px">
            <strong>📍 Complaint Location</strong><br/>
            ${mapModal.address}<br/>
            <em style="color:#6b7280">${mapModal.title}</em>
          </div>
        `).openPopup()

        // Try to get user's current location and draw fastest route
        if (navigator.geolocation) {
          navigator.geolocation.getCurrentPosition(
            async (pos) => {
              const userLat = pos.coords.latitude
              const userLng = pos.coords.longitude

              // User location marker
              L.circleMarker([userLat, userLng], {
                radius: 10,
                fillColor: '#3b82f6',
                color: '#fff',
                weight: 3,
                fillOpacity: 1,
              }).addTo(map).bindPopup('<strong>📍 Your Location</strong>')

              // Fetch fastest route from OSRM (free, no API key needed)
              try {
                const res = await fetch(
                  `https://router.project-osrm.org/route/v1/driving/${userLng},${userLat};${lng},${lat}?overview=full&geometries=geojson`
                )
                const json = await res.json()

                if (json.routes && json.routes.length > 0) {
                  const route = json.routes[0]
                  const coords = route.geometry.coordinates.map(
                    ([lng, lat]: [number, number]) => [lat, lng] as [number, number]
                  )

                  L.polyline(coords, {
                    color: '#22c55e',
                    weight: 5,
                    opacity: 0.85,
                  }).addTo(map)

                  const distKm = (route.distance / 1000).toFixed(1)
                  const durMin = Math.round(route.duration / 60)

                  // Info box — inject directly into map container
                  const infoDiv = document.createElement('div')
                  infoDiv.style.cssText = 'position:absolute;bottom:20px;left:10px;z-index:1000;background:white;padding:8px 12px;border-radius:8px;box-shadow:0 2px 8px rgba(0,0,0,0.15);font-size:12px;pointer-events:none'
                  infoDiv.innerHTML = `<strong>🚗 Fastest Route</strong><br/>Distance: <strong>${distKm} km</strong><br/>Est. time: <strong>${durMin} min</strong>`
                  mapRef.current?.appendChild(infoDiv)

                  // Fit map to show full route
                  const bounds = L.latLngBounds([[userLat, userLng], [lat, lng]])
                  map.fitBounds(bounds, { padding: [40, 40] })
                }
              } catch {
                // OSRM failed — just show straight line
                L.polyline([[userLat, userLng], [lat, lng]], {
                  color: '#f59e0b',
                  weight: 3,
                  dashArray: '8 6',
                }).addTo(map)
                map.fitBounds(L.latLngBounds([[userLat, userLng], [lat, lng]]), { padding: [40, 40] })
              }
            },
            () => {
              // Geolocation denied — just show complaint marker
            }
          )
        }
      }
    }

    // Small delay to ensure modal DOM is rendered
    setTimeout(initMap, 100)

    return () => {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove()
        mapInstanceRef.current = null
      }
    }
  }, [mapModal])

  const complaints = data?.content ?? []

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-gray-800">My Tasks</h1>
        <span className="text-sm text-gray-500">{data?.totalElements ?? 0} total tasks</span>
      </div>

      <div className="bg-white rounded-xl shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="flex items-center justify-center h-48">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
          </div>
        ) : complaints.length === 0 ? (
          <div className="text-center py-16 text-gray-400">
            <AlertCircle className="w-12 h-12 mx-auto mb-3 opacity-25" />
            <p className="font-medium">No tasks assigned to you</p>
            <p className="text-sm mt-1">Tasks will appear here once admin assigns complaints to you</p>
          </div>
        ) : (
          <ul className="divide-y divide-gray-100">
            {complaints.map((c: Complaint) => (
              <li key={c.id} className="px-5 py-4 hover:bg-gray-50 transition">
                <div className="flex items-start justify-between gap-4">
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2 mb-1 flex-wrap">
                      <span className="text-xs text-gray-400 font-mono">#{c.id}</span>
                      <PriorityBadge priority={c.priority} />
                      {c.wasteType && (
                        <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full">
                          {c.wasteType}
                        </span>
                      )}
                    </div>
                    <p className="font-medium text-gray-800">{c.title}</p>
                    {c.description && (
                      <p className="text-sm text-gray-500 mt-0.5 line-clamp-2">{c.description}</p>
                    )}
                    <p className="text-xs text-gray-400 mt-1">📍 {c.address}</p>
                    <p className="text-xs text-gray-400">Reported by: {c.citizenName}</p>

                    {/* Action buttons */}
                    <div className="flex items-center gap-3 mt-2 flex-wrap">
                      {/* Navigate button */}
                      <button
                        onClick={() => setMapModal(c)}
                        className="flex items-center gap-1.5 text-xs text-blue-600 hover:text-blue-800 font-medium bg-blue-50 hover:bg-blue-100 px-2.5 py-1 rounded-lg transition"
                      >
                        <Navigation className="w-3.5 h-3.5" />
                        Get Route
                      </button>

                      {/* Post to marketplace (only for resolved tasks) */}
                      {c.status === 'RESOLVED' && (
                        <button
                          onClick={() => {
                            setListingModal(c)
                            reset({
                              title: `Collected ${c.wasteType ?? 'Waste'} — ${c.address.slice(0, 40)}`,
                              materialType: mapWasteToMaterial(c.wasteType),
                              quantityKg: '',
                              pricePerKg: '',
                              description: `Collected from complaint #${c.id}: ${c.title}`,
                            })
                          }}
                          className="flex items-center gap-1.5 text-xs text-green-600 hover:text-green-800 font-medium bg-green-50 hover:bg-green-100 px-2.5 py-1 rounded-lg transition"
                        >
                          <Package className="w-3.5 h-3.5" />
                          List on Marketplace
                        </button>
                      )}
                    </div>
                  </div>

                  <div className="shrink-0 flex flex-col items-end gap-2">
                    <StatusBadge status={c.status} />
                    {!['RESOLVED', 'REJECTED'].includes(c.status) && (
                      <select
                        defaultValue=""
                        onChange={(e) => {
                          if (e.target.value) {
                            statusMutation.mutate({ id: c.id, status: e.target.value })
                            e.target.value = ''
                          }
                        }}
                        className="border border-gray-200 rounded px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-primary-500 bg-white"
                      >
                        <option value="" disabled>Update status...</option>
                        {WORKER_STATUS_OPTIONS.map((s) => (
                          <option key={s} value={s}>{s.replace('_', ' ')}</option>
                        ))}
                      </select>
                    )}
                    <span className="text-xs text-gray-400">
                      {new Date(c.createdAt).toLocaleDateString('en-IN')}
                    </span>
                    {c.status === 'RESOLVED' && c.resolvedAt && (
                      <span className="text-xs text-green-600">
                        ✓ {new Date(c.resolvedAt).toLocaleDateString('en-IN')}
                      </span>
                    )}
                  </div>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between text-sm text-gray-600">
          <span>Page {page + 1} of {data.totalPages}</span>
          <div className="flex gap-2">
            <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}
              className="p-1.5 rounded-lg border border-gray-200 disabled:opacity-40 hover:bg-gray-50">
              <ChevronLeft className="w-4 h-4" />
            </button>
            <button disabled={page >= data.totalPages - 1} onClick={() => setPage((p) => p + 1)}
              className="p-1.5 rounded-lg border border-gray-200 disabled:opacity-40 hover:bg-gray-50">
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* Map Modal — fastest route */}
      {mapModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-2xl overflow-hidden">
            <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
              <div>
                <h2 className="font-semibold text-gray-800">Navigate to Complaint</h2>
                <p className="text-xs text-gray-500 mt-0.5 truncate max-w-md">{mapModal.address}</p>
              </div>
              <button onClick={() => setMapModal(null)}
                className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-500">
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Complaint info */}
            <div className="px-5 py-3 bg-gray-50 border-b border-gray-100 flex items-center gap-3 text-sm">
              <MapPin className="w-4 h-4 text-red-500 shrink-0" />
              <div className="min-w-0">
                <span className="font-medium text-gray-800">{mapModal.title}</span>
                {mapModal.latitude && mapModal.longitude ? (
                  <span className="text-gray-500 ml-2 text-xs">
                    ({mapModal.latitude.toFixed(4)}, {mapModal.longitude.toFixed(4)})
                  </span>
                ) : (
                  <span className="text-amber-600 ml-2 text-xs">No GPS coordinates — showing address only</span>
                )}
              </div>
            </div>

            {/* Map */}
            <div ref={mapRef} className="h-96 w-full" />

            <div className="px-5 py-3 bg-gray-50 border-t border-gray-100 text-xs text-gray-500 flex items-center gap-2">
              <Navigation className="w-3.5 h-3.5 text-green-600" />
              Green line = fastest driving route via OSRM · Blue dot = your location · Red pin = complaint
            </div>
          </div>
        </div>
      )}

      {/* Marketplace Listing Modal */}
      {listingModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-md">
            <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
              <h2 className="font-semibold text-gray-800">List Collected Waste on Marketplace</h2>
              <button onClick={() => { setListingModal(null); reset() }}
                className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-500">
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleSubmit((d) => listingMutation.mutate(d))} className="p-5 space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Title *</label>
                <input {...register('title', { required: 'Required' })}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
                {errors.title && <p className="text-red-500 text-xs mt-1">{errors.title.message}</p>}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Material Type *</label>
                <select {...register('materialType', { required: 'Required' })}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500">
                  <option value="">— Select —</option>
                  {MATERIAL_TYPES.map((m) => <option key={m} value={m}>{m}</option>)}
                </select>
                {errors.materialType && <p className="text-red-500 text-xs mt-1">{errors.materialType.message}</p>}
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Quantity (kg) *</label>
                  <input type="number" step="0.1" {...register('quantityKg', { required: 'Required', min: { value: 0.1, message: '> 0' } })}
                    placeholder="e.g. 25"
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
                  {errors.quantityKg && <p className="text-red-500 text-xs mt-1">{errors.quantityKg.message}</p>}
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Price/kg (₹) *</label>
                  <input type="number" step="0.01" {...register('pricePerKg', { required: 'Required', min: { value: 0.01, message: '> 0' } })}
                    placeholder="e.g. 8"
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
                  {errors.pricePerKg && <p className="text-red-500 text-xs mt-1">{errors.pricePerKg.message}</p>}
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                <textarea {...register('description')} rows={2}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none" />
              </div>

              <div className="flex gap-3 pt-1">
                <button type="button" onClick={() => { setListingModal(null); reset() }}
                  className="flex-1 border border-gray-300 text-gray-600 py-2.5 rounded-lg text-sm hover:bg-gray-50">
                  Cancel
                </button>
                <button type="submit" disabled={listingMutation.isPending}
                  className="flex-1 bg-primary-600 text-white py-2.5 rounded-lg text-sm font-medium hover:bg-primary-700 disabled:opacity-60">
                  {listingMutation.isPending ? 'Posting...' : 'Post to Marketplace'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

// Map waste type to closest marketplace material
function mapWasteToMaterial(wasteType?: string): MaterialType {
  const map: Record<string, MaterialType> = {
    PLASTIC: 'PLASTIC',
    ELECTRONIC: 'ELECTRONIC',
    HAZARDOUS: 'OTHER',
    CONSTRUCTION: 'OTHER',
    ORGANIC: 'OTHER',
    MIXED: 'OTHER',
  }
  return (wasteType && map[wasteType]) ? map[wasteType] : 'OTHER'
}
