import { useEffect, useRef, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { MapPin, Calendar, Clock, Route, CheckCircle } from 'lucide-react'
import { routesApi } from '../../api/routes'
import { useAuthStore } from '../../store/authStore'
import { RouteStatusBadge } from '../../components/StatusBadge'
import type { CollectionRoute, RouteStatus } from '../../types'

const STATUS_TRANSITIONS: Record<RouteStatus, RouteStatus | null> = {
  SCHEDULED: 'IN_PROGRESS',
  IN_PROGRESS: 'COMPLETED',
  COMPLETED: null,
  CANCELLED: null,
}

const STATUS_LABELS: Record<RouteStatus, string> = {
  SCHEDULED: 'Start Route',
  IN_PROGRESS: 'Mark Completed',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
}

export default function WorkerRoutesPage() {
  const user = useAuthStore((s) => s.user)
  const queryClient = useQueryClient()
  const [selectedRoute, setSelectedRoute] = useState<CollectionRoute | null>(null)
  const mapRef = useRef<HTMLDivElement>(null)
  const mapInstanceRef = useRef<any>(null)

  const { data: routes = [], isLoading } = useQuery<CollectionRoute[]>({
    queryKey: ['worker-routes', user?.userId],
    queryFn: () => routesApi.getByWorker(user!.userId),
    enabled: !!user,
  })

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: RouteStatus }) =>
      routesApi.updateStatus(id, status),
    onSuccess: (updated) => {
      toast.success(`Route marked as ${updated.status.replace('_', ' ')}`)
      queryClient.invalidateQueries({ queryKey: ['worker-routes'] })
      setSelectedRoute(updated)
    },
    onError: () => toast.error('Failed to update route status'),
  })

  // Initialize Leaflet map
  useEffect(() => {
    let map: any

    const initMap = async () => {
      const L = (await import('leaflet')).default
      await import('leaflet/dist/leaflet.css')

      delete (L.Icon.Default.prototype as any)._getIconUrl
      L.Icon.Default.mergeOptions({
        iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
        iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
        shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
      })

      if (!mapRef.current || mapInstanceRef.current) return

      map = L.map(mapRef.current).setView([20.5937, 78.9629], 5)
      mapInstanceRef.current = map

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors',
      }).addTo(map)
    }

    initMap()

    return () => {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove()
        mapInstanceRef.current = null
      }
    }
  }, [])

  // Plot selected route on map
  useEffect(() => {
    if (!mapInstanceRef.current || !selectedRoute) return

    const plotRoute = async () => {
      const L = (await import('leaflet')).default
      const map = mapInstanceRef.current

      // Clear existing layers except tile layer
      map.eachLayer((layer: any) => {
        if (layer._url === undefined) map.removeLayer(layer)
      })

      // If route has stops JSON, parse and plot them
      if ((selectedRoute as any).stops) {
        try {
          const stops = JSON.parse((selectedRoute as any).stops)
          if (Array.isArray(stops) && stops.length > 0) {
            const latLngs: [number, number][] = stops.map((s: any) => [s.lat, s.lng])
            L.polyline(latLngs, { color: '#22c55e', weight: 4, opacity: 0.8 }).addTo(map)

            stops.forEach((stop: any, idx: number) => {
              const marker = L.circleMarker([stop.lat, stop.lng], {
                radius: 9,
                fillColor: '#22c55e',
                color: '#fff',
                weight: 2,
                fillOpacity: 0.9,
              }).addTo(map)
              marker.bindPopup(`<strong>Stop ${idx + 1}</strong><br/>${stop.address ?? ''}`)
            })

            map.fitBounds(L.latLngBounds(latLngs), { padding: [30, 30] })
          }
        } catch {
          // stops not parseable — show area marker if pincode exists
        }
      }
    }

    plotRoute()
  }, [selectedRoute])

  const activeRoutes = routes.filter((r) => r.status === 'SCHEDULED' || r.status === 'IN_PROGRESS')
  const completedRoutes = routes.filter((r) => r.status === 'COMPLETED' || r.status === 'CANCELLED')

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
      </div>
    )
  }

  return (
    <div className="space-y-5">
      <h1 className="text-xl font-bold text-gray-800">My Routes</h1>

      {routes.length === 0 ? (
        <div className="bg-white rounded-xl shadow-sm p-12 text-center text-gray-400">
          <Route className="w-12 h-12 mx-auto mb-3 opacity-25" />
          <p className="font-medium">No routes assigned yet</p>
          <p className="text-sm mt-1">Your collection routes will appear here once assigned by admin</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
          {/* Route list */}
          <div className="space-y-3">
            {/* Active routes */}
            {activeRoutes.length > 0 && (
              <>
                <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide">Active</h2>
                {activeRoutes.map((route) => (
                  <RouteCard
                    key={route.id}
                    route={route}
                    isSelected={selectedRoute?.id === route.id}
                    onSelect={() => setSelectedRoute(route)}
                    onStatusUpdate={(id, status) => statusMutation.mutate({ id, status })}
                    isPending={statusMutation.isPending}
                  />
                ))}
              </>
            )}

            {/* Completed routes */}
            {completedRoutes.length > 0 && (
              <>
                <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mt-4">History</h2>
                {completedRoutes.map((route) => (
                  <RouteCard
                    key={route.id}
                    route={route}
                    isSelected={selectedRoute?.id === route.id}
                    onSelect={() => setSelectedRoute(route)}
                    onStatusUpdate={(id, status) => statusMutation.mutate({ id, status })}
                    isPending={statusMutation.isPending}
                  />
                ))}
              </>
            )}
          </div>

          {/* Map */}
          <div className="bg-white rounded-xl shadow-sm overflow-hidden">
            <div className="px-4 py-3 border-b border-gray-100">
              <h2 className="font-semibold text-gray-700 text-sm">
                {selectedRoute ? `Map — ${selectedRoute.routeName}` : 'Select a route to view on map'}
              </h2>
            </div>
            <div ref={mapRef} className="h-[420px] w-full" />
          </div>
        </div>
      )}
    </div>
  )
}

