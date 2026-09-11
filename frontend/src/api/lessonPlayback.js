import { httpClient } from './http.js';
import { apiUrl } from './apiUrl.js';

export const fetchLessonPlayback = async (lessonId) => {
  const response = await httpClient(apiUrl(`/lessons/${lessonId}/playback`), { method: 'GET' });
  return response.data;
};

export const submitPlaybackEvent = async (lessonId, { sessionToken, segmentId, eventType, currentPositionSeconds }) => {
  const response = await httpClient(apiUrl(`/lesson-progress/${lessonId}/playback-events`), {
    method: 'POST',
    body: JSON.stringify({
      sessionToken,
      segmentId,
      eventType,
      currentPositionSeconds,
    }),
  });
  return response.data;
};

export const fetchLessonProgress = async (lessonId) => {
  const params = new URLSearchParams();
  if (lessonId) params.append('lessonId', lessonId);
  const response = await httpClient(apiUrl(`/lesson-progress?${params.toString()}`), { method: 'GET' });
  return response.data;
};
