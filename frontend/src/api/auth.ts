import api from './axios'
import type { AuthResponse } from '../types'

export interface RegisterPayload {
  firstName: string
  lastName: string
  email: string
  password: string
  phoneNumber?: string
  role: string
  address?: string
  city?: string
  pincode?: string
}

export interface LoginPayload {
  email: string
  password: string
}

export const authApi = {
  register: (data: RegisterPayload) =>
    api.post<AuthResponse>('/auth/register', data).then((r) => r.data),

  login: (data: LoginPayload) =>
    api.post<AuthResponse>('/auth/login', data).then((r) => r.data),
}
