import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/client'
import Navbar from '../components/Navbar'
import StatusBadge from '../components/StatusBadge'
import { getUser } from '../hooks/useAuth'

interface Appointment {
  id: string
  doctorName: string
  hospitalName: string
  specialization: string
  slotStart: string
  slotEnd: string
  status: string
  notes: string
}

const specializationIcon: Record<string, string> = {
  Cardiology: '❤️',
  Orthopedics: '🦴',
  Neurology: '🧠',
  Dermatology: '✨',
  Pediatrics: '👶',
}

function formatTime(iso: string) {
  const timePart = iso.split('T')[1] ?? iso
  const [h, m] = timePart.split(':').map(Number)
  const ampm = h >= 12 ? 'PM' : 'AM'
  const h12 = h % 12 || 12
  return `${h12}:${String(m).padStart(2, '0')} ${ampm}`
}

function formatDate(iso: string) {
  const [datePart] = iso.split('T')
  const [year, month, day] = datePart.split('-').map(Number)
  const monthNames = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec']
  const dayNames = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat']
  const weekday = dayNames[new Date(year, month - 1, day).getDay()]
  return { day, month: monthNames[month - 1], year, weekday }
}

export default function DashboardPage() {
  const navigate = useNavigate()
  const user = getUser()
  const [appointments, setAppointments] = useState<Appointment[]>([])
  const [loading, setLoading] = useState(true)

  async function fetchAppointments() {
    try {
      const { data } = await api.get('/api/appointments')
      setAppointments(data)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchAppointments() }, [])

  async function handleCancel(id: string) {
    if (!confirm('Cancel this appointment?')) return
    await api.delete(`/api/appointments/${id}`)
    fetchAppointments()
  }

  const confirmed = appointments.filter(a => a.status === 'CONFIRMED').length
  const pending = appointments.filter(a => a.status === 'PENDING').length
  const cancelled = appointments.filter(a => a.status === 'CANCELLED').length

  return (
    <div className="min-h-screen">
      <Navbar />

      {/* Hero banner */}
      <div className="max-w-5xl mx-auto px-6 pt-8 pb-2">
        <div
          className="rounded-3xl text-white px-8 py-8 relative overflow-hidden"
          style={{ background: 'linear-gradient(135deg, #1e2a4a 0%, #2d3f6b 60%, #3b4f82 100%)' }}
        >
          {/* decorative circles */}
          <div className="absolute -top-10 -right-10 w-48 h-48 rounded-full bg-white/5" />
          <div className="absolute -bottom-8 -right-4 w-32 h-32 rounded-full bg-white/5" />
          <div className="absolute top-4 right-32 w-16 h-16 rounded-full bg-white/5" />

          <div className="relative z-10">
            <p className="text-blue-300 text-sm mb-1 font-medium">Welcome back</p>
            <h1 className="text-3xl font-bold mb-7 tracking-tight">{user?.name} 👋</h1>

            <div className="grid grid-cols-3 gap-3">
              <div className="bg-white/10 rounded-2xl px-4 py-4 text-center border border-white/10 backdrop-blur-sm">
                <p className="text-4xl font-bold text-green-300">{confirmed}</p>
                <p className="text-xs text-blue-200 mt-1.5 font-medium uppercase tracking-wide">Confirmed</p>
              </div>
              <div className="bg-white/10 rounded-2xl px-4 py-4 text-center border border-white/10 backdrop-blur-sm">
                <p className="text-4xl font-bold text-yellow-300">{pending}</p>
                <p className="text-xs text-blue-200 mt-1.5 font-medium uppercase tracking-wide">Pending</p>
              </div>
              <div className="bg-white/10 rounded-2xl px-4 py-4 text-center border border-white/10 backdrop-blur-sm">
                <p className="text-4xl font-bold text-gray-300">{cancelled}</p>
                <p className="text-xs text-blue-200 mt-1.5 font-medium uppercase tracking-wide">Cancelled</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="max-w-5xl mx-auto px-6 py-8">
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-lg font-semibold text-navy-700">All Appointments</h2>
          <button
            onClick={() => navigate('/book')}
            className="bg-navy-700 text-white text-sm px-5 py-2 rounded-full hover:bg-navy-800 transition-colors"
          >
            + New Appointment
          </button>
        </div>

        {loading && (
          <div className="text-center py-20 text-gray-400">Loading...</div>
        )}

        {!loading && appointments.length === 0 && (
          <div className="bg-white rounded-2xl shadow-sm p-14 text-center">
            <div className="text-5xl mb-4">📅</div>
            <h3 className="text-lg font-semibold text-gray-700 mb-2">No appointments yet</h3>
            <p className="text-gray-400 text-sm mb-6">Book your first appointment with one of our doctors</p>
            <button
              onClick={() => navigate('/book')}
              className="bg-navy-700 text-white px-6 py-2.5 rounded-full text-sm hover:bg-navy-800 transition-colors"
            >
              Book Now
            </button>
          </div>
        )}

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {appointments.map((appt) => {
            const { day, month, weekday } = formatDate(appt.slotStart)
            const icon = specializationIcon[appt.specialization] ?? '🩺'
            return (
              <div
                key={appt.id}
                className={`bg-white rounded-2xl shadow-sm overflow-hidden border-l-4 ${
                  appt.status === 'CONFIRMED' ? 'border-green-400' :
                  appt.status === 'PENDING'   ? 'border-yellow-400' :
                                                'border-gray-200'
                }`}
              >
                <div className="p-5">
                  <div className="flex items-start justify-between mb-3">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-purple-50 to-blue-50 flex items-center justify-center text-xl shrink-0">
                        {icon}
                      </div>
                      <div>
                        <h3 className="font-semibold text-navy-700 text-sm leading-tight">{appt.doctorName}</h3>
                        <p className="text-xs text-gray-400">{appt.specialization}</p>
                      </div>
                    </div>
                    <StatusBadge status={appt.status} />
                  </div>

                  <div className="flex items-center gap-3 bg-gray-50 rounded-xl px-3 py-2.5 mb-4">
                    <div className="text-center shrink-0">
                      <p className="text-xs text-gray-400 uppercase">{weekday}</p>
                      <p className="text-xl font-bold text-navy-700 leading-tight">{day}</p>
                      <p className="text-xs text-gray-400">{month}</p>
                    </div>
                    <div className="w-px h-10 bg-gray-200" />
                    <div>
                      <p className="text-sm font-medium text-navy-700">{formatTime(appt.slotStart)} – {formatTime(appt.slotEnd)}</p>
                      <p className="text-xs text-gray-400 truncate">{appt.hospitalName}</p>
                    </div>
                  </div>

                  {appt.notes && (
                    <p className="text-xs text-gray-400 italic mb-3 truncate">"{appt.notes}"</p>
                  )}

                  <div className="flex gap-2">
                    <button
                      onClick={() => navigate(`/appointments/${appt.id}`)}
                      className="flex-1 text-sm border border-navy-700 text-navy-700 py-1.5 rounded-full hover:bg-navy-700 hover:text-white transition-colors"
                    >
                      View Details
                    </button>
                    {appt.status !== 'CANCELLED' && (
                      <button
                        onClick={() => handleCancel(appt.id)}
                        className="text-sm border border-red-200 text-red-400 px-4 py-1.5 rounded-full hover:bg-red-50 transition-colors"
                      >
                        Cancel
                      </button>
                    )}
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      </div>
    </div>
  )
}
