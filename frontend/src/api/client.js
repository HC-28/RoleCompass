import axios from 'axios'

const TOKEN_KEY = 'rolecompass_token'

export const getStoredToken = () => sessionStorage.getItem(TOKEN_KEY)

export const setStoredToken = (token) => {
  sessionStorage.setItem(TOKEN_KEY, token)
}

export const clearStoredToken = () => {
  sessionStorage.removeItem(TOKEN_KEY)
}

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
})

apiClient.interceptors.request.use((config) => {
  const token = getStoredToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearStoredToken()
    }
    return Promise.reject(error)
  },
)
