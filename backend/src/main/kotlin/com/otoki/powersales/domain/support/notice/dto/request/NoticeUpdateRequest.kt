package com.otoki.powersales.domain.support.notice.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class NoticeUpdateRequest(
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
     * 낙관적 락 버전 — 수정 화면 진입 시 상세조회로 받은 version 을 그대로 되돌려보낸다.
     * 저장 시점의 DB version 과 다르면(= 다른 사용자가 먼저 수정) JPA 가 충돌을 감지해 409 로 거부한다.
     * 하위호환: 미전송(null) 시 낙관적 락 검사를 건너뛴다(구 클라이언트/외부 호출 대비).
     */
    val version: Long? = null,

    /**
     * 이번 편집 세션에서 본문에 삽입 목적으로 업로드한 인라인 이미지 refid(= upload_file.id) 목록.
     * 저장 시 최종 본문에서 빠진 이미지를 정리(S3+soft-delete)하는 대상 판별에 쓴다.
     * 서버는 이 목록에 든 파일만 정리 후보로 보므로, 타 세션이 올린 미저장 파일에는 간섭하지 않는다.
     * 미전송(null) 시 세션 업로드분 정리를 수행하지 않는다(하위호환). 수정 시 이 공지 소속분 정리는 별도 수행.
     */
    val sessionUploadedRefids: List<String>? = null,

    /** true=발행(PUBLISHED), false=임시저장(DRAFT). 임시저장은 무조건 DRAFT 로 전환(발행취소 효과). */
    val publish: Boolean = false
)
