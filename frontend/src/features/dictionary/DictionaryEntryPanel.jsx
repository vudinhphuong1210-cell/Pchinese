import React, { useState } from 'react';

export function DictionaryEntryPanel({ entry, onClose, onSaveWord }) {
  const [isSaving, setIsSaving] = useState(false);
  const [saveError, setSaveError] = useState(null);
  const [saveSucceeded, setSaveSucceeded] = useState(false);

  if (!entry) return null;

  let parsedSenses = [];
  try {
    parsedSenses = typeof entry.senses === 'string' ? JSON.parse(entry.senses) : entry.senses || [];
  } catch (_e) {
    parsedSenses = [];
  }

  const handleSaveWord = async () => {
    if (!onSaveWord || isSaving) return;
    setIsSaving(true);
    setSaveError(null);
    try {
      await onSaveWord(entry.dictionaryEntryId);
      setSaveSucceeded(true);
    } catch (error) {
      setSaveSucceeded(false);
      setSaveError(error?.error?.message || 'Không thể lưu từ vào kho từ vựng. Vui lòng thử lại.');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div
      className="p-6 bg-white dark:bg-gray-800 rounded-xl shadow-lg border border-gray-200 dark:border-gray-700"
      role="region"
      aria-label="Chi tiết từ điển"
    >
      <div className="flex justify-between items-start mb-4">
        <div>
          <h2 className="text-4xl font-bold text-gray-900 dark:text-white flex items-baseline gap-3">
            <span>{entry.simplifiedHanzi}</span>
            {entry.traditionalHanzi && entry.traditionalHanzi !== entry.simplifiedHanzi && (
              <span className="text-xl text-gray-400 font-normal">({entry.traditionalHanzi})</span>
            )}
          </h2>
          <p className="text-lg font-medium text-emerald-600 dark:text-emerald-400 mt-1">{entry.primaryPinyin}</p>
        </div>
        <button
          onClick={onClose}
          className="min-w-[44px] min-h-[44px] p-2 text-gray-500 hover:text-gray-700 dark:hover:text-gray-300 rounded-lg focus:ring-2 focus:ring-emerald-500"
          aria-label="Đóng chi tiết"
        >
          ✕
        </button>
      </div>

      <div className="flex gap-2 mb-4">
        {entry.hskLevel && (
          <span className="px-2.5 py-1 text-xs font-semibold bg-emerald-100 dark:bg-emerald-900/40 text-emerald-700 dark:text-emerald-300 rounded-full">
            HSK {entry.hskLevel}
          </span>
        )}
        {entry.wordType && (
          <span className="px-2.5 py-1 text-xs font-semibold bg-blue-100 dark:bg-blue-900/40 text-blue-700 dark:text-blue-300 rounded-full">
            {entry.wordType}
          </span>
        )}
      </div>

      <div className="space-y-4 mb-6">
        <h3 className="text-sm font-semibold uppercase tracking-wider text-gray-500 dark:text-gray-400">Nghĩa và Ví dụ</h3>
        {parsedSenses.map((sense, idx) => (
          <div key={idx} className="p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg">
            <p className="font-medium text-gray-900 dark:text-gray-100">
              {idx + 1}. {sense.meaning_vi || sense.meaning}
            </p>
            {sense.examples && sense.examples.length > 0 && (
              <ul className="mt-2 pl-4 border-l-2 border-emerald-500 space-y-1 text-sm text-gray-600 dark:text-gray-300">
                {sense.examples.map((ex, exIdx) => (
                  <li key={exIdx}>
                    <span className="font-semibold text-gray-800 dark:text-gray-200">{ex.zh}</span> — {ex.vi}
                  </li>
                ))}
              </ul>
            )}
          </div>
        ))}
      </div>

      {onSaveWord && (
        <div className="space-y-3">
          {saveError && <p className="text-sm text-destructive" role="alert">{saveError}</p>}
          {saveSucceeded && <p className="text-sm text-emerald-600 dark:text-emerald-400" role="status">Đã lưu vào kho từ vựng cá nhân.</p>}
          <button
            type="button"
            onClick={handleSaveWord}
            disabled={isSaving}
            className="min-w-[44px] min-h-[44px] w-full py-3 px-4 bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white font-medium rounded-lg shadow transition focus:ring-2 focus:ring-emerald-500 focus:ring-offset-2"
          >
            {isSaving ? 'Đang lưu...' : 'Lưu vào từ vựng cá nhân'}
          </button>
        </div>
      )}
    </div>
  );
}
