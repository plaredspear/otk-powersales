package com.otoki.powersales.platform.common.storage

interface StorageService {
	fun upload(domain: String, originalName: String, bytes: ByteArray, contentType: String): UploadResult

	/** public 객체 다운로드. 현재 production 호출처 없음 — private 연산과의 대칭 API 로 유지. */
	fun download(key: String): ByteArray

	fun getUrl(key: String, expiresInSeconds: Int): String

	fun delete(key: String)

	// ───── private(인증 기반) 객체 연산 ─────
	// 실제 S3 key = "private/" + 반환/인자 uniqueKey 로 합성된다. 반환 uniqueKey 와 인자 uniqueKey 는
	// segment 를 포함하지 않는다(= uploads/...) — DB UploadFile.uniqueKey 와 동일 형태.
	// public 객체와 달리 고정 URL 이 아니라 presigned URL 로만 조회 가능하다.

	/** private/ 하위로 업로드. 반환 UploadResult.key 는 segment 없는 uniqueKey. */
	fun uploadPrivate(domain: String, originalName: String, bytes: ByteArray, contentType: String): UploadResult

	/**
	 * private/ 하위로 업로드하되 uniqueKey 를 호출자가 직접 지정한다. 실제 S3 key = "private/" + uniqueKey.
	 * 교육 첨부처럼 DB 컬럼 길이 제약(예: file_key length=30)으로 buildKey 의 `uploads/<domain>/<날짜>/<uuid>`
	 * 형식을 쓸 수 없고 짧은 key 규칙을 도메인이 소유해야 하는 경우에 사용한다.
	 */
	fun uploadPrivateWithKey(uniqueKey: String, bytes: ByteArray, contentType: String): UploadResult

	/**
	 * private/ 하위로 대용량 바이너리(앱 패키지 APK/IPA) 업로드. uploadPrivate 와 달리 contentType 화이트리스트를
	 * 적용하지 않고 크기 상한도 [StorageConstants.APP_PACKAGE_MAX_BYTES] 로 별도 적용한다. 반환 key 는 segment 없는 uniqueKey.
	 */
	fun uploadLargePrivate(domain: String, originalName: String, bytes: ByteArray, contentType: String): UploadResult

	/** private/ 객체의 presigned GET URL 발급. */
	fun getPresignedUrl(uniqueKey: String, expiresInSeconds: Int): String

	/** private/ 객체 바이트 다운로드. */
	fun downloadPrivate(uniqueKey: String): ByteArray

	/** private/ 객체 삭제. */
	fun deletePrivate(uniqueKey: String)
}
