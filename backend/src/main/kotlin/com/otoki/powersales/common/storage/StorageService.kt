package com.otoki.powersales.common.storage

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

	/** private/ 객체의 presigned GET URL 발급. */
	fun getPresignedUrl(uniqueKey: String, expiresInSeconds: Int): String

	/** private/ 객체 바이트 다운로드. */
	fun downloadPrivate(uniqueKey: String): ByteArray

	/** private/ 객체 삭제. */
	fun deletePrivate(uniqueKey: String)
}
