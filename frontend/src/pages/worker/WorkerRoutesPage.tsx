import { useEffect, useRef } from 'react'
import { MapPin, Calendar, Clock, Route } from 'lucide-react'

// Sample static route data — replace with real API call once route endpoints are added
const SAMPLE_ROUTES = [
  {
    id: 1,
    routeName: 'Sector 12 Morning Route',
    areaName: 'Sector 12',
    scheduledDate: '2026-05-19',
    estimatedDistanceKm: 8.4,
    estimatedDurationMin: 90,
    status: 'SCHEDULED',
    stops: [
      { lat: 28.6139, lng: 77.209, address: 'Stop 1 — Main Market' },
      { lat: 28.617, lng: 77.212, address: 'Stop 2 — Bus Stand' },
      { lat: 28.62, lng: 77.215, address: 'Stop 3 — School Road' },
    ],
  },
  {
    id: 2,
    routeName: 'Sector 14 Afternoon Route',
    areaName: 'Sector 14',
    scheduledDate: '2026-05-19',
    estimatedDistanceKm: 5.2,
    estimatedDurationMin: 60,
    status: 'IN_PROGRESS',
    stops: [
      { lat: 28.625, lng: 77.22, address: 'Stop 1 — Park Gate' },
      { lat: 28.628, lng: 77.224, address: 'Stop 2 — Hospital' },
    ],
  },
]

const STATUS_COLORS: Record<string, string> = {
  SCHEDULED: 'bg-blue-100 text-blue-700',
  IN_PROGRESS: 'bg-indigo-100 text-indigo-700',
  COMPLETED: 'bg-green-100 text-green-700',
  CANCELLED: 'bg-red-100 text-red-700',
}

export default function WorkerRoutesPage() {
  const mapRef = useRef<HTMLDivElement>(null)
  const mapInstanceRef = useRef<any>(null)

  useEffect(() => {
    // Dynamically import Leaflet to avoid SSR issues
    let L: any
    let map: any

    const initMap = async () => {
      L = (await import('leaflet')).default
      await import('leaflet/dist/leaflet.css')

      // Fix default marker icon paths broken by bundlers
      delete (L.Icon.Default.prototype as any)._getIconUrl
      L.Icon.Default.mergeOptions({
        iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
        iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
        shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
      })

      if (!mapRef.current || mapInstanceRef.current) return

      map = L.map(mapRef.current).setView([28.6139, 77.209], 13)
      mapInstanceRef.current = map

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors',
      }).addTo(map)

      // Plot all stops from all routes
      SAMPLE_ROUTES.forEach((route, routeIdx) => {
        const color = routeIdx === 0 ? '#3b82f6' : '#8b5cf6'
        const latLngs: [number, number][] = route.stops.map((s) => [s.lat, s.lng])

        // Draw polyline for the route
        L.polyline(latLngs, { color, weight: 3, opacity: 0.8 }).addTo(map)

        // Add markers for each stop
        route.stops.forEach((stop, idx) => {
          const marker = L.circleMarker([stop.lat, stop.lng], {
            radius: 8,
            fillColor: color,
            color: '#fff',
            weight: 2,
            opacity: 1,
            fillOpacity: 0.9,
          }).addTo(map)

          marker.bindPopup(`
            <div style="font-size:13px">
              <strong>${route.routeName}</strong><br/>
              Stop ${idx + 1}: ${stop.address}
            </div>
          `)
        })
      })
    }

    initMap()

    return () => {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove()
        mapInstanceRef.current = null
      }
    }
  }, [])

  return (
    <div className="space-y-5">
      <h1 className="text-xl font-bold text-gray-800">My Routes</h1>

      {/* Route cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {SAMPLE_ROUTES.map((route) => (
          <div key={route.id} className="bg-white rounded-xl shadow-sm p-5">
            <div className="flex items-start justify-between mb-3">
              <div>
                <p className="font-semibold text-gray-800">{route.routeName}</p>
                <p className="text-xs text-gray-500 mt-0.5">{route.areaName}</p>
              </div>
              <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${STATUS_COLORS[route.status]}`}>
                {route.status.replace('_', ' ')}
              </span>
            </div>

            <div className="grid grid-cols-3 gap-3 text-xs text-gray-600 mb-4">
              <div className="flex items-center gap-1.5">
                <Calendar className="w-3.5 h-3.5 text-gray-400" />
                {route.scheduledDate}
              </div>
              <div className="flex items-center gap-1.5">
                <Route className="w-3.5 h-3.5 text-gray-400" />
                {route.estimatedDistanceKm} km
              </div>
              <div className="flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5 text-gray-400" />
                {route.estimatedDurationMin} min
              </div>
            </div>

            <div className="space-y-1.5">
              <p className="text-xs font-medium text-gray-500 uppercase tracking-wide">Stops ({route.stops.length})</p>
              {route.stops.map((stop, idx) => (
                <div key={idx} className="flex items-center gap-2 text-xs text-gray-600">
                  <div className="w-5 h-5 rounded-full bg-primary-100 text-primary-700 flex items-center justify-center font-bold shrink-0">
                    {idx + 1}
                  </div>
                  <MapPin className="w-3 h-3 text-gray-400 shrink-0" />
                  {stop.address}
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>

      {/* Map */}
      <div className="bg-white rounded-xl shadow-sm overflow-hidden">
        <div className="px-5 py-3 border-b border-gray-100">
          <h2 className="font-semibold text-gray-700">Route Map</h2>
          <p className="text-xs text-gray-400 mt-0.5">Blue = Route 1 · Purple = Route 2</p>
        </div>
        <div ref={mapRef} className="h-96 w-full" />
      </div>
    </div>
  )
}
