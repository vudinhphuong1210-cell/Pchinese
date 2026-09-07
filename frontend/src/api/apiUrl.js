/**
 * The browser talks only to the Spring Boot public API.  Development defaults
 * to Vite's /api proxy; deployments may set window.PCHINESE_API_BASE_URL before
 * the bundle loads (for example, https://app.example.com/api/v1).
 */
const configuredBaseUrl = globalThis.PCHINESE_API_BASE_URL || '/api/v1';

export const API_BASE_URL = String(configuredBaseUrl).replace(/\/+$/, '');

export function apiUrl(path) {
  return `${API_BASE_URL}/${String(path).replace(/^\/+/, '')}`;
}
