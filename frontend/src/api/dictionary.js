import { httpClient } from './http.js';
import { apiUrl } from './apiUrl.js';

export async function searchDictionary(query, page = 0, pageSize = 20) {
  const params = new URLSearchParams({
    q: query,
    page: String(page),
    pageSize: String(pageSize),
  });
  return httpClient(apiUrl(`/dictionary?${params.toString()}`), {
    skipAuth: true,
  });
}

export async function getDictionaryDetail(entryId) {
  return httpClient(apiUrl(`/dictionary/${entryId}`), {
    skipAuth: true,
  });
}

export async function getSavedWords(page = 0, pageSize = 20) {
  const params = new URLSearchParams({
    page: String(page),
    pageSize: String(pageSize),
  });
  return httpClient(apiUrl(`/saved-words?${params.toString()}`));
}

export async function saveWord(dictionaryEntryId, personalNotePlaintext = null) {
  return httpClient(apiUrl('/saved-words'), {
    method: 'POST',
    body: {
      dictionaryEntryId,
      personalNotePlaintext,
    },
  });
}

export async function updateSavedWordNote(savedWordId, notePlaintext, expectedVersion) {
  return httpClient(apiUrl(`/saved-words/${savedWordId}`), {
    method: 'PATCH',
    body: {
      notePlaintext,
      expectedVersion,
    },
  });
}

export async function deleteSavedWord(savedWordId) {
  return httpClient(apiUrl(`/saved-words/${savedWordId}`), {
    method: 'DELETE',
  });
}
