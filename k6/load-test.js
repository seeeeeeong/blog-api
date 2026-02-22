import http from "k6/http";
import { check, sleep } from "k6";
import { Trend, Counter } from "k6/metrics";

const BASE_URL = "https://djhuk2ifstzzm.cloudfront.net/api";

const postListDuration = new Trend("post_list_duration");
const postDetailDuration = new Trend("post_detail_duration");
const searchDuration = new Trend("search_duration");
const errorCount = new Counter("error_count");

export const options = {
  stages: [
    { duration: "1m", target: 30 },
    { duration: "3m", target: 30 },
    { duration: "1m", target: 60 },
    { duration: "2m", target: 60 },
    { duration: "1m", target: 0 },
  ],
  thresholds: {
    http_req_duration: ["p(95)<3000"],
    error_count: ["count<100"],
  },
};

const SEARCH_KEYWORDS = ["spring", "kotlin", "docker", "aws", "database"];

// 테스트 시작 전 실제 post ID 수집
export function setup() {
  const res = http.get(`${BASE_URL}/v1/posts?page=0&size=50`);
  if (res.status !== 200) {
    console.error("포스트 목록 조회 실패, 기본값 사용");
    return { postIds: [], categoryIds: [] };
  }

  const body = res.json();
  const postIds = body.data && body.data.content ? body.data.content.map((p) => p.id) : [];

  const catRes = http.get(`${BASE_URL}/v1/categories`);
  const catBody = catRes.status === 200 ? catRes.json() : {};
  const categoryIds = catBody.data ? catBody.data.map((c) => c.id) : [];

  console.log(`포스트 ${postIds.length}개, 카테고리 ${categoryIds.length}개 로드 완료`);
  return { postIds, categoryIds };
}

export default function (data) {
  const { postIds, categoryIds } = data;
  const scenario = Math.random();

  if (scenario < 0.35) {
    // 35% - 포스트 목록 (페이지 순환)
    const page = Math.floor(Math.random() * 3);
    const res = http.get(`${BASE_URL}/v1/posts?page=${page}&size=10`);
    postListDuration.add(res.timings.duration);
    if (!check(res, { "posts 200": (r) => r.status === 200 })) errorCount.add(1);

  } else if (scenario < 0.60) {
    // 25% - 포스트 상세 (실제 존재하는 ID 사용)
    if (postIds.length > 0) {
      const postId = postIds[Math.floor(Math.random() * postIds.length)];
      const res = http.get(`${BASE_URL}/v1/posts/${postId}`);
      postDetailDuration.add(res.timings.duration);
      if (!check(res, { "post detail 200": (r) => r.status === 200 })) errorCount.add(1);
    }

  } else if (scenario < 0.75) {
    // 15% - 검색
    const keyword = SEARCH_KEYWORDS[Math.floor(Math.random() * SEARCH_KEYWORDS.length)];
    const res = http.get(`${BASE_URL}/v1/posts/search?query=${keyword}`);
    searchDuration.add(res.timings.duration);
    if (!check(res, { "search 200": (r) => r.status === 200 })) errorCount.add(1);

  } else if (scenario < 0.88) {
    // 13% - 카테고리별 포스트
    if (categoryIds.length > 0) {
      const catId = categoryIds[Math.floor(Math.random() * categoryIds.length)];
      const res = http.get(`${BASE_URL}/v1/posts/categories/${catId}?page=0&size=10`);
      if (!check(res, { "category posts 200": (r) => r.status === 200 })) errorCount.add(1);
    }

  } else if (scenario < 0.95) {
    // 7% - 댓글 조회
    if (postIds.length > 0) {
      const postId = postIds[Math.floor(Math.random() * postIds.length)];
      const res = http.get(`${BASE_URL}/v1/posts/${postId}/comments`);
      if (!check(res, { "comments 200": (r) => r.status === 200 })) errorCount.add(1);
    }

  } else {
    // 5% - 헬스체크
    const res = http.get(`${BASE_URL}/health`);
    if (!check(res, { "health 200": (r) => r.status === 200 })) errorCount.add(1);
  }

  sleep(Math.random() * 0.5);
}
