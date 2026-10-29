export default function RiskBar({ value }: { value: number }) {
  const color = value >= 0.9 ? 'bg-bad' : value > 0.4 ? 'bg-warn' : 'bg-ok'
  return (
    <div className="flex items-center gap-2">
      <div className="h-1.5 w-20 overflow-hidden rounded bg-line">
        <div className={`h-full ${color}`} style={{ width: `${value * 100}%` }} />
      </div>
      <span className="font-mono text-xs text-mute">{Math.round(value * 100)} %</span>
    </div>
  )
}