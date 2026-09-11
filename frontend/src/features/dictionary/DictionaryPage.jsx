import React, { useState } from 'react';
import { searchDictionary, getDictionaryDetail } from '../../api/dictionary.js';
import { DictionaryEntryPanel } from './DictionaryEntryPanel.jsx';

export function DictionaryPage({ onSaveWord }) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState(null);
  const [selectedEntry, setSelectedEntry] = useState(null);
  const [loading, setLoading] = useState(false);
  const [validationError, setValidationError] = useState(null);
  const [page, setPage] = useState(0);

  const handleSearch = async (e, targetPage = 0) => {
    if (e) e.preventDefault();
    const trimmed = query.trim();
    if (!trimmed) {
      setValidationError('Vui lòng nhập từ cần tìm kiếm (1 - 120 ký tự).');
      setResults(null);
      return;
    }
    const codePoints = Array.from(trimmed).length;
    if (codePoints > 120) {
      setValidationError('Từ khóa tìm kiếm không được vượt quá 120 ký tự.');
      setResults(null);
      return;
    }

    setValidationError(null);
    setLoading(true);
    try {
      const res = await searchDictionary(trimmed, targetPage, 20);
      setResults(res.data);
      setPage(targetPage);
    } catch (err) {
      setValidationError(err.error?.message || 'Có lỗi xảy ra khi tìm kiếm.');
      setResults(null);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDetail = async (entryId) => {
    try {
      const res = await getDictionaryDetail(entryId);
      setSelectedEntry(res.data);
    } catch (err) {
      setValidationError(err.error?.message || 'Không thể mở chi tiết từ.');
    }
  };

  return (
    <div className="max-w-4xl mx-auto p-4 space-y-6">
      <header>
        <h1 className="text-3xl font-extrabold text-gray-900 dark:text-white">Từ điển Trung - Việt</h1>
        <p className="text-gray-600 dark:text-gray-400 mt-1">Tìm kiếm bằng Chữ Hán, Pinyin không dấu hoặc Tiếng Việt</p>
      </header>

      <form onSubmit={(e) => handleSearch(e, 0)} className="flex gap-2">
        <div className="flex-1">
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Nhập chữ Hán, pinyin (vd: xuesheng) hoặc nghĩa tiếng Việt..."
            className="w-full px-4 py-3 min-h-[44px] rounded-lg border border-gray-300 dark:border-gray-600 dark:bg-gray-800 dark:text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none"
            aria-label="Khung tìm kiếm từ điển"
          />
        </div>
        <button
          type="submit"
          disabled={loading}
          className="min-w-[44px] min-h-[44px] px-6 py-3 bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white font-semibold rounded-lg shadow focus:ring-2 focus:ring-emerald-500 transition"
        >
          {loading ? 'Đang tìm...' : 'Tìm kiếm'}
        </button>
      </form>

      {validationError && (
        <div className="p-4 bg-red-50 dark:bg-red-950/40 text-red-700 dark:text-red-300 rounded-lg text-sm" role="alert">
          {validationError}
        </div>
      )}

      {selectedEntry ? (
        <DictionaryEntryPanel
          entry={selectedEntry}
          onClose={() => setSelectedEntry(null)}
          onSaveWord={onSaveWord}
        />
      ) : (
        results && (
          <div className="space-y-4">
            {results.items.length === 0 ? (
              <div className="text-center py-12 bg-gray-50 dark:bg-gray-800/50 rounded-xl">
                <p className="text-gray-500 dark:text-gray-400">Không tìm thấy mục từ điển phù hợp.</p>
              </div>
            ) : (
              <>
                <ul className="divide-y divide-gray-200 dark:divide-gray-700 bg-white dark:bg-gray-800 rounded-xl shadow border border-gray-200 dark:border-gray-700 overflow-hidden">
                  {results.items.map((item) => (
                    <li key={item.dictionaryEntryId}>
                      <button
                        onClick={() => handleOpenDetail(item.dictionaryEntryId)}
                        className="w-full min-h-[44px] p-4 text-left hover:bg-emerald-50 dark:hover:bg-gray-700/50 transition flex justify-between items-center focus:outline-none focus:bg-emerald-50 dark:focus:bg-gray-700/50"
                      >
                        <div>
                          <div className="flex items-baseline gap-2">
                            <span className="text-2xl font-bold text-gray-900 dark:text-white">{item.simplifiedHanzi}</span>
                            <span className="text-sm font-medium text-emerald-600 dark:text-emerald-400">{item.primaryPinyin}</span>
                          </div>
                          <p className="text-sm text-gray-600 dark:text-gray-300 mt-1 line-clamp-1">
                            {item.senses}
                          </p>
                        </div>
                        {item.hskLevel && (
                          <span className="px-2 py-0.5 text-xs font-semibold bg-emerald-100 dark:bg-emerald-900/40 text-emerald-700 dark:text-emerald-300 rounded">
                            HSK {item.hskLevel}
                          </span>
                        )}
                      </button>
                    </li>
                  ))}
                </ul>

                {results.totalPages > 1 && (
                  <div className="flex justify-between items-center pt-2">
                    <button
                      disabled={page === 0}
                      onClick={() => handleSearch(null, page - 1)}
                      className="min-w-[44px] min-h-[44px] px-4 py-2 border rounded-lg disabled:opacity-50"
                    >
                      Trang trước
                    </button>
                    <span className="text-sm text-gray-600 dark:text-gray-400">
                      Trang {page + 1} / {results.totalPages}
                    </span>
                    <button
                      disabled={page >= results.totalPages - 1}
                      onClick={() => handleSearch(null, page + 1)}
                      className="min-w-[44px] min-h-[44px] px-4 py-2 border rounded-lg disabled:opacity-50"
                    >
                      Trang sau
                    </button>
                  </div>
                )}
              </>
            )}
          </div>
        )
      )}
    </div>
  );
}
