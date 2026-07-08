package com.otoki.powersales.domain.org.employee.sfsync

import com.otoki.powersales.external.sf.outbound.SfApiResponse
import com.otoki.powersales.external.sf.outbound.SfOAuthFailedException
import com.otoki.powersales.external.sf.outbound.SfOutboundClient
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingCategory2
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import tools.jackson.databind.ObjectMapper

@DisplayName("StaffReviewFetchClientImpl (SF IF_SendStaffReviewToPWS fetch) 테스트")
class StaffReviewFetchClientImplTest {

    private lateinit var sfOutboundClient: SfOutboundClient
    private lateinit var client: StaffReviewFetchClientImpl

    @BeforeEach
    fun setUp() {
        sfOutboundClient = mockk()
        client = StaffReviewFetchClientImpl(sfOutboundClient, ObjectMapper())
    }

    private fun stub(rawBody: String) {
        every { sfOutboundClient.callApi("/IF_SendStaffReviewToPWS", any()) } returns
            SfApiResponse(resultCode = "200", resultMsg = "OK", rawBody = rawBody)
    }

    @Test
    @DisplayName("MOD_DT 를 /IF_SendStaffReviewToPWS 로 전송하고 최상위 배열 응답을 FetchDto 로 변환")
    fun parsesTopLevelArray() {
        val apiMapSlot = slot<Map<String, Any?>>()
        every { sfOutboundClient.callApi("/IF_SendStaffReviewToPWS", capture(apiMapSlot)) } returns
            SfApiResponse(
                resultCode = "200",
                resultMsg = "OK",
                rawBody = """
                    [
                      {
                        "Id": "a0X0000000000001AAA",
                        "Name": "SR-0001",
                        "DKRetailEmployeeId": "005EMP000000001AAA",
                        "EmployeeName": "홍길동",
                        "EmployeeNumber": "E100",
                        "EmployeeType": "정규직",
                        "EntryDate": "2020-01-15",
                        "EmployeeTotalScore": "25",
                        "Jikwee": "사원",
                        "JobCode": "J01",
                        "FirstDayofMonth": "20260401",
                        "Branch": "서울지점",
                        "CostCenterCode": "1001",
                        "DKRetailWorkingCategory1": "진열",
                        "DKRetailWorkingCategory2": "전담",
                        "Attendance": "3",
                        "EducationalEvaluation": "5",
                        "CreatedById": "005USR000000001AAA",
                        "LastModifiedById": "005USR000000002AAA"
                      }
                    ]
                """.trimIndent(),
            )

        val result = client.fetch("20260410")

        assertThat(apiMapSlot.captured).containsExactlyEntriesOf(mapOf("MOD_DT" to "20260410"))
        assertThat(result).hasSize(1)
        val dto = result.single()
        assertThat(dto.sfid).isEqualTo("a0X0000000000001AAA")
        assertThat(dto.name).isEqualTo("SR-0001")
        assertThat(dto.employeeSfid).isEqualTo("005EMP000000001AAA")
        assertThat(dto.employeeName).isEqualTo("홍길동")
        assertThat(dto.employeeType).isEqualTo("정규직")
        // ISO 날짜 파싱.
        assertThat(dto.entryDate).isEqualTo(LocalDate.of(2020, 1, 15))
        // yyyyMMdd 무구분 날짜 파싱.
        assertThat(dto.firstDayOfMonth).isEqualTo(LocalDate.of(2026, 4, 1))
        assertThat(dto.employeeTotalScore).isEqualTo(25.0)
        assertThat(dto.attendanceScore).isEqualTo(3.0)
        assertThat(dto.educationEvaluationScore).isEqualTo(5.0)
        // 근무유형 한글 displayName → enum 변환.
        assertThat(dto.workingCategory1).isEqualTo(WorkingCategory1.DISPLAY)
        assertThat(dto.workingCategory2).isEqualTo(WorkingCategory2.DEDICATED)
        assertThat(dto.createdBySfid).isEqualTo("005USR000000001AAA")
        assertThat(dto.lastModifiedBySfid).isEqualTo("005USR000000002AAA")
    }

    @Test
    @DisplayName("data wrapper 로 감싼 배열 응답도 추출")
    fun parsesDataWrapper() {
        stub("""{"RESULT_CODE":"200","data":[{"Id":"a0X1","Name":"SR-1"}]}""")

        val result = client.fetch("20260101")

        assertThat(result).hasSize(1)
        assertThat(result.single().sfid).isEqualTo("a0X1")
    }

    @Test
    @DisplayName("운영 SF 응답의 \"Result\" 래퍼(대문자 R)도 대소문자 무시로 추출")
    fun parsesResultWrapperCaseInsensitive() {
        stub("""{"RESULT_CODE":"200","Result":[{"Id":"a0X1","Name":"SR-1"}]}""")

        val result = client.fetch("20260101")

        assertThat(result).hasSize(1)
        assertThat(result.single().sfid).isEqualTo("a0X1")
    }

    @Test
    @DisplayName("알 수 없는 근무유형 값은 null 로 매핑 (예외 없음)")
    fun unknownWorkingCategoryMapsToNull() {
        stub("""[{"Id":"a0X1","Name":"SR-1","DKRetailWorkingCategory1":"미정의값"}]""")

        val result = client.fetch("20260101")

        assertThat(result.single().workingCategory1).isNull()
    }

    @Test
    @DisplayName("빈 배열 응답 — 빈 리스트")
    fun emptyArray() {
        stub("[]")
        assertThat(client.fetch("20260101")).isEmpty()
    }

    @Test
    @DisplayName("배열을 찾을 수 없는 응답(RESULT 래퍼만) — 빈 리스트 (예외 없음)")
    fun noArrayFound() {
        stub("""{"RESULT_CODE":"200","RESULT_MSG":"OK"}""")
        assertThat(client.fetch("20260101")).isEmpty()
    }

    @Test
    @DisplayName("빈 응답 본문 — 빈 리스트")
    fun blankBody() {
        stub("")
        assertThat(client.fetch("20260101")).isEmpty()
    }

    @Test
    @DisplayName("깨진 JSON — 빈 리스트 (예외 흡수)")
    fun malformedJson() {
        stub("{not-json")
        assertThat(client.fetch("20260101")).isEmpty()
    }

    @Test
    @DisplayName("SF 호출 예외(OAuth 실패 등) — 빈 리스트 (배치 비중단)")
    fun callFailureSwallowed() {
        every { sfOutboundClient.callApi("/IF_SendStaffReviewToPWS", any()) } throws
            SfOAuthFailedException("재발급 후에도 401")

        assertThat(client.fetch("20260101")).isEmpty()
        verify(exactly = 1) { sfOutboundClient.callApi("/IF_SendStaffReviewToPWS", any()) }
    }
}
