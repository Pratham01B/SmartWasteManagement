import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { ChevronLeft, ChevronRight, AlertCircle } from 'lucide-react'
import { complaintsApi } from '../../api/complaints'
import { StatusBadge, PriorityBadge } from '../../components/StatusBadge'
import type { Complaint, ComplaintStatus } from '../../types'

// Statuses a worker can transition to
const WORKER_STATUS_OPTIONS: ComplaintStatus[] = ['IN_PROGRESS', 'RESOLVED']

export default function WorkerTasksPage() {
  const [page, setPage] = useState(0)
  const queryClient = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['worker-complaints', page],
    queryFn: () => complaintsApi.getWorkerComplaints(page),
  })

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) =>
      complaintsApi.updateStatus(id, status),
    onSuccess: () => {
      toast.success('Status updated')
      queryClient.invalidateQueries({ queryKey: ['worker-complaints'] })
    },
    onError: () => toast.error('Failed to update status'),
  })

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
          </div>
        ) : (
          <ul className="divide-y divide-gray-100">
            {complaints.map((c: Complaint) => (
              <li key={c.id} className="px-5 py-4 hover:bg-gray-50 transition">
                <div className="flex items-start justify-between gap-4">
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2 mb-1 flex-wrap">
                      <span className="text-xs text-gray-400">#{c.id}</span>
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
                  </div>

                  <div className="shrink-0 flex flex-col items-end gap-2">
                    <StatusBadge status={c.status} />
                    {/* Only show update if not yet resolved/rejected */}
                    {!['RESOLVED', 'REJECTED'].includes(c.status) && (
                      <select
                        defaultValue=""
                        onChange={(e) => {
                          if (e.target.value) {
                            statusMutation.mutate({ id: c.id, status: e.target.value })
                            e.target.value = ''
                          }
                        }}
                        className="border border-gray-200 rounded px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-primary-500"
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
    </div>
  )
}
