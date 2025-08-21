package com.aicounseling.app.domain.user.controller

import com.aicounseling.app.domain.user.dto.NicknameUpdateRequest
import com.aicounseling.app.domain.user.dto.UserResponse
import com.aicounseling.app.domain.user.service.UserService
import com.aicounseling.app.global.rq.Rq
import com.aicounseling.app.global.rsData.RsData
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
    private val rq: Rq,
) {
    @GetMapping("/me")
    fun getMyInfo(): RsData<UserResponse> {
        val userId =
            rq.currentUserId
                ?: return rq.unauthorizedResponse("로그인이 필요합니다")

        val user = userService.getUser(userId)
        return rq.successResponse(
            data = UserResponse.from(user),
            message = "조회 성공",
        )
    }

    @PatchMapping("/nickname")
    fun updateNickname(
        @Valid @RequestBody request: NicknameUpdateRequest,
    ): RsData<UserResponse> {
        val userId =
            rq.currentUserId
                ?: return rq.unauthorizedResponse("로그인이 필요합니다")

        val updatedUser =
            userService.changeNickname(
                userId,
                request.nickname,
            )

        return rq.successResponse(
            data = UserResponse.from(updatedUser),
            message = "닉네임 변경 성공",
        )
    }
}
