import { useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import {
  Star, Trophy, Zap, Shield, Crown,
  CheckCircle, Clock, Plus, TrendingUp, Gift,
} from 'lucide-react'
import { complaintsApi } from '../../api/complaints'
import { authApi } from '../../api/auth'
import { useAuthStore } from '../../store/authStore'
import type { Complaint } from '../../types'

// ─── Reward tier definitions ────────────────────────────────────────────────
const TIERS = [
  { name: 'Newcomer',   min: 0,   max: 49,  icon: Zap,    color: 'text-gray-500',   bg: 'bg-gray-100',   ring: 'ring-gray-300',   bar: 'bg-gray-400'   },
  { name: 'Reporter',   min: 50,  max: 149, icon: Star,   color: 'text-blue-600',   bg: 'bg-blue-50',    ring: 'ring-blue-300',   bar: 'bg-blue-500'   },
  { name: 'Guardian',   min: 150, max: 299, icon: Shield, color: 'text-emerald-600',bg: 'bg-emerald-50', ring: 'ring-emerald-300',bar: 'bg-emerald-500'},
  { name: 'Champion',   min: 300, max: 499, icon: Trophy, color: 'text-amber-600',  bg: 'bg-amber-50',   ring: 'ring-amber-300',  bar: 'bg-amber-500'  },
  { name: 'Eco Legend', min: 500, max: Infinity, icon: Crown, color: 'text-purple-600', bg: 'bg-purple-50', ring: 'ring-purple-300', bar: 'bg-purple-500' },
]

// Points awarded per action
const POINTS_ON_FILE    = 0   // backend awards 0 on creation
const POINTS_ON_RESOLVE = 10  // matches ComplaintService.REWARD_POINTS_ON_RESOLVE

function getTier(points: number) {
  return TIERS.find((t) => points >= t.min && points <= t.max) ?? TIERS[0]
}

function getNextTier(points: number) {
  const idx = TIERS.findIndex((t) => points >= t.min && points <= t.max)
  return idx < TIERS.length - 1 ? TIERS[idx + 1] : null
}

// ─── Transaction row derived from a complaint ────────────────────────────────
interface Transaction {
  id: string
  label: string
  points: number
  date: string
  type: 'earned' | 'pending'
}

function buildTransactions(complaints: Complaint[]): Transaction[] {
  const txns: Transaction[] = []

  complaints.forEach((c) => {
    // Points earned on resolution
    if (c.status === 'RESOLVED' && c.rewardPointsAwarded > 0) {
      txns.push({
        id: `resolve-${c.id}`,
        label: `Complaint resolved: "${c.title}"`,
        points: c.rewardPointsAwarded,
        date: c.resolvedAt ?? c.createdAt,
        type: 'earned',
      })
    }
    // Pending points (complaint filed but not yet resolved)
    if (['PENDING', 'ASSIGNED', 'IN_PROGRESS'].includes(c.status)) {
      txns.push({
        id: `pending-${c.id}`,
        label: `Pending: "${c.title}"`,
        points: POINTS_ON_RESOLVE,
        date: c.createdAt,
        type: 'pending',
      })
    }
  })

  // Sort newest first
  return txns.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())
}

