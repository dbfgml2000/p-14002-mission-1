package com.back.domain.post.postComment.dto

import com.back.domain.post.postComment.entity.PostComment
import java.time.LocalDateTime

class PostCommentDto private constructor( // private을 걸어 해당 생성자 직접 호출 못하게 함
    val id: Int,
    val createDate: LocalDateTime,
    val modifyDate: LocalDateTime,
    val authorId: Int,
    val authorName: String,
    val postId: Int,
    val content: String
) {
    constructor(postComment: PostComment) : this(
        id = postComment.id,
        createDate = postComment.createDate,
        modifyDate = postComment.modifyDate,
        authorId = postComment.author.id,
        authorName = postComment.author.name,
        postId = postComment.post.id,
        content = postComment.content
    )
}