package com.blog.api.domain.post.repository

import com.blog.api.domain.post.entity.Post
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * 벡터 유사도 검색을 위한 Custom Repository 인터페이스
 */
interface PostRepositoryCustom {

    /**
     * 벡터 유사도 기반 게시글 검색
     * @param queryVector 검색 쿼리의 벡터
     * @param categoryId 카테고리 필터 (nullable)
     * @param status 게시글 상태 필터 (nullable)
     * @param pageable 페이징 정보
     * @return 유사도 순으로 정렬된 게시글 목록
     */
    fun searchBySimilarity(
        queryVector: FloatArray,
        categoryId: Long? = null,
        status: String? = null,
        pageable: Pageable
    ): Page<Post>
}
