package com.otoki.powersales.common.storage

import java.time.Duration

object StorageConstants {
	const val MAX_FILE_BYTES: Long = 20L * 1024 * 1024

	// private/ 객체의 실제 S3 key 합성 규칙. 버킷 정책: public/ 만 anonymous read 허용, private/ 는 차단.
	// (public 객체의 가시성 prefix 는 S3_PUBLIC_URL_PREFIX 가 URL 단에서 담당하므로 별도 segment 상수 불요.)
	// DB UploadFile.uniqueKey 에는 segment 를 포함하지 않고(= uploads/...), StorageService 레이어가
	// 실제 S3 연산(PUT/GET/DELETE/presign) 시점에 이 helper 로 합성한다 (S3/Local impl 공통 SoT).
	private const val PRIVATE_SEGMENT: String = "private"

	fun privateKey(uniqueKey: String): String = "$PRIVATE_SEGMENT/$uniqueKey"

	// 제품 클레임 이미지 presigned URL 만료 시간(초). 목록/상세 공통. 짧은 세션 내 소비 + 클라이언트
	// 자동 복구로 만료 마찰을 흡수하므로 보수적으로 10분.
	const val CLAIM_PRESIGN_TTL_SECONDS: Int = 600

	// 공지 본문 인라인 이미지 + 첨부 이미지 presigned URL 만료 시간(초). 본문은 긴 글을 천천히 스크롤하며
	// 읽으므로 클레임(600s)보다 길게 30분. web 은 페이지 로드 시 1회 소비, mobile 은 cacheKey(refid)로
	// 재요청을 흡수하므로 만료 마찰이 작다.
	const val NOTICE_PRESIGN_TTL_SECONDS: Int = 1800
	val ALLOWED_CONTENT_TYPES: Set<String> = setOf(
		"image/jpeg",
		"image/png",
		"image/heic",
		"application/pdf",
		"image/jpg",
		"image/gif",
		"image/webp",
		"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
		"application/vnd.ms-excel"
	)
	val DEFAULT_PRESIGN_TTL: Duration = Duration.ofMinutes(5)
}
