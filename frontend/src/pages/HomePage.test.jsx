import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { HomePage } from './HomePage.jsx';
import { fetchLessons, fetchTopics } from '../api/catalog.js';
import { checkInToday, getDailyStreak } from '../api/dailyStreak.js';

jest.mock('../api/catalog.js', () => ({
  fetchTopics: jest.fn(),
  fetchLessons: jest.fn(),
}));

jest.mock('../api/dailyStreak.js', () => ({
  getDailyStreak: jest.fn(),
  checkInToday: jest.fn(),
}));

const week = [
  ['T2', '2026-09-07', 'COMPLETED'], ['T3', '2026-09-08', 'COMPLETED'],
  ['T4', '2026-09-09', 'MISSED'], ['T5', '2026-09-10', 'MISSED'],
  ['T6', '2026-09-11', 'MISSED'], ['T7', '2026-09-12', 'TODAY'],
  ['CN', '2026-09-13', 'UPCOMING'],
].map(([label, date, status]) => ({ label, date, status }));

function renderPage(authState = { isAuthenticated: false }) {
  const props = {
    authState,
    onOpenLesson: jest.fn(),
    onSelectTab: jest.fn(),
    onOpenUpgradeModal: jest.fn(),
    onOpenAuth: jest.fn(),
  };
  render(<HomePage {...props} />);
  return props;
}

describe('HomePage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    fetchTopics.mockResolvedValue({ content: [{ id: 'topic-1', title: 'Đời sống' }] });
    fetchLessons.mockResolvedValue({
      content: [{ id: 'lesson-1', title: 'Chào hỏi tự nhiên', hskLevel: 1, estimatedDurationSeconds: 95, publishedSegmentCount: 8, accessLevel: 'FREE' }],
      totalElements: 1,
    });
    getDailyStreak.mockResolvedValue({ currentStreak: 2, longestStreak: 4, totalXp: 40, checkedInToday: false, weekdays: week });
    checkInToday.mockResolvedValue({ currentStreak: 3, longestStreak: 4, totalXp: 50, checkedInToday: true, alreadyCheckedIn: false, weekdays: week.map((day) => day.status === 'TODAY' ? { ...day, status: 'COMPLETED' } : day) });
  });

  test('guest sees the complete home experience and registration CTA', async () => {
    const props = renderPage();

    expect(screen.getByRole('heading', { name: 'Học tiếng Trung thú vị và hiệu quả hơn mỗi ngày' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Bốn kỹ năng, cùng một vòng tiến bộ' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Câu hỏi thường gặp' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Bắt đầu học miễn phí/i }));
    expect(props.onOpenAuth).toHaveBeenCalledWith('register');

    await waitFor(() => expect(screen.getByTestId('home-lesson-lesson-1')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('home-lesson-lesson-1'));
    expect(props.onOpenLesson).toHaveBeenCalledWith('lesson-1');
  });

  test('learner check-in is reconciled from the backend response', async () => {
    renderPage({ isAuthenticated: true });

    const button = await screen.findByTestId('daily-check-in-button');
    expect(button).toHaveTextContent('Điểm danh hôm nay');
    fireEvent.click(button);

    await waitFor(() => expect(checkInToday).toHaveBeenCalledTimes(1));
    expect(await screen.findByText('Tuyệt vời! Bạn vừa nhận 10 XP.')).toBeInTheDocument();
    expect(button).toBeDisabled();
  });

  test('filters lessons by HSK and opens only one FAQ panel at a time', async () => {
    renderPage();
    await screen.findByTestId('home-lesson-lesson-1');

    fireEvent.click(screen.getByRole('button', { name: 'HSK 2' }));
    await waitFor(() => expect(fetchLessons).toHaveBeenLastCalledWith(expect.objectContaining({ hskLevel: 'hsk2' })));

    fireEvent.click(screen.getByRole('button', { name: 'AI Buddy có tự chấm mọi kết quả học không?' }));
    expect(screen.getByText(/Điểm số, tiến độ, quota/)).toBeInTheDocument();
    expect(screen.queryByText(/Nếu bạn thường nghe không kịp/)).not.toBeInTheDocument();
  });

  test('learner view displays currentStreak, longestStreak and totalXp', async () => {
    renderPage({ isAuthenticated: true });
    await screen.findByTestId('home-lesson-lesson-1');
    expect(await screen.findByTestId('current-streak-value')).toHaveTextContent('2');
    expect(screen.getByTestId('longest-streak-value')).toHaveTextContent('4');
    expect(screen.getByTestId('total-xp-value')).toHaveTextContent('40');
    expect(screen.getByText('kỷ lục')).toBeInTheDocument();
  });

  test('supports flipping flashcard and navigating vocabulary in interactive demo', async () => {
    renderPage();
    await screen.findByTestId('home-lesson-lesson-1');
    expect(screen.getByTestId('flashcard-counter')).toHaveTextContent('1 / 8');
    expect(screen.getByTestId('flashcard-hanzi')).toHaveTextContent('坚持');
    expect(screen.getByText('jiān chí')).toBeInTheDocument();

    // Flip card to see meaning
    fireEvent.click(screen.getByTestId('flashcard-demo-card'));
    expect(screen.getByTestId('flashcard-meaning')).toHaveTextContent('kiên trì');
    expect(screen.getByText('每天坚持学习。')).toBeInTheDocument();

    // Next card
    fireEvent.click(screen.getByTestId('flashcard-next-btn'));
    expect(screen.getByTestId('flashcard-counter')).toHaveTextContent('2 / 8');
    expect(screen.getByTestId('flashcard-hanzi')).toHaveTextContent('进步');
  });

  test('renders premium user status correctly in pricing section', async () => {
    renderPage({ isAuthenticated: true, isPremium: true });
    await screen.findByTestId('home-lesson-lesson-1');
    expect(screen.getByText('ĐANG SỬ DỤNG')).toBeInTheDocument();
    expect(screen.getByText('Gói của bạn')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Xem quyền lợi & hạn mức/i })).toBeInTheDocument();
  });

  test('renders community stats section', async () => {
    renderPage();
    await screen.findByTestId('home-lesson-lesson-1');
    expect(screen.getByTestId('community-stats-section')).toBeInTheDocument();
    expect(screen.getByText('cấp độ HSK')).toBeInTheDocument();
    expect(screen.getByText('lượt AI mỗi chu kỳ Free')).toBeInTheDocument();
  });
});
