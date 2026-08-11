package com.otoki.powersales.platform.common.service

import com.otoki.powersales.platform.common.exception.InvalidFileException
import com.otoki.powersales.platform.common.storage.StorageConstants
import com.otoki.powersales.platform.common.storage.StorageService
import com.otoki.powersales.platform.common.storage.UploadResult
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile

/**
 * 교육 첨부 content-type 보정 가드.
 *
 * 브라우저/OS 는 MIME 레지스트리에 없는 확장자(.hwp 등)에서 content-type 을 비워 보내거나
 * `application/octet-stream` 으로 보낸다. 그대로 두면 화이트리스트 검증 이전에 InvalidFileException 으로
 * 막히므로 파일명 확장자로 보정하되, 보정 결과는 여전히 EDUCATION_ALLOWED_CONTENT_TYPES 를 통과해야 한다
 * (= fallback 이 허용 범위를 넓히지 않는다).
 */
@DisplayName("FileStorageService - 교육 첨부 content-type 보정")
class FileStorageServiceEducationContentTypeTest {

    private val storageService: StorageService = mockk()
    private val service = FileStorageService(storageService)

    /** uploadPrivateWithKey 에 실제로 전달된 content-type 을 회수한다. */
    private fun uploadedContentType(filename: String, declaredContentType: String?): String {
        var captured: String? = null
        every { storageService.uploadPrivateWithKey(any(), any(), any()) } answers {
            captured = thirdArg()
            UploadResult(
                key = firstArg(),
                contentType = thirdArg(),
                originalName = filename,
                sizeBytes = 3
            )
        }

        val file = MockMultipartFile("files", filename, declaredContentType, byteArrayOf(1, 2, 3))
        service.uploadEducationFile(file, "20260811000000")

        return captured ?: error("uploadPrivateWithKey 가 호출되지 않았다")
    }

    @Nested
    @DisplayName("content-type 이 비어 있거나 octet-stream 인 경우")
    inner class UnknownContentTypeTests {

        @Test
        @DisplayName("null content-type -> 확장자로 보정")
        fun nullContentType_resolvedFromExtension() {
            assertThat(uploadedContentType("manual.hwp", null)).isEqualTo("application/x-hwp")
        }

        @Test
        @DisplayName("빈 문자열 content-type -> 확장자로 보정")
        fun blankContentType_resolvedFromExtension() {
            assertThat(uploadedContentType("manual.pptx", "")).isEqualTo(
                "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            )
        }

        @Test
        @DisplayName("application/octet-stream -> 확장자로 보정 (동영상 포함)")
        fun octetStream_resolvedFromExtension() {
            assertThat(uploadedContentType("lesson.mp4", "application/octet-stream")).isEqualTo("video/mp4")
        }

        @Test
        @DisplayName("확장자 대문자 / 매핑 미등재 확장자 처리")
        fun extensionCaseAndUnknownExtension() {
            assertThat(uploadedContentType("LESSON.MP4", null)).isEqualTo("video/mp4")

            assertThatThrownBy { uploadedContentType("payload.exe", null) }
                .isInstanceOf(InvalidFileException::class.java)
        }

        @Test
        @DisplayName("확장자 없는 파일명 -> 보정 불가로 거부")
        fun noExtension_rejected() {
            assertThatThrownBy { uploadedContentType("manual", "application/octet-stream") }
                .isInstanceOf(InvalidFileException::class.java)
        }
    }

    @Nested
    @DisplayName("content-type 이 실려 온 경우")
    inner class DeclaredContentTypeTests {

        @Test
        @DisplayName("선언된 content-type 을 그대로 사용 - 확장자로 덮어쓰지 않는다")
        fun declaredContentType_isKept() {
            assertThat(uploadedContentType("lesson.mp4", "video/mp4")).isEqualTo("video/mp4")
            assertThat(uploadedContentType("photo.jpg", "image/jpeg")).isEqualTo("image/jpeg")
        }
    }

    @Nested
    @DisplayName("보정 결과와 화이트리스트 정합")
    inner class WhitelistConsistencyTests {

        @Test
        @DisplayName("확장자 보정으로 나올 수 있는 모든 값은 교육 화이트리스트에 포함된다")
        fun everyResolvedTypeIsAllowed() {
            // determineFileType(EducationService) 이 f00001/f00002/f00003 으로 분류하는 확장자 전량.
            val extensions = listOf(
                "jpg", "jpeg", "png", "gif",
                "mp4", "avi", "wmv", "mkv", "mov", "m4v",
                "pdf", "docx", "txt", "hwp", "pptx", "xlsx"
            )

            extensions.forEach { ext ->
                val resolved = StorageConstants.resolveEducationContentType(null, "file.$ext")

                assertThat(resolved)
                    .describedAs("확장자 %s 의 fallback 매핑이 없다", ext)
                    .isNotNull()
                assertThat(StorageConstants.EDUCATION_ALLOWED_CONTENT_TYPES)
                    .describedAs("확장자 %s 보정값(%s)이 화이트리스트에 없다", ext, resolved)
                    .contains(resolved)
            }
        }
    }
}
