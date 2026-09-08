import React from 'react';
import { SettingsPage } from '../features/settings/SettingsPage.jsx';

export const SETTINGS_ROUTE = {
  id: 'settings',
  path: '/settings',
  title: 'Cài đặt tài khoản',
};

export function SettingsRouteHandler({ activeTab }) {
  if (activeTab !== SETTINGS_ROUTE.id) {
    return null;
  }
  return <SettingsPage />;
}
