import React, { useState, useId } from 'react';

export function FlashcardItem({ card, onRate, isSubmitting }) {
  const [isFlipped, setIsFlipped] = useState(false);
  const cardTitleId = useId();

  if (!card) return null;

  const parsedSenses = () => {
    if (!card.senses) return [];
    try {
      if (typeof card.senses === 'string') {
        return JSON.parse(card.senses);
      }
      return card.senses;
    } catch {
      return [];
    }
  };

  const sensesList = parsedSenses();

  const handleKeyDown = (e) => {
    if (e.key === ' ' || e.key === 'Enter') {
      e.preventDefault();
      setIsFlipped((prev) => !prev);
    }
    if (isFlipped && !isSubmitting) {
      if (e.key === '1') onRate('AGAIN');
      if (e.key === '2') onRate('HARD');
      if (e.key === '3') onRate('GOOD');
      if (e.key === '4') onRate('EASY');
    }
  };

  return (
    <div
      className="w-full max-w-lg mx-auto bg-white dark:bg-gray-800 rounded-2xl shadow-xl border border-gray-100 dark:border-gray-700 overflow-hidden transition-all duration-300 transform"
      tabIndex={0}
      onKeyDown={handleKeyDown}
      aria-labelledby={cardTitleId}
    >
      {/* Header Badge */}
      <div className="flex items-center justify-between px-6 pt-5 pb-2 border-b border-gray-100 dark:border-gray-700">
        <div className="flex items-center gap-2">
          {card.hskLevel && (
            <span className="px-2.5 py-0.5 text-xs font-bold tracking-wide text-amber-700 bg-amber-50 dark:bg-amber-900/30 dark:text-amber-300 rounded-full">
              HSK {card.hskLevel}
            </span>
          )}
          {card.wordType && (
            <span className="px-2.5 py-0.5 text-xs font-medium text-gray-600 bg-gray-100 dark:bg-gray-700 dark:text-gray-300 rounded-full">
              {card.wordType}
            </span>
          )}
        </div>
        <span className="text-xs font-semibold text-gray-600 dark:text-gray-400">
          Trạng thái: {card.status}
        </span>
      </div>

      {/* Card Content Area */}
      <div className="p-8 text-center min-h-[220px] flex flex-col justify-center items-center">
        {/* Front of Card */}
        {!isFlipped ? (
          <div className="space-y-4">
            <h2 id={cardTitleId} className="text-5xl font-extrabold text-gray-900 dark:text-white tracking-wider">
              {card.simplifiedHanzi}
            </h2>
            {card.traditionalHanzi && card.traditionalHanzi !== card.simplifiedHanzi && (
              <p className="text-lg text-gray-600 dark:text-gray-400">
                Phồn thể: {card.traditionalHanzi}
              </p>
            )}
            <div className="pt-4">
              <button
                type="button"
                onClick={() => setIsFlipped(true)}
                className="inline-flex items-center gap-2 px-5 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-medium text-sm rounded-xl shadow-md transition-all duration-200 hover:shadow-indigo-500/25 active:scale-95"
              >
                <span>Lật thẻ xem đáp án</span>
                <kbd className="hidden sm:inline-block px-1.5 py-0.5 text-[10px] bg-indigo-800 text-indigo-100 rounded">Space</kbd>
              </button>
            </div>
          </div>
        ) : (
          /* Back of Card */
          <div className="w-full space-y-4 text-left">
            <div className="border-b border-gray-100 dark:border-gray-700 pb-3 flex justify-between items-baseline">
              <div>
                <span className="text-3xl font-bold text-indigo-600 dark:text-indigo-400">
                  {card.simplifiedHanzi}
                </span>
                <span className="ml-3 text-xl font-medium text-gray-600 dark:text-gray-300">
                  [{card.primaryPinyin}]
                </span>
              </div>
              <button
                type="button"
                onClick={() => setIsFlipped(false)}
                className="text-xs text-indigo-600 hover:underline"
              >
                Ẩn đáp án
              </button>
            </div>

            {/* Senses & Vietnamese Meanings */}
            <div className="space-y-2">
              <h4 className="text-xs font-bold text-gray-600 uppercase tracking-wider">Nghĩa tiếng Việt:</h4>
              {sensesList.length > 0 ? (
                sensesList.map((sense, idx) => (
                  <div key={idx} className="bg-gray-50 dark:bg-gray-900/50 p-3 rounded-lg text-sm">
                    <p className="font-semibold text-gray-800 dark:text-gray-200">
                      {idx + 1}. {sense.meaning_vi || sense.meaning}
                    </p>
                    {sense.examples && sense.examples.length > 0 && (
                      <div className="mt-1.5 space-y-1 text-xs text-gray-600 dark:text-gray-400">
                        {sense.examples.map((ex, exIdx) => (
                          <p key={exIdx}>
                            • <span className="font-medium">{ex.zh}</span>: {ex.vi}
                          </p>
                        ))}
                      </div>
                    )}
                  </div>
                ))
              ) : (
                <p className="text-sm text-gray-500 italic">Chưa có thông tin nghĩa.</p>
              )}
            </div>

            {/* Personal Note */}
            {card.personalNotePlaintext && (
              <div className="bg-amber-50 dark:bg-amber-900/20 border-l-4 border-amber-400 p-3 rounded-r-lg">
                <p className="text-xs font-bold text-amber-800 dark:text-amber-300">Ghi chú cá nhân:</p>
                <p className="text-xs text-amber-900 dark:text-amber-200 mt-0.5">
                  {card.personalNotePlaintext}
                </p>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Action Buttons Footer (Visible on Back) */}
      {isFlipped && (
        <div className="px-6 py-4 bg-gray-50 dark:bg-gray-900/60 border-t border-gray-100 dark:border-gray-700">
          <p className="text-xs text-center font-medium text-gray-500 dark:text-gray-400 mb-3">
            Đánh giá mức độ ghi nhớ từ này:
          </p>
          <div className="grid grid-cols-4 gap-2">
            <button
              type="button"
              disabled={isSubmitting}
              onClick={() => onRate('AGAIN')}
              className="flex flex-col items-center justify-center p-2.5 bg-rose-50 hover:bg-rose-100 dark:bg-rose-950/40 dark:hover:bg-rose-900/60 text-rose-700 dark:text-rose-300 rounded-xl font-semibold border border-rose-200 dark:border-rose-800 transition-all text-xs disabled:opacity-50"
            >
              <span>Quên (Again)</span>
              <span className="text-[10px] text-rose-500 font-normal mt-0.5">10 phút</span>
            </button>
            <button
              type="button"
              disabled={isSubmitting}
              onClick={() => onRate('HARD')}
              className="flex flex-col items-center justify-center p-2.5 bg-orange-50 hover:bg-orange-100 dark:bg-orange-950/40 dark:hover:bg-orange-900/60 text-orange-700 dark:text-orange-300 rounded-xl font-semibold border border-orange-200 dark:border-orange-800 transition-all text-xs disabled:opacity-50"
            >
              <span>Khó (Hard)</span>
              <span className="text-[10px] text-orange-500 font-normal mt-0.5">1 ngày</span>
            </button>
            <button
              type="button"
              disabled={isSubmitting}
              onClick={() => onRate('GOOD')}
              className="flex flex-col items-center justify-center p-2.5 bg-emerald-50 hover:bg-emerald-100 dark:bg-emerald-950/40 dark:hover:bg-emerald-900/60 text-emerald-700 dark:text-emerald-300 rounded-xl font-semibold border border-emerald-200 dark:border-emerald-800 transition-all text-xs disabled:opacity-50"
            >
              <span>Tốt (Good)</span>
              <span className="text-[10px] text-emerald-500 font-normal mt-0.5">3 ngày</span>
            </button>
            <button
              type="button"
              disabled={isSubmitting}
              onClick={() => onRate('EASY')}
              className="flex flex-col items-center justify-center p-2.5 bg-sky-50 hover:bg-sky-100 dark:bg-sky-950/40 dark:hover:bg-sky-900/60 text-sky-700 dark:text-sky-300 rounded-xl font-semibold border border-sky-200 dark:border-sky-800 transition-all text-xs disabled:opacity-50"
            >
              <span>Dễ (Easy)</span>
              <span className="text-[10px] text-sky-500 font-normal mt-0.5">7 ngày</span>
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
