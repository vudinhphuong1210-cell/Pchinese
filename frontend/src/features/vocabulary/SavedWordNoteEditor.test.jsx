import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { SavedWordNoteEditor } from './SavedWordNoteEditor.jsx';

describe('SavedWordNoteEditor UI', () => {
  it('renders textarea and enforces 500-character limit notice', () => {
    render(
      <SavedWordNoteEditor
        savedWordId="test-id"
        currentNote="Ghi chú cũ"
        currentVersion={1}
        onSaveNote={jest.fn()}
        onCancel={jest.fn()}
      />
    );

    const textarea = screen.getByRole('textbox', { name: /ghi chú cá nhân/i });
    expect(textarea.value).toBe('Ghi chú cũ');
    expect(screen.getByText(/10 \/ 500 ký tự/i)).toBeInTheDocument();
  });
});
