import React, { useState } from 'react';

export function SavedWordNoteEditor({ savedWordId, currentNote, currentVersion, onSaveNote, onCancel }) {
  const [noteText, setNoteText] = useState(currentNote || '');
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  const handleSave = async (e) => {
    e.preventDefault();
    if (noteText.length > 500) {
      setError('Ghi chú không được vượt quá 500 ký tự.');
      return;
    }

    setError(null);
    setSaving(true);
    try {
      await onSaveNote(savedWordId, noteText, currentVersion);
    } catch (err) {
      if (err?.error?.code === 'STATE_CONFLICT') {
        setError('Ghi chú đã được cập nhật từ thiết bị khác. Vui lòng tải lại trang.');
      } else {
        setError(err?.error?.message || 'Không thể lưu ghi chú.');
      }
    } finally {
      setSaving(false);
    }
  };

  return (
    <form onSubmit={handleSave} className="mt-3 p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg space-y-2">
      <label htmlFor={`note-editor-${savedWordId}`} className="block text-xs font-semibold text-gray-600 dark:text-gray-300">
        Ghi chú cá nhân (tối đa 500 ký tự)
      </label>
      <textarea
        id={`note-editor-${savedWordId}`}
        value={noteText}
        onChange={(e) => setNoteText(e.target.value)}
        rows={3}
        maxLength={500}
        placeholder="Viết ghi chú cá nhân tại đây..."
        className="w-full p-2.5 text-sm rounded-md border border-gray-300 dark:border-gray-600 dark:bg-gray-800 dark:text-white focus:ring-2 focus:ring-emerald-500 focus:outline-none"
      />
      <div className="flex justify-between items-center text-xs text-gray-500 dark:text-gray-400">
        <span>{noteText.length} / 500 ký tự</span>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={onCancel}
            className="min-w-[44px] min-h-[44px] px-3 py-1.5 text-gray-600 dark:text-gray-300 hover:text-gray-800 dark:hover:text-white"
          >
            Hủy
          </button>
          <button
            type="submit"
            disabled={saving}
            className="min-w-[44px] min-h-[44px] px-4 py-1.5 bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white font-medium rounded shadow transition"
          >
            {saving ? 'Đang lưu...' : 'Lưu ghi chú'}
          </button>
        </div>
      </div>
      {error && <p className="text-xs text-red-600 dark:text-red-400 mt-1" role="alert">{error}</p>}
    </form>
  );
}
