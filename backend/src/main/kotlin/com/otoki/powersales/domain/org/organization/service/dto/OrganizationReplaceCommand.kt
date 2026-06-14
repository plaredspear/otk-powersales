package com.otoki.powersales.domain.org.organization.service.dto

/**
 * 조직 마스터 REPLACE_ALL 도메인 입력 커맨드.
 *
 * 시멘틱: 전체 교체 (UPSERT 가 아니라 deleteAll → INSERT all) — `OrganizationReplaceService.replaceAll` 진입.
 *
 * 외부 채널(SAP 인바운드 등) 의 페이로드를 도메인 용어 모델로 변환한 형태. `@JsonProperty` 가 침투하지 않는다.
 */
data class OrganizationReplaceCommand(
    val ccCd2: String?,
    val orgCd2: String?,
    val orgNm2: String?,
    val ccCd3: String?,
    val orgCd3: String?,
    val orgNm3: String?,
    val ccCd4: String?,
    val orgCd4: String?,
    val orgNm4: String?,
    val ccCd5: String?,
    val orgCd5: String?,
    val orgNm5: String?
)
