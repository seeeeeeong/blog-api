package com.blog.api.core.support.auth

import com.blog.api.core.enum.UserRole

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class ResolveCurrentUser

data class CurrentUser(
    val userId: Long?,
    val role: UserRole?,
)
