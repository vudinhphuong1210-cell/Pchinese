import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import { Sidebar } from './Sidebar.jsx';

describe('F01 sidebar', () => {
  test('does not expose User Management to an authenticated learner', () => {
    render(
      <Sidebar
        activeTab="dashboard"
        onSelectTab={jest.fn()}
        currentTheme="dark-pink"
        onSelectTheme={jest.fn()}
        authState={{ isAuthenticated: true, isAdmin: false }}
        onLogout={jest.fn()}
      />
    );

    expect(screen.queryByTestId('nav-sessions')).not.toBeInTheDocument();
    expect(screen.queryByTestId('nav-admin')).not.toBeInTheDocument();
    expect(screen.queryByTestId('nav-admin-audit')).not.toBeInTheDocument();
  });

  test('does not display redundant person icons', () => {
    render(
      <Sidebar
        activeTab="dashboard"
        onSelectTab={jest.fn()}
        currentTheme="dark-pink"
        onSelectTheme={jest.fn()}
        authState={{ isAuthenticated: true, isAdmin: false }}
        onLogout={jest.fn()}
      />
    );

    expect(screen.queryByTestId('add-account-btn')).not.toBeInTheDocument();
  });

  test('exposes User Management only when the server-derived session roles include ADMIN', () => {
    render(
      <Sidebar
        activeTab="dashboard"
        onSelectTab={jest.fn()}
        currentTheme="dark-pink"
        onSelectTheme={jest.fn()}
        authState={{ isAuthenticated: true, isAdmin: true }}
        onLogout={jest.fn()}
      />
    );

    expect(screen.getByTestId('nav-admin')).toBeInTheDocument();
    expect(screen.getByTestId('nav-admin-audit')).toBeInTheDocument();
    expect(screen.getByTestId('nav-admin')).toHaveTextContent('Quản lý người dùng');
  });

  test('allows toggling sidebar collapsed state', () => {
    render(
      <Sidebar
        activeTab="dashboard"
        onSelectTab={jest.fn()}
        currentTheme="dark-pink"
        onSelectTheme={jest.fn()}
        authState={{ isAuthenticated: true, isAdmin: false }}
        onLogout={jest.fn()}
      />
    );

    const toggleBtn = screen.getByTestId('sidebar-toggle-btn');
    expect(toggleBtn).toBeInTheDocument();
    expect(screen.getByText('PCHINESE')).toBeInTheDocument();

    // Click to collapse
    fireEvent.click(toggleBtn);
    expect(screen.queryByText('PCHINESE')).not.toBeInTheDocument();

    // Click to expand again
    const expandBtn = screen.getByTestId('sidebar-toggle-btn');
    fireEvent.click(expandBtn);
    expect(screen.getByText('PCHINESE')).toBeInTheDocument();
  });

  test('offers all ten semantic themes', () => {
    render(
      <Sidebar
        activeTab="dashboard"
        onSelectTab={jest.fn()}
        currentTheme="dark-pink"
        onSelectTheme={jest.fn()}
        authState={{ isAuthenticated: true, isAdmin: false }}
        onLogout={jest.fn()}
      />
    );

    fireEvent.click(screen.getByTestId('theme-picker-trigger'));
    expect(screen.getAllByTestId(/^theme-option-/)).toHaveLength(10);
    expect(screen.getByTestId('theme-option-light-blue')).toBeInTheDocument();
    expect(screen.getByTestId('theme-option-dark-red')).toBeInTheDocument();
  });

  test('opens the dictionary, personal vocabulary and SRS review workspaces', () => {
    const onSelectTab = jest.fn();
    render(
      <Sidebar
        activeTab="dashboard"
        onSelectTab={onSelectTab}
        currentTheme="dark-pink"
        onSelectTheme={jest.fn()}
        authState={{ isAuthenticated: true, isAdmin: false }}
        onLogout={jest.fn()}
      />
    );

    fireEvent.click(screen.getByTestId('nav-dictionary'));
    fireEvent.click(screen.getByTestId('nav-vocabulary'));
    fireEvent.click(screen.getByTestId('nav-review'));

    expect(onSelectTab).toHaveBeenNthCalledWith(1, 'dictionary');
    expect(onSelectTab).toHaveBeenNthCalledWith(2, 'vocabulary');
    expect(onSelectTab).toHaveBeenNthCalledWith(3, 'review');
  });
});
