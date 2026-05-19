import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { ClipboardList, CheckCircle, Loader2, MapPin } from 'lucide-react'
import { complaintsApi } from '../../api/complaints'
import { useAuthStore } from '../../store/authStore'
import { StatusBadge, PriorityBadge } from '../../components/StatusBadge'
import type { Complaint } from '../../types'

export default function WorkerDashboard() {
  const user = useAuthStore((s) => s.user)

  const { data, isLoading } = useQuery({
    queryKey: ['worker-complaints', 0],
    queryFn: () => complaintsApi.getWorkerComplaints(0),
  })

  const complaints = data?.content ?? []
  const pending = complaints.filter((c: Complaint) => c.status === 'ASSIGNED').length
  const inProgress = complaints.filter((c: Complaint) => c.status === 'IN_PROGRESS').length
  const resolved = complaints.filter((c: Complaint) => c.status === 'RESOLVED').length

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-xl font-bold text-gray-800">Worker Dashboard</h1>
        <p className="text-sm text-gray-500 mt-0.5">Hello, {user?.fullName} — here's your workload today</p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-3 gap-4">
        <div className="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-2">
          <div className="bg-blue-50 w-9 h-9 rounded-lg flex items-center justify-center">
            <ClipboardList className="text-blue-600 w-5 h-5" />
          </div>
          <p className="text-2xl font-bold text-gray-800">{pending}</p>
          <p className="text-xs text-gray-500">Assigned to me</p>
        </div>
        <div className="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-2">
          <div className="bg-indigo-50 w-9 h-9 rounded-lg flex items-center justify-center">
            <Loader2 className="text-indigo-600 w-5 h-5" />
          </div>
          <p className="text-2xl font-bold text-gray-800">{inProgress}</p>
          <p className="text-xs text-gray-500">In Progress</p>
        </div>
        <div className="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-2">
          <div className="bg-green-50 w-9 h-9 rounded-lg flex items-center justify-center">
            <CheckCircle className="text-green-600 w-5 h-5" />
          </div>
          <p className="text-2xl font-bold text-gray-800">{resolved}</p>
          <p className="text-xs text-gray-500">Resolved</p>
        </div>
      </div>

      {/* Quick links */}
      <div className="grid grid-cols-2 gap-4">
        <Link
          to="/worker/tasks"
          className="bg-white rounded-xl shadow-sm p-5 flex items-center gap-4 hover:shadow-md transition group"
        >
          <div className="bg-primary-50 w-12 h-12 rounded-xl flex items-center justify-center group-hover:bg-primary-100 transition">
            <ClipboardList className="text-primary-600 w-6 h-6" />
          </div>
          <div>
            <p className="font-semibold text-gray-800">My Tasks</p>
            <p className="text-xs text-gray-500 mt-0.5">View & update assigned complaints</p>
          </div>
        </Link>
        <Link
          to="/worker/routes"
          className="bg-white rounded-xl shadow-sm p-5 flex items-center gap-4 hover:shadow-md transition group"
        >
          <div className="bg-cyan-50 w-12 h-12 rounded-xl flex items-center justify-center group-hover:bg-cyan-100 transition">
            <MapPin className="text-cyan-600 w-6 h-6" />
          </div>
          <div>
            <p className="font-semibold text-gray-800">My Routes</p>
            <p className="text-xs text-gray-500 mt-0.5">View collection routes on map</p>
          </div>
        </Link>
      </div>

      {/* Recent tasks */}
      <div className="bg-white rounded-xl shadow-sm">
        <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
          <h2 className="font-semibold text-gray-700">Recent Tasks</h2>
          <Link to="/worker/tasks" className="text-sm text-primary-600 hover:underline">View all</Link>
        </div>

        {isLoading ? (
          <div className="flex items-center justify-center h-32">
            <div className="animate-spin rounded-full h-7 w-7 border-b-2 border-primary-600" />
          </div>
        ) : complaints.length === 0 ? (
          <div className="text-center py-12 text-gray-400">
            <ClipboardList className="w-10 h-10 mx-auto mb-2 opacity-30" />
            <p className="text-sm">No tasks assigned yet</p>
          </div>
        ) : (
          <ul className="divide-y divide-gray-50">
            {complaints.slice(0, 5).map((c: Complaint) => (
              <li key={c.id} className="px-5 py-3 flex items-center justify-between hover:bg-gray-50 transition">
                <div className="min-w-0">
                  <p className="text-sm font-medium text-gray-800 truncate">{c.title}</p>
                  <p className="text-xs text-gray-400 mt-0.5">{c.address}</p>
                </div>
                <div className="flex items-center gap-2 ml-4 shrink-0">
                  <PriorityBadge priority={c.priority} />
                  <StatusBadge status={c.status} />
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
