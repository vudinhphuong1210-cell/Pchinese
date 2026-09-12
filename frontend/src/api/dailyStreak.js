import { httpClient } from './http.js';
import { apiUrl } from './apiUrl.js';

export async function getDailyStreak() {
  const response = await httpClient(apiUrl('/daily-streak'), { method: 'GET' });
  return response.data;
}

export async function checkInToday() {
  const response = await httpClient(apiUrl('/daily-streak/check-ins'), { method: 'POST' });
  return response.data;
}
