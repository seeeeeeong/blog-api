import http from "k6/http";
import { check, sleep } from "k6";
import { Rate } from "k6/metrics";

export const options = {
  stages: [
    { duration: "30s", target: 5 },
    { duration: "2m", target: 10 },
    { duration: "30s", target: 0 },
  ],
};

const errorRate = new Rate("errors");
const baseUrl = __ENV.API_BASE_URL || "http://localhost:8080";
const maxPostId = Number(__ENV.MAX_POST_ID || 2000);

export default function () {
  if (Math.random() < 0.8) {
    const res = http.get(`${baseUrl}/api/v1/posts?page=0&size=10`);
    const ok = check(res, { "list status 200": (r) => r.status === 200 });
    errorRate.add(!ok);
  } else {
    const id = Math.floor(Math.random() * maxPostId) + 1;
    const res = http.get(`${baseUrl}/api/v1/posts/${id}`);
    const ok = check(res, { "detail status 200": (r) => r.status === 200 });
    errorRate.add(!ok);
  }
  sleep(0.2);
}
