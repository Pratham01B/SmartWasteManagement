import api from './axios'
import type { Worker } from '../types'

export const workersApi = {
  getAll: () =>
    api.get<Worker[]>('/workers').then((r) => r.data),

  getActive: () =>
    api.get<Worker[]>('/workers/active').then((r) => r.data),

  getById: (id: number) =>
    api.get<Worker>(`/workers/${id}`).then((r) => r.data),

  toggleStatus: (id: number) =>
    api.patch<Worker>(`/workers/${id}/toggle-status`).then((r) => r.data),
}
