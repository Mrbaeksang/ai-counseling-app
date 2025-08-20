package com.aicounseling.app.domain.session.dto

data class BookmarkResponse(
    val sessionId: Long,
    val bookmarked: Boolean,
)
