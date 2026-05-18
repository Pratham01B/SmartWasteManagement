import api from './axios'
import type { Complaint, Page } from '../types'

export const complaintsApi = {
  create: (data: Partial<Complaint>) =>
    api.post<Complaint>('/complaints', data).then((r) => r.data),

  getAll: (page = 0, size = 20) =>
    api.get<Page<Complaint>>(`/complaints?page=${page}&size=${size}`).then((r) => r.data),

  getMy: (page = 0) =>
    api.get<Page<Complaint>>(`/complaints/my?page=${page}`).then((r) => r.data),

  getWorkerComplaints: (page = 0) =>
    api.get<Page<Complaint>>(`/complaints/worker?page=${page}`).then((r) => r.data),

  getById: (id: number) =>
    api.get<Complaint>(`/complaints/${id}`).then((r) => r.data),

  assign: (complaintId: number, workerId: number) =>
    api.patch<Complaint>(`/complaints/${complaintId}/assign/${workerId}`).then((r) => r.data),

  updateStatus: (complaintId: number, status: string) =>
    api.patch<Complaint>(`/complaints/${complaintId}/status?status=${status}`).then((r) => r.data),
}
