import { apiUrl } from './apiUrl.js';
import { httpClient } from './http.js';

export const aiBuddyApi = {
  list(page = 0, size = 20) {
    return httpClient(apiUrl(`/ai-conversations?page=${page}&size=${size}`));
  },
  create(payload) {
    return httpClient(apiUrl('/ai-conversations'), { method: 'POST', body: payload });
  },
  read(conversationId) {
    return httpClient(apiUrl(`/ai-conversations/${conversationId}`));
  },
  rename(conversationId, title) {
    return httpClient(apiUrl(`/ai-conversations/${conversationId}`), { method: 'PATCH', body: { title } });
  },
  remove(conversationId) {
    return httpClient(apiUrl(`/ai-conversations/${conversationId}`), { method: 'DELETE' });
  },
  send(conversationId, content, clientRequestId) {
    return httpClient(apiUrl(`/ai-conversations/${conversationId}/messages`), {
      method: 'POST', body: { content, clientRequestId },
    });
  },
};
