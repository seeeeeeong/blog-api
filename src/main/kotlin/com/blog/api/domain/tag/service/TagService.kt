package com.blog.api.domain.tag.service

import com.blog.api.domain.tag.dto.TagResponse
import com.blog.api.domain.tag.entity.Tag
import com.blog.api.domain.tag.repository.TagRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TagService(
    private val tagRepository: TagRepository
) {
    
    fun getAllTags(): List<TagResponse> {
        return tagRepository.findAll()
            .map { TagResponse.from(it) }
    }
    
    @Transactional
    fun getOrCreateTags(tagNames: List<String>): List<Tag> {
        val existingTags = tagRepository.findByNameIn(tagNames)
        val existingTagNames = existingTags.map { it.name }
        
        val newTagNames = tagNames.filter { it !in existingTagNames }
        val newTags = newTagNames.map { Tag(name = it) }
        
        return existingTags + tagRepository.saveAll(newTags)
    }
}