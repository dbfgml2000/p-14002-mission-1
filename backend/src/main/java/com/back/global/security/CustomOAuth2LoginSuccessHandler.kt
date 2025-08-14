package com.back.global.security

import com.back.domain.member.member.service.MemberService
import com.back.global.rq.Rq
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import lombok.RequiredArgsConstructor
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*

@Component
@RequiredArgsConstructor
class CustomOAuth2LoginSuccessHandler(
    private val memberService: MemberService,
    private val rq: Rq
) : AuthenticationSuccessHandler {

    @Throws(IOException::class, ServletException::class)
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse?,
        authentication: Authentication?
    ) {
        val actor = rq.actorFromDb
        val accessToken = memberService.genAccessToken(actor)

        rq.apply {
            setCookie("apiKey", actor.apiKey)
            setCookie("accessToken", accessToken)
        }

        val redirectUrl2 = request.getParameter("state") // state 파라미터 확인
            ?.let { state ->
                runCatching {
                    String(Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8) // Base64 URL-safe 디코딩
                        .substringBefore("#") // '#' 앞은 redirectUrl, 뒤는 originState
                }.getOrNull()
            }
            ?.takeIf { it.isNotBlank() }
            ?: "/" // 기본 리다이렉트 url

        // 최종 리다이렉트
        rq.sendRedirect(redirectUrl2)
    }
}
