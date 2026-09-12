import { beginRequest, endRequest } from './requestActivity.js';

const TOKEN_KEY = 'shixianwen-admin-token';
const DEVICE_KEY = 'shixianwen-admin-device-id';
const inFlightRequests = new Map();
let unauthorizedEventSent = false;
export const token = {
  get: () => sessionStorage.getItem(TOKEN_KEY) || '',
  set: (value) => {
    if (value) {
      sessionStorage.setItem(TOKEN_KEY, value);
      localStorage.removeItem(TOKEN_KEY);
      unauthorizedEventSent = false;
      return;
    }
    sessionStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(TOKEN_KEY);
  },
};
function deviceId() {
  let value = localStorage.getItem(DEVICE_KEY);
  if (!value) {
    value =
      globalThis.crypto?.randomUUID?.() ||
      `admin-${Date.now()}-${Math.random().toString(36).slice(2)}`;
    localStorage.setItem(DEVICE_KEY, value);
  }
  return value;
}
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
      headers.set('X-Device-Id', deviceId());
      if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`);
      if (requestOptions.body && !(requestOptions.body instanceof FormData)) {
        headers.set('Content-Type', 'application/json');
      }

      const response = await fetch(`/api/admin${path}`, { ...requestOptions, headers });
      const payload = await response.json().catch(() => null);
      if (!response.ok || payload?.success === false) {
        if (response.status === 401) {
          token.set('');
          if (!unauthorizedEventSent) {
            unauthorizedEventSent = true;
            window.dispatchEvent(new Event('shixianwen-admin-unauthorized'));
          }
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

async function downloadWithdrawalFile(path) {
  const response = await fetch(`/api/admin${path}`, {
    method: path.endsWith('/export') ? 'POST' : 'GET',
    headers: {
      Authorization: `Bearer ${token.get()}`,
      'X-Device-Id': deviceId(),
    },
  });
  if (!response.ok) {
    const payload = await response.json().catch(() => null);
    throw new Error(payload?.message || '导出失败');
  }
  const disposition = response.headers.get('Content-Disposition') || '';
  const encodedName = disposition.match(/filename\*=UTF-8''([^;]+)/)?.[1];
  return {
    blob: await response.blob(),
    filename: encodedName ? decodeURIComponent(encodedName) : '普通提现.xlsx',
  };
}

export const adminApi = {
  setupStatus: () => request('/auth/setup-status'),
  setup: (body) => request('/auth/setup', { method: 'POST', body: JSON.stringify(body) }),
  login: (body) => request('/auth/login', { method: 'POST', body: JSON.stringify(body) }),
  me: () => request('/auth/me'),
  changePassword: (body) =>
    request('/auth/change-password', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  adminUsers: ({ keyword = '', page = 0, size = 20 } = {}) =>
    request(`/admin-users?keyword=${encodeURIComponent(keyword)}&page=${page}&size=${size}`),
  createAdminUser: (body) =>
    request('/admin-users', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  updateAdminUser: (id, body) =>
    request(`/admin-users/${id}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
  assignAdminUserRoles: (id, ids) =>
    request(`/admin-users/${id}/roles`, {
      method: 'PUT',
      body: JSON.stringify({ ids }),
    }),
  deleteAdminUser: (id) => request(`/admin-users/${id}`, { method: 'DELETE' }),
  resetAdminPassword: (id) => request(`/admin-users/${id}/reset-password`, { method: 'POST' }),
  adminRoles: ({ keyword = '', page = 0, size = 20 } = {}) =>
    request(`/roles?keyword=${encodeURIComponent(keyword)}&page=${page}&size=${size}`),
  adminRoleOptions: () => request('/roles/options'),
  createAdminRole: (body) => request('/roles', { method: 'POST', body: JSON.stringify(body) }),
  updateAdminRole: (id, body) =>
    request(`/roles/${id}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
  assignRolePermissions: (id, ids) =>
    request(`/roles/${id}/permissions`, {
      method: 'PUT',
      body: JSON.stringify({ ids }),
    }),
  deleteAdminRole: (id) => request(`/roles/${id}`, { method: 'DELETE' }),
  adminPermissions: ({ keyword = '', module = '', page = 0, size = 20 } = {}) =>
    request(
      `/permissions?keyword=${encodeURIComponent(keyword)}&module=${encodeURIComponent(module)}&page=${page}&size=${size}`,
    ),
  adminPermissionOptions: () => request('/permissions/options'),
  adminPermissionModules: () => request('/permissions/modules'),
  createAdminPermission: (body) =>
    request('/permissions', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  updateAdminPermission: (id, body) =>
    request(`/permissions/${id}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
  deleteAdminPermission: (id) => request(`/permissions/${id}`, { method: 'DELETE' }),
  logout: () => request('/auth/logout', { method: 'POST' }),
  realtimeTicket: () =>
    request('/auth/realtime-ticket', {
      method: 'POST',
      globalLoading: false,
    }),
  dashboard: () => request('/dashboard'),
  analytics: (section, filters = {}) =>
    request(
      `/analytics/${section}?${new URLSearchParams({
        startDate: filters.startDate || '',
        endDate: filters.endDate || '',
        platform: filters.platform || '',
        environment: filters.environment || 'prod',
        includeTest: String(Boolean(filters.includeTest)),
      })}`,
    ),
  analyticsRaw: ({ eventName = '', page = 0, size = 20, ...filters } = {}) =>
    request(
      `/analytics/raw?${new URLSearchParams({
        startDate: filters.startDate || '',
        endDate: filters.endDate || '',
        platform: filters.platform || '',
        environment: filters.environment || 'prod',
        includeTest: String(Boolean(filters.includeTest)),
        eventName,
        page,
        size,
      })}`,
    ),
  exportWithdrawals: () => downloadWithdrawalFile('/withdrawals/export'),
  downloadWithdrawalBatch: (batchNo) =>
    downloadWithdrawalFile(`/withdrawals/export/${encodeURIComponent(batchNo)}`),
  permanentBanPayouts: ({ keyword = '', status = '', page = 0, size = 20 } = {}) =>
    request(
      `/permanent-ban-payouts?keyword=${encodeURIComponent(keyword)}&status=${encodeURIComponent(status)}&page=${page}&size=${size}`,
    ),
  exportPermanentBanPayouts: () => downloadWithdrawalFile('/permanent-ban-payouts/export'),
  downloadPermanentBanPayoutBatch: (batchNo) =>
    downloadWithdrawalFile(`/permanent-ban-payouts/export/${encodeURIComponent(batchNo)}`),
  importPermanentBanPayoutResults: (file) => {
    const form = new FormData();
    form.append('file', file);
    return request('/permanent-ban-payouts/results', { method: 'POST', body: form });
  },
  retryPermanentBanPayout: (id) =>
    request(`/permanent-ban-payouts/${id}/retry`, { method: 'PATCH' }),
  platformFee: () => request('/platform-fee'),
  updatePlatformFee: (androidRatePercent, iosRatePercent) =>
    request('/platform-fee', {
      method: 'PUT',
      body: JSON.stringify({ androidRatePercent, iosRatePercent }),
    }),
  inquiryCapacity: () => request('/inquiry-capacity'),
  updateInquiryCapacity: (questionerLimit, answererLimit) =>
    request('/inquiry-capacity', {
      method: 'PUT',
      body: JSON.stringify({ questionerLimit, answererLimit }),
    }),
  appGlobalSettings: () => request('/app-settings'),
  updateAppGlobalSettings: (body) =>
    request('/app-settings', {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
  curatedApplications: ({ keyword = '', status = '', page = 0, size = 20 } = {}) =>
    request(`/curated-chat/applications?${new URLSearchParams({ keyword, status, page, size })}`),
  curatedMaterials: (id) => request(`/curated-chat/applications/${id}/materials`),
  reviewCuratedJob: (id, body) =>
    request(`/curated-chat/applications/${id}/job-review`, { method: 'POST', body: JSON.stringify(body) }),
  setCuratedMemberActive: (userId, active, reason = '') =>
    request(`/curated-chat/members/${userId}/status`, { method: 'PATCH', body: JSON.stringify({ active, reason }) }),
  appTestAccounts: (page = 0, size = 20) => request(`/app-test-accounts?page=${page}&size=${size}`),
  createAppTestAccount: (body) =>
    request('/app-test-accounts', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  updateAppTestAccount: (id, body) =>
    request(`/app-test-accounts/${id}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
  deleteAppTestAccount: (id) =>
    request(`/app-test-accounts/${id}`, {
      method: 'DELETE',
    }),
  appVersions: (page = 0, size = 20) => request(`/app-versions?page=${page}&size=${size}`),
  createAppVersion: (body) =>
    request('/app-versions', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  updateAppVersion: (id, body) =>
    request(`/app-versions/${id}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
  publishAppVersion: (id) =>
    request(`/app-versions/${id}/publish`, {
      method: 'POST',
    }),
  unpublishAppVersion: (id) =>
    request(`/app-versions/${id}/unpublish`, {
      method: 'POST',
    }),
  deleteAppVersion: (id) =>
    request(`/app-versions/${id}`, {
      method: 'DELETE',
    }),
  banners: (page = 0, size = 20) => request(`/banners?page=${page}&size=${size}`),
  uploadBannerImage: (file) => {
    const form = new FormData();
    form.append('image', file);
    return request('/banners/images', { method: 'POST', body: form });
  },
  createBanner: (body) =>
    request('/banners', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  updateBanner: (id, body) =>
    request(`/banners/${id}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
  setBannerEnabled: (id, enabled) =>
    request(`/banners/${id}/enabled`, {
      method: 'PATCH',
      body: JSON.stringify({ enabled }),
    }),
  deleteBanner: (id) =>
    request(`/banners/${id}`, {
      method: 'DELETE',
    }),
  announcements: (page = 0, size = 20) => request(`/announcements?page=${page}&size=${size}`),
  createAnnouncement: (body) =>
    request('/announcements', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  updateAnnouncement: (id, body) =>
    request(`/announcements/${id}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
  publishAnnouncement: (id) =>
    request(`/announcements/${id}/publish`, {
      method: 'POST',
    }),
  withdrawAnnouncement: (id) =>
    request(`/announcements/${id}/withdraw`, {
      method: 'POST',
    }),
  deleteAnnouncement: (id) =>
    request(`/announcements/${id}`, {
      method: 'DELETE',
    }),
  users: (query) => request(`/users?${query}`),
  table: (type, query = '') => request(`/${type}?${query}`),
  materials: (id) => request(`/certifications/${id}/materials`),
  review: (id, body) =>
    request(`/certifications/${id}/review`, { method: 'POST', body: JSON.stringify(body) }),
  retryCertificationMedia: (id) =>
    request(`/certifications/${id}/media-processing/retry`, { method: 'POST' }),
  setCertificationEnabled: (id, enabled) =>
    request(`/certifications/${id}/enabled`, {
      method: 'PATCH',
      body: JSON.stringify({ enabled }),
    }),
  deleteCertification: (id) => request(`/certifications/${id}`, { method: 'DELETE' }),
  userStatus: (id, body) =>
    request(`/users/${id}/status`, { method: 'PATCH', body: JSON.stringify(body) }),
  withdrawalStatus: (id, status) =>
    request(`/withdrawals/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  reconciliationSummary: () => request('/finance-reconciliation/summary'),
  reconciliationTasks: (page = 0, size = 20) =>
    request(`/finance-reconciliation/tasks?page=${page}&size=${size}`),
  reconciliationDifferences: (status = '', page = 0, size = 20) =>
    request(
      `/finance-reconciliation/differences?status=${encodeURIComponent(status)}&page=${page}&size=${size}`,
    ),
  fundVouchers: (keyword = '', page = 0, size = 20) =>
    request(
      `/finance-reconciliation/vouchers?keyword=${encodeURIComponent(keyword)}&page=${page}&size=${size}`,
    ),
  runReconciliation: () => request('/finance-reconciliation/run', { method: 'POST' }),
  importAlipayBill: (file, billDate) => {
    const form = new FormData();
    form.append('file', file);
    return request(`/finance-reconciliation/alipay-bills?billDate=${billDate}`, {
      method: 'POST',
      body: form,
    });
  },
  importWithdrawalResults: (file) => {
    const form = new FormData();
    form.append('file', file);
    return request('/finance-reconciliation/withdrawal-results', { method: 'POST', body: form });
  },
  resolveReconciliationDifference: (id, resolution) =>
    request(`/finance-reconciliation/differences/${id}/resolve`, {
      method: 'POST',
      body: JSON.stringify({ resolution }),
    }),
  answerQualitySummary: () => request('/answer-quality/summary'),
  answerQualityEvaluations: ({ keyword = '', risk = '', page = 0, size = 20 } = {}) => request(
    `/answer-quality/evaluations?keyword=${encodeURIComponent(keyword)}&risk=${encodeURIComponent(risk)}&page=${page}&size=${size}`,
  ),
  inquiryRiskWatch: ({ keyword = '', status = '', page = 0, size = 20 } = {}) =>
    request(
      `/inquiry-disputes/risk-watch?${new URLSearchParams({ keyword, status, page, size })}`,
    ),
  dismissInquiryRiskWatch: (userId, reason) =>
    request(`/inquiry-disputes/risk-watch/${userId}`, {
      method: 'PUT',
      body: JSON.stringify({ reason }),
    }),
  recordStatus: (type, id, status, resolution = '') =>
    request(`/${type}/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status, resolution }),
    }),
  logs: (query = '') => request(`/audit-logs?${query}`),
  securityEvents: ({ severity = '', status = '', type = '', page = 0, size = 20 }) =>
    request(`/security-events?${new URLSearchParams({ severity, status, type, page, size })}`),
  reviewSecurityEvent: (id) => request(`/security-events/${id}/review`, { method: 'PATCH' }),
  customerServiceConversations: ({ silent = false } = {}) =>
    request('/customer-service/conversations', { globalLoading: !silent }),
  customerServiceMessages: (userId, { silent = false } = {}) =>
    request(`/customer-service/users/${userId}/messages`, { globalLoading: !silent }),
  readCustomerServiceMessages: (userId) =>
    request(`/customer-service/users/${userId}/read`, { method: 'PUT', globalLoading: false }),
  replyCustomerService: (userId, content) =>
    request(`/customer-service/users/${userId}/reply`, {
      method: 'POST',
      body: JSON.stringify({ content }),
    }),
};
