package com.otoki.powersales.domain.activity.suggestion.service

import com.otoki.powersales.domain.activity.suggestion.service.AdminLogisticsClaimRegistTestService
import com.otoki.powersales.domain.activity.suggestion.dto.request.AdminLogisticsClaimRegistTestRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import tools.jackson.databind.ObjectMapper

@DisplayName("AdminLogisticsClaimRegistTestService (SF 물류 클레임 등록 전송 테스트 도구)")
class AdminLogisticsClaimRegistTestServiceTest {

    private val objectMapper = ObjectMapper()
    private val service = AdminLogisticsClaimRegistTestService(objectMapper)

    private fun baseRequest() = AdminLogisticsClaimRegistTestRequest(
        sapAccountCode = "ACC001",
        productCode = "P001",
        employeeCode = "100123",
        claimType = "배송시간 지연",
        claimDate = "2026-06-13",
        title = "배송 지연 클레임",
        description = "상세 내용",
        carNumber = "12가3456",
    )

    @Suppress("UNCHECKED_CAST")
    private fun parsePayload(json: String): Map<String, Any?> =
        objectMapper.readValue(json, Map::class.java) as Map<String, Any?>

    @BeforeEach
    fun setUp() {
        // no-op (서비스는 무상태 — DB/외부 호출 없음)
    }

    @Test
    @DisplayName("apiMap key 셋 정합 — 레거시 ProposalRegist Input 클래스 key 와 동일 + 물류 클레임 필드 채워짐")
    fun test_buildsApiMapWithLegacyKeys() {
        val response = service.test(userId = 1L, request = baseRequest(), photo1 = null, photo2 = null)

        // SF 전송 미구현 단계 — 항상 미전송 상태
        assertThat(response.success).isFalse()
        assertThat(response.resultCode).isNull()
        assertThat(response.rawResponse).isNull()
        assertThat(response.note).contains("미리보기")

        val payload = parsePayload(response.requestPayload)
        // 레거시 Input 클래스 16개 key 모두 존재
        assertThat(payload.keys).containsExactlyInAnyOrder(
            "Category", "Type", "ProductCode", "SAPAccountCode", "accountCode",
            "Title", "Description", "EmployeeCode", "CarNumber", "claimList",
            "logclaimDate",
            "S3ImageUniqueKey1", "S3ImageFileSize1", "S3ImageFileName1",
            "S3ImageUniqueKey2", "S3ImageFileSize2", "S3ImageFileName2",
        )
        assertThat(payload["Category"]).isEqualTo("물류 클레임")
        assertThat(payload["claimList"]).isEqualTo("배송시간 지연")
        assertThat(payload["logclaimDate"]).isEqualTo("2026-06-13")
        assertThat(payload["CarNumber"]).isEqualTo("12가3456")
        // accountCode 와 SAPAccountCode 모두 거래처 코드로 채워짐 (레거시는 accountCode 만 사용)
        assertThat(payload["accountCode"]).isEqualTo("ACC001")
        assertThat(payload["SAPAccountCode"]).isEqualTo("ACC001")
        // 사용처 없는 Type 은 null
        assertThat(payload["Type"]).isNull()
    }

    @Test
    @DisplayName("입력값 trim 처리 + 차량번호 공백이면 null")
    fun test_trimsAndNullsBlankCarNumber() {
        val request = baseRequest().copy(
            sapAccountCode = "  ACC001  ",
            productCode = "  P001 ",
            carNumber = "   ",
        )
        val payload = parsePayload(
            service.test(userId = 1L, request = request, photo1 = null, photo2 = null).requestPayload,
        )
        assertThat(payload["accountCode"]).isEqualTo("ACC001")
        assertThat(payload["ProductCode"]).isEqualTo("P001")
        assertThat(payload["CarNumber"]).isNull()
    }

    @Test
    @DisplayName("사진 첨부 시 파일명/크기가 S3Image 메타에 반영, UniqueKey 는 항상 null (업로드 전)")
    fun test_photoMetaReflectedUniqueKeyNull() {
        val photo1 = MockMultipartFile("photo1", "front.jpg", "image/jpeg", ByteArray(123))
        val photo2 = MockMultipartFile("photo2", "back.jpg", "image/jpeg", ByteArray(45))

        val payload = parsePayload(
            service.test(userId = 1L, request = baseRequest(), photo1 = photo1, photo2 = photo2).requestPayload,
        )
        assertThat(payload["S3ImageFileName1"]).isEqualTo("front.jpg")
        assertThat(payload["S3ImageFileSize1"]).isEqualTo("123")
        assertThat(payload["S3ImageUniqueKey1"]).isNull()
        assertThat(payload["S3ImageFileName2"]).isEqualTo("back.jpg")
        assertThat(payload["S3ImageFileSize2"]).isEqualTo("45")
        assertThat(payload["S3ImageUniqueKey2"]).isNull()
    }

    @Test
    @DisplayName("사진 미첨부 시 S3Image 메타는 모두 null")
    fun test_noPhotoNullMeta() {
        val payload = parsePayload(
            service.test(userId = 1L, request = baseRequest(), photo1 = null, photo2 = null).requestPayload,
        )
        assertThat(payload["S3ImageFileName1"]).isNull()
        assertThat(payload["S3ImageFileSize1"]).isNull()
        assertThat(payload["S3ImageFileName2"]).isNull()
        assertThat(payload["S3ImageFileSize2"]).isNull()
    }
}
