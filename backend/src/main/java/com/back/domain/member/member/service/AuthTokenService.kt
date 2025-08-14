package com.back.domain.member.member.service

import com.back.domain.member.member.entity.Member
import com.back.standard.util.Ut
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class AuthTokenService(
    @Value("\${custom.jwt.secretKey}")
    private val jwtSecretKey: String,
    @Value("\${custom.accessToken.expirationSeconds}")
    private val accessTokenExpirationSeconds: Int
) {

    fun genAccessToken(member: Member): String {
        return Ut.jwt.toString(
            jwtSecretKey,
            accessTokenExpirationSeconds,
            mapOf(
                "id" to member.id.toLong(),
                "username" to member.username,
                "name" to member.nickname
            )
        )
    }

    fun payload(accessToken: String): Map<String, Any>? {
        val parsedPayload = Ut.jwt.payload(jwtSecretKey, accessToken) ?: return null

        return mapOf(
            "id" to parsedPayload["id"] as Int,
            "username" to parsedPayload["username"] as String,
            "name" to parsedPayload["name"] as String
        )
    }
}