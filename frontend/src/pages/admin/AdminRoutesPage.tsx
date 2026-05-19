import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { useForm } from 'react-hook-form'
import { Plus, Trash2, X, Route } from 'lucide-react'
import { routesApi, type CreateRoutePayload } from '../../api/routes'
import { workersApi } from '../../api/workers'
import type { CollectionRoute, RouteStatus, Worker } from '../../types'

const STATUS_OPTIONS: RouteStatus[] = ['SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED']

const ROUTE_STATUS_STYLES: Record<RouteStatus, string> = {
  SCHEDULED: 'bg-blue-100 text-blue-700',
  IN_PROGRESS: 'bg-amber-100 text-amber-700',
  COMPLETED: 'bg-green-100 text-green-700',
  CANCELLED: 'bg-red-100 text-red-600',
}

interface RouteFormData {
  workerId: string
  routeName: string
  scheduledDate: string
  areaName: string
  pincode: string
  estimatedDistanceKm: string
  estimatedDurationMin: string
}

export default function AdminRoutesPage() {
  const [showForm, setShowForm] = useState(false)
  const [filterStatus, setFilterStatus] = useState<RouteStatus | ''>('')
  const queryClient = useQueryClient()

  const { data: routes = [], isLoading } = useQuery<CollectionRoute[]>({
    queryKey: ['admin-routes'],
    queryFn: () => routesApi.getAll(),
  })

  const { data: workers = [] } = useQuery<Worker[]>({
    queryKey: ['workers-active'],
    queryFn: () => workersApi.getActive(),
  })

  const { register, handleSubmit, reset, formState: { errors } } = useForm<RouteFormData>()

  const createMutation = useMutation({
    mutationFn: (data: CreateRoutePayload) => routesApi.create(data),
    onSuccess: () => {
      toast.success('Route created successfully')
      queryClient.invalidateQueries({ queryKey: ['admin-routes'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard-stats'] })
      setShowForm(false)
      reset()
    },
    onError: (err: any) =>
      toast.error(err.response?.data?.message ?? 'Failed to create route'),
  })

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: RouteStatus }) =>
      routesApi.updateStatus(id, status),
    onSuccess: () => {
      toast.success('Route status updated')
      queryClient.invalidateQueries({ queryKey: ['admin-routes'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard-stats'] })
    },
    onError: () => toast.error('Failed to update status'),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => routesApi.delete(id),
    onSuccess: () => {
      toast.success('Route deleted')
      queryClient.invalidateQueries({ queryKey: ['admin-routes'] })
    },
    onError: () => toast.error('Failed to delete route'),
  })

  const onSubmit = (data: RouteFormData) => {
    createMutation.mutate({
      workerId: Number(data.workerId),
      routeName: data.routeName,
      scheduledDate: data.scheduledDate,
      areaName: data.areaName || undefined,
      pincode: data.pincode || undefined,
      estimatedDistanceKm: data.estimatedDistanceKm ? Number(data.estimatedDistanceKm) : undefined,
      estimatedDurationMin: data.estimatedDurationMin ? Number(data.estimatedDurationMin) : undefined,
    })
  }

  const filtered = routes.filter(
    (r) => !filterStatus || r.status === filterStatus
  )

  // Summary counts
  const counts = STATUS_OPTIONS.reduce((acc, s) => {
    acc[s] = routes.filter((r) => r.status === s).length
    return acc
  }, {} as Record<RouteStatus, number>)

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-gray-800">Route Management</h1>
        <button
          onClick={() => setShowForm(true)}
          className="flex items-center gap-2 bg-primary-600 hover:bg-primary-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition"
        >
          <Plus className="w-4 h-4" />
          New Route
        </button>
      </div>

      {/* Summary cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {STATUS_OPTIONS.map((s) => (
          <div key={s} className="bg-white rounded-xl shadow-sm p-4 flex items-center gap-3">
            <div className={`p-2 rounded-lg ${ROUTE_STATUS_STYLES[s].replace('text-', 'text-').split(' ')[0]}`}>
              <Route className={`w-5 h-5 ${ROUTE_STATUS_STYLES[s].split(' ')[1]}`} />
            </div>
            <div>
              <p className="text-2xl font-bold text-gray-800">{counts[s]}</p>
              <p className="text-xs text-gray-500 capitalize">{s.replace('_', ' ')}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Filter */}
      <div className="flex gap-2 flex-wrap">
        <button
          onClick={() => setFilterStatus('')}
          className={`px-3 py-1.5 rounded-lg text-sm font-medium transition ${
            filterStatus === '' ? 'bg-primary-600 text-white' : 'bg-white border border-gray-300 text-gray-600 hover:bg-gray-50'
          }`}
        >
          All
        </button>
        {STATUS_OPTIONS.map((s) => (
          <button
            key={s}
            onClick={() => setFilterStatus(s)}
            className={`px-3 py-1.5 rounded-lg text-sm font-medium transition ${
              filterStatus === s ? 'bg-primary-600 text-white' : 'bg-white border border-gray-300 text-gray-600 hover:bg-gray-50'
            }`}
          >
            {s.replace('_', ' ')}
          </button>
        ))}
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="flex items-center justify-center h-48">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">ID</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Route Name</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Worker</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Area</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Date</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Distance</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Duration</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Status</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {filtered.length === 0 ? (
                  <tr>
                    <td colSpan={9} className="text-center py-10 text-gray-400">
                      No routes found
                    </td>
                  </tr>
                ) : (
                  filtered.map((r) => (
                    <tr key={r.id} className="hover:bg-gray-50 transition">
                      <td className="px-4 py-3 text-gray-500 font-mono text-xs">#{r.id}</td>
                      <td className="px-4 py-3 font-medium text-gray-800">{r.routeName}</td>
                      <td className="px-4 py-3 text-gray-600">{r.workerName}</td>
                      <td className="px-4 py-3 text-gray-500">{r.areaName ?? '—'}</td>
                      <td className="px-4 py-3 text-gray-500 whitespace-nowrap">{r.scheduledDate}</td>
                      <td className="px-4 py-3 text-gray-500">
                        {r.estimatedDistanceKm != null ? `${r.estimatedDistanceKm} km` : '—'}
                      </td>
                      <td className="px-4 py-3 text-gray-500">
                        {r.estimatedDurationMin != null ? `${r.estimatedDurationMin} min` : '—'}
                      </td>
                      <td className="px-4 py-3">
                        <select
                          value={r.status}
                          onChange={(e) =>
                            statusMutation.mutate({ id: r.id, status: e.target.value as RouteStatus })
                          }
                          className="border border-gray-200 rounded px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-primary-500 bg-white"
                        >
                          {STATUS_OPTIONS.map((s) => (
                            <option key={s} value={s}>{s.replace('_', ' ')}</option>
                          ))}
                        </select>
                      </td>
                      <td className="px-4 py-3">
                        <button
                          onClick={() => {
                            if (confirm(`Delete route "${r.routeName}"?`)) {
                              deleteMutation.mutate(r.id)
                            }
                          }}
                          disabled={deleteMutation.isPending}
                          className="text-red-400 hover:text-red-600 transition disabled:opacity-40"
                          title="Delete route"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Create Route Modal */}
      {showForm && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-xl p-6 w-full max-w-lg max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-5">
              <h2 className="text-base font-semibold text-gray-800">Create New Route</h2>
              <button
                onClick={() => { setShowForm(false); reset() }}
                className="p-1 rounded-lg hover:bg-gray-100 text-gray-500"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
              {/* Worker */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Assign Worker <span className="text-red-500">*</span>
                </label>
                <select
                  {...register('workerId', { required: 'Worker is required' })}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                >
                  <option value="">— Select worker —</option>
                  {workers.map((w) => (
                    <option key={w.id} value={w.id}>
                      {w.fullName} {w.city ? `(${w.city})` : ''}
                    </option>
                  ))}
                </select>
                {errors.workerId && (
                  <p className="text-red-500 text-xs mt-1">{errors.workerId.message}</p>
                )}
              </div>

              {/* Route Name */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Route Name <span className="text-red-500">*</span>
                </label>
                <input
                  {...register('routeName', { required: 'Route name is required' })}
                  placeholder="e.g. Sector 5 Morning Route"
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                />
                {errors.routeName && (
                  <p className="text-red-500 text-xs mt-1">{errors.routeName.message}</p>
                )}
              </div>

              {/* Scheduled Date */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Scheduled Date <span className="text-red-500">*</span>
                </label>
                <input
                  type="date"
                  {...register('scheduledDate', { required: 'Date is required' })}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                />
                {errors.scheduledDate && (
                  <p className="text-red-500 text-xs mt-1">{errors.scheduledDate.message}</p>
                )}
              </div>

              {/* Area + Pincode */}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Area Name</label>
                  <input
                    {...register('areaName')}
                    placeholder="e.g. Sector 5"
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

              {/* Distance + Duration */}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Est. Distance (km)
                  </label>
                  <input
                    type="number"
                    step="0.1"
                    {...register('estimatedDistanceKm')}
                    placeholder="e.g. 12.5"
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Est. Duration (min)
                  </label>
                  <input
                    type="number"
                    {...register('estimatedDurationMin')}
                    placeholder="e.g. 90"
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                </div>
              </div>

              <div className="flex gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => { setShowForm(false); reset() }}
                  className="flex-1 border border-gray-300 text-gray-600 py-2.5 rounded-lg text-sm hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={createMutation.isPending}
                  className="flex-1 bg-primary-600 text-white py-2.5 rounded-lg text-sm font-medium hover:bg-primary-700 disabled:opacity-60"
                >
                  {createMutation.isPending ? 'Creating...' : 'Create Route'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
