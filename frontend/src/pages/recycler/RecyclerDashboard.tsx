import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Recycle, ShoppingBag, Package, TrendingUp, Plus } from 'lucide-react'
import { marketplaceApi } from '../../api/marketplace'
import { useAuthStore } from '../../store/authStore'
import type { MarketplaceListing } from '../../types'

const MATERIAL_COLORS: Record<string, string> = {
  PLASTIC:    'bg-blue-100 text-blue-700',
  PAPER:      'bg-yellow-100 text-yellow-700',
  METAL:      'bg-gray-100 text-gray-700',
  GLASS:      'bg-cyan-100 text-cyan-700',
  ELECTRONIC: 'bg-purple-100 text-purple-700',
  RUBBER:     'bg-orange-100 text-orange-700',
  TEXTILE:    'bg-pink-100 text-pink-700',
  OTHER:      'bg-green-100 text-green-700',
}

export default function RecyclerDashboard() {
  const user = useAuthStore((s) => s.user)

  // My listings
  const { data: myListingsPage } = useQuery({
    queryKey: ['my-listings', 0],
    queryFn: () => marketplaceApi.getMy(0),
  })

  // All active listings (browse)
  const { data: allListingsPage } = useQuery({
    queryKey: ['marketplace', 0],
    queryFn: () => marketplaceApi.getAll(0, 6),
  })

  const myListings = myListingsPage?.content ?? []
  const recentListings = allListingsPage?.content ?? []
  const activeMyListings = myListings.filter((l) => l.status === 'ACTIVE').length
  const soldMyListings = myListings.filter((l) => l.status === 'SOLD').length

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-gray-800">Recycler Dashboard</h1>
          <p className="text-sm text-gray-500 mt-0.5">Welcome back, {user?.fullName}</p>
        </div>
        <Link
          to="/recycler/marketplace"
          className="flex items-center gap-2 bg-primary-600 hover:bg-primary-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition"
        >
          <Plus className="w-4 h-4" />
          New Listing
        </Link>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-2">
          <div className="bg-green-50 w-9 h-9 rounded-lg flex items-center justify-center">
            <Package className="text-green-600 w-5 h-5" />
          </div>
          <p className="text-2xl font-bold text-gray-800">{myListingsPage?.totalElements ?? 0}</p>
          <p className="text-xs text-gray-500">Total Listings</p>
        </div>
        <div className="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-2">
          <div className="bg-blue-50 w-9 h-9 rounded-lg flex items-center justify-center">
            <ShoppingBag className="text-blue-600 w-5 h-5" />
          </div>
          <p className="text-2xl font-bold text-gray-800">{activeMyListings}</p>
          <p className="text-xs text-gray-500">Active Listings</p>
        </div>
        <div className="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-2">
          <div className="bg-emerald-50 w-9 h-9 rounded-lg flex items-center justify-center">
            <TrendingUp className="text-emerald-600 w-5 h-5" />
          </div>
          <p className="text-2xl font-bold text-gray-800">{soldMyListings}</p>
          <p className="text-xs text-gray-500">Sold</p>
        </div>
        <div className="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-2">
          <div className="bg-purple-50 w-9 h-9 rounded-lg flex items-center justify-center">
            <Recycle className="text-purple-600 w-5 h-5" />
          </div>
          <p className="text-2xl font-bold text-gray-800">{allListingsPage?.totalElements ?? 0}</p>
          <p className="text-xs text-gray-500">Market Listings</p>
        </div>
      </div>

      {/* Quick links */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <Link
          to="/recycler/marketplace"
          className="bg-white rounded-xl shadow-sm p-5 flex items-center gap-4 hover:shadow-md transition group"
        >
          <div className="bg-green-50 w-12 h-12 rounded-xl flex items-center justify-center group-hover:bg-green-100 transition">
            <ShoppingBag className="text-green-600 w-6 h-6" />
          </div>
          <div>
            <p className="font-semibold text-gray-800">Browse Marketplace</p>
            <p className="text-xs text-gray-500 mt-0.5">Find recyclable materials to buy</p>
          </div>
        </Link>
        <Link
          to="/recycler/marketplace?tab=my"
          className="bg-white rounded-xl shadow-sm p-5 flex items-center gap-4 hover:shadow-md transition group"
        >
          <div className="bg-blue-50 w-12 h-12 rounded-xl flex items-center justify-center group-hover:bg-blue-100 transition">
            <Package className="text-blue-600 w-6 h-6" />
          </div>
          <div>
            <p className="font-semibold text-gray-800">My Listings</p>
            <p className="text-xs text-gray-500 mt-0.5">Manage your posted materials</p>
          </div>
        </Link>
      </div>

      {/* Recent marketplace listings */}
      <div className="bg-white rounded-xl shadow-sm">
        <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
          <h2 className="font-semibold text-gray-700">Recent Marketplace Listings</h2>
          <Link to="/recycler/marketplace" className="text-sm text-primary-600 hover:underline">
            View all
          </Link>
        </div>

        {recentListings.length === 0 ? (
          <div className="text-center py-10 text-gray-400">
            <Recycle className="w-10 h-10 mx-auto mb-2 opacity-25" />
            <p className="text-sm">No listings yet — be the first to post!</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 p-5">
            {recentListings.map((listing: MarketplaceListing) => (
              <ListingCard key={listing.id} listing={listing} />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

function ListingCard({ listing }: { listing: MarketplaceListing }) {
  return (
    <div className="border border-gray-100 rounded-xl p-4 hover:shadow-sm transition">
      {listing.imageUrl && (
        <img
          src={listing.imageUrl}
          alt={listing.title}
          className="w-full h-32 object-cover rounded-lg mb-3"
        />
      )}
      <div className="flex items-start justify-between gap-2 mb-2">
        <p className="font-medium text-gray-800 text-sm truncate">{listing.title}</p>
        <span className={`text-xs px-2 py-0.5 rounded-full font-medium shrink-0 ${MATERIAL_COLORS[listing.materialType] ?? 'bg-gray-100 text-gray-600'}`}>
          {listing.materialType}
        </span>
      </div>
      <div className="flex items-center justify-between text-xs text-gray-500">
        <span>{listing.quantityKg} kg</span>
        <span className="font-semibold text-green-700">₹{listing.pricePerKg}/kg</span>
      </div>
      {listing.city && (
        <p className="text-xs text-gray-400 mt-1">📍 {listing.city}</p>
      )}
      <p className="text-xs text-gray-400 mt-1">by {listing.sellerName}</p>
    </div>
  )
}
