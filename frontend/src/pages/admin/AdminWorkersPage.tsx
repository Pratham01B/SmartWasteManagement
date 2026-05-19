import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { Search, UserCheck, UserX, Users } from 'lucide-react'
import { workersApi } from '../../api/workers'
import type { Worker } from '../../types'

export default function AdminWorkersPage() {
  const [search, setSearch] = useState('')
  const [filterActive, setFilterActive] = useState<'all' | 'active' | 'inactive'>('all')
  const queryClient = useQueryClient()

  const { data: workers = [], isLoading } = useQuery<Worker[]>({
    queryKey: ['all-workers'],
    queryFn: () => workersApi.getAll(),
  })

  const toggleMutation = useMutation({
    mutationFn: (id: number) => workersApi.toggleStatus(id),
    onSuccess: (updated) => {
      toast.success(
        updated.isActive ? 'Worker activated' : 'Worker deactivated'
      )
      queryClient.invalidateQueries({ queryKey: ['all-workers'] })
      queryClient.invalidateQueries({ queryKey: ['workers-active'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard-stats'] })
    },
    onError: () => toast.error('Failed to update worker status'),
  })

  const filtered = workers.filter((w) => {
    const matchesSearch =
      !search ||
      w.fullName.toLowerCase().includes(search.toLowerCase()) ||
      w.email.toLowerCase().includes(search.toLowerCase()) ||
      (w.city ?? '').toLowerCase().includes(search.toLowerCase())
    const matchesFilter =
      filterActive === 'all' ||
      (filterActive === 'active' && w.isActive) ||
      (filterActive === 'inactive' && !w.isActive)
    return matchesSearch && matchesFilter
  })

  const activeCount = workers.filter((w) => w.isActive).length
  const inactiveCount = workers.length - activeCount

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-gray-800">Worker Management</h1>
        <span className="text-sm text-gray-500">{workers.length} total workers</span>
      </div>

      {/* Summary cards */}
      <div className="grid grid-cols-3 gap-4">
        <div className="bg-white rounded-xl shadow-sm p-4 flex items-center gap-3">
          <div className="bg-blue-50 p-2 rounded-lg">
            <Users className="w-5 h-5 text-blue-600" />
          </div>
          <div>
            <p className="text-2xl font-bold text-gray-800">{workers.length}</p>
            <p className="text-xs text-gray-500">Total Workers</p>
          </div>
        </div>
        <div className="bg-white rounded-xl shadow-sm p-4 flex items-center gap-3">
          <div className="bg-green-50 p-2 rounded-lg">
            <UserCheck className="w-5 h-5 text-green-600" />
          </div>
          <div>
            <p className="text-2xl font-bold text-gray-800">{activeCount}</p>
            <p className="text-xs text-gray-500">Active</p>
          </div>
        </div>
        <div className="bg-white rounded-xl shadow-sm p-4 flex items-center gap-3">
          <div className="bg-red-50 p-2 rounded-lg">
            <UserX className="w-5 h-5 text-red-500" />
          </div>
          <div>
            <p className="text-2xl font-bold text-gray-800">{inactiveCount}</p>
            <p className="text-xs text-gray-500">Inactive</p>
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap gap-3">
        <div className="relative flex-1 min-w-[200px] max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by name, email, city..."
            className="w-full pl-9 pr-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>
        <div className="flex rounded-lg border border-gray-300 overflow-hidden text-sm">
          {(['all', 'active', 'inactive'] as const).map((f) => (
            <button
              key={f}
              onClick={() => setFilterActive(f)}
              className={`px-4 py-2 capitalize transition ${
                filterActive === f
                  ? 'bg-primary-600 text-white'
                  : 'bg-white text-gray-600 hover:bg-gray-50'
              }`}
            >
              {f}
            </button>
          ))}
        </div>
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
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Name</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Email</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Phone</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">City</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Pincode</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Status</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {filtered.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="text-center py-10 text-gray-400">
                      No workers found
                    </td>
                  </tr>
                ) : (
                  filtered.map((w) => (
                    <tr key={w.id} className="hover:bg-gray-50 transition">
                      <td className="px-4 py-3 text-gray-500 font-mono text-xs">#{w.id}</td>
                      <td className="px-4 py-3 font-medium text-gray-800">{w.fullName}</td>
                      <td className="px-4 py-3 text-gray-600">{w.email}</td>
                      <td className="px-4 py-3 text-gray-500">{w.phoneNumber ?? '—'}</td>
                      <td className="px-4 py-3 text-gray-500">{w.city ?? '—'}</td>
                      <td className="px-4 py-3 text-gray-500">{w.pincode ?? '—'}</td>
                      <td className="px-4 py-3">
                        <span
                          className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${
                            w.isActive
                              ? 'bg-green-100 text-green-700'
                              : 'bg-red-100 text-red-600'
                          }`}
                        >
                          {w.isActive ? 'Active' : 'Inactive'}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <button
                          onClick={() => toggleMutation.mutate(w.id)}
                          disabled={toggleMutation.isPending}
                          className={`flex items-center gap-1 text-xs font-medium transition disabled:opacity-50 ${
                            w.isActive
                              ? 'text-red-500 hover:text-red-700'
                              : 'text-green-600 hover:text-green-800'
                          }`}
                        >
                          {w.isActive ? (
                            <><UserX className="w-3.5 h-3.5" /> Deactivate</>
                          ) : (
                            <><UserCheck className="w-3.5 h-3.5" /> Activate</>
                          )}
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
    </div>
  )
}
