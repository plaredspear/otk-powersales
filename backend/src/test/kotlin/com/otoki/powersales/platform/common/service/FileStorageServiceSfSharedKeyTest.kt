package com.otoki.powersales.platform.common.service

import com.otoki.powersales.platform.common.storage.StorageService
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * SF 공유 버킷 key 형식 가드 — 레거시 `AWSService.uploadAWS` 의 `System.currentTimeMillis() + 사번`.
 *
 * SF 는 이 key 를 레거시 버킷 주소에 그대로 concat 하므로 형식이 곧 계약이다. prefix(`uploads/`)나
 * 확장자가 섞이면 SF 가 만드는 URL 이 달라진다.
 */
@DisplayName("FileStorageService - SF 공유 버킷 key 형식")
class FileStorageServiceSfSharedKeyTest {

    private val storageService: StorageService = mockk()
    private val service = FileStorageService(storageService)

    private fun capturedKeys(count: Int, employeeCode: String?): List<String> {
        val keys = mutableListOf<String>()
        every { storageService.uploadSfShared(any(), any(), any()) } answers {
            firstArg<String>().also { keys += it }
        }
        repeat(count) {
            service.uploadSuggestionPhotoForSf(byteArrayOf(1, 2, 3), "image/jpeg", employeeCode)
        }
        return keys
    }

    @Test
    fun `key 는 13자리 epoch millis + 사번 — prefix·확장자 없음`() {
        val key = capturedKeys(1, "20030239").single()

        assertThat(key).matches("^\\d{13}20030239$")
        assertThat(key).doesNotContain("/")
        assertThat(key).doesNotContain(".")
    }

    @Test
    fun `같은 밀리초에 연속 업로드해도 key 가 겹치지 않는다`() {
        // 레거시는 여기서 key 가 겹쳐 2번째 사진이 1번째를 덮어쓰고 두 UploadFile__c 가 같은 이미지를 가리켰다.
        val keys = capturedKeys(50, "20030239")

        assertThat(keys).doesNotHaveDuplicates()
        assertThat(keys).allMatch { it.matches(Regex("^\\d{13}20030239$")) }
    }

    @Test
    fun `사번이 없으면 millis 만으로 구성된다`() {
        val key = capturedKeys(1, null).single()

        assertThat(key).matches("^\\d{13}$")
    }
}
