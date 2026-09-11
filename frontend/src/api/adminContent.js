import { apiUrl } from './apiUrl.js';
import { httpClient } from './http.js';

/** F04 uses the shared memory-only authenticated client and standard API envelope. */
async function request(path, options = {}) {
  const response = await httpClient(apiUrl(path), options);
  const normalize = (item) => {
    if (!item?.entityType) return item;
    const common = { ...item };
    if (item.entityType === 'TOPIC') return { ...common, topicId: item.id, publicationState: item.publicationStatus };
    if (item.entityType === 'LESSON') return { ...common, lessonId: item.id, topicId: item.parentId, publicationState: item.publicationStatus };
    if (item.entityType === 'SEGMENT') return { ...common, segmentId: item.id, lessonId: item.parentId, publicationState: item.publicationStatus };
    return {
      ...common,
      mediaAssetId: item.id,
      approvalStatus: item.publicationStatus,
      providerName: item.providerName,
      providerAssetIdentifier: item.youtubeVideoId,
    };
  };
  return { ...response, data: Array.isArray(response?.data) ? response.data.map(normalize) : normalize(response?.data) };
}

function query(page = 0, size = 50) {
  return `?page=${encodeURIComponent(page)}&size=${encodeURIComponent(size)}`;
}

export const adminContentApi = {
  listTopics: (page, size) => request(`/admin/content/topics${query(page, size)}`),
  getTopic: (id) => request(`/admin/content/topics/${id}`),
  createTopic: (data) => request('/topics', { method: 'POST', body: data }),
  updateTopic: (id, data) => request(`/topics/${id}`, { method: 'PATCH', body: data }),
  publishTopic: (id, expectedVersion) => request(`/topics/${id}/publish`, { method: 'POST', body: { expectedVersion } }),
  unpublishTopic: (id, expectedVersion) => request(`/topics/${id}/unpublish`, { method: 'POST', body: { expectedVersion } }),
  archiveTopic: (id, expectedVersion) => request(`/topics/${id}/archive`, { method: 'POST', body: { expectedVersion } }),

  listLessons: (page, size) => request(`/admin/content/lessons${query(page, size)}`),
  getLesson: (id) => request(`/admin/content/lessons/${id}`),
  createLesson: (data) => request('/lessons', { method: 'POST', body: data }),
  updateLesson: (id, data) => request(`/lessons/${id}`, { method: 'PATCH', body: data }),
  publishLesson: (id, expectedVersion) => request(`/lessons/${id}/publish`, { method: 'POST', body: { expectedVersion } }),
  unpublishLesson: (id, expectedVersion) => request(`/lessons/${id}/unpublish`, { method: 'POST', body: { expectedVersion } }),
  archiveLesson: (id, expectedVersion) => request(`/lessons/${id}/archive`, { method: 'POST', body: { expectedVersion } }),

  listSegments: (page, size) => request(`/admin/content/segments${query(page, size)}`),
  getSegment: (id) => request(`/admin/content/segments/${id}`),
  createSegment: (data) => request('/segments', { method: 'POST', body: data }),
  updateSegment: (id, data) => request(`/segments/${id}`, { method: 'PATCH', body: data }),
  publishSegment: (id, expectedVersion) => request(`/segments/${id}/publish`, { method: 'POST', body: { expectedVersion } }),
  unpublishSegment: (id, expectedVersion) => request(`/segments/${id}/unpublish`, { method: 'POST', body: { expectedVersion } }),
  archiveSegment: (id, expectedVersion) => request(`/segments/${id}/archive`, { method: 'POST', body: { expectedVersion } }),

  listMedia: (page, size) => request(`/admin/content/media${query(page, size)}`),
  getMedia: (id) => request(`/admin/content/media/${id}`),
  createMedia: (data) => request('/media', { method: 'POST', body: data }),
  updateMedia: (id, data) => request(`/media/${id}`, { method: 'PATCH', body: data }),
  approveMedia: (id, expectedVersion) => request(`/media/${id}/approve`, { method: 'POST', body: { expectedVersion } }),
  rejectMedia: (id, expectedVersion) => request(`/media/${id}/reject`, { method: 'POST', body: { expectedVersion } }),
  quarantineMedia: (id, expectedVersion) => request(`/media/${id}/quarantine`, { method: 'POST', body: { expectedVersion } }),
};
