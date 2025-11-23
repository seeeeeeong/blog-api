package com.blog.api.domain.tag.controller

import com.blog.api.domain.tag.service.TagService
import com.blog.api.global.response.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tags")
class TagController(
    private val tagService: TagService
) {

    @GetMapping
    fun getAllTags() = ApiResponse.success(
        tagService.getAllTags()
    )
}