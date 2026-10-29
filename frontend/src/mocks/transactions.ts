import type { Transaction } from '../types'

export const transactions: Transaction[] = [
  { id: 'TX-9F21C', client: 'C. Moreau', amount: 12.4, channel: 'Card, FR', risk: 0.04, status: 'APPROVED', model: 'v4.2' },
  { id: 'TX-9F21B', client: 'A. Kowalski', amount: 8000, channel: 'Wire, RU', risk: 0.97, status: 'BLOCKED', model: 'v4.2' },
  { id: 'TX-9F219', client: 'L. Benali', amount: 550, channel: 'Card, ES', risk: 0.52, status: 'HUMAN_REVIEW', model: 'v4.2' },
  { id: 'TX-9F218', client: 'D. Okonkwo', amount: 89.99, channel: 'SEPA, DE', risk: 0.09, status: 'APPROVED', model: 'v4.2' },
  { id: 'TX-9F217', client: 'S. Haddad', amount: 2450, channel: 'Card, AE', risk: 0.71, status: 'HUMAN_REVIEW', model: 'v4.2' },
  { id: 'TX-9F216', client: 'M. Rossi', amount: 640, channel: 'Card, IT', risk: 0.11, status: 'APPROVED', model: 'v4.2' },
  { id: 'TX-9F215', client: 'K. Tanaka', amount: 18500, channel: 'Wire, SG', risk: 0.95, status: 'BLOCKED', model: 'v4.2' },
]