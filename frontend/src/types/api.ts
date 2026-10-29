import type { Decision } from './index'

export type RiskBand = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
export type ReviewAction = 'APPROVED' | 'REJECTED' | 'ESCALATED'

// Une explication : le nom de la variable (f) et sa contribution (c)
export interface Factor {
  f: string
  c: number
}

// Ce que le contrat gelé garantit (RiskScoreResponse)
export interface RiskScoreResponse {
  tx_id: string
  decision: Decision
  risk_score: number
  risk_band: RiskBand
  model_version: string
  uncertainty: number
  factors: Factor[]
  audit_id: string
  decided_at: string
}

// Ce dont l'écran a besoin en plus.
// Le "?" veut dire : pas garanti par le contrat, on affiche "n/a" si absent.
export interface ReviewItem extends RiskScoreResponse {
  client_name?: string
  amount?: number
  currency?: string
  channel?: string
  context?: string
  history?: string
  probability?: number
  interval?: [number, number]
}