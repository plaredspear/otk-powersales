package com.otoki.powersales.domain.support.notice.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class NoticeCreateRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    @field:Size(max = 200, message = "제목은 200자 이하여야 합니다")
    val title: String,

    @field:NotBlank(message = "공개범위는 필수입니다")
    val scope: String,

    @field:NotBlank(message = "카테고리는 필수입니다")
    val category: String,

    @field:NotBlank(message = "내용은 필수입니다")
    val content: String,

    val branch: String? = null,
    val branchCode: String? = null,

    /**
     * 이번 편집 세션에서 본문에 삽입 목적으로 업로드한 인라인 이미지 refid(= upload_file.id) 목록.
     * 저장 시 최종 본문에서 빠진 이미지를 정리(S3+soft-delete)하는 대상 판별에 쓴다.
     * 서버는 이 목록에 든 파일만 정리 후보로 보므로, 타 세션이 올린 미저장 파일에는 간섭하지 않는다.
     * 미전송(null) 시 정리를 수행하지 않는다(하위호환).
     */
    val sessionUploadedRefids: List<String>? = null,

    /** true=발행(PUBLISHED), false=임시저장(DRAFT). 저장/발행 버튼 분리. 기본 임시저장. */
    val publish: Boolean = false
)
