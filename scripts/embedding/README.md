# Embedding search tuning

This folder contains tools to calibrate the `embedding.search-max-distance` threshold for pgvector L2 search.

## Why
`maxDistance` should be chosen empirically using your real content. A value that is too high returns loosely related posts; a value that is too low returns nothing (and forces keyword fallback).

## Quick start
1) Pick a few representative queries and get their embedding vectors (same model as production).
2) For each query, run the SQL script:

```sh
psql "$DATABASE_URL" \
  -v query_vector='[0.01,0.02,...]' \
  -v limit=20 \
  -v k=10 \
  -f scripts/embedding/tune-max-distance.sql
```

3) Review the top-N distances and the distribution summary.
4) Set `embedding.search-max-distance` to a value slightly above the distance of the last result you still consider relevant.
   - If you want ~top-K results per query, use the `suggested_max_distance` line as a starting point.

## Notes
- Always embed with the same model and text preprocessing used in production.
- Tune with multiple queries and take the **max** of the chosen distances to avoid under-recalling other topics.
