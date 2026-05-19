// ============================================================
// Shared TypeScript types
// ============================================================

export type Role = 'ADMIN' | 'CITIZEN' | 'WORKER' | 'RECYCLER'

export interface User {
  userId: number
  email: string
  fullName: string
  role: Role
  rewardPoints: number
}

export interface UserProfile {
  userId: number
  email: string
  fullName: string
  phoneNumber?: string
  city?: string
  pincode?: string
  role: Role
  rewardPoints: number
  createdAt: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  userId: number
  email: string
  fullName: string
  role: Role
  rewardPoints: number
}

export type ComplaintStatus = 'PENDING' | 'ASSIGNED' | 'IN_PROGRESS' | 'RESOLVED' | 'REJECTED'
export type WasteType = 'ORGANIC' | 'PLASTIC' | 'ELECTRONIC' | 'HAZARDOUS' | 'CONSTRUCTION' | 'MIXED'
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'

export interface Complaint {
  id: number
  title: string
  description: string
  address: string
  latitude?: number
  longitude?: number
  pincode?: string
  wasteType?: WasteType
  status: ComplaintStatus
  priority: Priority
  imageUrl?: string
  rewardPointsAwarded: number
  citizenId: number
  citizenName: string
  assignedWorkerId?: number
  assignedWorkerName?: string
  createdAt: string
  resolvedAt?: string
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface DashboardStats {
  totalComplaints: number
  pendingComplaints: number
  resolvedComplaints: number
  complaintsToday: number
  resolutionRate: number
  totalCitizens: number
  totalWorkers: number
  activeWorkers: number
  activeRoutes: number
  completedRoutesToday: number
  wasteTypeBreakdown: Record<string, number>
  statusBreakdown: Record<string, number>
}

export interface Worker {
  id: number
  fullName: string
  email: string
  phoneNumber?: string
  city?: string
  pincode?: string
  isActive: boolean
}

export type RouteStatus = 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'

export interface CollectionRoute {
  id: number
  workerId: number
  workerName: string
  routeName: string
  scheduledDate: string
  areaName?: string
  pincode?: string
  estimatedDistanceKm?: number
  estimatedDurationMin?: number
  status: RouteStatus
  startedAt?: string
  completedAt?: string
  createdAt: string
}

export type MaterialType = 'PLASTIC' | 'PAPER' | 'METAL' | 'GLASS' | 'ELECTRONIC' | 'RUBBER' | 'TEXTILE' | 'OTHER'
export type ListingStatus = 'ACTIVE' | 'SOLD' | 'EXPIRED' | 'CANCELLED'

export interface MarketplaceListing {
  id: number
  sellerId: number
  sellerName: string
  title: string
  description?: string
  materialType: MaterialType
  quantityKg: number
  pricePerKg: number
  totalPrice: number
  city?: string
  pincode?: string
  imageUrl?: string
  status: ListingStatus
  createdAt: string
}
