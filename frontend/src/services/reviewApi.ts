import { reviewQueue } from '../mocks/reviewQueue'
import type { ReviewAction, ReviewItem } from '../types/api'

const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

// Plus tard, cette fonction appellera : GET /api/v1/transactions/human-review
export async function getReviewQueue(): Promise<ReviewItem[]> {
  await wait(300)
  return [...reviewQueue].sort((a, b) => b.uncertainty - a.uncertainty)
}

// Plus tard : POST /api/v1/transactions/{id}/review
// Ici, on simule une panne 1 fois sur 5 pour tester le retour arrière.
export async function submitReview(txId: string, action: ReviewAction): Promise<void> {
  await wait(400)
  if (Math.random() < 0.2) {
    throw new Error('Network error')
  }
  console.log('Review saved', txId, action)
}