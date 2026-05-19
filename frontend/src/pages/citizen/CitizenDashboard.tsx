import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { AlertCircle, CheckCircle, Clock, Plus, Star, ChevronRight } from 'lucide-react'
import { complaintsApi } from '../../api/complaints'
import { authApi } from '../../api/auth'
import { useAuthStore } from '../../store/authStore'
import { StatusBadge } from '../../components/StatusBadge'
import type { Complaint } from '../../types'

export default function CitizenDashboard() {
  const user = useAuthStore((s) => s.user)
  const updateRewardPoints = useAuthStore((s) => s.updateRewardPoints)

  const { data, isLoading } = useQuery({
    queryKey: ['my-complaints', 0],
    queryFn: () => complaintsApi.getMy(0),
  })

  // Poll /auth/me every 30s to keep reward points fresh after resolutions
  useQuery({
    queryKey: ['my-profile'],
    queryFn: async () => {
      const profile = await authApi.getMe()
      updateRewardPoints(profile.rewardPoints)
      return profile
    },
    refetchInterval: 30_000,
  })

  const complaints = data?.content ?? []
  const pending = complaints.filter((c: Complaint) => c.status === 'PENDING').length
  const inProgress = complaints.filter((c: Complaint) => ['ASSIGNED', 'IN_PROGRESS'].includes(c.status)).length
  const resolved = complaints.filter((c: Complaint) => c.status === 'RESOLVED').length
  // Points that will be earned once active complaints are resolved
  const pendingPoints = (pending + inProgress) * 10

  return (
    <div className="space-y-6">
      {/* Welcome */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-gray-800">Welcome, {user?.fullName?.split(' ')[0]} 👋</h1>
          <p className="text-sm text-gray-500 mt-0.5">Track your complaints and earn reward points</p>
        </div>
        <Link
          to="/citizen/complaints/new"
          className="flex items-center gap-2 bg-primary-600 hover:bg-primary-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition"
        >
          <Plus className="w-4 h-4" />
          File Complaint
        </Link>
      </div>

      {/* Reward points banner — links to full rewards page */}
      <Link
        to="/citizen/rewards"
        className="block bg-gradient-to-r from-amber-400 to-orange-400 rounded-xl p-5 text-white hover:opacity-95 transition"
      >
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm font-medium opacity-90">Your Reward Points</p>
            <p className="text-4xl font-bold mt-1">{user?.rewardPoints ?? 0}</p>
            <div className="flex items-center gap-3 mt-2">
              <p className="text-xs opacity-80">+10 pts per resolved complaint</p>
              {pendingPoints > 0 && (
                <span className="text-xs bg-white/20 px-2 py-0.5 rounded-full">
                  ~{pendingPoints} pts pending
                </span>
              )}
            </div>
          </div>
          <div className="flex flex-col items-end gap-2">
            <Star className="w-12 h-12 opacity-20" />
            <span className="flex items-center gap-1 text-xs font-medium opacity-80">
              View Rewards <ChevronRight className="w-3 h-3" />
            </span>
          </div>
        </div>
      </Link>

      {/* Stats */}
      <div className="grid grid-cols-3 gap-4">
        <div className="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-2">
          <div className="bg-yellow-50 w-9 h-9 rounded-lg flex items-center justify-center">
            <Clock className="text-yellow-600 w-5 h-5" />
          </div>
          <p className="text-2xl font-bold text-gray-800">{pending}</p>
          <p className="text-xs text-gray-500">Pending</p>
        </div>
        <div className="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-2">
          <div className="bg-blue-50 w-9 h-9 rounded-lg flex items-center justify-center">
            <AlertCircle className="text-blue-600 w-5 h-5" />
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

      {/* Recent complaints */}
      <div className="bg-white rounded-xl shadow-sm">
        <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
          <h2 className="font-semibold text-gray-700">Recent Complaints</h2>
          <Link to="/citizen/complaints" className="text-sm text-primary-600 hover:underline">View all</Link>
        </div>

        {isLoading ? (
          <div className="flex items-center justify-center h-32">
            <div className="animate-spin rounded-full h-7 w-7 border-b-2 border-primary-600" />
          </div>
        ) : complaints.length === 0 ? (
          <div className="text-center py-12 text-gray-400">
            <AlertCircle className="w-10 h-10 mx-auto mb-2 opacity-30" />
            <p className="text-sm">No complaints yet. File your first one!</p>
          </div>
        ) : (
          <ul className="divide-y divide-gray-50">
            {complaints.slice(0, 5).map((c: Complaint) => (
              <li key={c.id} className="px-5 py-3 flex items-center justify-between hover:bg-gray-50 transition">
                <div className="min-w-0">
                  <p className="text-sm font-medium text-gray-800 truncate">{c.title}</p>
                  <p className="text-xs text-gray-400 mt-0.5">{c.address}</p>
                </div>
                <div className="flex items-center gap-3 ml-4 shrink-0">
                  <StatusBadge status={c.status} />
                  <span className="text-xs text-gray-400">
                    {new Date(c.createdAt).toLocaleDateString('en-IN')}
                  </span>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
