package com.aicounseling.app.domain.user.controller

import com.aicounseling.app.domain.user.dto.NicknameUpdateRequest
import com.aicounseling.app.domain.user.dto.UserResponse
import com.aicounseling.app.domain.user.service.UserService
import com.aicounseling.app.global.constants.AppConstants
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
                ?: return RsData.of(AppConstants.Response.UNAUTHORIZED_CODE, "로그인이 필요합니다", null)

        val user = userService.getUser(userId)
        return RsData.of(
            AppConstants.Response.SUCCESS_CODE,
            "사용자 정보 조회 성공",
            UserResponse.from(user),
        )
    }

    @PatchMapping("/nickname")
    fun updateNickname(
        @Valid @RequestBody request: NicknameUpdateRequest,
    ): RsData<UserResponse> {
        val userId =
            rq.currentUserId
                ?: return RsData.of(AppConstants.Response.UNAUTHORIZED_CODE, "로그인이 필요합니다", null)

        val updatedUser =
            userService.changeNickname(
                userId,
                request.nickname,
            )

        return RsData.of(
            AppConstants.Response.SUCCESS_CODE,
            "닉네임 변경 성공",
            UserResponse.from(updatedUser),
        )
    }
}
