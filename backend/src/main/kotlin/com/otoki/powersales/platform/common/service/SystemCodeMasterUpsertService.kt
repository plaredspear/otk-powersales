package com.otoki.powersales.platform.common.service

import com.otoki.powersales.platform.common.entity.SystemCodeMaster
import com.otoki.powersales.platform.common.repository.SystemCodeMasterRepository
import com.otoki.powersales.platform.common.service.dto.SystemCodeMasterUpsertCommand
import com.otoki.powersales.platform.common.service.dto.SystemCodeMasterUpsertFailedRow
import com.otoki.powersales.platform.common.service.dto.SystemCodeMasterUpsertResult
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * 시스템 공통 코드 마스터 UPSERT 도메인 서비스.
 *
 * ## 레거시 매핑
 * - 진입점: SAP 인바운드 어댑터 [com.otoki.powersales.external.sap.inbound.service.SapSystemCodeMasterService]
 * - origin spec: #559 (SAP 제품 마스터 인바운드) — 어댑터/도메인 분리: #635 P1-B
 *
 * ## 레거시 동작 요약
 * 1. 입력: `List<SystemCodeMasterUpsertCommand>` — UPSERT 키 `externalKey = companyCode + ';' + groupCode + ';' + detailCode`.
 * 2. 행 단위 적재: 레거시 `IF_REST_SAP_SystemCodeMaster` 정합 — `CompanyCode`/`GroupCode`/`DetailCode` 모두 SF
 *    `nillable=true` (`required=false`) 라 **명시 필수 검증을 두지 않고 raw 적재**한다. blank 는 null 로 정규화하되,
 *    externalKey 합성은 레거시 `obj.CompanyCode + ';' + obj.GroupCode + ';' + obj.DetailCode` 동등하게
 *    null 을 빈 문자열로 이어붙인다 (세 코드 공란이면 `";;"`).
 * 3. `externalKey` 는 SF external key 이자 신규 `external_key` UNIQUE 제약이므로, 행마다 별도 트랜잭션
 *    ([Propagation.REQUIRES_NEW]) 에서 flush 해 UNIQUE 충돌이 나면 그 행만 failure 로 격리한다
 *    (레거시 `Database.upsert(ExternalKey__c, false)` 동등).
 * 4. 외부 호출: [SystemCodeMasterRowUpsertService.persistRow] (행 단위 saveAndFlush).
 *
 * `sap.*` 패키지 의존 0건 — SAP audit / `ClientIpResolver` / `RequestContextHolder` / `SapResultWrapper` 침투 금지.
 */
@Service
class SystemCodeMasterUpsertService(
    private val rowUpsertService: SystemCodeMasterRowUpsertService
) {

    fun upsert(commands: List<SystemCodeMasterUpsertCommand>): SystemCodeMasterUpsertResult {
        val failures = mutableListOf<SystemCodeMasterUpsertFailedRow>()
        var successCount = 0

        commands.forEach { command ->
            // 레거시 IF_REST_SAP_SystemCodeMaster 정합 — 세 코드 명시 검증 없이 raw 적재 (blank → null).
            val companyCode = command.companyCode?.takeIf { it.isNotBlank() }
            val groupCode = command.groupCode?.takeIf { it.isNotBlank() }
            val detailCode = command.detailCode?.takeIf { it.isNotBlank() }

            // 레거시 `obj.CompanyCode + ';' + obj.GroupCode + ';' + obj.DetailCode` 동등 — null 은 빈 문자열로 연결.
            val externalKey = "${companyCode.orEmpty()};${groupCode.orEmpty()};${detailCode.orEmpty()}"
            try {
                rowUpsertService.persistRow(command, externalKey, companyCode, groupCode, detailCode)
                successCount++
            } catch (ex: DataIntegrityViolationException) {
                failures += SystemCodeMasterUpsertFailedRow(externalKey, "적재 실패: ${ex.mostSpecificCauseText()}")
            }
        }

        return SystemCodeMasterUpsertResult(
            successCount = successCount,
            failureCount = failures.size,
            failures = failures
        )
    }
}

/**
 * [SystemCodeMasterUpsertService] 의 행 단위 트랜잭션 경계 빈.
 *
 * 한 요청 안의 여러 행 중 `external_key` UNIQUE 충돌이 난 행만 격리하기 위해 행마다 [Propagation.REQUIRES_NEW]
 * 로 별도 트랜잭션을 연다 (같은 빈 내 self-invocation 은 Spring AOP 프록시를 우회해 트랜잭션 경계가 적용되지
 * 않으므로 별도 빈으로 분리). 충돌 시 본 트랜잭션만 롤백되고 호출 루프는 다음 행을 계속 처리한다.
 */
@Service
class SystemCodeMasterRowUpsertService(
    private val systemCodeMasterRepository: SystemCodeMasterRepository
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun persistRow(
        command: SystemCodeMasterUpsertCommand,
        externalKey: String,
        companyCode: String?,
        groupCode: String?,
        detailCode: String?
    ) {
        val existing = systemCodeMasterRepository.findByExternalKey(externalKey)
        val entity = if (existing == null) {
            SystemCodeMaster(
                companyCode = companyCode,
                groupCode = groupCode,
                detailCode = detailCode,
                externalKey = externalKey,
                groupCodeName = command.groupCodeName,
                detailCodeName = command.detailCodeName,
                seq = command.seq
            )
        } else {
            existing.companyCode = companyCode
            existing.groupCode = groupCode
            existing.detailCode = detailCode
            existing.groupCodeName = command.groupCodeName
            existing.detailCodeName = command.detailCodeName
            existing.seq = command.seq
            existing
        }
        systemCodeMasterRepository.saveAndFlush(entity)
    }
}

private fun Throwable.mostSpecificCauseText(): String {
    var cause: Throwable = this
    while (cause.cause != null && cause.cause !== cause) cause = cause.cause!!
    return cause.message ?: cause::class.simpleName ?: "unknown"
}
