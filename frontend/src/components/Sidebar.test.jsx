import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { Sidebar } from './Sidebar.jsx';

describe('F01 sidebar', () => {
  test('does not expose self-service session management to an authenticated learner', () => {
    render(
      <Sidebar
        activeTab="dashboard"
        onSelectTab={jest.fn()}
        currentTheme="dark-pink"
        onSelectTheme={jest.fn()}
        authState={{ isAuthenticated: true }}
        onLogout={jest.fn()}
      />
    );

    expect(screen.queryByTestId('nav-sessions')).not.toBeInTheDocument();
    expect(screen.getByTestId('nav-admin')).toBeInTheDocument();
  });
});
