import api from './axios'
import type { AuthResponse, UserProfile } from '../types'

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
    api.post<AuthResponse>('/auth/register', data).then((r: { data: any }) => r.data),

  login: (data: LoginPayload) =>
    api.post<AuthResponse>('/auth/login', data).then((r: { data: any }) => r.data),

  getMe: () =>
    api.get<UserProfile>('/auth/me').then((r) => r.data),

  forgotPassword: (email: string) =>
    api.post('/auth/forgot-password', { email }).then((r) => r.data),

  resetPassword: (token: string, newPassword: string) =>
    api.post('/auth/reset-password', { token, newPassword }).then((r) => r.data),
}
