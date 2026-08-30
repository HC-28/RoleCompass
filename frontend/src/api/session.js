import { apiClient } from './client'

export const startSession = async () => {
  const { data } = await apiClient.post('/api/session/start')
  return data
}

export const submitAnswers = async (sessionId, payload) => {
  const { data } = await apiClient.post(`/api/session/${sessionId}/answers`, payload)
  return data
}

export const predict = async (sessionId) => {
  const { data } = await apiClient.post(`/api/session/${sessionId}/predict`)
  return data
}

export const getProfile = async () => {
  const { data } = await apiClient.get('/api/session/profile')
  return data
}
