import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { Search, ChevronLeft, ChevronRight, UserCheck, X } from 'lucide-react'
import { complaintsApi } from '../../api/complaints'
import { workersApi } from '../../api/workers'
import { StatusBadge, PriorityBadge } from '../../components/StatusBadge'
import type { Complaint, ComplaintStatus, Worker } from '../../types'

const STATUS_OPTIONS: ComplaintStatus[] = ['PENDING', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'REJECTED']

export default function AdminComplaintsPage() {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<ComplaintStatus | ''>('')
  const [assignModal, setAssignModal] = useState<Complaint | null>(null)
  const [selectedWorkerId, setSelectedWorkerId] = useState('')
  const queryClient = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['admin-complaints', page],
    queryFn: () => complaintsApi.getAll(page, 15),
  })

  const { data: workers = [] } = useQuery<Worker[]>({
    queryKey: ['workers-active'],
    queryFn: () => workersApi.getActive(),
  })

  const assignMutation = useMutation({
    mutationFn: ({ complaintId, wId }: { complaintId: number; wId: number }) =>
      complaintsApi.assign(complaintId, wId),
    onSuccess: () => {
      toast.success('Worker assigned successfully')
      queryClient.invalidateQueries({ queryKey: ['admin-complaints'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard-stats'] })
      setAssignModal(null)
      setSelectedWorkerId('')
    },
    onError: (err: any) =>
      toast.error(err.response?.data?.message ?? 'Failed to assign worker'),
  })

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) =>
      complaintsApi.updateStatus(id, status),
    onSuccess: () => {
      toast.success('Status updated')
      queryClient.invalidateQueries({ queryKey: ['admin-complaints'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard-stats'] })
    },
    onError: (err: any) =>
      toast.error(err.response?.data?.message ?? 'Failed to update status'),
  })

  const filtered = (data?.content ?? []).filter((c: Complaint) => {
    const matchesSearch =
      !search ||
      c.title.toLowerCase().includes(search.toLowerCase()) ||
      c.citizenName.toLowerCase().includes(search.toLowerCase()) ||
      c.address.toLowerCase().includes(search.toLowerCase())
    const matchesStatus = !statusFilter || c.status === statusFilter
    return matchesSearch && matchesStatus
  })

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-gray-800">Complaint Management</h1>
        <span className="text-sm text-gray-500">{data?.totalElements ?? 0} total complaints</span>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap gap-3">
        <div className="relative flex-1 min-w-[200px] max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by title, citizen, address..."
            className="w-full pl-9 pr-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value as ComplaintStatus | '')}
          className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
        >
          <option value="">All Statuses</option>
          {STATUS_OPTIONS.map((s) => (
            <option key={s} value={s}>{s.replace('_', ' ')}</option>
          ))}
        </select>
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
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Title</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Citizen</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Address</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Type</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Priority</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Status</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Assigned Worker</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {filtered.length === 0 ? (
                  <tr>
                    <td colSpan={9} className="text-center py-10 text-gray-400">
                      No complaints found
                    </td>
                  </tr>
                ) : (
                  filtered.map((c: Complaint) => (
                    <tr key={c.id} className="hover:bg-gray-50 transition">
                      <td className="px-4 py-3 text-gray-500 font-mono text-xs">#{c.id}</td>
                      <td className="px-4 py-3 font-medium text-gray-800 max-w-[180px]">
                        <span className="truncate block" title={c.title}>{c.title}</span>
                      </td>
                      <td className="px-4 py-3 text-gray-600">{c.citizenName}</td>
                      <td className="px-4 py-3 text-gray-500 max-w-[160px]">
                        <span className="truncate block" title={c.address}>{c.address}</span>
                      </td>
                      <td className="px-4 py-3 text-gray-500 text-xs">{c.wasteType ?? '—'}</td>
                      <td className="px-4 py-3">
                        <PriorityBadge priority={c.priority} />
                      </td>
                      <td className="px-4 py-3">
                        <select
                          value={c.status}
                          onChange={(e) =>
                            statusMutation.mutate({ id: c.id, status: e.target.value })
                          }
                          className="border border-gray-200 rounded px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-primary-500 bg-white"
                        >
                          {STATUS_OPTIONS.map((s) => (
                            <option key={s} value={s}>{s.replace('_', ' ')}</option>
                          ))}
                        </select>
                      </td>
                      <td className="px-4 py-3 text-xs">
                        {c.assignedWorkerName ? (
                          <span className="text-green-700 font-medium">{c.assignedWorkerName}</span>
                        ) : (
                          <span className="text-gray-400 italic">Unassigned</span>
                        )}
                      </td>
                      <td className="px-4 py-3">
                        <button
                          onClick={() => { setAssignModal(c); setSelectedWorkerId('') }}
                          className="flex items-center gap-1 text-xs text-primary-600 hover:text-primary-800 font-medium whitespace-nowrap"
                        >
                          <UserCheck className="w-3.5 h-3.5" />
                          {c.assignedWorkerName ? 'Reassign' : 'Assign'}
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

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between text-sm text-gray-600">
          <span>Page {page + 1} of {data.totalPages}</span>
          <div className="flex gap-2">
            <button
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
              className="p-1.5 rounded-lg border border-gray-200 disabled:opacity-40 hover:bg-gray-50"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <button
              disabled={page >= data.totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
              className="p-1.5 rounded-lg border border-gray-200 disabled:opacity-40 hover:bg-gray-50"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* Assign Worker Modal */}
      {assignModal && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-xl p-6 w-full max-w-md">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-base font-semibold text-gray-800">Assign Worker</h2>
              <button
                onClick={() => setAssignModal(null)}
                className="p-1 rounded-lg hover:bg-gray-100 text-gray-500"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Complaint summary */}
            <div className="bg-gray-50 rounded-lg p-3 mb-4 text-sm">
              <p className="font-medium text-gray-800 truncate">{assignModal.title}</p>
              <p className="text-gray-500 text-xs mt-0.5">{assignModal.address}</p>
              {assignModal.assignedWorkerName && (
                <p className="text-amber-600 text-xs mt-1">
                  Currently assigned to: <strong>{assignModal.assignedWorkerName}</strong>
                </p>
              )}
            </div>

            {/* Worker dropdown */}
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Select Worker
            </label>
            {workers.length === 0 ? (
              <p className="text-sm text-gray-400 py-2">No active workers available</p>
            ) : (
              <select
                value={selectedWorkerId}
                onChange={(e) => setSelectedWorkerId(e.target.value)}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 mb-4"
              >
                <option value="">— Select a worker —</option>
                {workers.map((w) => (
                  <option key={w.id} value={w.id}>
                    {w.fullName} {w.city ? `(${w.city})` : ''}
                  </option>
                ))}
              </select>
            )}

            <div className="flex gap-3">
              <button
                onClick={() => { setAssignModal(null); setSelectedWorkerId('') }}
                className="flex-1 border border-gray-300 text-gray-600 py-2 rounded-lg text-sm hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                disabled={!selectedWorkerId || assignMutation.isPending}
                onClick={() =>
                  assignMutation.mutate({
                    complaintId: assignModal.id,
                    wId: Number(selectedWorkerId),
                  })
                }
                className="flex-1 bg-primary-600 text-white py-2 rounded-lg text-sm hover:bg-primary-700 disabled:opacity-60"
              >
                {assignMutation.isPending ? 'Assigning...' : 'Assign Worker'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
