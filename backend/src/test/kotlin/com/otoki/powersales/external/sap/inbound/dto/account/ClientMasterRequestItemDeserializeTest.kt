package com.otoki.powersales.external.sap.inbound.dto.account

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

/**
 * SAP 거래처 마스터 페이로드 역직렬화 검증.
 *
 * 레거시 SF `IF_REST_SAP_ClientMasterReceive` 의 실제 수신 key 는 오타 `BusinessLiicenseNumber`(i 2개).
 * SAP RESTAdapter 호환을 위해 정상 철자 `BusinessLicenseNumber` + 오타 alias 둘 다 수용해야 한다.
 */
@DisplayName("ClientMasterRequestItem 역직렬화 검증")
class ClientMasterRequestItemDeserializeTest {

    private val mapper = JsonMapper.builder().build()

    @Test
    @DisplayName("정상 철자 BusinessLicenseNumber 로 역직렬화된다")
    fun deserialize_correctSpelling() {
        val json = """{"SAPAccountCode":"1032619","Name":"홍길동상회","BusinessLicenseNumber":"123-45-67890"}"""

        val item = mapper.readValue(json, ClientMasterRequestItem::class.java)

        assertThat(item.sapAccountCode).isEqualTo("1032619")
        assertThat(item.businessLicenseNumber).isEqualTo("123-45-67890")
    }

    @Test
    @DisplayName("레거시 오타 key BusinessLiicenseNumber 도 동일 필드로 역직렬화된다 (alias)")
    fun deserialize_legacyTypoAlias() {
        val json = """{"SAPAccountCode":"1032619","Name":"홍길동상회","BusinessLiicenseNumber":"123-45-67890"}"""

        val item = mapper.readValue(json, ClientMasterRequestItem::class.java)

        assertThat(item.businessLicenseNumber).isEqualTo("123-45-67890")
    }
}
