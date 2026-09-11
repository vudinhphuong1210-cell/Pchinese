import React, { useState, useEffect } from 'react';
import { getDueQueue } from '../../api/review.js';
import { FlashcardDeck } from './FlashcardDeck.jsx';

export function ReviewPage() {
  const [cards, setCards] = useState([]);
  const [totalDue, setTotalDue] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isFinished, setIsFinished] = useState(false);

  const fetchQueue = async () => {
    setIsLoading(true);
    setError(null);
    setIsFinished(false);

    try {
      const response = await getDueQueue(20);
      if (response.success && response.data) {
        setCards(response.data.items || []);
        setTotalDue(response.data.totalDueCount || 0);
        if ((response.data.items || []).length === 0) {
          setIsFinished(true);
        }
      } else {
        setError(response.error?.message || 'Không thể tải danh sách ôn tập.');
      }
    } catch (err) {
      setError('Lỗi kết nối máy chủ. Vui lòng thử lại sau.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchQueue();
  }, []);

  const handleQueueFinished = () => {
    setIsFinished(true);
    setCards([]);
  };

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-10 px-4 sm:px-6 lg:px-8 font-sans">
      <div className="max-w-3xl mx-auto space-y-8">
        {/* Page Header */}
        <div className="text-center space-y-2">
          <h1 className="text-3xl font-extrabold text-gray-900 dark:text-white tracking-tight sm:text-4xl">
            Ôn tập từ vựng (SRS)
          </h1>
          <p className="text-base text-gray-600 dark:text-gray-400">
            Hệ thống lặp lại ngắt quãng giúp bạn ghi nhớ từ vựng lâu dài.
          </p>
        </div>

        {/* Loading State */}
        {isLoading && (
          <div className="flex flex-col items-center justify-center p-12 bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700">
            <div className="w-10 h-10 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
            <p className="mt-4 text-sm font-medium text-gray-600 dark:text-gray-300">
              Đang chuẩn bị thẻ ôn tập...
            </p>
          </div>
        )}

        {/* Error State */}
        {error && !isLoading && (
          <div className="p-6 bg-rose-50 dark:bg-rose-950/30 border border-rose-200 dark:border-rose-800 rounded-2xl text-center space-y-4">
            <p className="text-sm font-semibold text-rose-800 dark:text-rose-200">
              {error}
            </p>
            <button
              type="button"
              onClick={fetchQueue}
              className="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white font-medium text-sm rounded-xl transition-all"
            >
              Thử lại
            </button>
          </div>
        )}

        {/* Active Deck */}
        {!isLoading && !error && !isFinished && cards.length > 0 && (
          <FlashcardDeck
            initialCards={cards}
            onQueueFinished={handleQueueFinished}
          />
        )}

        {/* Empty / Completion State */}
        {!isLoading && !error && (isFinished || cards.length === 0) && (
          <div className="p-10 bg-white dark:bg-gray-800 rounded-2xl shadow-lg border border-gray-100 dark:border-gray-700 text-center space-y-6">
            <div className="inline-flex items-center justify-center w-16 h-16 bg-emerald-100 dark:bg-emerald-900/50 text-emerald-600 dark:text-emerald-300 rounded-full text-3xl">
              🎉
            </div>
            <div className="space-y-2">
              <h2 className="text-2xl font-bold text-gray-900 dark:text-white">
                Tuyệt vời! Bạn đã hoàn thành lượt ôn tập
              </h2>
              <p className="text-sm text-gray-600 dark:text-gray-400 max-w-md mx-auto">
                Không còn từ vựng nào đến hạn ôn tập trong lượt này. Hãy quay lại sau để tiếp tục duy trì thói quen học tập nhé!
              </p>
            </div>
            <div>
              <button
                type="button"
                onClick={fetchQueue}
                className="px-6 py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-sm rounded-xl shadow-md transition-all hover:shadow-indigo-500/25 active:scale-95"
              >
                Kiểm tra lại từ vựng đến hạn
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
