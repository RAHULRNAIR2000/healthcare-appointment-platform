import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/client'
import Navbar from '../components/Navbar'

interface Doctor {
  id: string
  name: string
  hospitalName: string
  specialization: string
  availableFrom: string
  availableTo: string
  slotDurationMinutes: number
}

interface Slot {
  slotStart: string
  slotEnd: string
}

type Step = 1 | 2 | 3

export default function BookPage() {
  const navigate = useNavigate()
  const [step, setStep] = useState<Step>(1)
  const [doctors, setDoctors] = useState<Doctor[]>([])
  const [selectedDoctor, setSelectedDoctor] = useState<Doctor | null>(null)
  const [selectedDate, setSelectedDate] = useState<string>('')
  const [slots, setSlots] = useState<Slot[]>([])
  const [selectedSlot, setSelectedSlot] = useState<Slot | null>(null)
  const [notes, setNotes] = useState('')
  const [loading, setLoading] = useState(false)
  const [slotsLoading, setSlotsLoading] = useState(false)
  const [error, setError] = useState('')

  // Calendar state
  const today = new Date()
  const [calMonth, setCalMonth] = useState(today.getMonth())
  const [calYear, setCalYear] = useState(today.getFullYear())

  useEffect(() => {
    api.get('/api/doctors').then(({ data }) => setDoctors(data))
  }, [])

  useEffect(() => {
    if (!selectedDoctor || !selectedDate) return
    setSlotsLoading(true)
    api.get(`/api/slots?doctorId=${selectedDoctor.id}&date=${selectedDate}`)
      .then(({ data }) => setSlots(data))
      .finally(() => setSlotsLoading(false))
  }, [selectedDoctor, selectedDate])

  async function handleConfirm() {
    if (!selectedDoctor || !selectedSlot) return
    setLoading(true)
    setError('')
    try {
      const { data } = await api.post('/api/appointments', {
        doctorId: selectedDoctor.id,
        slotStart: selectedSlot.slotStart,
        notes,
      })
      navigate(`/appointments/${data.id}`)
    } catch (err: any) {
      setError(err.response?.data?.error || 'Booking failed')
      setLoading(false)
    }
  }

  // --- Calendar helpers ---
  function getDaysInMonth(month: number, year: number) {
    return new Date(year, month + 1, 0).getDate()
  }
  function getFirstDayOfMonth(month: number, year: number) {
    return new Date(year, month, 1).getDay()
  }
  function toDateStr(day: number) {
    return `${calYear}-${String(calMonth + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  }
  function isPast(day: number) {
    const d = new Date(calYear, calMonth, day)
    d.setHours(0, 0, 0, 0)
    const t = new Date(); t.setHours(0, 0, 0, 0)
    return d < t
  }

  const monthNames = ['January','February','March','April','May','June','July','August','September','October','November','December']
  const daysInMonth = getDaysInMonth(calMonth, calYear)
  const firstDay = getFirstDayOfMonth(calMonth, calYear)

  function prevMonth() {
    if (calMonth === 0) { setCalMonth(11); setCalYear(y => y - 1) }
    else setCalMonth(m => m - 1)
  }
  function nextMonth() {
    if (calMonth === 11) { setCalMonth(0); setCalYear(y => y + 1) }
    else setCalMonth(m => m + 1)
  }

  function formatTime(iso: string) {
    const timePart = iso.split('T')[1] ?? iso
    const [h, m] = timePart.split(':').map(Number)
    const ampm = h >= 12 ? 'PM' : 'AM'
    const h12 = h % 12 || 12
    return `${h12}:${String(m).padStart(2, '0')} ${ampm}`
  }

  return (
    <div className="min-h-screen">
      <Navbar />
      <div className="max-w-4xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold text-navy-700 mb-2">Book an Appointment</h1>

        {/* Step indicator */}
        <div className="flex items-center gap-2 mb-8">
          {(['Pick Doctor', 'Pick Date', 'Pick Slot'] as const).map((label, i) => (
            <div key={i} className="flex items-center gap-2">
              <div className={`w-7 h-7 rounded-full flex items-center justify-center text-sm font-semibold ${step > i + 1 ? 'bg-green-500 text-white' : step === i + 1 ? 'bg-navy-700 text-white' : 'bg-gray-200 text-gray-400'}`}>
                {step > i + 1 ? '✓' : i + 1}
              </div>
              <span className={`text-sm hidden sm:block ${step === i + 1 ? 'text-navy-700 font-medium' : 'text-gray-400'}`}>{label}</span>
              {i < 2 && <div className="w-6 h-px bg-gray-200 mx-1" />}
            </div>
          ))}
        </div>

        {/* STEP 1: Pick Doctor */}
        {step === 1 && (
          <div>
            <p className="text-gray-500 text-sm mb-4">Select a doctor to continue</p>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {doctors.map((doc) => (
                <div
                  key={doc.id}
                  onClick={() => setSelectedDoctor(doc)}
                  className={`bg-white rounded-2xl p-5 cursor-pointer border-2 transition-all ${selectedDoctor?.id === doc.id ? 'border-navy-700 shadow-md' : 'border-transparent shadow-sm hover:shadow-md'}`}
                >
                  <div className="flex items-center gap-3 mb-2">
                    <div className="w-10 h-10 rounded-full bg-gradient-to-br from-purple-100 to-blue-100 flex items-center justify-center text-lg">
                      🩺
                    </div>
                    <div>
                      <h3 className="font-semibold text-navy-700 text-sm">{doc.name}</h3>
                      <p className="text-xs text-gray-500">{doc.specialization}</p>
                    </div>
                  </div>
                  <p className="text-xs text-gray-400">{doc.hospitalName}</p>
                  <p className="text-xs text-gray-400 mt-1">⏰ {doc.availableFrom} – {doc.availableTo} · {doc.slotDurationMinutes}min slots</p>
                </div>
              ))}
            </div>
            <div className="mt-6 flex justify-end">
              <button
                disabled={!selectedDoctor}
                onClick={() => setStep(2)}
                className="bg-navy-700 text-white px-8 py-2.5 rounded-full font-medium hover:bg-navy-800 transition-colors disabled:opacity-40"
              >
                Next →
              </button>
            </div>
          </div>
        )}

        {/* STEP 2: Pick Date */}
        {step === 2 && (
          <div>
            <p className="text-gray-500 text-sm mb-4">
              Booking with <span className="text-navy-700 font-medium">{selectedDoctor?.name}</span> — select a date
            </p>
            <div className="bg-white rounded-2xl shadow-sm p-6 max-w-sm">
              <div className="flex items-center justify-between mb-4">
                <button onClick={prevMonth} className="text-gray-400 hover:text-navy-700 text-lg px-2">‹</button>
                <span className="font-semibold text-navy-700">{monthNames[calMonth]} {calYear}</span>
                <button onClick={nextMonth} className="text-gray-400 hover:text-navy-700 text-lg px-2">›</button>
              </div>
              <div className="grid grid-cols-7 gap-1 mb-2">
                {['Su','Mo','Tu','We','Th','Fr','Sa'].map(d => (
                  <div key={d} className="text-center text-xs text-gray-400 font-medium py-1">{d}</div>
                ))}
              </div>
              <div className="grid grid-cols-7 gap-1">
                {Array.from({ length: firstDay }).map((_, i) => <div key={`e${i}`} />)}
                {Array.from({ length: daysInMonth }, (_, i) => i + 1).map(day => {
                  const dateStr = toDateStr(day)
                  const past = isPast(day)
                  const selected = selectedDate === dateStr
                  return (
                    <button
                      key={day}
                      disabled={past}
                      onClick={() => { setSelectedDate(dateStr); setSelectedSlot(null) }}
                      className={`aspect-square rounded-full text-sm font-medium transition-colors
                        ${past ? 'text-gray-300 cursor-not-allowed' : ''}
                        ${selected ? 'bg-navy-700 text-white' : !past ? 'hover:bg-blue-50 text-gray-700' : ''}
                      `}
                    >
                      {day}
                    </button>
                  )
                })}
              </div>
            </div>
            <div className="mt-6 flex justify-between">
              <button onClick={() => setStep(1)} className="text-sm text-gray-500 hover:text-navy-700">← Back</button>
              <button
                disabled={!selectedDate}
                onClick={() => setStep(3)}
                className="bg-navy-700 text-white px-8 py-2.5 rounded-full font-medium hover:bg-navy-800 transition-colors disabled:opacity-40"
              >
                Next →
              </button>
            </div>
          </div>
        )}

        {/* STEP 3: Pick Slot */}
        {step === 3 && (
          <div>
            <p className="text-gray-500 text-sm mb-4">
              <span className="text-navy-700 font-medium">{selectedDoctor?.name}</span> · {selectedDate} — pick a time slot
            </p>
            <div className="flex gap-6 flex-col sm:flex-row">
              <div className="flex-1">
                {slotsLoading && <p className="text-gray-400 text-sm">Loading slots...</p>}
                {!slotsLoading && slots.length === 0 && (
                  <div className="bg-white rounded-2xl p-6 text-center text-gray-400 text-sm shadow-sm">
                    No available slots on this date.<br/>
                    <button onClick={() => setStep(2)} className="text-navy-700 mt-2 hover:underline text-sm">Pick another date</button>
                  </div>
                )}
                <div className="grid grid-cols-2 gap-2 max-h-80 overflow-y-auto pr-1">
                  {slots.map((slot) => {
                    const selected = selectedSlot?.slotStart === slot.slotStart
                    return (
                      <button
                        key={slot.slotStart}
                        onClick={() => setSelectedSlot(slot)}
                        className={`py-3 rounded-xl text-sm font-medium border-2 transition-colors
                          ${selected ? 'bg-navy-700 text-white border-navy-700' : 'bg-white text-navy-700 border-gray-200 hover:border-navy-700'}`}
                      >
                        {formatTime(slot.slotStart)}
                      </button>
                    )
                  })}
                </div>
              </div>

              {selectedSlot && (
                <div className="bg-white rounded-2xl shadow-sm p-5 sm:w-64">
                  <h4 className="font-semibold text-navy-700 mb-3 text-sm">Confirm Booking</h4>
                  <div className="text-xs text-gray-500 space-y-1 mb-4">
                    <p>👨‍⚕️ {selectedDoctor?.name}</p>
                    <p>🏥 {selectedDoctor?.hospitalName}</p>
                    <p>📅 {selectedDate}</p>
                    <p>⏰ {formatTime(selectedSlot.slotStart)} – {formatTime(selectedSlot.slotEnd)}</p>
                  </div>
                  <textarea
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                    placeholder="Notes (optional)"
                    rows={2}
                    className="w-full border border-gray-200 rounded-xl px-3 py-2 text-xs focus:outline-none focus:ring-2 focus:ring-navy-700/30 mb-3 resize-none"
                  />
                  {error && <p className="text-red-500 text-xs mb-2">{error}</p>}
                  <button
                    onClick={handleConfirm}
                    disabled={loading}
                    className="w-full bg-navy-700 text-white py-2.5 rounded-full text-sm font-medium hover:bg-navy-800 transition-colors disabled:opacity-60"
                  >
                    {loading ? 'Booking...' : 'Confirm Booking'}
                  </button>
                </div>
              )}
            </div>
            <div className="mt-6">
              <button onClick={() => setStep(2)} className="text-sm text-gray-500 hover:text-navy-700">← Back</button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
