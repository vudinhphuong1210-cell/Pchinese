import React, { useState, useEffect } from 'react';
import { FlashcardItem } from './FlashcardItem.jsx';
import { submitReview } from '../../api/review.js';

export function FlashcardDeck({ initialCards = [], onQueueFinished }) {
  const [deck, setDeck] = useState(initialCards);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState(null);
  const [reviewedCount, setReviewedCount] = useState(0);

  useEffect(() => {
    setDeck(initialCards);
    setCurrentIndex(0);
    setReviewedCount(0);
  }, [initialCards]);

  if (!deck || deck.length === 0) {
    return null;
  }

  const currentCard = deck[currentIndex];

  const handleRating = async (rating) => {
    if (!currentCard || isSubmitting) return;

    setIsSubmitting(true);
    setErrorMsg(null);

    const clientReviewId = crypto.randomUUID ? crypto.randomUUID() : `rev-${Date.now()}-${Math.random()}`;

    try {
      const response = await submitReview(
        currentCard.srsScheduleId,
        clientReviewId,
        rating,
        currentCard.scheduleVersion
      );

      if (response.success) {
        setReviewedCount((prev) => prev + 1);

        if (rating === 'AGAIN') {
          // 10-min AGAIN rotation: Move card to the end of the current batch queue
          setDeck((prevDeck) => {
            const updated = [...prevDeck];
            const [rotated] = updated.splice(currentIndex, 1);
            // update schedule version if returned
            const nextVersion = response.data?.newScheduleVersion ?? rotated.scheduleVersion + 1;
            return [...updated, { ...rotated, scheduleVersion: nextVersion }];
          });
        } else {
          // Card completed for this queue session
          setDeck((prevDeck) => {
            const updated = [...prevDeck];
            updated.splice(currentIndex, 1);
            return updated;
          });
        }

        // Adjust index if out of bounds
        if (currentIndex >= deck.length - 1) {
          setCurrentIndex(0);
        }

        if (deck.length === 1 && rating !== 'AGAIN') {
          if (onQueueFinished) {
            onQueueFinished();
          }
        }
      } else {
        setErrorMsg(response.error?.message || 'Có lỗi xảy ra khi gửi kết quả ôn tập.');
      }
    } catch (err) {
      if (err?.code === 'STATE_CONFLICT' || err?.status === 409) {
        setErrorMsg('Lịch học đã được cập nhật từ thiết bị khác. Vui lòng tải lại danh sách ôn tập.');
      } else {
        setErrorMsg('Không thể kết nối máy chủ. Vui lòng thử lại.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="w-full space-y-6">
      {/* Queue Progress Bar */}
      <div className="max-w-lg mx-auto flex items-center justify-between text-xs font-semibold text-gray-500 dark:text-gray-400">
        <span>Còn lại: {deck.length} từ trong lượt ôn tập này</span>
        <span>Đã hoàn thành: {reviewedCount} lượt</span>
      </div>

      {/* Error Notification */}
      {errorMsg && (
        <div className="max-w-lg mx-auto p-4 bg-rose-50 border-l-4 border-rose-500 text-rose-800 rounded-r-lg text-sm dark:bg-rose-950/40 dark:text-rose-200">
          <p className="font-semibold">Thông báo:</p>
          <p>{errorMsg}</p>
        </div>
      )}

      {/* Active Card */}
      {currentCard && (
        <FlashcardItem
          card={currentCard}
          onRate={handleRating}
          isSubmitting={isSubmitting}
        />
      )}
    </div>
  );
}
