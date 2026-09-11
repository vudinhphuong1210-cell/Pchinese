import { httpClient } from './http.js';
import { apiUrl } from './apiUrl.js';

export const startDictationAttempt = async (segmentId) => {
  const response = await httpClient(apiUrl('/dictation-attempts'), {
    method: 'POST',
    body: JSON.stringify({ segmentId }),
  });
  return response.data;
};

export const submitDictationAttempt = async (attemptId, userAnswer) => {
  const response = await httpClient(apiUrl(`/dictation-attempts/${attemptId}/submit`), {
    method: 'POST',
    body: JSON.stringify({ userAnswer }),
  });
  return response.data;
};

export const fetchDictationAttempts = async (segmentId) => {
  const params = new URLSearchParams();
  if (segmentId) params.append('segmentId', segmentId);
  const response = await httpClient(apiUrl(`/dictation-attempts?${params.toString()}`), {
    method: 'GET',
  });
  return response.data;
};

export const fetchDictationAttemptDetail = async (attemptId) => {
  const response = await httpClient(apiUrl(`/dictation-attempts/${attemptId}`), {
    method: 'GET',
  });
  return response.data;
};
