import http from "k6/http";
import { check, sleep } from "k6";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8090";
const KEYCLOAK_URL = __ENV.KEYCLOAK_URL || "http://localhost:8080";
const REALM = __ENV.KEYCLOAK_REALM || "viajesgvr";
const CLIENT_ID = __ENV.KEYCLOAK_CLIENT_ID || "viajesgvr-frontend";
const ADMIN_USERNAME = __ENV.K6_ADMIN_USERNAME || "admin";
const ADMIN_PASSWORD = __ENV.K6_ADMIN_PASSWORD;

if (!ADMIN_PASSWORD) {
  throw new Error("Debe definir K6_ADMIN_PASSWORD antes de ejecutar la prueba.");
}

export const options = {
  vus: Number(__ENV.VUS || 10),
  duration: __ENV.DURATION || "30s",
  thresholds: {
    http_req_failed: ["rate<0.05"],
    http_req_duration: ["p(95)<2000"],
  },
};

export function setup() {
  const tokenUrl = `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`;

  const payload =
    `grant_type=password` +
    `&client_id=${encodeURIComponent(CLIENT_ID)}` +
    `&username=${encodeURIComponent(ADMIN_USERNAME)}` +
    `&password=${encodeURIComponent(ADMIN_PASSWORD)}`;

  const response = http.post(tokenUrl, payload, {
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
  });

  check(response, {
    "token obtenido correctamente": (r) => r.status === 200 && !!JSON.parse(r.body).access_token,
  });

  return {
    token: JSON.parse(response.body).access_token,
  };
}

export default function (data) {
  const params = {
    headers: {
      Authorization: `Bearer ${data.token}`,
    },
  };

  const salesUrl = `${BASE_URL}/api/reports/sales?startDate=2026-01-01&endDate=2026-12-31`;
  const rankingUrl = `${BASE_URL}/api/reports/tour-packages-ranking?startDate=2026-01-01&endDate=2026-12-31`;

  const salesResponse = http.get(salesUrl, params);

  check(salesResponse, {
    "ventas responde 200": (r) => r.status === 200,
    "ventas tiene cuerpo": (r) => r.body && r.body.length > 0,
  });

  const rankingResponse = http.get(rankingUrl, params);

  check(rankingResponse, {
    "ranking responde 200": (r) => r.status === 200,
    "ranking tiene cuerpo": (r) => r.body && r.body.length > 0,
  });

  sleep(1);
}
