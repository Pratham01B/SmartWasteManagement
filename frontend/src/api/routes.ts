import api from './axios'
import type { CollectionRoute, RouteStatus } from '../types'

export interface CreateRoutePayload {
  workerId: number
  routeName: string
  scheduledDate: string
  areaName?: string
  pincode?: string
  estimatedDistanceKm?: number
  estimatedDurationMin?: number
}

export const routesApi = {
  getAll: () =>
    api.get<CollectionRoute[]>('/routes').then((r) => r.data),

  getByWorker: (workerId: number) =>
    api.get<CollectionRoute[]>(`/routes/worker/${workerId}`).then((r) => r.data),

  create: (data: CreateRoutePayload) =>
    api.post<CollectionRoute>('/routes', data).then((r) => r.data),

  updateStatus: (id: number, status: RouteStatus) =>
    api.patch<CollectionRoute>(`/routes/${id}/status?status=${status}`).then((r) => r.data),

  delete: (id: number) =>
    api.delete(`/routes/${id}`),
}
