import { apiClient } from './client'

export const login = async (payload) => {
  const { data } = await apiClient.post('/api/auth/login', payload)
  return data
}

export const register = async (payload) => {
  const { data } = await apiClient.post('/api/auth/register', payload)
  return data
}
