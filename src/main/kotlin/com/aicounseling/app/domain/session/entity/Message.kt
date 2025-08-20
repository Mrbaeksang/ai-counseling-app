package com.aicounseling.app.domain.session.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "messages")
@EntityListeners(AuditingEntityListener::class)
class Message(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    val session: ChatSession,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val senderType: SenderType,
    @Column(columnDefinition = "TEXT", nullable = false)
    val content: String,
    @Column(columnDefinition = "TEXT")
    val aiPhaseAssessment: String? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: LocalDateTime
}

enum class SenderType {
    USER,
    AI,
}
