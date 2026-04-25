package com.otoki.powersales.common.storage

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.amazon.awssdk.awscore.exception.AwsErrorDetails
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse
import software.amazon.awssdk.services.s3.model.S3Exception

@ExtendWith(MockitoExtension::class)
@DisplayName("S3StorageService 테스트")
class S3StorageServiceTest {

	@Mock
	private lateinit var s3Client: S3Client

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
			whenever(s3Client.putObject(any<PutObjectRequest>(), any<RequestBody>()))
				.thenReturn(PutObjectResponse.builder().build())

			val bytes = byteArrayOf(1, 2, 3)
			val result = service.upload("daily-sales", "photo.jpg", bytes, "image/jpeg")

			val captor = ArgumentCaptor.forClass(PutObjectRequest::class.java)
			verify(s3Client).putObject(captor.capture(), any<RequestBody>())
			val captured = captor.value
			assertThat(captured.bucket()).isEqualTo("test-bucket")
			assertThat(captured.key()).startsWith("uploads/daily-sales/")
			assertThat(captured.key()).endsWith(".jpg")
			assertThat(captured.contentType()).isEqualTo("image/jpeg")

			assertThat(result.key).isEqualTo(captured.key())
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
			whenever(s3Client.putObject(any<PutObjectRequest>(), any<RequestBody>())).thenThrow(s3Ex)

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
			whenever(s3Client.getObjectAsBytes(any<GetObjectRequest>()))
				.thenThrow(NoSuchKeyException.builder().build() as NoSuchKeyException)

			assertThatThrownBy { service.download("uploads/x/y.jpg") }
				.isInstanceOf(StorageNotFoundException::class.java)
		}

		@Test
		@DisplayName("정상 다운로드 -> bytes 반환")
		fun download_success() {
			val bytes = byteArrayOf(9, 8, 7)
			val responseBytes: ResponseBytes<GetObjectResponse> =
				ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), bytes)
			whenever(s3Client.getObjectAsBytes(any<GetObjectRequest>())).thenReturn(responseBytes)

			assertThat(service.download("uploads/x/y.jpg")).isEqualTo(bytes)
		}
	}

	@Nested
	@DisplayName("delete - S3 DeleteObject")
	inner class DeleteTests {

		@Test
		@DisplayName("정상 삭제 - DeleteObject 호출")
		fun delete_success() {
			service.delete("uploads/x/y.jpg")
			verify(s3Client).deleteObject(any<DeleteObjectRequest>())
		}

		@Test
		@DisplayName("NoSuchKeyException 발생 -> 무시 (idempotent)")
		fun delete_idempotent() {
			whenever(s3Client.deleteObject(any<DeleteObjectRequest>()))
				.thenThrow(NoSuchKeyException.builder().build() as NoSuchKeyException)

			service.delete("uploads/x/y.jpg")
		}
	}
}
