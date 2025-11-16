package com.blog.api.domain.tag.dto

import com.blog.api.domain.tag.entity.Tag

data class TagResponse(
    val id: Long,
    val name: String
) {
    companion object {
        fun from(tag: Tag): TagResponse {
            return TagResponse(
                id = tag.id!!,
                name = tag.name
            )
        }
    }
}