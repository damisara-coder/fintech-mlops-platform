import { useEffect, useState } from 'react'
import FactorBar from '../components/FactorBar'
import { getReviewQueue, submitReview } from '../services/reviewApi'
import type { ReviewAction, ReviewItem } from '../types/api'

const byUncertainty = (a: ReviewItem, b: ReviewItem) => b.uncertainty - a.uncertainty

const verbs: Record<ReviewAction, string> = {
  APPROVED: 'approuvée',
  REJECTED: 'rejetée (fraude)',
  ESCALATED: 'escaladée',
}

export default function ReviewQueue() {
  const [items, setItems] = useState<ReviewItem[]>([])
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [toast, setToast] = useState<{ text: string; error: boolean } | null>(null)

  useEffect(() => {
    getReviewQueue().then((data) => {
      setItems(data)
      setSelectedId(data[0]?.tx_id ?? null)
      setLoading(false)
    })
  }, [])

  // Le message disparaît tout seul après 3 secondes
  useEffect(() => {
    if (!toast) return
    const timer = setTimeout(() => setToast(null), 3000)
    return () => clearTimeout(timer)
  }, [toast])

  const selected = items.find((i) => i.tx_id === selectedId)

  async function handleAction(action: ReviewAction) {
    if (!selected) return
    const item = selected
    const index = items.findIndex((i) => i.tx_id === item.tx_id)

    // 1. On retire la ligne tout de suite (optimistic update)
    const remaining = items.filter((i) => i.tx_id !== item.tx_id)
    setItems(remaining)
    setSelectedId(remaining[index]?.tx_id ?? remaining[index - 1]?.tx_id ?? null)

    try {
      // 2. On envoie la décision au serveur
      await submitReview(item.tx_id, action)
      setToast({ text: `${item.tx_id} ${verbs[action]}`, error: false })
    } catch {
      // 3. Échec : on remet la ligne et on prévient l'analyste
      setItems((current) => [...current, item].sort(byUncertainty))
      setSelectedId(item.tx_id)
      setToast({ text: `Erreur réseau : ${item.tx_id} remise dans la file. Réessaie.`, error: true })
    }
  }

  if (loading) {
    return <p className="text-mute">Chargement de la file...</p>
  }

  return (
    <div>
      <h1 className="text-xl font-bold">Human Review Queue</h1>
      <p className="mb-5 text-mute">
        Transactions sur lesquelles le modèle refuse de décider, les plus incertaines en premier.
      </p>

      <div className="grid gap-4 md:grid-cols-[1fr_1.6fr]">
        {/* Colonne de gauche : la liste */}
        <div>
          <h2 className="mb-2 text-sm font-semibold text-mute">
            File d'attente ({items.length})
          </h2>
          {items.map((item) => (
            <button
              key={item.tx_id}
              onClick={() => setSelectedId(item.tx_id)}
              className={`mb-2 block w-full rounded-xl border p-3 text-left ${
                item.tx_id === selectedId
                  ? 'border-accent bg-accent/10'
                  : 'border-line bg-panel hover:border-mute'
              }`}
            >
              <div className="flex items-center justify-between gap-2">
                <span className="font-mono text-sm font-semibold">{item.tx_id}</span>
                <span className="rounded-full bg-warn/15 px-2 py-0.5 text-xs text-warn">
                  incert. {item.uncertainty.toFixed(2)}
                </span>
              </div>
              <div className="mt-1 text-sm">
                {item.client_name ?? 'n/a'}
                {item.amount !== undefined &&
                  `, ${item.amount.toLocaleString('fr-FR', {
                    style: 'currency',
                    currency: item.currency ?? 'EUR',
                  })}`}
              </div>
              <div className="text-xs text-mute">
                {[item.channel, item.context].filter(Boolean).join(', ')}
              </div>
            </button>
          ))}
          {items.length === 0 && (
            <div className="rounded-xl border border-line bg-panel p-6 text-center text-mute">
              La file est vide. Rien à revoir.
            </div>
          )}
        </div>

        {/* Colonne de droite : l'explication et les boutons */}
        <div className="rounded-xl border border-line bg-panel p-5">
          {!selected ? (
            <p className="text-mute">Choisis une transaction dans la liste.</p>
          ) : (
            <div>
              <div className="flex flex-wrap items-start justify-between gap-2">
                <div>
                  <div className="font-semibold">
                    {selected.tx_id}, {selected.client_name ?? 'client inconnu'}
                  </div>
                  <div className="text-xs text-mute">
                    Fraude estimée à{' '}
                    {selected.probability !== undefined
                      ? `${Math.round(selected.probability * 100)} %`
                      : 'n/a'}
                    , intervalle de confiance{' '}
                    {selected.interval
                      ? `[${selected.interval[0]} ; ${selected.interval[1]}]`
                      : 'n/a'}
                  </div>
                </div>
                <span className="font-mono text-xs text-cyan">{selected.model_version}</span>
              </div>

              <p className="mt-4 text-sm font-semibold">Pourquoi le modèle hésite-t-il ?</p>
              <p className="mb-2 text-xs text-mute">
                Rouge : pousse vers la fraude. Vert : pousse vers légitime.
              </p>
              {selected.factors.map((f) => (
                <FactorBar key={f.f} factor={f} />
              ))}

              <div className="mt-4 font-mono text-xs text-mute">
                Historique du client : {selected.history ?? 'n/a'}
              </div>

              <div className="mt-5 flex flex-wrap gap-3">
                <button
                  onClick={() => handleAction('APPROVED')}
                  className="rounded-lg bg-ok px-5 py-2.5 font-semibold text-white hover:opacity-90"
                >
                  Approuver
                </button>
                <button
                  onClick={() => handleAction('REJECTED')}
                  className="rounded-lg bg-bad px-5 py-2.5 font-semibold text-white hover:opacity-90"
                >
                  Rejeter (fraude)
                </button>
                <button
                  onClick={() => handleAction('ESCALATED')}
                  className="rounded-lg bg-line px-5 py-2.5 font-semibold text-white hover:opacity-90"
                >
                  Escalader
                </button>
              </div>
              <p className="mt-3 text-xs text-mute">
                Ta décision est enregistrée dans le journal d'audit et servira de label pour
                le prochain entraînement du modèle.
              </p>
            </div>
          )}
        </div>
      </div>

      {toast && (
        <div
          role="status"
          className={`fixed bottom-6 left-1/2 -translate-x-1/2 rounded-lg border px-4 py-2.5 text-sm ${
            toast.error ? 'border-bad bg-panel text-bad' : 'border-ok bg-panel text-ok'
          }`}
        >
          {toast.text}
        </div>
      )}
    </div>
  )
}