import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { DictionaryPage } from './DictionaryPage.jsx';
import * as dictionaryApi from '../../api/dictionary.js';

jest.mock('../../api/dictionary.js');

describe('DictionaryPage UI', () => {
  it('renders search input and validates empty query', async () => {
    render(<DictionaryPage />);
    const input = screen.getByRole('textbox', { name: /khung tìm kiếm từ điển/i });
    const button = screen.getByRole('button', { name: /tìm kiếm/i });

    fireEvent.change(input, { target: { value: '' } });
    fireEvent.click(button);

    expect(await screen.findByText(/vui lòng nhập từ cần tìm kiếm/i)).toBeInTheDocument();
  });

  it('displays empty state when search returns zero results', async () => {
    dictionaryApi.searchDictionary.mockResolvedValueOnce({
      data: { items: [], page: 0, pageSize: 20, totalElements: 0, totalPages: 0 },
    });

    render(<DictionaryPage />);
    const input = screen.getByRole('textbox', { name: /khung tìm kiếm từ điển/i });
    const button = screen.getByRole('button', { name: /tìm kiếm/i });

    fireEvent.change(input, { target: { value: 'nonexistentword' } });
    fireEvent.click(button);

    expect(await screen.findByText(/không tìm thấy mục từ điển phù hợp/i)).toBeInTheDocument();
  });
});
