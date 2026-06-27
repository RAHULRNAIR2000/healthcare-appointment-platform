type Status = 'PENDING' | 'CONFIRMED' | 'CANCELLED'

const styles: Record<Status, string> = {
  PENDING: 'bg-yellow-100 text-yellow-700 border border-yellow-300',
  CONFIRMED: 'bg-green-100 text-green-700 border border-green-300',
  CANCELLED: 'bg-gray-100 text-gray-500 border border-gray-300',
}

export default function StatusBadge({ status }: { status: string }) {
  const s = status as Status
  return (
    <span className={`text-xs font-semibold px-2.5 py-1 rounded-full ${styles[s] ?? 'bg-gray-100 text-gray-500'}`}>
      {status}
    </span>
  )
}
