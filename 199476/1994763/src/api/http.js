import { beginRequest, endRequest } from './requestActivity.js';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';
const inFlightRequests = new Map();

export function getAccessToken() {
  return localStorage.getItem('shixianwen-access-token') || '';
}

export function setAccessToken(token) {
  if (token) localStorage.setItem('shixianwen-access-token', token);
  else localStorage.removeItem('shixianwen-access-token');
}

function requestBodyKey(body) {
  if (!body) return '';
  if (!(body instanceof FormData)) return String(body);

  return Array.from(body.entries())
    .map(([name, value]) => {
      if (value instanceof File) {
        return `${name}:file:${value.name}:${value.size}:${value.type}:${value.lastModified}`;
      }
      return `${name}:text:${String(value)}`;
    })
    .join('|');
}

function requestKey(path, options, token) {
  const method = String(options.method || 'GET').toUpperCase();
  return `${method}:${path}:${token}:${requestBodyKey(options.body)}`;
}

export function request(path, options = {}) {
  const { globalLoading = true, ...requestOptions } = options;
  const token = getAccessToken();
  const key = requestKey(path, requestOptions, token);
  const existingRequest = inFlightRequests.get(key);
  if (existingRequest) return existingRequest;

  const activeRequest = (async () => {
    if (globalLoading) beginRequest();
    try {
      const headers = new Headers(requestOptions.headers || {});
      if (token) headers.set('Authorization', `Bearer ${token}`);
      if (requestOptions.body && !(requestOptions.body instanceof FormData)) {
        headers.set('Content-Type', 'application/json');
      }

      const response = await fetch(`${API_BASE_URL}${path}`, { ...requestOptions, headers });
      const payload = await response.json().catch(() => null);
      if (!response.ok || payload?.success === false) {
        if (response.status === 401) {
          setAccessToken('');
          window.dispatchEvent(new Event('shixianwen-unauthorized'));
        }
        throw new Error(payload?.message || '请求失败，请稍后再试');
      }
      return payload?.data;
    } finally {
      inFlightRequests.delete(key);
      if (globalLoading) endRequest();
    }
  })();

  inFlightRequests.set(key, activeRequest);
  return activeRequest;
}

