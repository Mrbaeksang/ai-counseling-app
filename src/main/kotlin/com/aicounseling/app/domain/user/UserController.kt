package com.aicounseling.app.domain.user

import com.aicounseling.app.global.rsData.RsData
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
) {
    /**
     * ?�재 로그?�한 ?�용???�보 조회
     */
    @GetMapping("/me")
    fun getMyInfo(
        @AuthenticationPrincipal userId: Long,
    ): RsData<UserResponse> {
        val user = userService.getUser(userId) // ?�외??Service가 ?�짐

        return RsData.of(
            "200",
            "조회 ?�공",
            UserResponse.from(user),
        )
    }

    /**
     * ?�네??변�?     */
    @PatchMapping("/nickname")
    fun updateNickname(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: NicknameUpdateRequest,
    ): RsData<UserResponse> { // Mono ?�거!
        val updatedUser =
            userService.changeNickname(
                userId,
                request.nickname,
            )

        return RsData.of(
            "200",
            "?�네??변�??�공",
            UserResponse.from(updatedUser),
        )
    }
}
