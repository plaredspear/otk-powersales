package com.otoki.powersales.external.sap.inbound.dto.appointment

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

/**
 * SAP 인사발령 페이로드 역직렬화 검증.
 *
 * 레거시 SF `IF_REST_SAP_Appointment` 의 실제 수신 key 는 `Employeecode`(끝 code 소문자).
 * Apex 는 case-insensitive 이지만 Jackson 은 case-sensitive 이므로 정상 PascalCase
 * `EmployeeCode` + 레거시 소문자 alias 둘 다 수용해야 한다.
 */
@DisplayName("AppointmentRequestItem 역직렬화 검증")
class AppointmentRequestItemDeserializeTest {

    private val mapper = JsonMapper.builder().build()

    @Test
    @DisplayName("정상 철자 EmployeeCode 로 역직렬화된다")
    fun deserialize_correctSpelling() {
        val json = """{"EmployeeCode":"100123","JobCode":"J001","AppointDate":"20260401"}"""

        val item = mapper.readValue(json, AppointmentRequestItem::class.java)

        assertThat(item.employeeCode).isEqualTo("100123")
    }

    @Test
    @DisplayName("레거시 소문자 key Employeecode 도 동일 필드로 역직렬화된다 (alias)")
    fun deserialize_legacyLowercaseAlias() {
        val json = """{"Employeecode":"100123","JobCode":"J001","AppointDate":"20260401"}"""

        val item = mapper.readValue(json, AppointmentRequestItem::class.java)

        assertThat(item.employeeCode).isEqualTo("100123")
    }
}
