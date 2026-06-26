package com.otoki.powersales.domain.support.notice.dto.response

/**
 * 공지 본문 인라인 이미지 업로드 Response.
 *
 * 신규 작성/수정 화면(Quill)에서 드래그앤드롭 업로드 시 사용한다.
 * - [refid]: 본문 placeholder `<img data-refid="{refid}">` 에 박을 식별자 (= upload_file.id).
 * - [placeholder]: 본문에 그대로 삽입할 placeholder `<img>` 태그 (NoticeImagePlaceholder.build 결과).
 * - [previewUrl]: 에디터에서 즉시 미리보기용 presigned URL (만료되므로 본문 DB 에는 저장 금지 — placeholder 만 저장).
 *
 * 클라이언트는 에디터에 [previewUrl] 로 보여주되, 저장 시 본문에는 [placeholder] 가 들어가도록 처리한다.
 */
data class NoticeInlineImageResponse(
    val refid: String,
    val placeholder: String,
    val previewUrl: String
)
