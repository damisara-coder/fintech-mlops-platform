import type { Decision } from '../types'

const styles: Record<Decision, string> = {
  APPROVED: 'bg-ok/15 text-ok',
  BLOCKED: 'bg-bad/15 text-bad',
  HUMAN_REVIEW: 'bg-warn/15 text-warn',
}

const labels: Record<Decision, string> = {
  APPROVED: 'Approved',
  BLOCKED: 'Blocked',
  HUMAN_REVIEW: 'Human review',
}

export default function StatusBadge({ status }: { status: Decision }) {
  return (
    <span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${styles[status]}`}>
      {labels[status]}
    </span>
  )
}