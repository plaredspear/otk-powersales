package com.otoki.powersales.domain.activity.suggestion.service

import com.otoki.powersales.domain.activity.suggestion.dto.request.AdminLogisticsClaimRegistTestRequest
import com.otoki.powersales.domain.activity.suggestion.dto.response.AdminLogisticsClaimRegistTestResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import tools.jackson.databind.ObjectMapper

/**
 * SF 물류 클레임 등록(ProposalRegist) 전송 테스트 서비스 (개발자 도구 — 외부 API 테스트).
 *
 * 레거시 SF Apex REST `IF_REST_MOBILE_ProposalRegist.cls#doPost` 의 입력 JSON(Input 클래스) 과
 * 동일한 key 셋으로 전송 payload(apiMap) 를 구성한다. **SF 전송 API 정보 미확보 단계라 실제 SF POST 는
 * 수행하지 않고** 구성한 apiMap 미리보기만 반환한다 (SF 정보 확보 후 호출 추가 예정).
 *
 * 처리:
 *  1. 입력값 정규화(trim) + Category 를 `물류 클레임` 으로 고정.
 *  2. 레거시 Input key 순서대로 apiMap(LinkedHashMap) 구성 — 이미지는 S3 식별 정보(UniqueKey/Size/FileName)
 *     형태이나 신규 web 테스트는 파일 직접 첨부라, 첨부 메타(파일명/크기)만 미리보기에 반영한다.
 *  3. 자동 파생 필드(ProductId/AccountId/WERK 등) 는 SF 서버 측 SOQL 파생이므로 미리보기에서 제외.
 *
 * DB 변경/외부 호출이 없으므로 audit 적재 없이 slf4j INFO 로그만 남긴다 (claim-regist 테스트 도구와 동일 정책).
 */
@Service
class AdminLogisticsClaimRegistTestService(
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun test(
        userId: Long,
        request: AdminLogisticsClaimRegistTestRequest,
        photo1: MultipartFile?,
        photo2: MultipartFile?,
    ): AdminLogisticsClaimRegistTestResponse {
        val apiMap = buildApiMap(request, photo1, photo2)

        log.info(
            "SF_LOGISTICS_CLAIM_REGIST_TEST(preview) user={} sapAccountCode={} productCode={} employeeCode={} claimType={}",
            userId, request.sapAccountCode?.trim(), request.productCode?.trim(),
            request.employeeCode?.trim(), request.claimType?.trim(),
        )

        return AdminLogisticsClaimRegistTestResponse(
            success = false,
            resultCode = null,
            resultMsg = NOT_SENT_NOTE,
            rawResponse = null,
            requestPayload = objectMapper.writeValueAsString(apiMap),
            note = NOT_SENT_NOTE,
        )
    }

    /**
     * 레거시 `IF_REST_MOBILE_ProposalRegist` Input 클래스 key 셋 그대로 apiMap 구성.
     *
     * 이미지는 레거시가 S3 사전 업로드 후 식별 정보(UniqueKey/Size/FileName)만 전송하는 방식이므로,
     * 본 미리보기는 첨부 파일의 파일명/크기를 `S3ImageFileName*`/`S3ImageFileSize*` 에 반영하고
     * `S3ImageUniqueKey*` 는 업로드 전 단계라 null 로 둔다.
     */
    private fun buildApiMap(
        request: AdminLogisticsClaimRegistTestRequest,
        photo1: MultipartFile?,
        photo2: MultipartFile?,
    ): Map<String, Any?> = linkedMapOf(
        "Category" to CATEGORY_LOGISTICS_CLAIM,
        "Type" to null,
        "ProductCode" to request.productCode?.trim(),
        "SAPAccountCode" to request.sapAccountCode?.trim(),
        "accountCode" to request.sapAccountCode?.trim(),
        "Title" to request.title?.trim(),
        "Description" to request.description?.trim(),
        "EmployeeCode" to request.employeeCode?.trim(),
        "CarNumber" to request.carNumber?.trim()?.takeIf { it.isNotEmpty() },
        "claimList" to request.claimType?.trim(),
        "logclaimDate" to request.claimDate?.trim(),
        "S3ImageUniqueKey1" to null,
        "S3ImageFileSize1" to photo1?.size?.toString(),
        "S3ImageFileName1" to photo1?.originalFilename,
        "S3ImageUniqueKey2" to null,
        "S3ImageFileSize2" to photo2?.size?.toString(),
        "S3ImageFileName2" to photo2?.originalFilename,
    )

    companion object {
        /** 레거시 ProposalRegist `Category` picklist 의 물류 클레임 값. */
        private const val CATEGORY_LOGISTICS_CLAIM = "물류 클레임"
        private const val NOT_SENT_NOTE =
            "SF 전송 API 정보 미확보 단계 — 실제 SF 전송 없이 전송 payload(apiMap) 미리보기만 제공합니다."
    }
}
