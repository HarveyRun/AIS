const TOKEN_KEY = 'shixianwen-admin-token';
export const token = {
  get: () => localStorage.getItem(TOKEN_KEY) || '',
  set: (v) => (v ? localStorage.setItem(TOKEN_KEY, v) : localStorage.removeItem(TOKEN_KEY)),
};
export async function request(path, options = {}) {
  const headers = new Headers(options.headers || {});
  if (token.get()) headers.set('Authorization', `Bearer ${token.get()}`);
  if (options.body) headers.set('Content-Type', 'application/json');
  const response = await fetch(`/api/admin${path}`, { ...options, headers });
  const payload = await response.json().catch(() => null);
  if (!response.ok || payload?.success === false) {
    if (response.status === 401) {
      token.set('');
      window.dispatchEvent(new Event('shixianwen-admin-unauthorized'));
    }
    throw new Error(payload?.message || '请求失败');
  }
  return payload.data;
}
export const adminApi = {
  setupStatus: () => request('/auth/setup-status'),
  setup: (body) => request('/auth/setup', { method: 'POST', body: JSON.stringify(body) }),
  login: (body) => request('/auth/login', { method: 'POST', body: JSON.stringify(body) }),
  me: () => request('/auth/me'),
  logout: () => request('/auth/logout', { method: 'POST' }),
  dashboard: () => request('/dashboard'),
  users: (query) => request(`/users?${query}`),
  table: (type, query = '') => request(`/${type}?${query}`),
  materials: (id) => request(`/certifications/${id}/materials`),
  review: (id, body) =>
    request(`/certifications/${id}/review`, { method: 'POST', body: JSON.stringify(body) }),
  userStatus: (id, status) =>
    request(`/users/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  withdrawalStatus: (id, status) =>
    request(`/withdrawals/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  recordStatus: (type, id, status) =>
    request(`/${type}/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  logs: (query = '') => request(`/audit-logs?${query}`),
  customerServiceConversations: () => request('/customer-service/conversations'),
  customerServiceMessages: (userId) => request(`/customer-service/users/${userId}/messages`),
  replyCustomerService: (userId, content) =>
    request(`/customer-service/users/${userId}/reply`, {
      method: 'POST',
      body: JSON.stringify({ content }),
    }),
};
