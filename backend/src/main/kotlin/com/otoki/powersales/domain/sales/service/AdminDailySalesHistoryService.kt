package com.otoki.powersales.domain.sales.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.exception.AdminForbiddenException
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.sales.dto.response.DailySalesHistoryListResponse
import com.otoki.powersales.domain.sales.materialize.OroraAccountRange
import com.otoki.powersales.domain.sales.repository.DailySalesHistoryRepository
import com.otoki.powersales.platform.common.exception.BusinessException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * web admin 「기준정보 > ORORA 일매출」 조회 service.
 *
 * ## 데이터 source
 * 메인 RDS `daily_sales_history` (= ORORA `ECRM_MULCUST_MH_V` 를 일 1회 적재한 결과,
 * `OroraDailySalesChunkProcessor`). ORORA view 를 화면에서 직접 읽지 않는다 —
 * ORORA 도달은 VPC peering 한정(dev/prod) 이고, 화면/집계는 항상 RDS 적재본을 본다는
 * 기존 정책([MonthlySalesAdminQueryService] 과 동일) 을 따른다. SF 레거시도 화면 조회는
 * ORORA 직접이 아닌 `DailySalesHistory__c` SObject 를 읽었다.
 *
 * ## 동작 요약
 * 거래처 1곳(거래처코드) + 매출월(`yyyyMM`) 의 일별 적재 행 + 금액 합계 3종을 반환한다.
 * 거래처는 필수 조건 — 월 단위 전 거래처 스캔을 원천 차단한다. 부수 효과: 없음 (조회 전용).
 *
 * ## 가시성
 * 거래처 지점(`branch_code`) 이 호출자 [DataScope] 범위 밖이면 [AdminForbiddenException]
 * (POS/전산매출 admin 조회와 동일 정책).
 */
@Service
@Transactional(readOnly = true)
class AdminDailySalesHistoryService(
    private val accountRepository: AccountRepository,
    private val dailySalesHistoryRepository: DailySalesHistoryRepository,
) {

    /**
     * 거래처 + 매출월의 일별 매출 일람 조회.
     *
     * @param accountCode 거래처코드 (`account.external_key`). ORORA 원본 형식(선행 `000` 포함) 입력도
     *                    허용 — 적재본은 prefix 제거 형식이라 미매칭 시 prefix 를 떼고 1회 재조회한다.
     * @param salesMonth 매출발생년월. `yyyyMM` / `yyyy-MM` 모두 허용 (숫자만 추출해 정규화).
     * @throws BusinessException `INVALID_PARAMETER` (매출월 형식 오류) / `ACCOUNT_NOT_FOUND` (거래처 부재)
     * @throws AdminForbiddenException 거래처가 권한 범위 밖일 때
     */
    fun getDailySalesHistories(
        scope: DataScope,
        accountCode: String,
        salesMonth: String,
    ): DailySalesHistoryListResponse {
        val normalizedMonth = normalizeSalesMonth(salesMonth)
        val resolved = resolveAccount(accountCode)
        if (!scope.validateAccess(resolved.account.branchCode)) throw AdminForbiddenException()

        val rows = dailySalesHistoryRepository
            .findBySapAccountCodeAndSalesDateStartingWithOrderBySalesDateAscIdAsc(
                resolved.sapAccountCode,
                normalizedMonth,
            )

        return DailySalesHistoryListResponse.of(
            salesMonth = normalizedMonth,
            sapAccountCode = resolved.sapAccountCode,
            accountName = resolved.account.name,
            branchName = resolved.account.branchName,
            entities = rows,
        )
    }

    /**
     * 매출월 입력을 `yyyyMM` 6자리로 정규화. 숫자 외 문자(하이픈 등) 는 제거한다.
     */
    private fun normalizeSalesMonth(salesMonth: String): String {
        val digits = salesMonth.filter { it.isDigit() }
        val month = digits.drop(4).toIntOrNull()
        if (digits.length != 6 || month == null || month !in 1..12) {
            throw BusinessException(
                errorCode = "INVALID_PARAMETER",
                message = "매출발생년월 형식이 올바르지 않습니다 (yyyyMM): $salesMonth",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
        return digits
    }

    /**
     * 거래처코드로 Account 조회 — ORORA 원본 형식(선행 `000`) 입력도 흡수한다.
     */
    private fun resolveAccount(accountCode: String): ResolvedAccount {
        val trimmed = accountCode.trim()
        if (trimmed.isBlank()) {
            throw BusinessException(
                errorCode = "INVALID_PARAMETER",
                message = "거래처코드는 필수입니다",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
        val candidates = buildList {
            add(trimmed)
            if (trimmed.startsWith(OroraAccountRange.ACCOUNT_CODE_PREFIX)) {
                add(trimmed.removePrefix(OroraAccountRange.ACCOUNT_CODE_PREFIX))
            }
        }
        val matched = candidates.firstNotNullOfOrNull { candidate ->
            accountRepository.findByExternalKey(candidate)?.let { ResolvedAccount(it, candidate) }
        }
        return matched ?: throw BusinessException(
            errorCode = "ACCOUNT_NOT_FOUND",
            message = "거래처를 찾을 수 없습니다: $trimmed",
            httpStatus = HttpStatus.NOT_FOUND,
        )
    }

    /** 조회된 거래처 + 실제 매칭에 쓰인 거래처코드 (적재본 `sap_account_code` 형식). */
    private data class ResolvedAccount(val account: Account, val sapAccountCode: String)
}
