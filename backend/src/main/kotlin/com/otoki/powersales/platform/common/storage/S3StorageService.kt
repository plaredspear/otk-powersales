package com.otoki.powersales.platform.common.storage

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.ObjectCannedACL
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
	@Value("\${app.aws.s3.bucket}") private val bucket: String,
	// SF 공유 이미지 저장소 버킷 (레거시 ottogi-nonsap-{dev|prd}-imagerepository-s3). 미설정이면 SF 사본 업로드 skip.
	@Value("\${app.aws.s3.sf-image-bucket:}") private val sfImageBucket: String
) : StorageService {

	private val log = LoggerFactory.getLogger(javaClass)

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

	override fun uploadPrivateWithKey(uniqueKey: String, bytes: ByteArray, contentType: String): UploadResult {
		putObject(StorageConstants.privateKey(uniqueKey), bytes, contentType)
		return UploadResult(
			key = uniqueKey,
			contentType = contentType,
			originalName = uniqueKey.substringAfterLast('/'),
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

	override fun uploadSfShared(uniqueKey: String, bytes: ByteArray, contentType: String): String? {
		if (sfImageBucket.isBlank()) {
			log.warn("SF 공유 이미지 버킷 미설정(app.aws.s3.sf-image-bucket) — SF 사본 업로드 skip key={}", uniqueKey)
			return null
		}
		if (contentType !in StorageConstants.ALLOWED_CONTENT_TYPES) {
			throw UnsupportedMediaTypeException(contentType)
		}
		if (bytes.size.toLong() > StorageConstants.MAX_FILE_BYTES) {
			throw FileTooLargeException(bytes.size.toLong(), StorageConstants.MAX_FILE_BYTES)
		}

		// AWS SDK builder 는 가변 객체라 재사용하면 ACL 이 남는다 — 호출마다 새 builder 로 조립한다.
		fun request(acl: ObjectCannedACL?): PutObjectRequest = PutObjectRequest.builder()
			.bucket(sfImageBucket)
			.key(uniqueKey)
			.contentType(contentType)
			.contentLength(bytes.size.toLong())
			.apply { acl?.let { acl(it) } }
			.build()

		val body = RequestBody.fromBytes(bytes)
		try {
			s3Client.putObject(request(ObjectCannedACL.PUBLIC_READ), body)
		} catch (ex: S3Exception) {
			// 버킷이 "Bucket owner enforced"(ACL 비활성) 로 전환된 경우 ACL 지정 PUT 이 거절된다.
			// 그 설정에서는 익명 read 를 버킷 정책이 담당하므로 ACL 없이 재시도하면 동일 결과가 된다.
			if (ex.awsErrorDetails()?.errorCode() != ACL_NOT_SUPPORTED_ERROR_CODE) {
				throw StorageWriteFailedException(reason = ex.awsErrorDetails().errorMessage(), cause = ex)
			}
			try {
				s3Client.putObject(request(null), body)
			} catch (retry: S3Exception) {
				throw StorageWriteFailedException(reason = retry.awsErrorDetails().errorMessage(), cause = retry)
			}
		}
		return uniqueKey
	}

	override fun deleteSfShared(uniqueKey: String) {
		if (sfImageBucket.isBlank()) return
		try {
			s3Client.deleteObject(DeleteObjectRequest.builder().bucket(sfImageBucket).key(uniqueKey).build())
		} catch (_: NoSuchKeyException) {
			// idempotent
		}
	}

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

	private companion object {
		/** S3 "Bucket owner enforced"(ACL 비활성) 버킷이 ACL 지정 PUT 을 거절할 때의 error code. */
		const val ACL_NOT_SUPPORTED_ERROR_CODE = "AccessControlListNotSupported"
	}
}
