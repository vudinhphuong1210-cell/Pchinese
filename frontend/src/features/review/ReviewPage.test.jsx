import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ReviewPage } from './ReviewPage.jsx';
import * as reviewApi from '../../api/review.js';

jest.mock('../../api/review.js');

describe('ReviewPage component', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('renders empty completion state when no words are due', async () => {
    reviewApi.getDueQueue.mockResolvedValueOnce({
      success: true,
      data: { items: [], totalDueCount: 0 },
    });

    render(<ReviewPage />);

    await waitFor(() => {
      expect(screen.getByText(/Tuyệt vời! Bạn đã hoàn thành/i)).toBeInTheDocument();
    });
  });

  test('renders due card and flips on button click', async () => {
    const mockCard = {
      srsScheduleId: 'sched-1',
      savedWordId: 'sw-1',
      dictionaryEntryId: 'entry-1',
      simplifiedHanzi: '学习',
      traditionalHanzi: '學習',
      primaryPinyin: 'xué xí',
      hskLevel: 1,
      wordType: 'verb',
      senses: JSON.stringify([{ meaning_vi: 'học tập' }]),
      status: 'LEARNING',
      dueAt: new Date().toISOString(),
      intervalDays: 0,
      easeFactor: 2.5,
      scheduleVersion: 0,
    };

    reviewApi.getDueQueue.mockResolvedValueOnce({
      success: true,
      data: { items: [mockCard], totalDueCount: 1 },
    });

    render(<ReviewPage />);

    await waitFor(() => {
      expect(screen.getByText('学习')).toBeInTheDocument();
    });

    const flipBtn = screen.getByRole('button', { name: /Lật thẻ xem đáp án/i });
    fireEvent.click(flipBtn);

    expect(screen.getByText(/\[xué xí\]/i)).toBeInTheDocument();
    expect(screen.getByText(/học tập/i)).toBeInTheDocument();
  });
});
