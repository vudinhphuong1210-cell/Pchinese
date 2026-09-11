import React, { useState, useEffect } from 'react';
import { Sidebar } from './components/Sidebar.jsx';
import { DashboardPage } from './pages/DashboardPage.jsx';
import { LoginForm } from './features/auth/LoginForm.jsx';
import { RegisterForm } from './features/auth/RegisterForm.jsx';
import { VerifyEmailScreen } from './features/auth/VerifyEmailScreen.jsx';
import { ForgotPasswordForm } from './features/auth/ForgotPasswordForm.jsx';
import { ResetPasswordForm } from './features/auth/ResetPasswordForm.jsx';
import { AccountRolesTab } from './features/admin/AccountRolesTab.jsx';
import { SystemActivityTab } from './features/admin/SystemActivityTab.jsx';
import { ContentAdminPage } from './features/admin/content/ContentAdminPage.jsx';
import { SettingsPage } from './features/settings/SettingsPage.jsx';
import { EntitlementPage } from './features/entitlement/EntitlementPage.jsx';
import { PremiumUpgradeModal } from './features/entitlement/PremiumUpgradeModal.jsx';
import { authSessionStore } from './features/auth/authSessionStore.js';
import { authApi } from './api/auth.js';

export default function App() {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [authView, setAuthView] = useState('login'); // 'login' | 'register' | 'verify' | 'forgot' | 'reset'
  const [authActionToken, setAuthActionToken] = useState('');
  const [isUpgradeModalOpen, setIsUpgradeModalOpen] = useState(false);
  const [currentTheme, setCurrentTheme] = useState(() => {
    return localStorage.getItem('pchinese_theme') || 'dark-pink';
  });

  const [authState, setAuthState] = useState(() => authSessionStore.getState());

  useEffect(() => {
    const unsubscribe = authSessionStore.subscribe((newState) => {
      setAuthState(newState);
    });
    return unsubscribe;
  }, []);

  useEffect(() => {
    void authApi.refresh()
      .catch(() => {
        // A missing, expired, or revoked refresh cookie is a normal guest
        // state. The HTTP layer has already cleared any stale memory state.
      })
  }, []);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const action = params.get('auth');
    const token = params.get('token');
    if (action === 'login') {
      setActiveTab('auth');
      setAuthView('login');
      window.history.replaceState({}, document.title, window.location.pathname);
      return;
    }
    if (token && (action === 'verify' || action === 'reset')) {
      setActiveTab('auth');
      setAuthView(action);
      setAuthActionToken(token);
      window.history.replaceState({}, document.title, window.location.pathname);
    }
  }, []);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', currentTheme);
    localStorage.setItem('pchinese_theme', currentTheme);
  }, [currentTheme]);

  useEffect(() => {
    if ((activeTab === 'admin' || activeTab === 'admin-audit' || activeTab === 'admin-content') && !authState.isAdmin) {
      setActiveTab('dashboard');
    }
  }, [activeTab, authState.isAdmin]);

  const handleLogout = async () => {
    try {
      await authApi.logout();
    } catch (_err) {
      authSessionStore.clearSession();
    } finally {
      setActiveTab('dashboard');
    }
  };

  const handleAddAccount = () => {
    const loginUrl = `${window.location.pathname}?auth=login`;
    window.open(loginUrl, '_blank', 'noopener,noreferrer');
  };

  const handleSelectTab = (tab) => {
    if ((tab === 'admin' || tab === 'admin-audit' || tab === 'admin-content') && !authState.isAdmin) {
      setActiveTab('dashboard');
      return;
    }
    setActiveTab(tab);
    if (tab === 'auth') setAuthView('login');
  };

  return (
    <div className="min-h-screen bg-background text-foreground flex flex-col lg:flex-row antialiased font-sans">
      {/* Sidebar Desktop/Responsive */}
      <Sidebar
        activeTab={activeTab}
        onSelectTab={handleSelectTab}
        currentTheme={currentTheme}
        onSelectTheme={setCurrentTheme}
        authState={authState}
        onLogout={handleLogout}
        onAddAccount={handleAddAccount}
        onOpenUpgradeModal={() => setIsUpgradeModalOpen(true)}
      />

      {/* Main Workspace */}
      <main className="flex-1 min-w-0 p-4 sm:p-6 lg:p-10 overflow-y-auto">
        {activeTab === 'dashboard' && <DashboardPage />}

        {activeTab === 'entitlement' && <EntitlementPage />}

        {activeTab === 'auth' && (
          <div className="min-h-[70vh] flex flex-col items-center justify-center p-4">
            {authView === 'login' && (
              <LoginForm
                onLoginSuccess={() => setActiveTab('dashboard')}
                onNavigateToRegister={() => setAuthView('register')}
                onNavigateToForgotPassword={() => setAuthView('forgot')}
              />
            )}
            {authView === 'register' && (
              <RegisterForm
                onNavigateToLogin={() => setAuthView('login')}
                onNavigateToVerify={() => setAuthView('verify')}
              />
            )}
            {authView === 'verify' && (
              <VerifyEmailScreen
                initialToken={authActionToken}
                onNavigateToLogin={() => setAuthView('login')}
              />
            )}
            {authView === 'forgot' && (
              <ForgotPasswordForm
                onNavigateToLogin={() => setAuthView('login')}
                onNavigateToReset={() => setAuthView('reset')}
              />
            )}
            {authView === 'reset' && (
              <ResetPasswordForm
                initialToken={authActionToken}
                onNavigateToLogin={() => setAuthView('login')}
              />
            )}
          </div>
        )}

        {activeTab === 'settings' && (
          <SettingsPage onOpenUpgradeModal={() => setIsUpgradeModalOpen(true)} />
        )}

        {activeTab === 'admin-content' && authState.isAdmin && (
          <div className="py-2">
            <ContentAdminPage />
          </div>
        )}

        {activeTab === 'admin' && authState.isAdmin && (
          <div className="flex justify-center py-6">
            <AccountRolesTab />
          </div>
        )}

        {activeTab === 'admin-audit' && authState.isAdmin && (
          <div className="flex justify-center py-6">
            <SystemActivityTab />
          </div>
        )}
      </main>

      {/* Global Premium Upgrade Modal */}
      <PremiumUpgradeModal
        isOpen={isUpgradeModalOpen}
        onClose={() => setIsUpgradeModalOpen(false)}
      />
    </div>
  );
}
