package com.otoki.powersales.external.sap.inbound.service

import com.otoki.powersales.account.service.AccountCategoryUpsertService
import com.otoki.powersales.account.service.dto.AccountCategoryUpsertCommand
import com.otoki.powersales.external.sap.auth.audit.SapInboundAccepted
import com.otoki.powersales.external.sap.inbound.dto.account.AccountCategoryRequestItem
import com.otoki.powersales.external.sap.inbound.dto.account.AccountMasterDetail
import com.otoki.powersales.external.sap.inbound.dto.account.FailureItem
import org.springframework.stereotype.Service

/**
 * SAP 거래처 카테고리 마스터 인바운드 어댑터. (Spec #558 / 어댑터-도메인 분리: #635 P1-B / audit AOP 통합: #639)
 *
 * 책임:
 * - SAP 페이로드 [AccountCategoryRequestItem] → 도메인 커맨드 [AccountCategoryUpsertCommand] 매핑
 * - 도메인 서비스 [AccountCategoryUpsertService.upsert] 호출
 * - 도메인 결과 → SAP 응답 [AccountMasterDetail] 매핑
 *
 * `REQUEST_ACCEPTED` audit 기록은 [com.otoki.powersales.external.sap.auth.audit.SapInboundAuditAspect] 가
 * `@SapInboundAccepted("items")` annotation 을 트리거로 공통 처리 (#639).
 *
 * 트랜잭션은 도메인 측이 관리하며 어댑터는 `@Transactional` 을 부착하지 않는다 (audit 가 commit 후 기록되어야 함).
 * UPSERT 키 / 부분 실패 시멘틱은 [AccountCategoryUpsertService] KDoc 참조.
 */
@Service
class SapAccountCategoryService(
    private val accountCategoryUpsertService: AccountCategoryUpsertService
) {

    @SapInboundAccepted("items")
    fun upsert(items: List<AccountCategoryRequestItem>): AccountMasterDetail {
        val commands = items.map { it.toCommand() }
        val result = accountCategoryUpsertService.upsert(commands)
        return AccountMasterDetail(
            successCount = result.successCount,
            failureCount = result.failureCount,
            failures = result.failures.map { FailureItem(it.identifier, it.reason) }
        )
    }

    private fun AccountCategoryRequestItem.toCommand(): AccountCategoryUpsertCommand =
        AccountCategoryUpsertCommand(accountCode = accountCode, name = name)
}
