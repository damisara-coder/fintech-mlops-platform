import type { Decision } from './index'

export type RiskBand = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
export type ReviewAction = 'APPROVED' | 'REJECTED' | 'ESCALATED'

// Une explication : le nom de la variable (f) et sa contribution (c)
export interface Factor {
  f: string
  c: number
}

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

// Ce que l'ECRAN utilise. Les "?" : on affiche "n/a" si l'info manque.
export interface ReviewItem extends RiskScoreResponse {
  client_name?: string
  amount?: number
  currency?: string
  channel?: string
  context?: string
  history?: string
  probability?: number
  interval?: [number, number]
  sla_minutes?: number
}

// Ce que le BACKEND envoie (contrat gelé human-review-contract.json)
export interface HumanReviewDto {
  tx_id: string
  client: {
    id: string
    name: string
    account_age_days: number
    total_transactions: number
    fraud_history: number
    confidence_score?: number
  }
  transaction: {
    amount: number
    currency: string
    channel: string
    merchant_category: string
    country: string
    hour: number
    is_recurring_merchant?: boolean
    three_d_secure_passed?: boolean
    created_at?: string
  }
  model_evaluation: {
    decision: Decision
    predicted_class: string
    probability: number
    uncertainty_score: number
    conformal_interval: [number, number]
    model_version: string
  }
  shap_factors: {
    feature: string
    contribution: number
    type: 'fraud_risk' | 'safe' | 'neutral'
  }[]
  queue_sla_remaining_minutes?: number
}