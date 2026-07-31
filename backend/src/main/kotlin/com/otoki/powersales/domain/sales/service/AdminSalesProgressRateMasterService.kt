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
     * @param branchCodes 거래처 지점코드(account.branchCode) 필터 — 가시 범위와 AND 합성.
     *   호출부([com.otoki.powersales.admin.service.BranchScopeGateway])가 셀렉터 화이트리스트로 판정하고
     *   `BranchMapping` 확장까지 끝낸 코드 목록이다 (조직 개편 전 코드로 적재된 거래처 누락 방지 —
     *   다른 지점 조회 화면과 동일 규약). null 이면 지점 필터 미적용, 빈 목록이면 0건.
     */
    fun getList(
        scope: DataScope,
        keyword: String?,
        targetYear: String?,
        targetMonth: String?,
        branchCodes: List<String>?,
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
            // 호출부가 셀렉터 화이트리스트 판정 + BranchMapping 확장까지 끝낸 코드 목록
            // (null = 지점 필터 미적용 / 빈 목록 = 권한 밖 지점 요청으로 0건).
            branchCodes = branchCodes,
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
