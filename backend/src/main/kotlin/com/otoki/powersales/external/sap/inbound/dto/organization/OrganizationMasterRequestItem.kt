package com.otoki.powersales.external.sap.inbound.dto.organization

import com.fasterxml.jackson.annotation.JsonProperty
import com.otoki.powersales.domain.org.organization.entity.Organization

/**
 * SAP 조직 마스터 행 DTO. 페이로드 키는 SAP 호환을 위해 PascalCase / UPPER_CASE 로 유지한다.
 * 12 필드는 모두 nullable 이며, 모든 필드가 null 인 행은 검증에서 거부된다.
 */
data class OrganizationMasterRequestItem(
    @JsonProperty("CC_CD2") val ccCd2: String? = null,
    @JsonProperty("ORG_CD2") val orgCd2: String? = null,
    @JsonProperty("ORG_NM2") val orgNm2: String? = null,
    @JsonProperty("CC_CD3") val ccCd3: String? = null,
    @JsonProperty("ORG_CD3") val orgCd3: String? = null,
    @JsonProperty("ORG_NM3") val orgNm3: String? = null,
    @JsonProperty("CC_CD4") val ccCd4: String? = null,
    @JsonProperty("ORG_CD4") val orgCd4: String? = null,
    @JsonProperty("ORG_NM4") val orgNm4: String? = null,
    @JsonProperty("CC_CD5") val ccCd5: String? = null,
    @JsonProperty("ORG_CD5") val orgCd5: String? = null,
    @JsonProperty("ORG_NM5") val orgNm5: String? = null
) {
    fun isAllNull(): Boolean = ccCd2 == null && orgCd2 == null && orgNm2 == null &&
        ccCd3 == null && orgCd3 == null && orgNm3 == null &&
        ccCd4 == null && orgCd4 == null && orgNm4 == null &&
        ccCd5 == null && orgCd5 == null && orgNm5 == null

    fun toEntity(): Organization = Organization(
        costCenterLevel2 = ccCd2,
        orgCodeLevel2 = orgCd2,
        orgNameLevel2 = orgNm2,
        costCenterLevel3 = ccCd3,
        orgCodeLevel3 = orgCd3,
        orgNameLevel3 = orgNm3,
        costCenterLevel4 = ccCd4,
        orgCodeLevel4 = orgCd4,
        orgNameLevel4 = orgNm4,
        costCenterLevel5 = ccCd5,
        orgCodeLevel5 = orgCd5,
        orgNameLevel5 = orgNm5
    )
}