export const api = {
  matterCategories: (mainCategory) => request(`/public/discovery/matter-categories?mainCategory=${encodeURIComponent(mainCategory)}`),
  experienceCategories: (mainCategory) => request(`/public/discovery/experience-categories?mainCategory=${encodeURIComponent(mainCategory)}`),
  searchMatters: (keyword) => request(`/public/discovery/matters/search?keyword=${encodeURIComponent(keyword)}`),
  searchExperiences: (keyword) => request(`/public/discovery/experiences/search?keyword=${encodeURIComponent(keyword)}`),
  discoveryMatter: (id) => request(`/public/discovery/matters/${id}`),
  sendCode: (phone) => request('/auth/verification-codes', { method: 'POST', body: JSON.stringify({ phone }) }),
  login: (phone, code) => request('/auth/login', { method: 'POST', body: JSON.stringify({ phone, code }) }),
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
  answererEligibility: () => request('/users/me/answerer-eligibility'),
  certifications: () => request('/certifications/me'),
  submitBasicCertification: (type, title, files) => {
    const body = new FormData();
    if (title) body.append('title', title);
    files.forEach((file) => body.append('files', file));
    return request(`/certifications/basic/${type}`, { method: 'POST', body });
  },
  submitExperienceCertification: (existingId, title, description, files) => {
    const body = new FormData();
    if (existingId) body.append('existingId', existingId);
    body.append('title', title);
    body.append('description', description);
    files.forEach((file) => body.append('files', file));
    return request('/certifications/experiences', { method: 'POST', body });
  },
  answerers: (page = 0, size = 10, keyword = '', options = {}) => request(
    `/answerers?page=${page}&size=${size}&keyword=${encodeURIComponent(keyword)}`,
    options,
  ),
  answerersByMatter: (matterId) => request(`/answerers/by-matter/${matterId}`),
  answerersByExperience: (experienceId) => request(`/answerers/by-experience?experienceId=${encodeURIComponent(experienceId)}`),
  inquiries: ({ silent = false } = {}) => request('/inquiries', { globalLoading: !silent }),
  inquiry: (id) => request(`/inquiries/${id}`),
  createInquiry: (body) => request('/inquiries', { method: 'POST', body: JSON.stringify(body) }),
  acceptInquiry: (id) => request(`/inquiries/${id}/accept`, { method: 'POST' }),
  rejectInquiry: (id) => request(`/inquiries/${id}/reject`, { method: 'POST' }),
  cancelInquiry: (id) => request(`/inquiries/${id}/cancel`, { method: 'POST' }),
  sendInquiryMessage: (id, content) => request(`/inquiries/${id}/messages`, {
    method: 'POST',
    body: JSON.stringify({ content }),
    globalLoading: false,
  }),
  sendInquiryImage: (id, image) => {
    const body = new FormData();
    body.append('image', image);
    return request(`/inquiries/${id}/images`, { method: 'POST', body, globalLoading: false });
  },
  requestInquiryEnd: (id) => request(`/inquiries/${id}/request-end`, { method: 'POST' }),
  continueInquiry: (id) => request(`/inquiries/${id}/continue`, { method: 'POST' }),
  confirmInquiryEnd: (id) => request(`/inquiries/${id}/confirm-end`, { method: 'POST' }),
  wallet: () => request('/wallet'),
  walletTransactions: () => request('/wallet/transactions'),
  withdrawals: () => request('/wallet/withdrawals'),
  notifications: ({ silent = false } = {}) => request('/notifications', { globalLoading: !silent }),
  notificationUnreadCount: () => request('/notifications/unread-count', { globalLoading: false }),
  readNotification: (id) => request(`/notifications/${id}/read`, {
    method: 'PUT',
    globalLoading: false,
  }),
  readAllNotifications: () => request('/notifications/read-all', {
    method: 'PUT',
    globalLoading: false,
  }),
  markInquiryRead: (id) => request(`/inquiries/${id}/read`, {
    method: 'PUT',
    globalLoading: false,
  }),
  realtimeTicket: () => request('/realtime/tickets', {
    method: 'POST',
    globalLoading: false,
  }),
  bindBankCard: (body) => request('/wallet/bank-card', { method: 'PUT', body: JSON.stringify(body) }),
  bankCard: () => request('/wallet/bank-card'),
  withdraw: (amount) => request('/wallet/withdrawals', { method: 'POST', body: JSON.stringify({ amount }) }),
  createRecharge: (amount) => request('/recharges', { method: 'POST', body: JSON.stringify({ amount }) }),
  rechargeCapability: () => request('/recharges/capability'),
  rechargeOrder: (orderNo) => request(`/recharges/${encodeURIComponent(orderNo)}`),
  submitFeedback: (body) => request('/support/feedback', { method: 'POST', body: JSON.stringify(body) }),
  feedbackRecords: () => request('/support/feedback'),
  submitBusinessCooperation: (body) => request('/support/business-cooperations', { method: 'POST', body: JSON.stringify(body) }),
  customerServiceMessages: ({ silent = false } = {}) => request(
    '/support/customer-service/messages',
    { globalLoading: !silent },
  ),
  sendCustomerServiceMessage: (content) => request('/support/customer-service/messages', {
    method: 'POST',
    body: JSON.stringify({ content }),
    globalLoading: false,
  }),
  customerServiceUnreadCount: () => request(
    '/support/customer-service/unread-count',
    { globalLoading: false },
  ),
  readCustomerServiceMessages: () => request('/support/customer-service/read', {
    method: 'PUT',
    globalLoading: false,
  }),
};
