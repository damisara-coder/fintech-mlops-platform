import { useEffect, useState } from 'react'
import { transactions } from '../mocks/transactions'
import type { Decision, Transaction } from '../types'

export type FeedStatus = 'live' | 'reconnecting'

// Champs lus dans websocket-events.json (TX_DECIDED et QUEUE_UPDATED)
interface FeedEvent {
  type: string
  tx_id?: string
  client_name?: string
  amount?: string
  channel?: string
  decision?: Decision
  risk_score?: number
  model_version?: string
  pending?: number
}

const USE_MOCKS = import.meta.env.VITE_USE_MOCKS !== 'false'
const WS_URL = import.meta.env.VITE_WS_URL ?? 'ws://localhost:8080/ws/tx'
const MAX_ROWS = 12

const names = ['C. Moreau', 'A. Kowalski', 'L. Benali', 'D. Okonkwo', 'S. Haddad', 'M. Rossi', 'K. Tanaka']
const channels = ['Card : FR', 'Card : ES', 'SEPA : DE', 'Wire : SG', 'Card : IT']

function pick<T>(list: T[]): T {
  return list[Math.floor(Math.random() * list.length)]
}

// Le contrat envoie le montant en texte ("550.00 €") : on le remet en nombre
function parseAmount(text?: string): number {
  return Number(String(text ?? '0').replace(/[^\d.,-]/g, '').replace(',', '.')) || 0
}

function mockEvent(): FeedEvent {
  const r = Math.random()
  const risk = r < 0.75 ? Math.random() * 0.3 : r < 0.9 ? 0.5 + Math.random() * 0.3 : 0.9 + Math.random() * 0.09
  const decision: Decision = risk >= 0.9 ? 'BLOCKED' : risk > 0.4 ? 'HUMAN_REVIEW' : 'APPROVED'
  return {
    type: 'TX_DECIDED',
    tx_id: `TX-${Math.floor(Math.random() * 0xfffff).toString(16).toUpperCase()}`,
    client_name: pick(names),
    amount: `${(decision === 'BLOCKED' ? 8000 : Math.random() * 900).toFixed(2)} €`,
    channel: pick(channels),
    decision,
    risk_score: Math.round(risk * 100),
    model_version: 'v4.2',
  }
}

export function useLiveFeed() {
  const [rows, setRows] = useState<Transaction[]>(USE_MOCKS ? transactions.slice(0, 6) : [])
  const [pending, setPending] = useState<number | null>(null)
  const [status, setStatus] = useState<FeedStatus>(USE_MOCKS ? 'live' : 'reconnecting')

  useEffect(() => {
    function handle(e: FeedEvent) {
      if (e.type === 'TX_DECIDED' && e.tx_id && e.decision) {
        const row: Transaction = {
          id: e.tx_id,
          client: e.client_name ?? 'n/a',
          amount: parseAmount(e.amount),
          channel: e.channel ?? 'n/a',
          risk: (e.risk_score ?? 0) / 100,
          status: e.decision,
          model: e.model_version ?? 'n/a',
        }
        setRows((prev) => [row, ...prev].slice(0, MAX_ROWS))
      }
      if (e.type === 'QUEUE_UPDATED' && typeof e.pending === 'number') {
        setPending(e.pending)
      }
    }

    // Mode simulé : une transaction toute les 2 secondes
    if (USE_MOCKS) {
      let queue = 318
      const timer = setInterval(() => {
        const event = mockEvent()
        handle(event)
        if (event.decision === 'HUMAN_REVIEW') {
          queue += 1
          handle({ type: 'QUEUE_UPDATED', pending: queue })
        }
      }, 2000)
      return () => clearInterval(timer)
    }

    // Mode réel : WebSocket natif, avec reconnexion automatique
    let ws: WebSocket | null = null
    let retry = 0
    let timer: number | undefined
    let closed = false

    function connect() {
      ws = new WebSocket(WS_URL)
      ws.onopen = () => {
        retry = 0
        setStatus('live')
      }
      ws.onmessage = (message) => {
        try {
          handle(JSON.parse(message.data))
        } catch {
          // message illisible : on l'ignore plutôt que de planter
        }
      }
      ws.onclose = () => {
        if (closed) return
        setStatus('reconnecting')
        const delay = Math.min(1000 * 2 ** retry, 15000)
        retry += 1
        timer = window.setTimeout(connect, delay)
      }
    }

    connect()
    return () => {
      closed = true
      window.clearTimeout(timer)
      ws?.close()
    }
  }, [])

  return { rows, pending, status }
}