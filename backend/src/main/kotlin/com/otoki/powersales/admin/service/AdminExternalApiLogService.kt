package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.request.AdminExternalApiLogQuery
import com.otoki.powersales.admin.dto.response.ExternalApiLogDetail
import com.otoki.powersales.admin.dto.response.ExternalApiLogListResponse
import com.otoki.powersales.admin.dto.response.ExternalApiLogRow
import com.otoki.powersales.external.common.outboundlog.ExternalApiEndpointKeyResolver
import com.otoki.powersales.external.common.outboundlog.entity.ExternalApiLog
import com.otoki.powersales.external.common.outboundlog.repository.ExternalApiLogRepository
import com.otoki.powersales.platform.common.exception.BusinessException
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Admin 외부 API 호출 이력 조회 서비스 (개발자 도구 — 외부 API 테스트 > 호출 이력).
 *
 * `external_api_log` (SAP/SF/Naver outbound HTTP 호출 공통 로그) 를 조회한다.
 * web 각 외부 API 테스트 탭은 자기 [AdminExternalApiLogQuery.endpointKey] 로 자신의 호출 이력만 본다.
 */
@Service
@Transactional(readOnly = true)
class AdminExternalApiLogService(
    private val externalApiLogRepository: ExternalApiLogRepository,
) {

    /** 필터 셀렉터용 — resolver 가 분류 가능한 전체 endpoint key 목록. */
    fun endpointKeys(): List<String> = ExternalApiEndpointKeyResolver.ALL_KEYS

    fun search(query: AdminExternalApiLogQuery): ExternalApiLogListResponse {
        val page = (query.page - 1).coerceAtLeast(0)
        val size = query.size.coerceIn(1, MAX_PAGE_SIZE)
        val pageable = PageRequest.of(page, size)

        val result = externalApiLogRepository.search(
            targetSystem = query.targetSystem,
            endpointKey = query.endpointKey,
            success = query.success,
            httpMethod = query.httpMethod,
            from = query.from,
            to = query.to,
            pageable = pageable,
        )

        return ExternalApiLogListResponse(
            items = result.content.map { it.toRow() },
            totalCount = result.totalElements,
            currentPage = query.page,
            pageSize = size,
        )
    }

    fun getDetail(id: Long): ExternalApiLogDetail {
        val log = externalApiLogRepository.findById(id).orElseThrow {
            BusinessException(
                errorCode = "EXTERNAL_API_LOG_NOT_FOUND",
                message = "외부 API 호출 이력을 찾을 수 없습니다: $id",
                httpStatus = HttpStatus.NOT_FOUND,
            )
        }
        return log.toDetail()
    }

    private fun ExternalApiLog.toRow(): ExternalApiLogRow = ExternalApiLogRow(
        id = id,
        targetSystem = targetSystem,
        endpointKey = endpointKey,
        httpMethod = httpMethod,
        uri = uri,
        httpStatus = httpStatus,
        success = success,
        durationMs = durationMs,
        requestedAt = requestedAt,
        completedAt = completedAt,
    )

    private fun ExternalApiLog.toDetail(): ExternalApiLogDetail = ExternalApiLogDetail(
        id = id,
        targetSystem = targetSystem,
        endpointKey = endpointKey,
        httpMethod = httpMethod,
        uri = uri,
        httpStatus = httpStatus,
        success = success,
        durationMs = durationMs,
        errorDetail = errorDetail,
        requestedAt = requestedAt,
        completedAt = completedAt,
    )

    companion object {
        private const val MAX_PAGE_SIZE = 100
    }
}
