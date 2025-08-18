package com.aicounseling.app.domain.user

import com.aicounseling.app.global.exception.NotFoundException
import com.aicounseling.app.global.rsData.RsData
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService, // 생성자 주입으로 서비스 받기
) {
    /**
     * 현재 로그인한 사용자 정보 조회
     */
    @GetMapping("/me")
    fun getMyInfo(
        @AuthenticationPrincipal userId: Long, // JWT에서 추출한 userId
    ): RsData<UserResponse> {
        val user = userService.getUser(userId)  // 예외는 Service가 던짐
        
        return RsData.of(
            "200",
            "조회 성공",
            UserResponse.from(user),
        )
    }

    /**
     * 닉네임 변경
     */
    @PatchMapping("/nickname")
    fun updateNickname(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: NicknameUpdateRequest, // request는 내가 정한 변수명
    ): RsData<UserResponse> { // Mono 제거!
        val updatedUser =
            userService.changeNickname(
                userId,
                request.nickname,
            )

        return RsData.of(
            "200",
            "닉네임 변경 성공",
            UserResponse.from(updatedUser),
        )
    }
}
