-- Tune maxDistance for pgvector L2 search
--
-- Usage:
--   psql "$DATABASE_URL" \
--     -v query_vector='[0.01,0.02,...]' \
--     -v limit=20 \
--     -v k=10 \
--     -f scripts/embedding/tune-max-distance.sql

\if :{?query_vector}
\else
\echo 'ERROR: query_vector is required. Example: -v query_vector='"'"'[0.01,0.02,...]'"'"''
\quit 1
\endif

\if :{?limit}
\else
\set limit 20
\endif

\if :{?k}
\else
\set k 10
\endif

WITH distances AS (
    SELECT
        p.id,
        p.title,
        p.content_vector <-> CAST(:'query_vector' AS vector) AS distance
    FROM posts p
    WHERE p.status = 'PUBLISHED'
      AND p.content_vector IS NOT NULL
)
SELECT id, title, distance
FROM distances
ORDER BY distance
LIMIT :limit;

WITH distances AS (
    SELECT
        p.content_vector <-> CAST(:'query_vector' AS vector) AS distance
    FROM posts p
    WHERE p.status = 'PUBLISHED'
      AND p.content_vector IS NOT NULL
)
SELECT
    MIN(distance) AS min_distance,
    percentile_cont(0.10) WITHIN GROUP (ORDER BY distance) AS p10,
    percentile_cont(0.25) WITHIN GROUP (ORDER BY distance) AS p25,
    percentile_cont(0.50) WITHIN GROUP (ORDER BY distance) AS p50,
    percentile_cont(0.75) WITHIN GROUP (ORDER BY distance) AS p75,
    percentile_cont(0.90) WITHIN GROUP (ORDER BY distance) AS p90,
    percentile_cont(0.95) WITHIN GROUP (ORDER BY distance) AS p95,
    MAX(distance) AS max_distance
FROM distances;

WITH distances AS (
    SELECT
        p.content_vector <-> CAST(:'query_vector' AS vector) AS distance
    FROM posts p
    WHERE p.status = 'PUBLISHED'
      AND p.content_vector IS NOT NULL
    ORDER BY distance
    LIMIT 1 OFFSET (:k - 1)
)
SELECT
    distance AS kth_distance,
    distance * 1.05 AS suggested_max_distance
FROM distances;
