import { useQuery } from '@tanstack/react-query'
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend
} from 'recharts'
import api from '../../api/axios'
import type { DashboardStats } from '../../types'
import { Users, AlertCircle, CheckCircle, Truck } from 'lucide-react'

const COLORS = ['#22c55e', '#3b82f6', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4']

export default function AdminDashboard() {
  const { data: stats, isLoading } = useQuery<DashboardStats>({
    queryKey: ['dashboard-stats'],
    queryFn: () => api.get('/analytics/dashboard').then((r) => r.data),
    refetchInterval: 15_000,   // refresh every 15s — picks up worker resolutions live
  })

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-600" />
      </div>
    )
  }

  if (!stats) return null

  const wasteChartData = Object.entries(stats.wasteTypeBreakdown).map(([name, value]) => ({ name, value }))
  const statusChartData = Object.entries(stats.statusBreakdown).map(([name, value]) => ({ name, value }))

  const statCards = [
    { label: 'Total Complaints', value: stats.totalComplaints, icon: AlertCircle, color: 'text-blue-600', bg: 'bg-blue-50' },
    { label: 'Pending', value: stats.pendingComplaints, icon: AlertCircle, color: 'text-amber-600', bg: 'bg-amber-50' },
    { label: 'Resolved', value: stats.resolvedComplaints, icon: CheckCircle, color: 'text-green-600', bg: 'bg-green-50' },
    { label: 'Active Workers', value: stats.activeWorkers, icon: Users, color: 'text-purple-600', bg: 'bg-purple-50' },
    { label: 'Active Routes', value: stats.activeRoutes, icon: Truck, color: 'text-cyan-600', bg: 'bg-cyan-50' },
    { label: 'Resolution Rate', value: `${stats.resolutionRate}%`, icon: CheckCircle, color: 'text-emerald-600', bg: 'bg-emerald-50' },
  ]

  return (
    <div className="p-6 space-y-6">
      <h1 className="text-2xl font-bold text-gray-800">Analytics Dashboard</h1>

      {/* Stat cards */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
        {statCards.map((card) => (
          <div key={card.label} className="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-2">
            <div className={`${card.bg} w-9 h-9 rounded-lg flex items-center justify-center`}>
              <card.icon className={`${card.color} w-5 h-5`} />
            </div>
            <p className="text-2xl font-bold text-gray-800">{card.value}</p>
            <p className="text-xs text-gray-500">{card.label}</p>
          </div>
        ))}
      </div>

      {/* Charts row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Waste type bar chart */}
        <div className="bg-white rounded-xl shadow-sm p-5">
          <h2 className="text-base font-semibold text-gray-700 mb-4">Complaints by Waste Type</h2>
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={wasteChartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" tick={{ fontSize: 12 }} />
              <YAxis tick={{ fontSize: 12 }} />
              <Tooltip />
              <Bar dataKey="value" fill="#22c55e" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Status pie chart */}
        <div className="bg-white rounded-xl shadow-sm p-5">
          <h2 className="text-base font-semibold text-gray-700 mb-4">Complaint Status Breakdown</h2>
          <ResponsiveContainer width="100%" height={240}>
            <PieChart>
              <Pie data={statusChartData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={80} label>
                {statusChartData.map((_, index) => (
                  <Cell key={index} fill={COLORS[index % COLORS.length]} />
                ))}
              </Pie>
              <Legend />
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Today summary */}
      <div className="bg-white rounded-xl shadow-sm p-5">
        <h2 className="text-base font-semibold text-gray-700 mb-3">Today's Summary</h2>
        <div className="flex gap-8 text-sm text-gray-600">
          <span>New complaints today: <strong className="text-gray-800">{stats.complaintsToday}</strong></span>
          <span>Routes completed: <strong className="text-gray-800">{stats.completedRoutesToday}</strong></span>
          <span>Total citizens: <strong className="text-gray-800">{stats.totalCitizens}</strong></span>
          <span>Total workers: <strong className="text-gray-800">{stats.totalWorkers}</strong></span>
        </div>
      </div>
    </div>
  )
}
