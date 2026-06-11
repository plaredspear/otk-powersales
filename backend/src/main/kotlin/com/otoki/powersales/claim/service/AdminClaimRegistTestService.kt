package com.otoki.powersales.claim.service

import com.otoki.powersales.claim.dto.request.AdminClaimCreateRequest
import com.otoki.powersales.claim.dto.response.AdminClaimRegistTestResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import tools.jackson.databind.ObjectMapper

/**
 * SF Apex REST `/ClaimRegist` 전송 테스트 서비스 (개발자 도구 — 외부 API 테스트).
 *
 * 운영 클레임 등록([AdminClaimCreateService.createClaim]) 과 달리 **신규 DB(claim 테이블) 에는 저장하지 않고**
 * SF 로만 전송한다. Naver Geocode 테스트 탭과 동일하게 "외부 API 호출 + raw 응답 확인" 성격의 도구.
 *
 * 처리:
 *  1. [AdminClaimCreateService.ParsedInput.from] 으로 입력 검증/정규화 (운영 경로와 동일 규칙).
 *  2. [AdminClaimCreateService.buildApiMapFromBytes] 로 apiMap 구성 (이미지 미첨부 시 빈 바이트).
 *  3. [AdminClaimCreateService.invokeSf] 로 SF POST — 성공/실패 모두 응답에 담아 반환 (예외 throw 안 함).
 *  4. apiMap 을 JSON 으로 직렬화하되 이미지 Buffer 는 길이 표기로 마스킹해 미리보기로 함께 반환.
 *
 * DB 변경이 없으므로 audit 적재 없이 slf4j INFO 로그만 남긴다 (naver-geocode 테스트 도구와 동일 정책).
 */
@Service
class AdminClaimRegistTestService(
    private val createService: AdminClaimCreateService,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun test(
        userId: Long,
        request: AdminClaimCreateRequest,
        claimPhoto: MultipartFile?,
        partPhoto: MultipartFile?,
        receiptPhoto: MultipartFile?,
    ): AdminClaimRegistTestResponse {
        // 운영 경로와 동일한 검증/정규화 규칙 적용 (영수증 조건부 필수 포함).
        val parsed = AdminClaimCreateService.ParsedInput.from(request, receiptPhoto)

        val apiMap = createService.buildApiMapFromBytes(
            employeeCode = parsed.employeeCode,
            sapAccountCode = parsed.sapAccountCode,
            productCode = parsed.productCode,
            parsed = parsed,
            claimBytes = claimPhoto?.bytes ?: ByteArray(0),
            claimFilename = claimPhoto?.originalFilename ?: "claim",
            claimContentType = claimPhoto?.contentType ?: "image/jpeg",
            partBytes = partPhoto?.bytes ?: ByteArray(0),
            partFilename = partPhoto?.originalFilename ?: "part",
            partContentType = partPhoto?.contentType ?: "image/jpeg",
            receiptBytes = receiptPhoto?.bytes,
            receiptFilename = receiptPhoto?.originalFilename,
            receiptContentType = receiptPhoto?.contentType,
        )

        val result = createService.invokeSf(apiMap)

        log.info(
            "SF_CLAIM_REGIST_TEST user={} sapAccountCode={} productCode={} employeeCode={} success={} resultCode={}",
            userId, parsed.sapAccountCode, parsed.productCode, parsed.employeeCode,
            result.success, result.apiResponse?.resultCode,
        )

        return AdminClaimRegistTestResponse(
            success = result.success,
            resultCode = result.apiResponse?.resultCode,
            resultMsg = result.apiResponse?.resultMsg ?: result.errorSummary,
            rawResponse = result.apiResponse?.rawBody,
            requestPayload = objectMapper.writeValueAsString(maskImageBuffers(apiMap)),
        )
    }

    /** 이미지 Base64 Buffer 는 미리보기에서 길이 표기로 치환 — 응답이 비대해지는 것을 방지. */
    private fun maskImageBuffers(apiMap: Map<String, Any?>): Map<String, Any?> =
        apiMap.mapValues { (key, value) ->
            if (key.endsWith("ImageBuffer") && value is String && value.isNotEmpty()) {
                "<base64 ${value.length} chars>"
            } else {
                value
            }
        }
}
