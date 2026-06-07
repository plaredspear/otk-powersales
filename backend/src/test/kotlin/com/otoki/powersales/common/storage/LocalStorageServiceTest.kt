package com.otoki.powersales.common.storage

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("LocalStorageService 테스트")
class LocalStorageServiceTest {

	private lateinit var storage: LocalStorageService

	@BeforeEach
	fun setUp() {
		storage = LocalStorageService()
	}

	@Nested
	@DisplayName("upload - 업로드")
	inner class UploadTests {

		@Test
		@DisplayName("정상 업로드 - 키가 도메인/날짜 prefix 로 시작")
		fun upload_success() {
			val bytes = "hello".toByteArray()
			val result = storage.upload("daily-sales", "photo.jpg", bytes, "image/jpeg")

			assertThat(result.key).startsWith("uploads/daily-sales/")
			assertThat(result.key).endsWith(".jpg")
			assertThat(result.contentType).isEqualTo("image/jpeg")
			assertThat(result.originalName).isEqualTo("photo.jpg")
			assertThat(result.sizeBytes).isEqualTo(bytes.size.toLong())
		}

		@Test
		@DisplayName("업로드 후 download 로 동일 바이트 회수")
		fun upload_then_download() {
			val bytes = byteArrayOf(1, 2, 3, 4, 5)
			val result = storage.upload("education", "doc.pdf", bytes, "application/pdf")

			assertThat(storage.download(result.key)).isEqualTo(bytes)
		}

		@Test
		@DisplayName("허용되지 않은 contentType -> UnsupportedMediaTypeException")
		fun upload_unsupportedType() {
			assertThatThrownBy {
				storage.upload("education", "x.exe", ByteArray(10), "application/octet-stream")
			}.isInstanceOf(UnsupportedMediaTypeException::class.java)
		}

		@Test
		@DisplayName("크기 초과 -> FileTooLargeException")
		fun upload_tooLarge() {
			val bytes = ByteArray((StorageConstants.MAX_FILE_BYTES + 1).toInt())
			assertThatThrownBy {
				storage.upload("education", "x.pdf", bytes, "application/pdf")
			}.isInstanceOf(FileTooLargeException::class.java)
		}
	}

	@Nested
	@DisplayName("download - 다운로드")
	inner class DownloadTests {

		@Test
		@DisplayName("미존재 키 -> StorageNotFoundException")
		fun download_notFound() {
			assertThatThrownBy { storage.download("uploads/missing/key.jpg") }
				.isInstanceOf(StorageNotFoundException::class.java)
		}
	}

	@Nested
	@DisplayName("delete - 삭제")
	inner class DeleteTests {

		@Test
		@DisplayName("삭제 후 download 시 NotFound")
		fun delete_then_download() {
			val result = storage.upload("education", "x.png", byteArrayOf(1), "image/png")
			storage.delete(result.key)

			assertThatThrownBy { storage.download(result.key) }
				.isInstanceOf(StorageNotFoundException::class.java)
		}

		@Test
		@DisplayName("미존재 키 삭제 - no-op")
		fun delete_nonexistent_noop() {
			storage.delete("uploads/none.jpg")
		}
	}

	@Nested
	@DisplayName("getUrl - URL 생성")
	inner class GetUrlTests {

		@Test
		@DisplayName("로컬 구현은 local:// 스킴 반환")
		fun getUrl_localScheme() {
			val url = storage.getUrl("uploads/x/y.jpg", 60)
			assertThat(url).isEqualTo("local://uploads/x/y.jpg")
		}
	}

	@Nested
	@DisplayName("private 연산 - 인증 기반 객체")
	inner class PrivateTests {

		@Test
		@DisplayName("uploadPrivate - 반환 key 는 segment 없는 uniqueKey")
		fun uploadPrivate_returnsSegmentFreeKey() {
			val result = storage.uploadPrivate("claim", "photo.jpg", "hi".toByteArray(), "image/jpeg")

			assertThat(result.key).startsWith("uploads/claim/")
			assertThat(result.key).doesNotStartWith("private/")
		}

		@Test
		@DisplayName("uploadPrivate 후 downloadPrivate 로 동일 바이트 회수 (segment 합성 정합)")
		fun uploadPrivate_then_downloadPrivate() {
			val bytes = byteArrayOf(9, 8, 7)
			val result = storage.uploadPrivate("claim", "photo.jpg", bytes, "image/jpeg")

			assertThat(storage.downloadPrivate(result.key)).isEqualTo(bytes)
		}

		@Test
		@DisplayName("private 객체는 public download 로 회수 불가 (segment 격리)")
		fun privateObject_notAccessibleViaPublicDownload() {
			val result = storage.uploadPrivate("claim", "photo.jpg", byteArrayOf(1), "image/jpeg")

			assertThatThrownBy { storage.download(result.key) }
				.isInstanceOf(StorageNotFoundException::class.java)
		}

		@Test
		@DisplayName("deletePrivate 후 downloadPrivate 시 NotFound")
		fun deletePrivate_then_downloadPrivate() {
			val result = storage.uploadPrivate("claim", "photo.jpg", byteArrayOf(1), "image/jpeg")
			storage.deletePrivate(result.key)

			assertThatThrownBy { storage.downloadPrivate(result.key) }
				.isInstanceOf(StorageNotFoundException::class.java)
		}

		@Test
		@DisplayName("getPresignedUrl - 로컬 구현은 local:// 스킴 반환")
		fun getPresignedUrl_localScheme() {
			val url = storage.getPresignedUrl("uploads/claim/x.jpg", 600)
			assertThat(url).isEqualTo("local://uploads/claim/x.jpg")
		}
	}
}
