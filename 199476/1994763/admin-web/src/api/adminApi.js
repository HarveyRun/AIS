import { beginRequest, endRequest } from './requestActivity.js';

const TOKEN_KEY = 'shixianwen-admin-token';
const inFlightRequests = new Map();
export const token = {
  get: () => localStorage.getItem(TOKEN_KEY) || '',
  set: (v) => (v ? localStorage.setItem(TOKEN_KEY, v) : localStorage.removeItem(TOKEN_KEY)),
};
function requestKey(path, options, accessToken) {
  const method = String(options.method || 'GET').toUpperCase();
  return `${method}:${path}:${accessToken}:${String(options.body || '')}`;
}

export function request(path, options = {}) {
  const { globalLoading = true, ...requestOptions } = options;
  const accessToken = token.get();
  const key = requestKey(path, requestOptions, accessToken);
  const existingRequest = inFlightRequests.get(key);
  if (existingRequest) return existingRequest;

  const activeRequest = (async () => {
    if (globalLoading) beginRequest();
    try {
      const headers = new Headers(requestOptions.headers || {});
      if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`);
      if (requestOptions.body) headers.set('Content-Type', 'application/json');

      const response = await fetch(`/api/admin${path}`, { ...requestOptions, headers });
      const payload = await response.json().catch(() => null);
      if (!response.ok || payload?.success === false) {
        if (response.status === 401) {
          token.set('');
          window.dispatchEvent(new Event('shixianwen-admin-unauthorized'));
        }
        throw new Error(payload?.message || '请求失败');
      }
      return payload.data;
    } finally {
      inFlightRequests.delete(key);
      if (globalLoading) endRequest();
    }
  })();

  inFlightRequests.set(key, activeRequest);
  return activeRequest;
}
export const adminApi = {
  setupStatus: () => request('/auth/setup-status'),
  setup: (body) => request('/auth/setup', { method: 'POST', body: JSON.stringify(body) }),
  login: (body) => request('/auth/login', { method: 'POST', body: JSON.stringify(body) }),
  me: () => request('/auth/me'),
  logout: () => request('/auth/logout', { method: 'POST' }),
  realtimeTicket: () => request('/auth/realtime-ticket', {
    method: 'POST',
    globalLoading: false,
  }),
  dashboard: () => request('/dashboard'),
  users: (query) => request(`/users?${query}`),
  jobs: (jobName = '', page = 0, size = 20) =>
    request(`/jobs?jobName=${encodeURIComponent(jobName)}&page=${page}&size=${size}`),
  jobOptions: () => request('/job-options'),
  experienceOptions: () => request('/experience-options'),
  allJobUsers: (jobName = '', page = 0, size = 20, jobId = '') =>
    request(`/job-users?jobName=${encodeURIComponent(jobName)}&jobId=${jobId}&page=${page}&size=${size}`),
  createJob: (body) => request('/jobs', { method: 'POST', body: JSON.stringify(body) }),
  updateJob: (id, body) => request(`/jobs/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  deleteJob: (id) => request(`/jobs/${id}`, { method: 'DELETE' }),
  table: (type, query = '') => request(`/${type}?${query}`),
  materials: (id) => request(`/certifications/${id}/materials`),
  review: (id, body) =>
    request(`/certifications/${id}/review`, { method: 'POST', body: JSON.stringify(body) }),
  setCertificationEnabled: (id, enabled) =>
    request(`/certifications/${id}/enabled`, { method: 'PATCH', body: JSON.stringify({ enabled }) }),
  updateCertification: (id, body) =>
    request(`/certifications/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  deleteCertification: (id) => request(`/certifications/${id}`, { method: 'DELETE' }),
  userStatus: (id, status) =>
    request(`/users/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  withdrawalStatus: (id, status) =>
    request(`/withdrawals/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  recordStatus: (type, id, status) =>
    request(`/${type}/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  logs: (query = '') => request(`/audit-logs?${query}`),
  customerServiceConversations: ({ silent = false } = {}) => request(
    '/customer-service/conversations',
    { globalLoading: !silent },
  ),
  customerServiceMessages: (userId, { silent = false } = {}) => request(
    `/customer-service/users/${userId}/messages`,
    { globalLoading: !silent },
  ),
  readCustomerServiceMessages: (userId) => request(
    `/customer-service/users/${userId}/read`,
    { method: 'PUT', globalLoading: false },
  ),
  replyCustomerService: (userId, content) =>
    request(`/customer-service/users/${userId}/reply`, {
      method: 'POST',
      body: JSON.stringify({ content }),
    }),
  discovery: () => request('/discovery'),
  createDiscoveryCategory: (body) =>
    request('/discovery/categories', { method: 'POST', body: JSON.stringify(body) }),
  updateDiscoveryCategory: (id, body) =>
    request(`/discovery/categories/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  deleteDiscoveryCategory: (id) => request(`/discovery/categories/${id}`, { method: 'DELETE' }),
  createDiscoveryMatter: (body) =>
    request('/discovery/matters', { method: 'POST', body: JSON.stringify(body) }),
  updateDiscoveryMatter: (id, body) =>
    request(`/discovery/matters/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  deleteDiscoveryMatter: (id) => request(`/discovery/matters/${id}`, { method: 'DELETE' }),
  createDiscoveryExperience: (body) =>
    request('/discovery/experiences', { method: 'POST', body: JSON.stringify(body) }),
  updateDiscoveryExperience: (id, body) =>
    request(`/discovery/experiences/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  deleteDiscoveryExperience: (id) =>
    request(`/discovery/experiences/${id}`, { method: 'DELETE' }),
  experienceUsers: (id, page = 0, size = 20) =>
    request(`/discovery/experiences/${id}/users?page=${page}&size=${size}`),
  classifyExperience: (id, experienceId) =>
    request(`/discovery/certifications/${id}/experience`, {
      method: 'PATCH',
      body: JSON.stringify({ experienceId }),
    }),
};
