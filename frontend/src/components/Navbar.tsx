import { Link, useNavigate } from 'react-router-dom'
import { clearAuth, getUser } from '../hooks/useAuth'

export default function Navbar() {
  const navigate = useNavigate()
  const user = getUser()

  function handleLogout() {
    clearAuth()
    navigate('/login')
  }

  return (
    <nav className="bg-white/80 backdrop-blur-sm shadow-sm sticky top-0 z-50">
      <div className="max-w-6xl mx-auto px-4 py-3 flex items-center justify-between">
        <Link to="/dashboard" className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-full bg-navy-700 flex items-center justify-center">
            <span className="text-white text-xs font-bold">H</span>
          </div>
          <span className="font-semibold text-navy-700 text-lg">HealthCare</span>
        </Link>

        <div className="flex items-center gap-4">
          <span className="text-gray-500 text-sm hidden sm:block">
            Hello, <span className="text-navy-700 font-medium">{user?.name}</span>
          </span>
          <Link
            to="/book"
            className="bg-navy-700 text-white text-sm px-4 py-2 rounded-full hover:bg-navy-800 transition-colors"
          >
            + Book Appointment
          </Link>
          <button
            onClick={handleLogout}
            className="text-gray-500 text-sm hover:text-red-500 transition-colors"
          >
            Logout
          </button>
        </div>
      </div>
    </nav>
  )
}
