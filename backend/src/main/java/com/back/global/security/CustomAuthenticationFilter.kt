package com.back.global.security

import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.service.MemberService
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq
import com.back.standard.util.Ut.json.toString
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class CustomAuthenticationFilter(
    private val memberService: MemberService,
    private val rq: Rq
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        logger.debug("Processing request for ${request.requestURI}")

        try {
            work(request, response, filterChain)
        } catch (e: ServiceException) {
            val rsData = e.rsData
            response.apply {
                contentType = "application/json;charset=UTF-8"
                status = rsData.statusCode
                writer.write(toString(rsData))
            }
        }
    }

    private fun work(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val requestUri = request.requestURI

        // API 요청이 아니라면 패스
        if (!requestUri.startsWith("/api/")) {
            filterChain.doFilter(request, response)
            return
        }

        // 인증, 인가가 필요없는 API 요청이라면 패스
        val noAuthPaths = setOf(
            "/api/v1/members/login",
            "/api/v1/members/logout",
            "/api/v1/members/join"
        )
        if (requestUri in noAuthPaths) {
            filterChain.doFilter(request, response)
            return
        }

        // Authorization 헤더 또는 쿠키에서 apiKey, accessToken 추출
        val (apiKey, accessToken) = extractTokens()

        logger.debug("apiKey : $apiKey")
        logger.debug("accessToken : $accessToken")

        if (apiKey.isBlank() && accessToken.isBlank()) {
            filterChain.doFilter(request, response)
            return
        }

        // accessToken 검증 & Member 조회
        val (member, isAccessTokenValid) = resolveMember(apiKey, accessToken)

        // 토큰 갱신
        if (accessToken.isNotBlank() && !isAccessTokenValid) {
            val actorAccessToken = memberService.genAccessToken(member)
            rq.apply {
                setCookie("accessToken", actorAccessToken)
                setHeader("Authorization", actorAccessToken)
            }
        }

        // Spring Security 인증 설정
        val user: UserDetails = SecurityUser(
            member.id,
            member.username,
            "",
            member.name,
            member.authorities
        )

        val authentication = UsernamePasswordAuthenticationToken(
            user,
            user.password,
            user.authorities
        )

        // 이 시점 이후부터는 시큐리티가 이 요청을 인증된 사용자의 요청이다.
        SecurityContextHolder
            .getContext().authentication = authentication

        filterChain.doFilter(request, response)
    }

    private fun extractTokens(): Pair<String, String> {
        val headerAuthorization = rq.getHeader("Authorization", "")

        return if (headerAuthorization.isNotBlank()) {
            if (!headerAuthorization.startsWith("Bearer ")) {
                throw ServiceException("401-2", "Authorization 헤더가 Bearer 형식이 아닙니다.")
            }
            val parts = headerAuthorization.split(" ", limit = 3)
            val apiKey = parts.getOrNull(1).orEmpty()
            val accessToken = parts.getOrNull(2).orEmpty()
            apiKey to accessToken
        } else {
            rq.getCookieValue("apiKey", "") to rq.getCookieValue("accessToken", "")
        }
    }

    private fun resolveMember(apiKey: String, accessToken: String): Pair<Member, Boolean> {
        var isAccessTokenValid = false
        var member: Member? = null

        if (accessToken.isNotBlank()) {
            member = memberService.payload(accessToken)?.let { payload ->
                val id = payload["id"] as Int
                val username = payload["username"] as String?
                val name = payload["name"] as String?
                isAccessTokenValid = true
                Member(id, username, name)
            }
        }

        if (member == null) {
            member = memberService.findByApiKey(apiKey)
                .orElseThrow { ServiceException("401-3", "API 키가 유효하지 않습니다.") }
        }

        return member!! to false
    }
}
