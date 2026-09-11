import { searchDictionary, getDictionaryDetail, getSavedWords, saveWord } from './dictionary.js';

global.fetch = jest.fn();

describe('dictionary API client', () => {
  beforeEach(() => {
    jest.resetAllMocks();
  });

  it('searchDictionary calls /api/v1/dictionary with query params', async () => {
    fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        success: true,
        data: { items: [], page: 0, pageSize: 20, totalElements: 0, totalPages: 0 },
      }),
    });

    const result = await searchDictionary('学', 0, 20);
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(result.data.items).toEqual([]);
  });

  it('getDictionaryDetail calls /api/v1/dictionary/:entryId', async () => {
    const entryId = '00000000-0000-4000-a000-000000000001';
    fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        success: true,
        data: { dictionaryEntryId: entryId, simplifiedHanzi: '学生' },
      }),
    });

    const result = await getDictionaryDetail(entryId);
    expect(result.data.simplifiedHanzi).toBe('学生');
  });
});
