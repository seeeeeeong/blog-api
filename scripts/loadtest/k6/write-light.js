import http from "k6/http";
import { check, sleep } from "k6";
import { Rate } from "k6/metrics";

export const options = { vus: 5, duration: "2m" };

const errorRate = new Rate("errors");
const baseUrl = __ENV.API_BASE_URL || "http://localhost:8080";
const token = __ENV.API_TOKEN;
const categoryId = Number(__ENV.CATEGORY_ID || 1);

export default function () {
  const listRes = http.get(`${baseUrl}/api/v1/posts?page=0&size=10`);
  errorRate.add(listRes.status !== 200);

  if (token && Math.random() < 0.1) {
    const payload = JSON.stringify({
      categoryId,
      title: `Load Test Post ${Date.now()}`,
      content: "Load test content",
      isDraft: false,
    });

    const res = http.post(`${baseUrl}/api/v1/posts`, payload, {
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
    });
    const ok = check(res, { "create status 201": (r) => r.status === 201 });
    errorRate.add(!ok);
  }

  sleep(0.5);
}
