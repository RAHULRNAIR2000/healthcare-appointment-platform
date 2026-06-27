import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api from '../api/client'
import { saveAuth } from '../hooks/useAuth'

export default function LoginPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const { data } = await api.post('/api/auth/login', form)
      saveAuth(data.token, data.name, data.email)
      navigate('/dashboard')
    } catch (err: any) {
      setError(err.response?.data?.error || 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex">

      {/* Left panel */}
      <div
        className="hidden lg:flex lg:w-1/2 flex-col justify-between p-12 relative overflow-hidden"
        style={{ background: 'linear-gradient(145deg, #1a237e 0%, #1565c0 50%, #1976d2 100%)' }}
      >
        {/* Decorative grid lines */}
        <svg className="absolute inset-0 w-full h-full opacity-10" xmlns="http://www.w3.org/2000/svg">
          <defs>
            <pattern id="grid" width="60" height="60" patternUnits="userSpaceOnUse">
              <path d="M 60 0 L 0 0 0 60" fill="none" stroke="white" strokeWidth="0.5"/>
            </pattern>
          </defs>
          <rect width="100%" height="100%" fill="url(#grid)" />
        </svg>

        {/* Decorative diagonal lines */}
        <svg className="absolute inset-0 w-full h-full opacity-10" xmlns="http://www.w3.org/2000/svg">
          <line x1="30%" y1="0" x2="100%" y2="80%" stroke="white" strokeWidth="0.8"/>
          <line x1="50%" y1="0" x2="110%" y2="70%" stroke="white" strokeWidth="0.8"/>
          <line x1="10%" y1="0" x2="80%" y2="100%" stroke="white" strokeWidth="0.8"/>
        </svg>

        {/* Logo */}
        <div className="relative z-10">
          <div className="w-12 h-12 rounded-2xl bg-white/20 backdrop-blur-sm flex items-center justify-center border border-white/30">
            <span className="text-white text-xl font-bold">+</span>
          </div>
        </div>

        {/* Main text */}
        <div className="relative z-10">
          <h1 className="text-5xl font-bold text-white leading-tight mb-6">
            Hello,<br />HealthCare! 👋
          </h1>
          <p className="text-blue-200 text-lg leading-relaxed max-w-sm">
            Book appointments with top doctors, track your health journey, and get instant confirmations — all in one place.
          </p>
        </div>

        {/* Footer */}
        <div className="relative z-10">
          <p className="text-blue-300 text-sm">© 2026 HealthCare Platform. All rights reserved.</p>
        </div>
      </div>

      {/* Right panel */}
      <div className="w-full lg:w-1/2 flex items-center justify-center px-8 py-12 bg-white">
        <div className="w-full max-w-sm">

          {/* Mobile logo */}
          <div className="lg:hidden flex items-center gap-2 mb-8">
            <div className="w-9 h-9 rounded-xl bg-navy-700 flex items-center justify-center">
              <span className="text-white font-bold">+</span>
            </div>
            <span className="font-bold text-navy-700 text-lg">HealthCare</span>
          </div>

          <h2 className="text-3xl font-bold text-gray-900 mb-1">Welcome Back!</h2>
          <p className="text-gray-400 text-sm mb-8">
            Don't have an account?{' '}
            <Link to="/register" className="text-blue-600 font-medium hover:underline">
              Create a new account now.
            </Link>
          </p>

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <input
                type="email"
                required
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                placeholder="Email address"
                className="w-full border-b-2 border-gray-200 focus:border-blue-600 outline-none py-3 text-sm text-gray-700 placeholder-gray-400 transition-colors bg-transparent"
              />
            </div>
            <div>
              <input
                type="password"
                required
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
                placeholder="Password"
                className="w-full border-b-2 border-gray-200 focus:border-blue-600 outline-none py-3 text-sm text-gray-700 placeholder-gray-400 transition-colors bg-transparent"
              />
            </div>

            {error && (
              <p className="text-red-500 text-xs bg-red-50 border border-red-100 rounded-lg px-3 py-2">{error}</p>
            )}

            <div className="pt-2 space-y-3">
              <button
                type="submit"
                disabled={loading}
                className="w-full py-3.5 rounded-xl font-semibold text-white text-sm transition-all disabled:opacity-60"
                style={{ background: 'linear-gradient(135deg, #1a237e, #1976d2)' }}
              >
                {loading ? 'Signing in...' : 'Login Now'}
              </button>
            </div>
          </form>
        </div>
      </div>

    </div>
  )
}
