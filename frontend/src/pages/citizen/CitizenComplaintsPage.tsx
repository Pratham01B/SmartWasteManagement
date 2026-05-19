import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Plus, ChevronLeft, ChevronRight, AlertCircle } from 'lucide-react'
import { complaintsApi } from '../../api/complaints'
import { StatusBadge, PriorityBadge } from '../../components/StatusBadge'
import type { Complaint } from '../../types'

export default function CitizenComplaintsPage() {
  const [page, setPage] = useState(0)

  const { data, isLoading } = useQuery({
    queryKey: ['my-complaints', page],
    queryFn: () => complaintsApi.getMy(page),
  })

  const complaints = data?.content ?? []

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-gray-800">My Complaints</h1>
        <Link
          to="/citizen/complaints/new"
          className="flex items-center gap-2 bg-primary-600 hover:bg-primary-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition"
        >
          <Plus className="w-4 h-4" />
          New Complaint
        </Link>
      </div>

      <div className="bg-white rounded-xl shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="flex items-center justify-center h-48">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
          </div>
        ) : complaints.length === 0 ? (
          <div className="text-center py-16 text-gray-400">
            <AlertCircle className="w-12 h-12 mx-auto mb-3 opacity-25" />
            <p className="font-medium">No complaints filed yet</p>
            <p className="text-sm mt-1">
              <Link to="/citizen/complaints/new" className="text-primary-600 hover:underline">File your first complaint</Link>
            </p>
          </div>
        ) : (
          <ul className="divide-y divide-gray-100">
            {complaints.map((c: Complaint) => (
              <li key={c.id} className="px-5 py-4 hover:bg-gray-50 transition">
                <div className="flex items-start justify-between gap-4">
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="text-xs text-gray-400">#{c.id}</span>
                      <StatusBadge status={c.status} />
                      <PriorityBadge priority={c.priority} />
                    </div>
                    <p className="font-medium text-gray-800">{c.title}</p>
                    {c.description && (
                      <p className="text-sm text-gray-500 mt-0.5 line-clamp-2">{c.description}</p>
                    )}
                    <p className="text-xs text-gray-400 mt-1">{c.address}</p>
                  </div>
                  <div className="text-right shrink-0">
                    <p className="text-xs text-gray-400">{new Date(c.createdAt).toLocaleDateString('en-IN')}</p>
                    {c.wasteType && (
                      <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full mt-1 inline-block">
                        {c.wasteType}
                      </span>
                    )}
                    {c.rewardPointsAwarded > 0 && (
                      <p className="text-xs text-amber-600 font-medium mt-1">+{c.rewardPointsAwarded} pts</p>
                    )}
                    {c.assignedWorkerName && (
                      <p className="text-xs text-gray-500 mt-1">Worker: {c.assignedWorkerName}</p>
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
