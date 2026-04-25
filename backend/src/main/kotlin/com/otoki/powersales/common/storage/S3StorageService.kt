package com.otoki.powersales.common.storage

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
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

@Service
@ConditionalOnProperty(name = ["app.aws.s3.bucket"])
class S3StorageService(
	private val s3Client: S3Client,
	@Value("\${app.aws.s3.bucket}") private val bucket: String
) : StorageService {

	private val presigner: S3Presigner by lazy { S3Presigner.create() }

	override fun upload(domain: String, originalName: String, bytes: ByteArray, contentType: String): UploadResult {
		if (contentType !in StorageConstants.ALLOWED_CONTENT_TYPES) {
			throw UnsupportedMediaTypeException(contentType)
		}
		if (bytes.size.toLong() > StorageConstants.MAX_FILE_BYTES) {
			throw FileTooLargeException(bytes.size.toLong(), StorageConstants.MAX_FILE_BYTES)
		}

		val key = buildKey(domain, originalName)
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

		return UploadResult(
			key = key,
			contentType = contentType,
			originalName = originalName,
			sizeBytes = bytes.size.toLong()
		)
	}

	override fun download(key: String): ByteArray {
		val request = GetObjectRequest.builder().bucket(bucket).key(key).build()
		try {
			return s3Client.getObjectAsBytes(request).asByteArray()
		} catch (_: NoSuchKeyException) {
			throw StorageNotFoundException(key)
		}
	}

	override fun getUrl(key: String, expiresInSeconds: Int): String {
		val getRequest = GetObjectRequest.builder().bucket(bucket).key(key).build()
		val presignRequest = GetObjectPresignRequest.builder()
			.signatureDuration(Duration.ofSeconds(expiresInSeconds.toLong()))
			.getObjectRequest(getRequest)
			.build()
		return presigner.presignGetObject(presignRequest).url().toString()
	}

	override fun delete(key: String) {
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
