import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import api from '../api/client'
import Navbar from '../components/Navbar'
import StatusBadge from '../components/StatusBadge'

interface Log {
  eventType: string
  message: string
  createdAt: string
}

interface Appointment {
  id: string
  doctorName: string
  hospitalName: string
  specialization: string
  slotStart: string
  slotEnd: string
  status: string
  notes: string
  createdAt: string
  logs: Log[]
}

const logIcons: Record<string, string> = {
  BOOKED: '📋',
  NOTIFICATION_SENT: '🔔',
  CONFIRMED: '✅',
  CANCELLED: '❌',
}

export default function AppointmentDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [appointment, setAppointment] = useState<Appointment | null>(null)
  const [loading, setLoading] = useState(true)
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null)

  async function fetchAppointment() {
    try {
      const { data } = await api.get(`/api/appointments/${id}`)
      setAppointment(data)
      if (data.status !== 'PENDING') {
        if (pollRef.current) clearInterval(pollRef.current)
      }
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchAppointment()
    pollRef.current = setInterval(fetchAppointment, 2000)
    return () => { if (pollRef.current) clearInterval(pollRef.current) }
  }, [id])

  // Stop polling once confirmed or cancelled
  useEffect(() => {
    if (appointment && appointment.status !== 'PENDING') {
      if (pollRef.current) clearInterval(pollRef.current)
    }
  }, [appointment?.status])

  async function handleCancel() {
    if (!confirm('Cancel this appointment?')) return
    await api.delete(`/api/appointments/${id}`)
    fetchAppointment()
  }

  function formatTime(iso: string) {
    const timePart = iso.split('T')[1] ?? iso
    const [h, m] = timePart.split(':').map(Number)
    const ampm = h >= 12 ? 'PM' : 'AM'
    const h12 = h % 12 || 12
    return `${h12}:${String(m).padStart(2, '0')} ${ampm}`
  }

  function formatDateTime(iso: string) {
    const [datePart] = iso.split('T')
    const [year, month, day] = datePart.split('-').map(Number)
    const monthNames = ['January','February','March','April','May','June','July','August','September','October','November','December']
    const dayNames = ['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday']
    const weekday = dayNames[new Date(year, month - 1, day).getDay()]
    return `${weekday}, ${day} ${monthNames[month - 1]} ${year} at ${formatTime(iso)}`
  }

  if (loading) {
    return (
      <div className="min-h-screen">
        <Navbar />
        <div className="flex items-center justify-center py-20 text-gray-400">Loading...</div>
      </div>
    )
  }

  if (!appointment) {
    return (
      <div className="min-h-screen">
        <Navbar />
        <div className="flex items-center justify-center py-20 text-gray-400">Appointment not found.</div>
      </div>
    )
  }

  return (
    <div className="min-h-screen">
      <Navbar />
      <div className="max-w-2xl mx-auto px-4 py-8">
        <button onClick={() => navigate('/dashboard')} className="text-sm text-gray-500 hover:text-navy-700 mb-6 flex items-center gap-1">
          ← Back to Dashboard
        </button>

        {/* Main card */}
        <div className="bg-white rounded-2xl shadow-sm p-6 mb-4">
          <div className="flex items-start justify-between mb-4">
            <div>
              <h1 className="text-xl font-bold text-navy-700">{appointment.doctorName}</h1>
              <p className="text-sm text-gray-500">{appointment.hospitalName} · {appointment.specialization}</p>
            </div>
            <StatusBadge status={appointment.status} />
          </div>

          <div className="border-t border-gray-100 pt-4 space-y-2 text-sm text-gray-600">
            <p>📅 {formatDateTime(appointment.slotStart)}</p>
            <p>⏱ {formatTime(appointment.slotStart)} – {formatTime(appointment.slotEnd)}</p>
            {appointment.notes && <p>📝 {appointment.notes}</p>}
          </div>

          {/* Live PENDING indicator */}
          {appointment.status === 'PENDING' && (
            <div className="mt-4 bg-yellow-50 border border-yellow-200 rounded-xl px-4 py-3 flex items-center gap-3">
              <div className="w-3 h-3 rounded-full bg-yellow-400 animate-pulse shrink-0" />
              <p className="text-sm text-yellow-700">Waiting for confirmation from our system...</p>
            </div>
          )}

          {appointment.status === 'CONFIRMED' && (
            <div className="mt-4 bg-green-50 border border-green-200 rounded-xl px-4 py-3 flex items-center gap-3">
              <span className="text-green-600 text-lg">✅</span>
              <p className="text-sm text-green-700 font-medium">Your appointment is confirmed!</p>
            </div>
          )}

          {appointment.status !== 'CANCELLED' && (
            <button
              onClick={handleCancel}
              className="mt-4 text-sm border border-red-300 text-red-500 px-5 py-2 rounded-full hover:bg-red-50 transition-colors"
            >
              Cancel Appointment
            </button>
          )}
        </div>

        {/* Event timeline */}
        <div className="bg-white rounded-2xl shadow-sm p-6">
          <h2 className="font-semibold text-navy-700 mb-4 text-sm uppercase tracking-wide">Event Log</h2>
          {appointment.logs.length === 0 && (
            <p className="text-gray-400 text-sm">No events yet.</p>
          )}
          <div className="space-y-4">
            {appointment.logs.map((log, i) => (
              <div key={i} className="flex gap-3">
                <div className="flex flex-col items-center">
                  <div className="w-8 h-8 rounded-full bg-gray-50 border border-gray-200 flex items-center justify-center text-sm">
                    {logIcons[log.eventType] || '•'}
                  </div>
                  {i < appointment.logs.length - 1 && <div className="w-px flex-1 bg-gray-200 my-1" />}
                </div>
                <div className="pb-2">
                  <p className="text-sm font-medium text-gray-700">{log.eventType}</p>
                  <p className="text-xs text-gray-500">{log.message}</p>
                  <p className="text-xs text-gray-400 mt-0.5">
                    {new Date(log.createdAt).toLocaleString('en-IN')}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
