import { httpClient } from './http.js';
import { apiUrl } from './apiUrl.js';

export const fetchTopics = async ({ keyword, hskLevel, page = 0, size = 20 }) => {
  const params = new URLSearchParams();
  if (keyword) params.append('keyword', keyword);
  if (hskLevel) params.append('hskLevel', hskLevel);
  params.append('page', page);
  params.append('size', size);

  const response = await httpClient(apiUrl(`/topics?${params.toString()}`), { method: 'GET' });
  return response.data; // Assumes response is an envelope where { data } contains the payload
};

export const fetchTopicDetail = async (topicId) => {
  const response = await httpClient(apiUrl(`/topics/${topicId}`), { method: 'GET' });
  return response.data;
};

export const fetchLessons = async ({ topicId, keyword, hskLevel, page = 0, size = 20 }) => {
  const params = new URLSearchParams();
  if (topicId && topicId !== 'all') params.append('topicId', topicId);
  if (keyword) params.append('keyword', keyword);
  if (hskLevel && hskLevel !== 'all') {
    // extract number from 'hsk1', 'hsk2'
    const match = String(hskLevel).match(/\d+/);
    if (match) {
      params.append('hskLevel', match[0]);
    }
  }
  params.append('page', page);
  params.append('size', size);

  const response = await httpClient(apiUrl(`/lessons?${params.toString()}`), { method: 'GET' });
  return response.data;
};

export const fetchLessonDetail = async (lessonId) => {
  const response = await httpClient(apiUrl(`/lessons/${lessonId}`), { method: 'GET' });
  return response.data;
};
