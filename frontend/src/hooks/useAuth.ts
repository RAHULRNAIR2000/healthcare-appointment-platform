export function getToken(): string | null {
  return localStorage.getItem('token')
}

export function getUser(): { name: string; email: string } | null {
  const raw = localStorage.getItem('user')
  return raw ? JSON.parse(raw) : null
}

export function saveAuth(token: string, name: string, email: string) {
  localStorage.setItem('token', token)
  localStorage.setItem('user', JSON.stringify({ name, email }))
}

export function clearAuth() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
}

export function isLoggedIn(): boolean {
  return !!getToken()
}
