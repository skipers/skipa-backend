import http from 'k6/http';
import { check, group, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_PREFIX = __ENV.API_PREFIX || '/api/v1';
const LEGAL_LOGIN_ID = __ENV.LEGAL_LOGIN_ID || 'legal01';
const LEGAL_PASSWORD = __ENV.LEGAL_PASSWORD || '1234';
const BUSINESS_LOGIN_ID = __ENV.BUSINESS_LOGIN_ID || 'biz01';
const BUSINESS_PASSWORD = __ENV.BUSINESS_PASSWORD || '1234';

const TARGET_P95_MS = Number(__ENV.TARGET_P95_MS || 300);
const THINK_TIME_SECONDS = Number(__ENV.THINK_TIME_SECONDS || 0.2);

export const options = {
  scenarios: {
    api_response_time: {
      executor: 'ramping-vus',
      stages: [
        { duration: __ENV.RAMP_UP || '30s', target: Number(__ENV.VUS || 20) },
        { duration: __ENV.DURATION || '2m', target: Number(__ENV.VUS || 20) },
        { duration: __ENV.RAMP_DOWN || '30s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    [`http_req_duration{type:api}`]: [`p(95)<${TARGET_P95_MS}`],
  },
};

function apiPath(path) {
  return `${BASE_URL}${API_PREFIX}${path}`;
}

function jsonHeaders(token) {
  return {
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    tags: { type: 'api' },
  };
}

function login(loginId, password) {
  const res = http.post(
    apiPath('/auth/login'),
    JSON.stringify({ loginId, password }),
    {
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
      },
      tags: { type: 'setup', endpoint: 'POST /auth/login' },
    },
  );

  const ok = check(res, {
    [`login succeeds for ${loginId}`]: (r) => r.status === 200 && r.json('data.accessToken'),
  });

  if (!ok) {
    throw new Error(`Login failed for ${loginId}: status=${res.status}, body=${res.body}`);
  }

  return res.json('data.accessToken');
}

function getJson(path, token, endpointName, expectedStatuses = [200], metricType = 'api') {
  const res = http.get(apiPath(path), {
    ...jsonHeaders(token),
    tags: { type: metricType, endpoint: endpointName },
  });

  check(res, {
    [`${endpointName} status is expected`]: (r) => expectedStatuses.includes(r.status),
    [`${endpointName} response is json`]: (r) =>
      (r.headers['Content-Type'] || '').includes('application/json'),
  });

  return res;
}

function firstItemId(res) {
  if (res.status !== 200) {
    return null;
  }

  const items = res.json('data.items');
  if (!Array.isArray(items) || items.length === 0) {
    return null;
  }

  return items[0].id || null;
}

function firstReportId(res) {
  if (res.status !== 200) {
    return null;
  }

  const items = res.json('data.items');
  if (!Array.isArray(items) || items.length === 0) {
    return null;
  }

  return items[0].id || null;
}

export function setup() {
  const legalToken = login(LEGAL_LOGIN_ID, LEGAL_PASSWORD);
  const businessToken = login(BUSINESS_LOGIN_ID, BUSINESS_PASSWORD);

  const legalPatents = getJson(
    '/patents?page=0&size=10&sort=applicationNumber,asc',
    legalToken,
    'GET /patents',
    [200],
    'setup',
  );
  const businessPatents = getJson(
    '/patents/assigned?page=0&size=10&sort=applicationNumber,asc',
    businessToken,
    'GET /patents/assigned',
    [200],
    'setup',
  );

  const legalPatentId = firstItemId(legalPatents);
  const businessPatentId = firstItemId(businessPatents) || legalPatentId;
  let reportId = null;

  if (legalPatentId) {
    const reports = getJson(
      `/patents/${legalPatentId}/reports?page=0&size=10`,
      legalToken,
      'GET /patents/{patentId}/reports',
      [200],
      'setup',
    );
    reportId = firstReportId(reports);
  }

  return {
    legalToken,
    businessToken,
    legalPatentId,
    businessPatentId,
    reportId,
  };
}

export default function (data) {
  group('legal read APIs', () => {
    getJson('/auth/me', data.legalToken, 'GET /auth/me');
    getJson('/dashboard/legal', data.legalToken, 'GET /dashboard/legal');
    getJson('/departments', data.legalToken, 'GET /departments');
    getJson('/patents/summary', data.legalToken, 'GET /patents/summary');
    getJson('/patents?page=0&size=50&sort=applicationNumber,asc', data.legalToken, 'GET /patents');
    getJson('/patents/pending-approval?page=0&size=20', data.legalToken, 'GET /patents/pending-approval');
    getJson('/review-cycles/current', data.legalToken, 'GET /review-cycles/current', [200, 404]);
    getJson('/review-targets?page=0&size=50', data.legalToken, 'GET /review-targets');

    if (data.legalPatentId) {
      getJson(`/patents/${data.legalPatentId}`, data.legalToken, 'GET /patents/{patentId} [legal]');
      getJson(
        `/patents/${data.legalPatentId}/legal-status`,
        data.legalToken,
        'GET /patents/{patentId}/legal-status',
      );
      getJson(
        `/patents/${data.legalPatentId}/annuities`,
        data.legalToken,
        'GET /patents/{patentId}/annuities',
      );
      getJson(
        `/patents/${data.legalPatentId}/reports?page=0&size=10`,
        data.legalToken,
        'GET /patents/{patentId}/reports',
      );
    }

    if (data.legalPatentId && data.reportId) {
      getJson(
        `/patents/${data.legalPatentId}/reports/${data.reportId}/status`,
        data.legalToken,
        'GET /patents/{patentId}/reports/{reportId}/status',
      );
    }
  });

  group('business read APIs', () => {
    getJson('/auth/me', data.businessToken, 'GET /auth/me');
    getJson('/dashboard/business', data.businessToken, 'GET /dashboard/business');
    getJson('/patents/summary', data.businessToken, 'GET /patents/summary');
    getJson('/patents/assigned?page=0&size=50&sort=applicationNumber,asc', data.businessToken, 'GET /patents/assigned');
    getJson('/business-reviews/summary', data.businessToken, 'GET /business-reviews/summary');
    getJson('/business-reviews?page=0&size=50', data.businessToken, 'GET /business-reviews');
    getJson('/business-reviews/history?page=0&size=20', data.businessToken, 'GET /business-reviews/history');
    getJson('/patents/applications?page=0&size=20', data.businessToken, 'GET /patents/applications');

    if (data.businessPatentId) {
      getJson(`/patents/${data.businessPatentId}`, data.businessToken, 'GET /patents/{patentId} [business]');
    }
  });

  sleep(THINK_TIME_SECONDS);
}
