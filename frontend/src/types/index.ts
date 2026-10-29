export type Decision = 'APPROVED' | 'BLOCKED' | 'HUMAN_REVIEW'

export interface Transaction {
  id: string
  client: string
  amount: number
  channel: string
  risk: number
  status: Decision
  model: string
}