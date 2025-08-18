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
     * ?ÑÏû¨ Î°úÍ∑∏?∏Ìïú ?¨Ïö©???ïÎ≥¥ Ï°∞Ìöå
     */
    @GetMapping("/me")
    fun getMyInfo(
        @AuthenticationPrincipal userId: Long,
    ): RsData<UserResponse> {
        val user = userService.getUser(userId) // ?àÏô∏??ServiceÍ∞Ä ?òÏßê

        return RsData.of(
            "200",
            "Ï°∞Ìöå ?±Í≥µ",
            UserResponse.from(user),
        )
    }

    /**
     * ?âÎÑ§??Î≥ÄÍ≤?     */
    @PatchMapping("/nickname")
    fun updateNickname(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody request: NicknameUpdateRequest,
    ): RsData<UserResponse> { // Mono ?úÍ±∞!
        val updatedUser =
            userService.changeNickname(
                userId,
                request.nickname,
            )

        return RsData.of(
            "200",
            "?âÎÑ§??Î≥ÄÍ≤??±Í≥µ",
            UserResponse.from(updatedUser),
        )
    }
}
