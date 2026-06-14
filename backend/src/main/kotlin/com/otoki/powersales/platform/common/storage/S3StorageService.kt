package com.otoki.powersales.platform.common.storage

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.time.Duration
import java.time.LocalDate
import java.util.UUID

// 정책: 로컬 개발 시 S3 접근 미사용 (LocalStorageService stub 사용).
// dev/prod 에서만 S3StorageService 를 활성화한다. 그 외 프로파일(local 등)에서는
// StorageConfig 의 @ConditionalOnMissingBean 이 LocalStorageService 를 등록.
@Service
@Profile("dev | prod")
class S3StorageService(
	private val s3Client: S3Client,
	@Value("\${app.aws.s3.bucket}") private val bucket: String
) : StorageService {

	private val presigner: S3Presigner by lazy { S3Presigner.create() }

	override fun upload(domain: String, originalName: String, bytes: ByteArray, contentType: String): UploadResult {
		val key = buildKey(domain, originalName)
		putObject(key, bytes, contentType)
		return UploadResult(
			key = key,
			contentType = contentType,
			originalName = originalName,
			sizeBytes = bytes.size.toLong()
		)
	}

	override fun download(key: String): ByteArray = getObjectBytes(key)

	override fun getUrl(key: String, expiresInSeconds: Int): String = presignGet(key, expiresInSeconds)

	override fun delete(key: String) = deleteObject(key)

	override fun uploadPrivate(domain: String, originalName: String, bytes: ByteArray, contentType: String): UploadResult {
		val uniqueKey = buildKey(domain, originalName)
		putObject(StorageConstants.privateKey(uniqueKey), bytes, contentType)
		return UploadResult(
			key = uniqueKey,
			contentType = contentType,
			originalName = originalName,
			sizeBytes = bytes.size.toLong()
		)
	}

	override fun uploadLargePrivate(domain: String, originalName: String, bytes: ByteArray, contentType: String): UploadResult {
		val uniqueKey = buildKey(domain, originalName)
		putLargeObject(StorageConstants.privateKey(uniqueKey), bytes, contentType)
		return UploadResult(
			key = uniqueKey,
			contentType = contentType,
			originalName = originalName,
			sizeBytes = bytes.size.toLong()
		)
	}

	override fun getPresignedUrl(uniqueKey: String, expiresInSeconds: Int): String =
		presignGet(StorageConstants.privateKey(uniqueKey), expiresInSeconds)

	override fun downloadPrivate(uniqueKey: String): ByteArray = getObjectBytes(StorageConstants.privateKey(uniqueKey))

	override fun deletePrivate(uniqueKey: String) = deleteObject(StorageConstants.privateKey(uniqueKey))

	private fun putObject(key: String, bytes: ByteArray, contentType: String) {
		if (contentType !in StorageConstants.ALLOWED_CONTENT_TYPES) {
			throw UnsupportedMediaTypeException(contentType)
		}
		if (bytes.size.toLong() > StorageConstants.MAX_FILE_BYTES) {
			throw FileTooLargeException(bytes.size.toLong(), StorageConstants.MAX_FILE_BYTES)
		}

		val request = PutObjectRequest.builder()
			.bucket(bucket)
			.key(key)
			.contentType(contentType)
			.contentLength(bytes.size.toLong())
			.build()

		try {
			s3Client.putObject(request, RequestBody.fromBytes(bytes))
		} catch (ex: S3Exception) {
			throw StorageWriteFailedException(reason = ex.awsErrorDetails().errorMessage(), cause = ex)
		}
	}

	private fun putLargeObject(key: String, bytes: ByteArray, contentType: String) {
		if (bytes.size.toLong() > StorageConstants.APP_PACKAGE_MAX_BYTES) {
			throw FileTooLargeException(bytes.size.toLong(), StorageConstants.APP_PACKAGE_MAX_BYTES)
		}

		val request = PutObjectRequest.builder()
			.bucket(bucket)
			.key(key)
			.contentType(contentType)
			.contentLength(bytes.size.toLong())
			.build()

		try {
			s3Client.putObject(request, RequestBody.fromBytes(bytes))
		} catch (ex: S3Exception) {
			throw StorageWriteFailedException(reason = ex.awsErrorDetails().errorMessage(), cause = ex)
		}
	}

	private fun getObjectBytes(key: String): ByteArray {
		val request = GetObjectRequest.builder().bucket(bucket).key(key).build()
		try {
			return s3Client.getObjectAsBytes(request).asByteArray()
		} catch (_: NoSuchKeyException) {
			throw StorageNotFoundException(key)
		}
	}

	private fun presignGet(key: String, expiresInSeconds: Int): String {
		val getRequest = GetObjectRequest.builder().bucket(bucket).key(key).build()
		val presignRequest = GetObjectPresignRequest.builder()
			.signatureDuration(Duration.ofSeconds(expiresInSeconds.toLong()))
			.getObjectRequest(getRequest)
			.build()
		return presigner.presignGetObject(presignRequest).url().toString()
	}

	private fun deleteObject(key: String) {
		try {
			s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build())
		} catch (_: NoSuchKeyException) {
			// idempotent
		}
	}

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
}
