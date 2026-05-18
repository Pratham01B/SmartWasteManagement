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
