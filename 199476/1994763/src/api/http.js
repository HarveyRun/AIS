const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

export function getAccessToken() {
  return localStorage.getItem('shixianwen-access-token') || '';
}

export function setAccessToken(token) {
  if (token) localStorage.setItem('shixianwen-access-token', token);
  else localStorage.removeItem('shixianwen-access-token');
}

export async function request(path, options = {}) {
  const headers = new Headers(options.headers || {});
  const token = getAccessToken();
  if (token) headers.set('Authorization', `Bearer ${token}`);
  if (options.body && !(options.body instanceof FormData)) headers.set('Content-Type', 'application/json');

  const response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers });
  const payload = await response.json().catch(() => null);
  if (!response.ok || payload?.success === false) {
    if (response.status === 401) setAccessToken('');
    throw new Error(payload?.message || '请求失败，请稍后再试');
  }
  return payload?.data;
}

export const api = {
  discoveryCatalog: () => request('/public/discovery/catalog'),
  sendCode: (phone) => request('/auth/verification-codes', { method: 'POST', body: JSON.stringify({ phone }) }),
  login: (phone, code) => request('/auth/login', { method: 'POST', body: JSON.stringify({ phone, code }) }),
  register: (phone, code, nickname) => request('/auth/register', { method: 'POST', body: JSON.stringify({ phone, code, nickname }) }),
  logout: () => request('/auth/logout', { method: 'POST' }),
  me: () => request('/users/me'),
  updateProfile: (body) => request('/users/me', { method: 'PUT', body: JSON.stringify(body) }),
  updateAvatar: (file) => {
    const body = new FormData();
    body.append('avatar', file);
    return request('/users/me/avatar', { method: 'POST', body });
  },
  deleteAccount: () => request('/users/me', { method: 'DELETE' }),
  setAcceptingInquiries: (accepting) => request('/users/me/accepting-inquiries', { method: 'PATCH', body: JSON.stringify({ accepting }) }),
  answerers: (keyword = '') => request(`/answerers?keyword=${encodeURIComponent(keyword)}`),
  inquiries: () => request('/inquiries'),
  inquiry: (id) => request(`/inquiries/${id}`),
  createInquiry: (body) => request('/inquiries', { method: 'POST', body: JSON.stringify(body) }),
  wallet: () => request('/wallet'),
  walletTransactions: () => request('/wallet/transactions'),
  withdrawals: () => request('/wallet/withdrawals'),
  notifications: () => request('/notifications'),
  bindBankCard: (body) => request('/wallet/bank-card', { method: 'PUT', body: JSON.stringify(body) }),
  bankCard: () => request('/wallet/bank-card'),
  withdraw: (amount) => request('/wallet/withdrawals', { method: 'POST', body: JSON.stringify({ amount }) }),
  createRecharge: (amount) => request('/recharges', { method: 'POST', body: JSON.stringify({ amount }) }),
  submitFeedback: (body) => request('/support/feedback', { method: 'POST', body: JSON.stringify(body) }),
};
