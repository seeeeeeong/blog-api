package com.blog.api.storage.post

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface PostRepositoryCustom {
    fun searchByKeyword(query: String, categoryId: Long?, pageable: Pageable): Page<PostEntity>
}
