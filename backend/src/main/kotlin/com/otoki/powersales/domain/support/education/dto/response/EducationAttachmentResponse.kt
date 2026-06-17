package com.otoki.powersales.domain.support.education.dto.response

/**
 * 교육 게시물 첨부파일 Response
 *
 * `fileType` 은 레거시 `edu_file_type` picklist (f00001 이미지 / f00002 동영상 / f00003 문서 / f00004 기타).
 * 클라이언트가 이 값으로 인라인 이미지·동영상·문서 링크 렌더를 분기한다(레거시 view.jsp 정합).
 * `fileUrl` 은 조회용 presigned URL.
 */
data class EducationAttachmentResponse(
    val id: String,
    val fileName: String,
    val fileUrl: String,
    val fileType: String,
    val fileSize: Long  // bytes
)
