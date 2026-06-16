package com.otoki.powersales.domain.org.employee.sfsync

import com.otoki.powersales.external.sf.outbound.SfOAuthFailedException
import com.otoki.powersales.external.sf.outbound.SfOutboundClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

/**
 * SF Apex REST `/IF_SendStaffReviewToPWS` 사원평가 마스터 조회 테스트 서비스 (개발자 도구 — 외부 API 테스트).
 *
 * `MOD_DT`(기준 일자) 하나를 SF 로 POST 하면 SF 가 해당 일자(수정일 기준)로 변경된 사원평가(StaffReview) 마스터 목록을
 * 응답하는 SF → PWS 조회 인터페이스 ("알라딘 Staffreview 마스터 API" 문서 정합). 신규 DB 에는 저장하지 않고
 * SF 응답 원형만 그대로 반환한다 (클레임/거래처목표 마스터 조회 테스트와 동일한 "외부 API 호출 + raw 응답 확인" 성격).
 *
 * 처리:
 *  1. `{ "MOD_DT": modDt }` apiMap 구성.
 *  2. [SfOutboundClient.callApi] 로 SF POST — 클레임 등록과 동일한 OAuth/401 재시도 경로 재사용.
 *  3. 성공/실패 모두 응답에 담아 반환 (예외 throw 안 함).
 *
 * DB 변경이 없으므로 audit 적재 없이 slf4j INFO 로그만 남긴다.
 */
@Service
class AdminStaffReviewSyncTestService(
    private val sfOutboundClient: SfOutboundClient,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun test(
        userId: Long,
        request: AdminStaffReviewSyncTestRequest,
    ): AdminStaffReviewSyncTestResponse {
        val apiMap = mapOf<String, Any?>("MOD_DT" to request.modDt)
        val requestPayload = objectMapper.writeValueAsString(apiMap)

        val response = try {
            val apiResponse = sfOutboundClient.callApi(SF_ENDPOINT, apiMap)
            AdminStaffReviewSyncTestResponse(
                success = apiResponse.isSuccess(),
                resultCode = apiResponse.resultCode,
                resultMsg = apiResponse.resultMsg,
                rawResponse = apiResponse.rawBody,
                requestPayload = requestPayload,
            )
        } catch (e: SfOAuthFailedException) {
            log.warn("[staff-review-sync-test] SF OAuth 실패: {}", e.message)
            AdminStaffReviewSyncTestResponse(
                success = false,
                resultCode = null,
                resultMsg = e.message ?: "SF OAuth 실패",
                rawResponse = null,
                requestPayload = requestPayload,
            )
        } catch (e: Exception) {
            log.warn("[staff-review-sync-test] SF 호출 예외: {}", e.message)
            AdminStaffReviewSyncTestResponse(
                success = false,
                resultCode = null,
                resultMsg = e.message ?: e.javaClass.simpleName,
                rawResponse = null,
                requestPayload = requestPayload,
            )
        }

        log.info(
            "SF_STAFF_REVIEW_SYNC_TEST user={} modDt={} success={} resultCode={}",
            userId, request.modDt, response.success, response.resultCode,
        )
        return response
    }

    companion object {
        /** SF Apex REST suffix (apex base URL 뒤에 붙는다). */
        const val SF_ENDPOINT = "/IF_SendStaffReviewToPWS"
    }
}
