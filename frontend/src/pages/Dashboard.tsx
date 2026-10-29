import KpiCard from '../components/KpiCard'
import RiskBar from '../components/RiskBar'
import StatusBadge from '../components/StatusBadge'
import { transactions } from '../mocks/transactions'

export default function Dashboard() {
  return (
    <div>
      <h1 className="text-xl font-bold">Fraud Operations Dashboard</h1>
      <p className="mb-5 text-mute">
        Données simulées. Modèle <span className="font-mono text-cyan">fraud-xgb v4.2</span>
      </p>

      <div className="mb-4 grid grid-cols-2 gap-3 md:grid-cols-5">
        <KpiCard label="Transactions (24 h)" value="184 392" />
        <KpiCard label="Blocked as fraud" value="1 204" color="text-bad" />
        <KpiCard label="Waiting for review" value="318" color="text-warn" />
        <KpiCard label="ML latency p95" value="42 ms" />
        <KpiCard label="Pods running" value="14/14" color="text-ok" />
      </div>

      <div className="rounded-xl border border-line bg-panel p-4">
        <h2 className="mb-3 font-semibold">Live transaction feed</h2>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-xs text-mute">
                <th className="p-2 font-medium">ID</th>
                <th className="p-2 font-medium">Client</th>
                <th className="p-2 font-medium">Amount</th>
                <th className="p-2 font-medium">Channel</th>
                <th className="p-2 font-medium">Risk</th>
                <th className="p-2 font-medium">Status</th>
                <th className="p-2 font-medium">Model</th>
              </tr>
            </thead>
            <tbody>
              {transactions.map((t) => (
                <tr key={t.id} className="border-t border-line">
                  <td className="p-2 font-mono text-xs">{t.id}</td>
                  <td className="p-2">{t.client}</td>
                  <td className="p-2">
                    {t.amount.toLocaleString('fr-FR', { style: 'currency', currency: 'EUR' })}
                  </td>
                  <td className="p-2">{t.channel}</td>
                  <td className="p-2"><RiskBar value={t.risk} /></td>
                  <td className="p-2"><StatusBadge status={t.status} /></td>
                  <td className="p-2 font-mono text-xs">{t.model}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}