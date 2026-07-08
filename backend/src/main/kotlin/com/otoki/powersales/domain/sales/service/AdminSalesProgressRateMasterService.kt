package com.otoki.powersales.domain.sales.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.platform.auth.sharing.service.SharingRulePolicyEvaluator
import com.otoki.powersales.domain.sales.dto.response.SalesProgressRateMasterDetailResponse
import com.otoki.powersales.domain.sales.dto.response.SalesProgressRateMasterListItem
import com.otoki.powersales.domain.sales.dto.response.SalesProgressRateMasterListResponse
import com.otoki.powersales.domain.sales.exception.SalesProgressRateMasterNotFoundException
import com.otoki.powersales.domain.sales.repository.SalesProgressRateMasterRepository
import com.otoki.powersales.domain.sales.entity.QSalesProgressRateMaster
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 거래처목표등록마스터(SF `SalesProgressRateMaster__c`) admin 조회 서비스 (읽기 전용).
 *
 * SF 에서 주기적으로 fetch 한 데이터를 web admin 에서 SF ListView "모두" 동등 컬럼으로 조회.
 * 데이터 권위는 SF — 등록/수정/삭제 없음. OWD=Private 라 [SharingRulePolicyEvaluator] 로 가시 범위 필터.
 */
@Service
@Transactional(readOnly = true)
class AdminSalesProgressRateMasterService(
    private val repository: SalesProgressRateMasterRepository,
    private val policyEvaluator: SharingRulePolicyEvaluator,
) {

    /**
     * @param scope 호출자(controller) 에서 산출/주입한 현재 사용자의 DataScope.
     * @param branchCode 거래처 지점코드(account.branchCode) 필터 — 가시 범위와 AND 합성.
     */
    fun getList(
        scope: DataScope,
        keyword: String?,
        targetYear: String?,
        targetMonth: String?,
        branchCode: String?,
        page: Int,
        size: Int,
    ): SalesProgressRateMasterListResponse {
        val policyPredicate = policyEvaluator.buildPredicate(
            scope = scope,
            sObjectName = "SalesProgressRateMaster__c",
            entityPath = QSalesProgressRateMaster.salesProgressRateMaster
        )

        val pageable = PageRequest.of(page, size)
        val resultPage = repository.searchForAdmin(
            policyPredicate = policyPredicate,
            keyword = keyword,
            targetYear = targetYear,
            targetMonth = targetMonth,
            branchCode = branchCode,
            pageable = pageable
        )

        return SalesProgressRateMasterListResponse(
            content = resultPage.content.map { SalesProgressRateMasterListItem.from(it) },
            page = page,
            size = size,
            totalElements = resultPage.totalElements,
            totalPages = resultPage.totalPages
        )
    }

    fun getDetail(scope: DataScope, id: Long): SalesProgressRateMasterDetailResponse {
        val policyPredicate = policyEvaluator.buildPredicate(
            scope = scope,
            sObjectName = "SalesProgressRateMaster__c",
            entityPath = QSalesProgressRateMaster.salesProgressRateMaster
        )

        // 목록↔단건 가시성 일관성 — 목록에 안 보이는 레코드는 상세 조회 불가 (404).
        if (!repository.existsVisibleById(id, policyPredicate)) {
            throw SalesProgressRateMasterNotFoundException()
        }

        val entity = repository.findByIdWithRelations(id)
            ?: throw SalesProgressRateMasterNotFoundException()

        return SalesProgressRateMasterDetailResponse.from(entity)
    }
}
