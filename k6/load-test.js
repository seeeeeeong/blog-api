import http from "k6/http";
import { check, sleep } from "k6";
import { Trend, Counter } from "k6/metrics";

const BASE_URL = "https://djhuk2ifstzzm.cloudfront.net/api";

// 커스텀 메트릭
const postListDuration = new Trend("post_list_duration");
const postDetailDuration = new Trend("post_detail_duration");
const searchDuration = new Trend("search_duration");
const errorCount = new Counter("error_count");

export const options = {
  stages: [
    { duration: "1m", target: 30 },  // 1분간 30 VU까지 증가
    { duration: "3m", target: 30 },  // 3분간 30 VU 유지
    { duration: "1m", target: 60 },  // 1분간 60 VU까지 증가 (압박)
    { duration: "2m", target: 60 },  // 2분간 60 VU 유지
    { duration: "1m", target: 0 },   // 1분간 감소
  ],
  thresholds: {
    http_req_duration: ["p(95)<3000"],  // 95%가 3초 이내
    error_count: ["count<100"],
  },
};

const SEARCH_KEYWORDS = ["spring", "kotlin", "java", "docker", "aws"];

export default function () {
  const scenario = Math.random();

  if (scenario < 0.4) {
    // 40% - 포스트 목록 조회 (가장 많은 로그 발생)
    const page = Math.floor(Math.random() * 5);
    const res = http.get(`${BASE_URL}/v1/posts?page=${page}&size=10`);
    postListDuration.add(res.timings.duration);
    if (!check(res, { "posts list 200": (r) => r.status === 200 })) {
      errorCount.add(1);
    }

  } else if (scenario < 0.65) {
    // 25% - 포스트 상세 조회
    const postId = Math.floor(Math.random() * 20) + 1;
    const res = http.get(`${BASE_URL}/v1/posts/${postId}`);
    postDetailDuration.add(res.timings.duration);
    if (!check(res, { "post detail 200 or 404": (r) => r.status === 200 || r.status === 404 })) {
      errorCount.add(1);
    }

  } else if (scenario < 0.80) {
    // 15% - 검색 (DB 쿼리 + 로그 많이 발생)
    const keyword = SEARCH_KEYWORDS[Math.floor(Math.random() * SEARCH_KEYWORDS.length)];
    const res = http.get(`${BASE_URL}/v1/posts/search?query=${keyword}`);
    searchDuration.add(res.timings.duration);
    if (!check(res, { "search 200": (r) => r.status === 200 })) {
      errorCount.add(1);
    }

  } else if (scenario < 0.90) {
    // 10% - 카테고리 조회
    const res = http.get(`${BASE_URL}/v1/categories`);
    if (!check(res, { "categories 200": (r) => r.status === 200 })) {
      errorCount.add(1);
    }

  } else {
    // 10% - 헬스체크
    const res = http.get(`${BASE_URL}/health`);
    if (!check(res, { "health 200": (r) => r.status === 200 })) {
      errorCount.add(1);
    }
  }

  sleep(Math.random() * 0.5); // 0~0.5초 랜덤 대기
}
