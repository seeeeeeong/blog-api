import http from "k6/http";
import { check } from "k6";
import { Rate } from "k6/metrics";

export const options = { vus: 5, duration: "2m" };

const errorRate = new Rate("errors");
const baseUrl = __ENV.API_BASE_URL || "http://localhost:8080";
const queries = ["spring", "kotlin", "redis", "vector", "blog", "test"];

export default function () {
  const query = queries[Math.floor(Math.random() * queries.length)];
  const res = http.get(
    `${baseUrl}/api/v1/posts/search/similarity?query=${encodeURIComponent(query)}`
  );
  const ok = check(res, { "search status 200": (r) => r.status === 200 });
  errorRate.add(!ok);
}
