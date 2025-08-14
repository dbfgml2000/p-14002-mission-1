package com.back.domain.member.member.entity

import com.back.global.jpa.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.*

@Entity
class Member(
    id: Int,
    @field:Column(unique = true) val username: String,
    var password: String? = null,
    var nickname: String,
    @field:Column(unique = true) var apiKey: String,
    var profileImgUrl: String? = null,
) : BaseEntity(id) {

    constructor(id: Int, username: String, nickname: String) : this(
        id,
        username,
        null,
        nickname,
        ""
    )

    constructor(username: String, password: String?, nickname: String, profileImgUrl: String?) : this(
        0,
        username,
        password,
        nickname,
        UUID.randomUUID().toString(),
        profileImgUrl
    )

    fun modifyApiKey(apiKey: String) {
        this.apiKey = apiKey
    }

    val isAdmin: Boolean
        get() = username == "system" || username == "admin"

    val authorities: List<GrantedAuthority>
        get() = authoritiesAsStringList.map { SimpleGrantedAuthority(it) }

    private val authoritiesAsStringList: List<String>
        get() = buildList {
            if (isAdmin) add("ROLE_ADMIN")
        }

    fun modify(nickname: String, profileImgUrl: String) {
        this.nickname = nickname
        this.profileImgUrl = profileImgUrl
    }

    val profileImgUrlOrDefault: String
        get() = profileImgUrl ?: "https://placehold.co/600x600?text=U_U"
}
