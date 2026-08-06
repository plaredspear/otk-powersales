package com.otoki.powersales.domain.activity.suggestion.service

import com.otoki.powersales.platform.common.service.FileStorageService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * 레거시 `FieldTalkController.suggestProc` 1장 처리 정합 검증.
 *
 * 고정하는 계약: **축소본 1벌**을 만들어 private/SF 두 사본에 동일하게 쓰고, SF 로 가는 파일명·크기도
 * 축소본 기준이라는 것. 원본을 그대로 올리면 SF 화면 이미지가 레거시와 달라진다.
 */
@DisplayName("SuggestionPhotoUploader - 레거시 suggestProc 이미지 처리 정합")
class SuggestionPhotoUploaderTest {

    private val fileStorageService: FileStorageService = mockk()
    private val uploader = SuggestionPhotoUploader(fileStorageService)

    private fun photo(width: Int, height: Int, name: String = "photo.jpg"): MockMultipartFile {
        val out = ByteArrayOutputStream()
        ImageIO.write(BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "jpg", out)
        return MockMultipartFile("photos", name, "image/jpeg", out.toByteArray())
    }

    @Test
    fun `private 사본과 SF 사본은 동일한 축소본 바이트를 쓴다`() {
        val privateBytes = slot<ByteArray>()
        val sfBytes = slot<ByteArray>()
        every { fileStorageService.uploadSuggestionPhoto(capture(privateBytes), any(), any()) } returns "uploads/suggestion/x.jpg"
        every { fileStorageService.uploadSuggestionPhotoForSf(capture(sfBytes), any(), any()) } returns "1750000000000E777"

        val stored = uploader.store(photo(1300, 650), "E777")

        assertThat(privateBytes.captured).isEqualTo(sfBytes.captured)
        // 원본이 아니라 축소본이어야 한다.
        val image = ImageIO.read(ByteArrayInputStream(sfBytes.captured))
        assertThat(image.width to image.height).isEqualTo(650 to 325)
        assertThat(stored.uniqueKey).isEqualTo("uploads/suggestion/x.jpg")
        assertThat(stored.sfUniqueKey).isEqualTo("1750000000000E777")
    }

    @Test
    fun `파일명은 축소본 이름, 크기는 축소본 크기를 레거시 포맷으로 담는다`() {
        val sfBytes = slot<ByteArray>()
        every { fileStorageService.uploadSuggestionPhoto(any(), any(), any()) } returns "uploads/suggestion/x.jpg"
        every { fileStorageService.uploadSuggestionPhotoForSf(capture(sfBytes), any(), any()) } returns "key"

        val stored = uploader.store(photo(1300, 650), "E777")

        assertThat(stored.fileName).isEqualTo("photo_resize.jpg")
        // 원본 크기가 아니라 축소본 크기 기준 — 레거시 getFileSize(file.length()) 정합.
        assertThat(stored.fileSize).isEqualTo(LegacyFileSizeFormatter.format(sfBytes.captured.size.toLong()))
    }

    @Test
    fun `사번은 SF 공유 버킷 key 구성에 그대로 넘어간다`() {
        val employeeCode = slot<String>()
        every { fileStorageService.uploadSuggestionPhoto(any(), any(), any()) } returns "uploads/suggestion/x.jpg"
        every { fileStorageService.uploadSuggestionPhotoForSf(any(), any(), capture(employeeCode)) } returns "key"

        uploader.store(photo(100, 100), "E777")

        assertThat(employeeCode.captured).isEqualTo("E777")
    }

    @Test
    fun `공유 버킷 미설정이면 sfUniqueKey 는 null 이고 등록은 계속된다`() {
        every { fileStorageService.uploadSuggestionPhoto(any(), any(), any()) } returns "uploads/suggestion/x.jpg"
        every { fileStorageService.uploadSuggestionPhotoForSf(any(), any(), any()) } returns null

        val stored = uploader.store(photo(100, 100), "E777")

        assertThat(stored.sfUniqueKey).isNull()
        assertThat(stored.uniqueKey).isEqualTo("uploads/suggestion/x.jpg")
    }

    @Test
    fun `임시저장은 축소본 private 사본만 만든다 — SF 사본 없음`() {
        val privateBytes = slot<ByteArray>()
        val privateName = slot<String>()
        every { fileStorageService.uploadSuggestionPhoto(capture(privateBytes), capture(privateName), any()) } returns "uploads/suggestion/draft.jpg"

        val key = uploader.storeDraft(photo(1300, 650))

        assertThat(key).isEqualTo("uploads/suggestion/draft.jpg")
        assertThat(privateName.captured).isEqualTo("photo_resize.jpg")
        val image = ImageIO.read(ByteArrayInputStream(privateBytes.captured))
        assertThat(image.width to image.height).isEqualTo(650 to 325)
    }
}
