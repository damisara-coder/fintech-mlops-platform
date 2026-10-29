import type { HumanReviewDto } from '../types/api'

export const reviewQueue: HumanReviewDto[] = [
  {
    tx_id: 'TX-9F219',
    client: { id: 'CUST-10428', name: 'L. Benali', account_age_days: 2190, total_transactions: 214, fraud_history: 0, confidence_score: 0.99 },
    transaction: { amount: 550, currency: 'EUR', channel: 'Card : ES (new)', merchant_category: 'electronics_new', country: 'ES', hour: 3, is_recurring_merchant: false, three_d_secure_passed: true, created_at: '2026-12-05T03:12:44Z' },
    model_evaluation: { decision: 'HUMAN_REVIEW', predicted_class: 'FRAUDE', probability: 0.52, uncertainty_score: 0.34, conformal_interval: [0.48, 0.56], model_version: 'v4.2' },
    shap_factors: [
      { feature: 'merchant_category = electronics_new', contribution: 0.31, type: 'fraud_risk' },
      { feature: 'ip_country_risk = 0.61', contribution: 0.22, type: 'fraud_risk' },
      { feature: 'hour_deviation = +3.1 h', contribution: 0.14, type: 'fraud_risk' },
      { feature: 'account_age_days = 2190', contribution: -0.18, type: 'safe' },
      { feature: 'tx_count_24h = 2', contribution: -0.12, type: 'safe' },
      { feature: '3d_secure_passed = 1', contribution: -0.06, type: 'safe' },
    ],
    queue_sla_remaining_minutes: 4,
  },
  {
    tx_id: 'TX-9F217',
    client: { id: 'CUST-20311', name: 'S. Haddad', account_age_days: 900, total_transactions: 40, fraud_history: 1 },
    transaction: { amount: 2450, currency: 'EUR', channel: 'Card : AE', merchant_category: 'travel', country: 'AE', hour: 14, created_at: '2026-12-05T03:10:02Z' },
    model_evaluation: { decision: 'HUMAN_REVIEW', predicted_class: 'FRAUDE', probability: 0.71, uncertainty_score: 0.28, conformal_interval: [0.62, 0.8], model_version: 'v4.2' },
    shap_factors: [
      { feature: 'new_device_count = 2', contribution: 0.27, type: 'fraud_risk' },
      { feature: 'amount_zscore = 2.4', contribution: 0.2, type: 'fraud_risk' },
      { feature: 'ip_country_risk = 0.4', contribution: 0.1, type: 'fraud_risk' },
      { feature: 'account_age_days = 900', contribution: -0.15, type: 'safe' },
      { feature: '3d_secure_passed = 1', contribution: -0.08, type: 'safe' },
    ],
    queue_sla_remaining_minutes: 9,
  },
  {
    tx_id: 'TX-9F201',
    client: { id: 'CUST-30455', name: 'R. Dupont', account_age_days: 1400, total_transactions: 96, fraud_history: 0 },
    transaction: { amount: 980, currency: 'EUR', channel: 'SEPA : FR', merchant_category: 'utilities', country: 'FR', hour: 10, created_at: '2026-12-05T02:58:31Z' },
    model_evaluation: { decision: 'HUMAN_REVIEW', predicted_class: 'FRAUDE', probability: 0.46, uncertainty_score: 0.22, conformal_interval: [0.4, 0.52], model_version: 'v4.2' },
    shap_factors: [
      { feature: 'repeat_amount_7d = 3', contribution: 0.19, type: 'fraud_risk' },
      { feature: 'tx_count_1h = 3', contribution: 0.12, type: 'fraud_risk' },
      { feature: 'account_age_days = 1400', contribution: -0.2, type: 'safe' },
      { feature: 'is_recurring_merchant = 1', contribution: -0.14, type: 'safe' },
    ],
    queue_sla_remaining_minutes: 15,
  },
  {
    tx_id: 'TX-9F198',
    client: { id: 'CUST-40872', name: 'J. Smith', account_age_days: 80, total_transactions: 12, fraud_history: 0 },
    transaction: { amount: 75, currency: 'EUR', channel: 'Card : UK', merchant_category: 'online_shop', country: 'UK', hour: 21, created_at: '2026-12-05T02:41:09Z' },
    model_evaluation: { decision: 'HUMAN_REVIEW', predicted_class: 'FRAUDE', probability: 0.44, uncertainty_score: 0.19, conformal_interval: [0.39, 0.5], model_version: 'v4.2' },
    shap_factors: [
      { feature: '3d_secure_failures = 2', contribution: 0.17, type: 'fraud_risk' },
      { feature: 'account_age_days = 80', contribution: 0.1, type: 'fraud_risk' },
      { feature: 'amount_zscore = -0.4', contribution: -0.12, type: 'safe' },
      { feature: 'ip_country_risk = 0.1', contribution: -0.05, type: 'safe' },
    ],
    queue_sla_remaining_minutes: 22,
  },
]