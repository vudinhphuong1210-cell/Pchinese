import React from 'react';
import { ContentAdminPage } from '../features/admin/content/ContentAdminPage.jsx';

export const ADMIN_CONTENT_TAB_ID = 'admin-content';

export const adminContentRoutes = [
  {
    path: '/admin/content',
    tabId: ADMIN_CONTENT_TAB_ID,
    label: 'Quản lý nội dung',
    element: <ContentAdminPage />,
    requireAdmin: true,
  },
];
