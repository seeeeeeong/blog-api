package com.blog.api.core.domain.post

import com.blog.api.core.enum.PostStatus
import com.blog.api.core.support.error.CoreException
import com.blog.api.core.support.error.ErrorType
import com.blog.api.storage.category.CategoryRepository
import com.blog.api.storage.post.PostEntity
import com.blog.api.storage.post.PostRepository
import com.blog.api.storage.post.toPost
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostService(
    private val postRepository: PostRepository,
    private val categoryRepository: CategoryRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {

    @Transactional
    fun createPost(newPost: PostCreate): Post {
        requireCategoryExists(newPost.categoryId)
        val entity = PostEntity(
            userId = newPost.userId,
            categoryId = newPost.categoryId,
            title = newPost.title,
            content = newPost.content,
            thumbnailUrl = newPost.thumbnailUrl,
            status = newPost.status,
        )
        return postRepository.save(entity).toPost()
    }

    @Transactional
    fun updatePost(postId: Long, userId: Long, postUpdate: PostUpdate): Post {
        requireCategoryExists(postUpdate.categoryId)
        val post = getPostById(postId)
        requireOwner(post, userId)

        post.updateContent(
            categoryId = postUpdate.categoryId,
            title = postUpdate.title,
            content = postUpdate.content,
            thumbnailUrl = postUpdate.thumbnailUrl,
            status = postUpdate.status,
        )

        eventPublisher.publishEvent(PostCacheEvictEvent(postId))

        return post.toPost()
    }

    @Transactional
    fun deletePost(postId: Long, userId: Long) {
        val post = getPostById(postId)
        requireOwner(post, userId)
        post.softDelete()
        eventPublisher.publishEvent(PostCacheEvictEvent(postId))
        eventPublisher.publishEvent(
            PostDeletedEvent(
                postId = requireNotNull(post.id),
                userId = post.userId,
                thumbnailUrl = post.thumbnailUrl,
                content = post.content,
            ),
        )
    }

    @Transactional
    fun restorePost(postId: Long, userId: Long): Post {
        val post = getPostById(postId)
        requireOwner(post, userId)
        if (post.status != PostStatus.DELETED) throw CoreException(ErrorType.INVALID_INPUT)
        post.restore()
        return post.toPost()
    }

    private fun requireCategoryExists(categoryId: Long) {
        if (categoryRepository.existsById(categoryId)) return
        throw CoreException(ErrorType.CATEGORY_NOT_FOUND)
    }

    private fun getPostById(postId: Long): PostEntity =
        postRepository.findByIdOrNull(postId) ?: throw CoreException(ErrorType.POST_NOT_FOUND)

    private fun requireOwner(post: PostEntity, userId: Long) {
        if (post.userId == userId) return
        throw CoreException(ErrorType.FORBIDDEN)
    }
}
