package com.back.global.initData

import com.back.domain.member.member.service.MemberService
import com.back.domain.post.post.service.PostService
import com.back.global.app.CustomConfigProperties
import lombok.RequiredArgsConstructor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import org.springframework.transaction.annotation.Transactional

@Profile("!prod")
@Configuration
@RequiredArgsConstructor
class NotProdInitData(
    private val postService: PostService,
    private val memberService: MemberService,
    private val customConfigProperties: CustomConfigProperties
) {
    @Autowired
    @Lazy
    private lateinit var self: NotProdInitData

    @Bean
    fun notProdInitDataApplicationRunner(): ApplicationRunner {
        return ApplicationRunner { args: ApplicationArguments ->
            self.work1()
            self.work2()
        }
    }

    @Transactional
    fun work1() {
        if (memberService.count() > 0) return

        // 기본 멤버 생성
        listOf(
            Triple("system", "1234", "시스템"),
            Triple("admin", "1234", "관리자"),
            Triple("user1", "1234", "유저1"),
            Triple("user2", "1234", "유저2"),
            Triple("user3", "1234", "유저3")
        ).forEach { (username, password, nickname) ->
            val member = memberService.join(username, password, nickname)
            member.modifyApiKey(member.username)
        }

        // 설정에 따른 소셜 멤버 생성
        customConfigProperties.notProdMembers.forEach { notProdMember ->
            val socialMember = memberService.join(
                notProdMember.username,
                null,
                notProdMember.nickname,
                notProdMember.profileImgUrl
            )
            socialMember.modifyApiKey(notProdMember.apiKey)
        }
    }

    @Transactional
    fun work2() {
        if (postService.count() > 0) return

        val user1 = memberService.findByUsername("user1").orElseThrow()
        val user2 = memberService.findByUsername("user2").orElseThrow()
        val user3 = memberService.findByUsername("user3").orElseThrow()

        val post1 = postService.write(user1, "제목 1", "내용 1")
        val post2 = postService.write(user1, "제목 2", "내용 2")
        val post3 = postService.write(user2, "제목 3", "내용 3")

        post1.apply {
            addComment(user1, "댓글 1-1")
            addComment(user1, "댓글 1-2")
            addComment(user2, "댓글 1-3")
        }

        post2.apply {
            addComment(user3, "댓글 2-1")
            addComment(user3, "댓글 2-2")
        }
    }
}