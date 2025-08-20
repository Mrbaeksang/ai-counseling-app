package com.aicounseling.app.domain.session.controller

import com.aicounseling.app.domain.session.service.ChatSessionService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/sessions")
@Suppress("UnusedPrivateProperty")
class ChatSessionController(
    private val chatSessionService: ChatSessionService,
)
