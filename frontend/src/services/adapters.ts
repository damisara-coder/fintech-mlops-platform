import type { HumanReviewDto, ReviewItem, RiskBand } from '../types/api'

function bandFor(score: number): RiskBand {
  if (score < 30) return 'LOW'
  if (score < 65) return 'MEDIUM'
  if (score < 90) return 'HIGH'
  return 'CRITICAL'
}

export function toReviewItem(dto: HumanReviewDto): ReviewItem {
  const m = dto.model_evaluation
  const t = dto.transaction
  const score = Math.round(m.probability * 100)

  return {
    tx_id: dto.tx_id,
    decision: m.decision,
    risk_score: score,
    risk_band: bandFor(score),
    model_version: m.model_version,
    uncertainty: m.uncertainty_score,
    factors: dto.shap_factors.map((s) => ({ f: s.feature, c: s.contribution })),
    audit_id: '',
    decided_at: t.created_at ?? '',
    client_name: dto.client.name,
    amount: t.amount,
    currency: t.currency,
    channel: t.channel,
    context: `${t.merchant_category}, ${t.hour} h`,
    history: `${dto.client.fraud_history} fraud in ${dto.client.total_transactions} transactions`,
    probability: m.probability,
    interval: m.conformal_interval,
    sla_minutes: dto.queue_sla_remaining_minutes,
  }
}