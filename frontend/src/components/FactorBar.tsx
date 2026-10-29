import type { Factor } from '../types/api'

export default function FactorBar({ factor }: { factor: Factor }) {
  const pushesToFraud = factor.c > 0
  const width = Math.min(Math.abs(factor.c) * 200, 100)

  return (
    <div className="my-2 grid grid-cols-[1fr_1fr_3rem] items-center gap-3 text-sm">
      <span className="truncate font-mono text-xs">{factor.f}</span>
      <div className="h-2 overflow-hidden rounded bg-line">
        <div
          className={`h-full ${pushesToFraud ? 'bg-bad' : 'bg-ok'}`}
          style={{ width: `${width}%` }}
        />
      </div>
      <span className={`text-right font-mono text-xs ${pushesToFraud ? 'text-bad' : 'text-ok'}`}>
        {pushesToFraud ? '+' : ''}
        {factor.c.toFixed(2)}
      </span>
    </div>
  )
}