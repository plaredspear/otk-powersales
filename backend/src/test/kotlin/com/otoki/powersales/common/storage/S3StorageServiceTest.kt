package com.otoki.powersales.common.storage

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import software.amazon.awssdk.awscore.exception.AwsErrorDetails
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse
import software.amazon.awssdk.services.s3.model.S3Exception

@DisplayName("S3StorageService 테스트")
class S3StorageServiceTest {

	private val s3Client: S3Client = mockk()

	private lateinit var service: S3StorageService

	@BeforeEach
	fun setUp() {
		service = S3StorageService(s3Client, "test-bucket")
	}

	@Nested
	@DisplayName("upload - S3 PutObject")
	inner class UploadTests {

		@Test
		@DisplayName("정상 업로드 - PutObject 호출 + UploadResult 반환")
		fun upload_success() {
			val captured: CapturingSlot<PutObjectRequest> = slot()
			every { s3Client.putObject(capture(captured), any<RequestBody>()) } returns
				PutObjectResponse.builder().build()

			val bytes = byteArrayOf(1, 2, 3)
			val result = service.upload("daily-sales", "photo.jpg", bytes, "image/jpeg")

			val request = captured.captured
			assertThat(request.bucket()).isEqualTo("test-bucket")
			assertThat(request.key()).startsWith("uploads/daily-sales/")
			assertThat(request.key()).endsWith(".jpg")
			assertThat(request.contentType()).isEqualTo("image/jpeg")

			assertThat(result.key).isEqualTo(request.key())
			assertThat(result.sizeBytes).isEqualTo(bytes.size.toLong())
		}

		@Test
		@DisplayName("허용되지 않은 contentType -> UnsupportedMediaTypeException")
		fun upload_unsupportedType() {
			assertThatThrownBy {
				service.upload("education", "x.exe", ByteArray(10), "application/octet-stream")
			}.isInstanceOf(UnsupportedMediaTypeException::class.java)
		}

		@Test
		@DisplayName("크기 초과 -> FileTooLargeException")
		fun upload_tooLarge() {
			val bytes = ByteArray((StorageConstants.MAX_FILE_BYTES + 1).toInt())
			assertThatThrownBy {
				service.upload("education", "x.pdf", bytes, "application/pdf")
			}.isInstanceOf(FileTooLargeException::class.java)
		}

		@Test
		@DisplayName("S3Exception 발생 -> StorageWriteFailedException")
		fun upload_s3Failure() {
			val s3Ex = S3Exception.builder()
				.awsErrorDetails(AwsErrorDetails.builder().errorMessage("AccessDenied").build())
				.build() as S3Exception
			every { s3Client.putObject(any<PutObjectRequest>(), any<RequestBody>()) } throws s3Ex

			assertThatThrownBy {
				service.upload("daily-sales", "x.jpg", byteArrayOf(1), "image/jpeg")
			}.isInstanceOf(StorageWriteFailedException::class.java)
		}
	}

	@Nested
	@DisplayName("download - S3 GetObject")
	inner class DownloadTests {

		@Test
		@DisplayName("미존재 키 -> StorageNotFoundException")
		fun download_notFound() {
			every { s3Client.getObjectAsBytes(any<GetObjectRequest>()) } throws
				(NoSuchKeyException.builder().build() as NoSuchKeyException)

			assertThatThrownBy { service.download("uploads/x/y.jpg") }
				.isInstanceOf(StorageNotFoundException::class.java)
		}

		@Test
		@DisplayName("정상 다운로드 -> bytes 반환")
		fun download_success() {
			val bytes = byteArrayOf(9, 8, 7)
			val responseBytes: ResponseBytes<GetObjectResponse> =
				ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), bytes)
			every { s3Client.getObjectAsBytes(any<GetObjectRequest>()) } returns responseBytes

			assertThat(service.download("uploads/x/y.jpg")).isEqualTo(bytes)
		}
	}

	@Nested
	@DisplayName("delete - S3 DeleteObject")
	inner class DeleteTests {

		@Test
		@DisplayName("정상 삭제 - DeleteObject 호출")
		fun delete_success() {
			every { s3Client.deleteObject(any<DeleteObjectRequest>()) } returns DeleteObjectResponse.builder().build()

			service.delete("uploads/x/y.jpg")

			verify { s3Client.deleteObject(any<DeleteObjectRequest>()) }
		}

		@Test
		@DisplayName("NoSuchKeyException 발생 -> 무시 (idempotent)")
		fun delete_idempotent() {
			every { s3Client.deleteObject(any<DeleteObjectRequest>()) } throws
				(NoSuchKeyException.builder().build() as NoSuchKeyException)

			service.delete("uploads/x/y.jpg")
		}
	}

	@Nested
	@DisplayName("private 연산 - 실 S3 key 에 private/ segment 합성")
	inner class PrivateTests {

		@Test
		@DisplayName("uploadPrivate - PUT key 는 private/ + uniqueKey, 반환 key 는 segment 없는 uniqueKey")
		fun uploadPrivate_putsUnderPrivateSegment() {
			val captured: CapturingSlot<PutObjectRequest> = slot()
			every { s3Client.putObject(capture(captured), any<RequestBody>()) } returns
				PutObjectResponse.builder().build()

			val result = service.uploadPrivate("claim", "photo.jpg", byteArrayOf(1, 2, 3), "image/jpeg")

			val putKey = captured.captured.key()
			assertThat(putKey).startsWith("private/uploads/claim/")
			assertThat(result.key).isEqualTo(putKey.removePrefix("private/"))
			assertThat(result.key).startsWith("uploads/claim/")
		}

		@Test
		@DisplayName("downloadPrivate - GetObject key 에 private/ segment 합성")
		fun downloadPrivate_readsFromPrivateSegment() {
			val captured: CapturingSlot<GetObjectRequest> = slot()
			val bytes = byteArrayOf(4, 5, 6)
			every { s3Client.getObjectAsBytes(capture(captured)) } returns
				ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), bytes)

			val result = service.downloadPrivate("uploads/claim/2026/01/01/x.jpg")

			assertThat(result).isEqualTo(bytes)
			assertThat(captured.captured.key()).isEqualTo("private/uploads/claim/2026/01/01/x.jpg")
		}

		@Test
		@DisplayName("deletePrivate - DeleteObject key 에 private/ segment 합성")
		fun deletePrivate_deletesFromPrivateSegment() {
			val captured: CapturingSlot<DeleteObjectRequest> = slot()
			every { s3Client.deleteObject(capture(captured)) } returns DeleteObjectResponse.builder().build()

			service.deletePrivate("uploads/claim/2026/01/01/x.jpg")

			assertThat(captured.captured.key()).isEqualTo("private/uploads/claim/2026/01/01/x.jpg")
		}
	}
}
