package com.blog.api.domain.tag.repository

import com.blog.api.domain.tag.entity.Tag
import org.springframework.data.jpa.repository.JpaRepository

interface TagRepository : JpaRepository<Tag, Long> {
    
    fun findByName(name: String): Tag?
    
    fun findByNameIn(names: List<String>): List<Tag>
}