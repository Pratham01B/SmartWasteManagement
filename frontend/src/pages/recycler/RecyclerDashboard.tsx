import { Recycle, ShoppingBag, TrendingUp, Package } from 'lucide-react'

// Placeholder dashboard for RECYCLER role
// Marketplace module will be built once the backend marketplace API is ready
export default function RecyclerDashboard() {
  const features = [
    {
      icon: ShoppingBag,
      title: 'Buy Recyclables',
      description: 'Browse listings from citizens and waste collectors',
      color: 'bg-green-50 text-green-600',
      soon: false,
    },
    {
      icon: Package,
      title: 'Post a Listing',
      description: 'List recyclable materials you want to sell',
      color: 'bg-blue-50 text-blue-600',
      soon: false,
    },
    {
      icon: TrendingUp,
      title: 'Price Tracker',
      description: 'Track real-time scrap and recyclable material prices',
      color: 'bg-purple-50 text-purple-600',
      soon: true,
    },
  ]

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-xl font-bold text-gray-800">Recycler Dashboard</h1>
        <p className="text-sm text-gray-500 mt-0.5">Buy and sell recyclable materials through the SmartWaste marketplace</p>
      </div>

      {/* Hero banner */}
      <div className="bg-gradient-to-r from-green-500 to-emerald-600 rounded-xl p-6 text-white flex items-center justify-between">
        <div>
          <p className="text-sm font-medium opacity-90">Waste Marketplace</p>
          <p className="text-2xl font-bold mt-1">Coming Soon</p>
          <p className="text-sm opacity-80 mt-2 max-w-xs">
            Connect with citizens and collectors to buy recyclable materials at competitive prices.
          </p>
        </div>
        <Recycle className="w-20 h-20 opacity-20" />
      </div>

      {/* Feature cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {features.map((f) => (
          <div key={f.title} className="bg-white rounded-xl shadow-sm p-5 relative overflow-hidden">
            {f.soon && (
              <span className="absolute top-3 right-3 text-xs bg-amber-100 text-amber-700 px-2 py-0.5 rounded-full font-medium">
                Soon
              </span>
            )}
            <div className={`w-10 h-10 rounded-xl flex items-center justify-center mb-3 ${f.color}`}>
              <f.icon className="w-5 h-5" />
            </div>
            <p className="font-semibold text-gray-800">{f.title}</p>
            <p className="text-sm text-gray-500 mt-1">{f.description}</p>
          </div>
        ))}
      </div>

      {/* Stats placeholder */}
      <div className="bg-white rounded-xl shadow-sm p-5">
        <h2 className="font-semibold text-gray-700 mb-4">Your Activity</h2>
        <div className="grid grid-cols-3 gap-4 text-center">
          {[
            { label: 'Purchases', value: '—' },
            { label: 'Listings', value: '—' },
            { label: 'Total Spent', value: '—' },
          ].map((s) => (
            <div key={s.label} className="bg-gray-50 rounded-lg p-4">
              <p className="text-2xl font-bold text-gray-400">{s.value}</p>
              <p className="text-xs text-gray-500 mt-1">{s.label}</p>
            </div>
          ))}
        </div>
        <p className="text-xs text-gray-400 text-center mt-3">Activity data will appear once the marketplace is live</p>
      </div>
    </div>
  )
}
