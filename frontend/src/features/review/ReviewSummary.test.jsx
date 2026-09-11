import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { ReviewSummary } from './ReviewSummary.jsx';

describe('ReviewSummary component', () => {
  test('renders session statistics breakdown', () => {
    const stats = { total: 10, again: 2, hard: 1, good: 5, easy: 4 };
    const onRestart = jest.fn();

    render(<ReviewSummary stats={stats} onRestart={onRestart} />);

    expect(screen.getByText(/Tổng kết phiên ôn tập/i)).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument(); // again count
    expect(screen.getByText('1')).toBeInTheDocument(); // hard count
    expect(screen.getByText('5')).toBeInTheDocument(); // good count
    expect(screen.getByText('4')).toBeInTheDocument(); // easy count

    const restartBtn = screen.getByRole('button', { name: /Tải phiên ôn tập mới/i });
    fireEvent.click(restartBtn);
    expect(onRestart).toHaveBeenCalledTimes(1);
  });
});
