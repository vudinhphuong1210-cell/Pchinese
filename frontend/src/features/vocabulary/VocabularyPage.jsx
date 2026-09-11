import React, { useState, useEffect } from 'react';
import { getSavedWords, updateSavedWordNote, deleteSavedWord } from '../../api/dictionary.js';
import { SavedWordNoteEditor } from './SavedWordNoteEditor.jsx';

export function VocabularyPage() {
  const [savedWords, setSavedWords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [editingNoteId, setEditingNoteId] = useState(null);

  const fetchWords = async () => {
    setLoading(true);
    try {
      const res = await getSavedWords(0, 20);
      setSavedWords(res.data?.items || []);
      setError(null);
    } catch (err) {
      setError(err?.error?.message || 'Không thể tải danh sách từ vựng cá nhân.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchWords();
  }, []);

  const handleSaveNote = async (savedWordId, noteText, expectedVersion) => {
    await updateSavedWordNote(savedWordId, noteText, expectedVersion);
    setEditingNoteId(null);
    await fetchWords();
  };

  const handleDeleteWord = async (savedWordId) => {
    if (!window.confirm('Bạn có chắc chắn muốn xóa từ này khỏi kho từ vựng cá nhân?')) {
      return;
    }
    try {
      await deleteSavedWord(savedWordId);
      await fetchWords();
    } catch (err) {
      setError(err?.error?.message || 'Không thể xóa từ vựng.');
    }
  };

  if (loading) {
    return (
      <div className="p-8 text-center text-gray-500 dark:text-gray-400">
        Đang tải từ vựng cá nhân...
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto p-4 space-y-6">
      <header className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-extrabold text-gray-900 dark:text-white">Kho từ vựng cá nhân</h1>
          <p className="text-gray-600 dark:text-gray-400 mt-1">Danh sách từ đã lưu và ghi chú riêng của bạn</p>
        </div>
        <div className="px-3 py-1.5 bg-emerald-100 dark:bg-emerald-900/40 text-emerald-700 dark:text-emerald-300 font-semibold text-sm rounded-full">
          {savedWords.length} / 20 từ (Gói Free)
        </div>
      </header>

      {error && (
        <div className="p-4 bg-red-50 dark:bg-red-950/40 text-red-700 dark:text-red-300 rounded-lg text-sm" role="alert">
          {error}
        </div>
      )}

      {savedWords.length === 0 ? (
        <div className="text-center py-12 bg-gray-50 dark:bg-gray-800/50 rounded-xl">
          <p className="text-gray-500 dark:text-gray-400">Bạn chưa lưu từ vựng nào.</p>
        </div>
      ) : (
        <ul className="space-y-4">
          {savedWords.map((word) => (
            <li
              key={word.savedWordId}
              className="p-5 bg-white dark:bg-gray-800 rounded-xl shadow border border-gray-200 dark:border-gray-700"
            >
              <div className="flex justify-between items-start">
                <div>
                  {word.unavailable ? (
                    <div>
                      <span className="inline-block px-2 py-0.5 text-xs font-semibold bg-gray-200 dark:bg-gray-700 text-gray-600 dark:text-gray-300 rounded mb-1">
                        Không còn khả dụng
                      </span>
                      <p className="text-sm text-gray-500 dark:text-gray-400 italic">
                        Mục từ điển này đã bị gỡ xuất bản.
                      </p>
                    </div>
                  ) : (
                    <div>
                      <div className="flex items-baseline gap-3">
                        <span className="text-2xl font-bold text-gray-900 dark:text-white">{word.simplifiedHanzi}</span>
                        {word.traditionalHanzi && word.traditionalHanzi !== word.simplifiedHanzi && (
                          <span className="text-lg text-gray-400">({word.traditionalHanzi})</span>
                        )}
                        <span className="text-sm font-medium text-emerald-600 dark:text-emerald-400">{word.primaryPinyin}</span>
                      </div>
                      <p className="text-sm text-gray-600 dark:text-gray-300 mt-1">{word.senses}</p>
                    </div>
                  )}
                </div>

                <button
                  onClick={() => handleDeleteWord(word.savedWordId)}
                  className="min-w-[44px] min-h-[44px] px-3 py-1.5 text-sm text-red-600 hover:text-red-700 font-medium"
                  aria-label="Xóa từ vựng"
                >
                  Xóa
                </button>
              </div>

              {word.personalNotePlaintext && editingNoteId !== word.savedWordId && (
                <div className="mt-3 p-3 bg-emerald-50 dark:bg-gray-700/40 rounded-lg flex justify-between items-start">
                  <p className="text-sm text-gray-800 dark:text-gray-200">{word.personalNotePlaintext}</p>
                  <button
                    onClick={() => setEditingNoteId(word.savedWordId)}
                    className="min-w-[44px] min-h-[44px] text-xs text-emerald-600 dark:text-emerald-400 hover:underline ml-2"
                  >
                    Sửa
                  </button>
                </div>
              )}

              {!word.personalNotePlaintext && editingNoteId !== word.savedWordId && (
                <button
                  onClick={() => setEditingNoteId(word.savedWordId)}
                  className="min-w-[44px] min-h-[44px] text-xs text-gray-500 hover:text-emerald-600 dark:text-gray-400 mt-2"
                >
                  + Thêm ghi chú cá nhân
                </button>
              )}

              {editingNoteId === word.savedWordId && (
                <SavedWordNoteEditor
                  savedWordId={word.savedWordId}
                  currentNote={word.personalNotePlaintext}
                  currentVersion={word.version}
                  onSaveNote={handleSaveNote}
                  onCancel={() => setEditingNoteId(null)}
                />
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
