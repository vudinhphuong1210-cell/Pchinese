import { httpClient } from './http.js';
import { apiUrl } from './apiUrl.js';

export async function getDueQueue(limit = 20) {
  const params = new URLSearchParams({ limit: String(limit) });
  return httpClient(apiUrl(`/srs/due?${params.toString()}`));
}

export async function submitReview(srsScheduleId, clientReviewId, rating, expectedScheduleVersion) {
  return httpClient(apiUrl('/srs/review'), {
    method: 'POST',
    body: {
      srsScheduleId,
      clientReviewId,
      rating,
      expectedScheduleVersion,
    },
  });
}