// ─── Component ───────────────────────────────────────────────────────────────
export default function RewardsDashboard() {
  const { user, updateRewardPoints } = useAuthStore()

  // Fetch fresh profile to get latest points from server
  const { data: profile } = useQuery({
    queryKey: ['my-profile'],
    queryFn: authApi.getMe,
    staleTime: 30_000,
  })

  // Sync store whenever server returns updated points
  useEffect(() => {
    if (profile && profile.rewardPoints !== user?.rewardPoints) {
      updateRewardPoints(profile.rewardPoints)
    }
  }, [profile, user?.rewardPoints, updateRewardPoints])

  // Fetch ALL pages of complaints to build full transaction history
  const { data: page1 } = useQuery({ queryKey: ['my-complaints', 0], queryFn: () => complaintsApi.getMy(0) })
  const { data: page2 } = useQuery({
    queryKey: ['my-complaints', 1],
    queryFn: () => complaintsApi.getMy(1),
    enabled: (page1?.totalPages ?? 0) > 1,
  })

  const allComplaints: Complaint[] = [
    ...(page1?.content ?? []),
    ...(page2?.content ?? []),
  ]

  const totalPoints   = profile?.rewardPoints ?? user?.rewardPoints ?? 0
  const currentTier   = getTier(totalPoints)
  const nextTier      = getNextTier(totalPoints)
  const progressPct   = nextTier
    ? Math.min(100, ((totalPoints - currentTier.min) / (nextTier.min - currentTier.min)) * 100)
    : 100

  const totalEarned   = allComplaints.reduce((sum, c) => sum + (c.rewardPointsAwarded ?? 0), 0)
  const pendingPoints = allComplaints
    .filter((c) => ['PENDING', 'ASSIGNED', 'IN_PROGRESS'].includes(c.status))
    .length * POINTS_ON_RESOLVE

  const resolvedCount = allComplaints.filter((c) => c.status === 'RESOLVED').length
  const transactions  = buildTransactions(allComplaints)

  const TierIcon = currentTier.icon

  return (
    <div className="space-y-6 max-w-3xl mx-auto">

      {/* ── Header ── */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-gray-800">Rewards</h1>
          <p className="text-sm text-gray-500 mt-0.5">Earn points by reporting and resolving waste issues</p>
        </div>
        <Link
          to="/citizen/complaints/new"
          className="flex items-center gap-2 bg-primary-600 hover:bg-primary-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition"
        >
          <Plus className="w-4 h-4" />
          File Complaint
        </Link>
      </div>

      {/* ── Points hero card ── */}
      <div className={`rounded-2xl p-6 text-white relative overflow-hidden
        bg-gradient-to-br from-amber-400 via-orange-400 to-rose-400`}>
        <div className="relative z-10">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-sm font-medium opacity-90">Total Points</p>
              <p className="text-6xl font-extrabold mt-1 tracking-tight">{totalPoints}</p>
              <div className={`inline-flex items-center gap-1.5 mt-3 px-3 py-1 rounded-full text-sm font-semibold
                bg-white/20 backdrop-blur-sm`}>
                <TierIcon className="w-4 h-4" />
                {currentTier.name}
              </div>
            </div>
            <TierIcon className="w-24 h-24 opacity-10 absolute right-4 top-4" />
          </div>

          {/* Progress to next tier */}
          {nextTier && (
            <div className="mt-5">
              <div className="flex justify-between text-xs opacity-80 mb-1.5">
                <span>{currentTier.name}</span>
                <span>{nextTier.name} at {nextTier.min} pts</span>
              </div>
              <div className="h-2 bg-white/30 rounded-full overflow-hidden">
                <div
                  className="h-full bg-white rounded-full transition-all duration-700"
                  style={{ width: `${progressPct}%` }}
                />
              </div>
              <p className="text-xs opacity-75 mt-1.5">
                {nextTier.min - totalPoints} more points to reach {nextTier.name}
              </p>
            </div>
          )}
          {!nextTier && (
            <p className="mt-4 text-sm font-semibold opacity-90">
              🎉 You've reached the highest tier — Eco Legend!
            </p>
          )}
        </div>
      </div>

      {/* ── Stats row ── */}
      <div className="grid grid-cols-3 gap-4">
        <div className="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-2">
          <div className="bg-green-50 w-9 h-9 rounded-lg flex items-center justify-center">
            <TrendingUp className="text-green-600 w-5 h-5" />
          </div>
          <p className="text-2xl font-bold text-gray-800">{totalEarned}</p>
          <p className="text-xs text-gray-500">Points Earned</p>
        </div>
        <div className="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-2">
          <div className="bg-amber-50 w-9 h-9 rounded-lg flex items-center justify-center">
            <Clock className="text-amber-600 w-5 h-5" />
          </div>
          <p className="text-2xl font-bold text-gray-800">{pendingPoints}</p>
          <p className="text-xs text-gray-500">Points Pending</p>
        </div>
        <div className="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-2">
          <div className="bg-blue-50 w-9 h-9 rounded-lg flex items-center justify-center">
            <CheckCircle className="text-blue-600 w-5 h-5" />
          </div>
          <p className="text-2xl font-bold text-gray-800">{resolvedCount}</p>
          <p className="text-xs text-gray-500">Complaints Resolved</p>
        </div>
      </div>

      {/* ── How to earn ── */}
      <div className="bg-white rounded-xl shadow-sm p-5">
        <h2 className="font-semibold text-gray-700 mb-4 flex items-center gap-2">
          <Gift className="w-4 h-4 text-primary-600" />
          How to Earn Points
        </h2>
        <div className="space-y-3">
          {[
            {
              icon: Plus,
              iconBg: 'bg-blue-50',
              iconColor: 'text-blue-600',
              title: 'File a Complaint',
              desc: 'Report a waste issue in your area',
              pts: POINTS_ON_FILE,
              note: 'Points awarded on resolution',
            },
            {
              icon: CheckCircle,
              iconBg: 'bg-green-50',
              iconColor: 'text-green-600',
              title: 'Complaint Resolved',
              desc: 'Your complaint gets resolved by a worker',
              pts: POINTS_ON_RESOLVE,
              note: 'Automatically credited',
            },
          ].map((item) => (
            <div key={item.title} className="flex items-center gap-4 p-3 rounded-lg bg-gray-50">
              <div className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${item.iconBg}`}>
                <item.icon className={`w-5 h-5 ${item.iconColor}`} />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-800">{item.title}</p>
                <p className="text-xs text-gray-500">{item.desc}</p>
                <p className="text-xs text-gray-400 mt-0.5">{item.note}</p>
              </div>
              <div className="text-right shrink-0">
                <p className="text-lg font-bold text-primary-600">+{item.pts}</p>
                <p className="text-xs text-gray-400">pts</p>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* ── Tier ladder ── */}
      <div className="bg-white rounded-xl shadow-sm p-5">
        <h2 className="font-semibold text-gray-700 mb-4 flex items-center gap-2">
          <Trophy className="w-4 h-4 text-amber-500" />
          Reward Tiers
        </h2>
        <div className="space-y-2">
          {TIERS.map((tier) => {
            const TIcon = tier.icon
            const isActive = currentTier.name === tier.name
            const isUnlocked = totalPoints >= tier.min
            return (
              <div
                key={tier.name}
                className={`flex items-center gap-4 p-3 rounded-xl border transition
                  ${isActive
                    ? `border-2 ${tier.ring.replace('ring', 'border')} ${tier.bg}`
                    : isUnlocked
                      ? 'border-gray-100 bg-gray-50'
                      : 'border-gray-100 bg-white opacity-50'
                  }`}
              >
                <div className={`w-9 h-9 rounded-lg flex items-center justify-center ${tier.bg}`}>
                  <TIcon className={`w-5 h-5 ${tier.color}`} />
                </div>
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <p className={`text-sm font-semibold ${isActive ? tier.color : 'text-gray-700'}`}>
                      {tier.name}
                    </p>
                    {isActive && (
                      <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${tier.bg} ${tier.color}`}>
                        Current
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-gray-500">
                    {tier.max === Infinity ? `${tier.min}+ points` : `${tier.min} – ${tier.max} points`}
                  </p>
                </div>
                {isUnlocked && (
                  <CheckCircle className={`w-5 h-5 ${tier.color} shrink-0`} />
                )}
              </div>
            )
          })}
        </div>
      </div>

      {/* ── Transaction history ── */}
      <div className="bg-white rounded-xl shadow-sm">
        <div className="px-5 py-4 border-b border-gray-100">
          <h2 className="font-semibold text-gray-700">Points History</h2>
          <p className="text-xs text-gray-400 mt-0.5">All your earned and pending reward points</p>
        </div>

        {transactions.length === 0 ? (
          <div className="text-center py-12 text-gray-400">
            <Star className="w-10 h-10 mx-auto mb-2 opacity-25" />
            <p className="text-sm font-medium">No points history yet</p>
            <p className="text-xs mt-1">
              <Link to="/citizen/complaints/new" className="text-primary-600 hover:underline">
                File a complaint
              </Link>{' '}
              to start earning
            </p>
          </div>
        ) : (
          <ul className="divide-y divide-gray-50">
            {transactions.map((tx) => (
              <li key={tx.id} className="px-5 py-3.5 flex items-center justify-between hover:bg-gray-50 transition">
                <div className="flex items-center gap-3 min-w-0">
                  <div className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0
                    ${tx.type === 'earned' ? 'bg-green-50' : 'bg-amber-50'}`}>
                    {tx.type === 'earned'
                      ? <CheckCircle className="w-4 h-4 text-green-600" />
                      : <Clock className="w-4 h-4 text-amber-500" />
                    }
                  </div>
                  <div className="min-w-0">
                    <p className="text-sm text-gray-800 truncate">{tx.label}</p>
                    <p className="text-xs text-gray-400">
                      {new Date(tx.date).toLocaleDateString('en-IN', {
                        day: 'numeric', month: 'short', year: 'numeric',
                      })}
                    </p>
                  </div>
                </div>
                <div className="ml-4 shrink-0 text-right">
                  <p className={`text-sm font-bold ${tx.type === 'earned' ? 'text-green-600' : 'text-amber-500'}`}>
                    {tx.type === 'earned' ? '+' : '~'}{tx.points} pts
                  </p>
                  <p className="text-xs text-gray-400">{tx.type === 'earned' ? 'Earned' : 'Pending'}</p>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>

    </div>
  )
}
