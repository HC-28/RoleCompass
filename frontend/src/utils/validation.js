const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function validateEmail(email) {
  if (!email.trim()) {
    return 'Email is required'
  }
  if (!EMAIL_PATTERN.test(email.trim())) {
    return 'Enter a valid email address'
  }
  return null
}

export function validatePassword(password) {
  if (!password) {
    return 'Password is required'
  }
  if (password.length < 8) {
    return 'Password must be at least 8 characters'
  }
  return null
}

export function extractErrorMessage(error, fallback) {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = error.response
    return response?.data?.message ?? response?.data?.error ?? fallback
  }
  return fallback
}
