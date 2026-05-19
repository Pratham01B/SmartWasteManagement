import api from './axios'
import type { MarketplaceListing, MaterialType, ListingStatus, Page } from '../types'

export interface CreateListingPayload {
  title: string
  description?: string
  materialType: MaterialType
  quantityKg: number
  pricePerKg: number
  city?: string
  pincode?: string
  imageUrl?: string
}

export const marketplaceApi = {
  getAll: (page = 0, size = 12, materialType?: MaterialType) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (materialType) params.append('materialType', materialType)
    return api.get<Page<MarketplaceListing>>(`/marketplace?${params}`).then((r) => r.data)
  },

  getById: (id: number) =>
    api.get<MarketplaceListing>(`/marketplace/${id}`).then((r) => r.data),

  getMy: (page = 0) =>
    api.get<Page<MarketplaceListing>>(`/marketplace/my?page=${page}`).then((r) => r.data),

  create: (data: CreateListingPayload) =>
    api.post<MarketplaceListing>('/marketplace', data).then((r) => r.data),

  update: (id: number, data: CreateListingPayload) =>
    api.put<MarketplaceListing>(`/marketplace/${id}`, data).then((r) => r.data),

  updateStatus: (id: number, status: ListingStatus) =>
    api.patch<MarketplaceListing>(`/marketplace/${id}/status?status=${status}`).then((r) => r.data),

  delete: (id: number) =>
    api.delete(`/marketplace/${id}`),
}
