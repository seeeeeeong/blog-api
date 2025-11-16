package com.blog.api.domain.tag.controller

import com.blog.api.domain.tag.dto.TagResponse
import com.blog.api.domain.tag.service.TagService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tags")
class TagController(
    private val tagService: TagService
) {
    
    @GetMapping
    fun getAllTags(): ResponseEntity<List<TagResponse>> {
        return ResponseEntity.ok(tagService.getAllTags())
    }
}