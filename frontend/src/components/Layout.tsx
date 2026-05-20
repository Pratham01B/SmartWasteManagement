import { Link, useNavigate, useLocation } from 'react-router-dom'
import { Leaf, LogOut } from 'lucide-react'
import { useAuthStore } from '../store/authStore'

interface NavItem {
  label: string
  path: string
}

const NAV_ITEMS: Record<string, NavItem[]> = {
  ADMIN: [
    { label: 'Dashboard', path: '/admin/dashboard' },
    { label: 'Complaints', path: '/admin/complaints' },
    { label: 'Workers', path: '/admin/workers' },
    { label: 'Routes', path: '/admin/routes' },
  ],
  CITIZEN: [
    { label: 'Dashboard', path: '/citizen/dashboard' },
    { label: 'My Complaints', path: '/citizen/complaints' },
    { label: 'File Complaint', path: '/citizen/complaints/new' },

    { label: 'Classify Waste', path: '/citizen/classify' },
    { label: 'Rewards', path: '/citizen/rewards' },
  ],
  WORKER: [
    { label: 'Dashboard', path: '/worker/dashboard' },
    { label: 'My Tasks', path: '/worker/tasks' },
    { label: 'My Routes', path: '/worker/routes' },
  ],
  RECYCLER: [
    { label: 'Dashboard', path: '/recycler/dashboard' },
    { label: 'Marketplace', path: '/recycler/marketplace' },
  ],
}

interface Props {
  children: React.ReactNode
}

export default function Layout({ children }: Props) {
  const { user, clearAuth } = useAuthStore()
  const navigate = useNavigate()
  const location = useLocation()

  const navItems = user ? (NAV_ITEMS[user.role] ?? []) : []

  const handleLogout = () => {
    clearAuth()
    navigate('/login')
  }

  return (
    <div className="min-h-screen flex flex-col">
      {/* Top nav */}
      <header className="bg-white border-b border-gray-200 sticky top-0 z-10">
        <div className="max-w-7xl mx-auto px-4 h-14 flex items-center justify-between">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-2">
            <div className="bg-primary-600 p-1.5 rounded-lg">
              <Leaf className="text-white w-4 h-4" />
            </div>
            <span className="font-bold text-gray-800">SmartWaste</span>
          </Link>

          {/* Nav links */}
          <nav className="hidden md:flex items-center gap-1">
            {navItems.map((item) => (
              <Link
                key={item.path}
                to={item.path}
                className={`px-3 py-1.5 rounded-lg text-sm font-medium transition ${
                  location.pathname === item.path
                    ? 'bg-primary-50 text-primary-700'
                    : 'text-gray-600 hover:bg-gray-100'
                }`}
              >
                {item.label}
              </Link>
            ))}
          </nav>

          {/* User info */}
          <div className="flex items-center gap-3">
            {user?.role === 'CITIZEN' && (
              <span className="text-xs bg-amber-100 text-amber-700 px-2 py-1 rounded-full font-medium">
                {user.rewardPoints} pts
              </span>
            )}
            <span className="text-sm text-gray-600 hidden sm:block">{user?.fullName}</span>
            <button
              onClick={handleLogout}
              className="p-1.5 rounded-lg text-gray-500 hover:bg-gray-100 transition"
              title="Logout"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </div>
      </header>

      {/* Page content */}
      <main className="flex-1 max-w-7xl mx-auto w-full px-4 py-6">
        {children}
      </main>
    </div>
  )
}
