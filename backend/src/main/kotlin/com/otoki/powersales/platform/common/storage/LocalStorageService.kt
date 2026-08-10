package com.otoki.powersales.platform.common.storage

import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class LocalStorageService : StorageService {

	private val store = ConcurrentHashMap<String, ByteArray>()

	override fun upload(domain: String, originalName: String, bytes: ByteArray, contentType: String): UploadResult {
		if (contentType !in StorageConstants.ALLOWED_CONTENT_TYPES) {
			throw UnsupportedMediaTypeException(contentType)
		}
		if (bytes.size.toLong() > StorageConstants.MAX_FILE_BYTES) {
			throw FileTooLargeException(bytes.size.toLong(), StorageConstants.MAX_FILE_BYTES)
		}

		val key = buildKey(domain, originalName)
		store[key] = bytes.copyOf()

		return UploadResult(
			key = key,
			contentType = contentType,
			originalName = originalName,
			sizeBytes = bytes.size.toLong()
		)
	}

	override fun download(key: String): ByteArray = store[key] ?: throw StorageNotFoundException(key)

	override fun getUrl(key: String, expiresInSeconds: Int): String = "local://$key"

	override fun delete(key: String) {
		store.remove(key)
	}

	override fun uploadPrivate(domain: String, originalName: String, bytes: ByteArray, contentType: String): UploadResult {
		if (contentType !in StorageConstants.ALLOWED_CONTENT_TYPES) {
			throw UnsupportedMediaTypeException(contentType)
		}
		if (bytes.size.toLong() > StorageConstants.MAX_FILE_BYTES) {
			throw FileTooLargeException(bytes.size.toLong(), StorageConstants.MAX_FILE_BYTES)
		}

		val uniqueKey = buildKey(domain, originalName)
		store[StorageConstants.privateKey(uniqueKey)] = bytes.copyOf()

		return UploadResult(
			key = uniqueKey,
			contentType = contentType,
			originalName = originalName,
			sizeBytes = bytes.size.toLong()
		)
	}

	override fun uploadPrivateWithKey(uniqueKey: String, bytes: ByteArray, contentType: String): UploadResult {
		if (contentType !in StorageConstants.EDUCATION_ALLOWED_CONTENT_TYPES) {
			throw UnsupportedMediaTypeException(contentType)
		}
		if (bytes.size.toLong() > StorageConstants.EDUCATION_MAX_FILE_BYTES) {
			throw FileTooLargeException(bytes.size.toLong(), StorageConstants.EDUCATION_MAX_FILE_BYTES)
		}

		store[StorageConstants.privateKey(uniqueKey)] = bytes.copyOf()

		return UploadResult(
			key = uniqueKey,
			contentType = contentType,
			originalName = uniqueKey.substringAfterLast('/'),
			sizeBytes = bytes.size.toLong()
		)
	}

	override fun uploadLargePrivate(domain: String, originalName: String, bytes: ByteArray, contentType: String): UploadResult {
		if (bytes.size.toLong() > StorageConstants.APP_PACKAGE_MAX_BYTES) {
			throw FileTooLargeException(bytes.size.toLong(), StorageConstants.APP_PACKAGE_MAX_BYTES)
		}

		val uniqueKey = buildKey(domain, originalName)
		store[StorageConstants.privateKey(uniqueKey)] = bytes.copyOf()

		return UploadResult(
			key = uniqueKey,
			contentType = contentType,
			originalName = originalName,
			sizeBytes = bytes.size.toLong()
		)
	}

	override fun getPresignedUrl(uniqueKey: String, expiresInSeconds: Int): String = "local://$uniqueKey"

	override fun downloadPrivate(uniqueKey: String): ByteArray =
		store[StorageConstants.privateKey(uniqueKey)] ?: throw StorageNotFoundException(uniqueKey)

	override fun deletePrivate(uniqueKey: String) {
		store.remove(StorageConstants.privateKey(uniqueKey))
	}

	// SF 공유 이미지 저장소는 로컬에 없으므로 별도 네임스페이스로만 구분해 담아둔다 (S3 impl 과 동일 계약).
	override fun uploadSfShared(uniqueKey: String, bytes: ByteArray, contentType: String): String {
		if (contentType !in StorageConstants.ALLOWED_CONTENT_TYPES) {
			throw UnsupportedMediaTypeException(contentType)
		}
		if (bytes.size.toLong() > StorageConstants.MAX_FILE_BYTES) {
			throw FileTooLargeException(bytes.size.toLong(), StorageConstants.MAX_FILE_BYTES)
		}

		store[sfSharedKey(uniqueKey)] = bytes.copyOf()
		return uniqueKey
	}

	override fun deleteSfShared(uniqueKey: String) {
		store.remove(sfSharedKey(uniqueKey))
	}

	private fun sfSharedKey(uniqueKey: String): String = "$SF_SHARED_NAMESPACE/$uniqueKey"

	private fun buildKey(domain: String, originalName: String): String {
		val today = LocalDate.now()
		val ext = extractExtension(originalName)
		return "uploads/%s/%04d/%02d/%02d/%s%s".format(
			domain,
			today.year,
			today.monthValue,
			today.dayOfMonth,
			UUID.randomUUID().toString(),
			ext
		)
	}

	private fun extractExtension(filename: String): String {
		val dot = filename.lastIndexOf('.')
		return if (dot > 0) filename.substring(dot) else ""
	}

	private companion object {
		const val SF_SHARED_NAMESPACE = "sf-shared"
	}
}
