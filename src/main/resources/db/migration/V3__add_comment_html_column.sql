-- 댓글 HTML을 DB에 저장하여 매 요청마다 Markdown 변환 비용 제거
ALTER TABLE comments ADD COLUMN IF NOT EXISTS content_html TEXT;