// ── Route Card Component ──────────────────────────────────

interface RouteCardProps {
  route: CollectionRoute
  isSelected: boolean
  onSelect: () => void
  onStatusUpdate: (id: number, status: RouteStatus) => void
  isPending: boolean
}

function RouteCard({ route, isSelected, onSelect, onStatusUpdate, isPending }: RouteCardProps) {
  const nextStatus = STATUS_TRANSITIONS[route.status]

  return (
    <div
      onClick={onSelect}
      className={`bg-white rounded-xl shadow-sm p-4 cursor-pointer transition border-2 ${
        isSelected ? 'border-primary-500' : 'border-transparent hover:border-gray-200'
      }`}
    >
      <div className="flex items-start justify-between mb-2">
        <div className="min-w-0">
          <p className="font-semibold text-gray-800 truncate">{route.routeName}</p>
          {route.areaName && <p className="text-xs text-gray-500 mt-0.5">{route.areaName}</p>}
        </div>
        <RouteStatusBadge status={route.status} />
      </div>

      <div className="grid grid-cols-3 gap-2 text-xs text-gray-500 mb-3">
        <div className="flex items-center gap-1">
          <Calendar className="w-3.5 h-3.5 text-gray-400" />
          {route.scheduledDate}
        </div>
        {route.estimatedDistanceKm != null && (
          <div className="flex items-center gap-1">
            <Route className="w-3.5 h-3.5 text-gray-400" />
            {route.estimatedDistanceKm} km
          </div>
        )}
        {route.estimatedDurationMin != null && (
          <div className="flex items-center gap-1">
            <Clock className="w-3.5 h-3.5 text-gray-400" />
            {route.estimatedDurationMin} min
          </div>
        )}
      </div>

      {nextStatus && (
        <button
          onClick={(e) => {
            e.stopPropagation()
            onStatusUpdate(route.id, nextStatus)
          }}
          disabled={isPending}
          className={`w-full flex items-center justify-center gap-1.5 py-1.5 rounded-lg text-xs font-medium transition disabled:opacity-50 ${
            nextStatus === 'IN_PROGRESS'
              ? 'bg-amber-50 text-amber-700 hover:bg-amber-100'
              : 'bg-green-50 text-green-700 hover:bg-green-100'
          }`}
        >
          <CheckCircle className="w-3.5 h-3.5" />
          {STATUS_LABELS[route.status]}
        </button>
      )}
    </div>
  )
}
