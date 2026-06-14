package com.otoki.powersales.platform.common.service

import com.otoki.powersales.platform.common.entity.SystemCodeMaster
import com.otoki.powersales.platform.common.repository.SystemCodeMasterRepository
import com.otoki.powersales.platform.common.service.dto.SystemCodeMasterUpsertCommand
import com.otoki.powersales.platform.common.service.dto.SystemCodeMasterUpsertFailedRow
import com.otoki.powersales.platform.common.service.dto.SystemCodeMasterUpsertResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 시스템 공통 코드 마스터 UPSERT 도메인 서비스.
 *
 * ## 레거시 매핑
 * - 진입점: SAP 인바운드 어댑터 [com.otoki.powersales.external.sap.inbound.service.SapSystemCodeMasterService]
 * - origin spec: #559 (SAP 제품 마스터 인바운드) — 어댑터/도메인 분리: #635 P1-B
 *
 * ## 레거시 동작 요약
 * 1. 입력: `List<SystemCodeMasterUpsertCommand>` — UPSERT 키 `companyCode + ';' + groupCode + ';' + detailCode`.
 * 2. 행 단위 검증: 필수값 (`companyCode`/`groupCode`/`detailCode`) 누락 → failures.
 * 3. 정상 행: 신규 [SystemCodeMaster] 생성 또는 기존 entity 의 mutable 필드 갱신. 본 청크 내 동일 externalKey 가 다시 등장하면 `keyCache` 로 in-memory 매칭.
 * 4. 외부 호출: [SystemCodeMasterRepository.saveAll] (성공 행만 일괄). 행 검증 실패는 트랜잭션 롤백하지 않음.
 *
 * ## 신규 차이 — 동등 (생략)
 *
 * `sap.*` 패키지 의존 0건 — SAP audit / `ClientIpResolver` / `RequestContextHolder` / `SapResultWrapper` 침투 금지.
 */
@Service
class SystemCodeMasterUpsertService(
    private val systemCodeMasterRepository: SystemCodeMasterRepository
) {

    @Transactional
    fun upsert(commands: List<SystemCodeMasterUpsertCommand>): SystemCodeMasterUpsertResult {
        val failures = mutableListOf<SystemCodeMasterUpsertFailedRow>()
        val toSave = mutableListOf<SystemCodeMaster>()
        val keyCache = mutableMapOf<String, SystemCodeMaster>()

        commands.forEach { command ->
            val companyCode = command.companyCode?.takeIf { it.isNotBlank() }
            val groupCode = command.groupCode?.takeIf { it.isNotBlank() }
            val detailCode = command.detailCode?.takeIf { it.isNotBlank() }

            if (companyCode == null) {
                failures += SystemCodeMasterUpsertFailedRow(null, "CompanyCode 필수")
                return@forEach
            }
            if (groupCode == null) {
                failures += SystemCodeMasterUpsertFailedRow(null, "GroupCode 필수")
                return@forEach
            }
            if (detailCode == null) {
                failures += SystemCodeMasterUpsertFailedRow(null, "DetailCode 필수")
                return@forEach
            }

            val externalKey = "$companyCode;$groupCode;$detailCode"
            val existing = keyCache[externalKey]
                ?: systemCodeMasterRepository.findByExternalKey(externalKey)?.also { keyCache[externalKey] = it }

            val entity = if (existing == null) {
                SystemCodeMaster(
                    companyCode = companyCode,
                    groupCode = groupCode,
                    detailCode = detailCode,
                    externalKey = externalKey,
                    groupCodeName = command.groupCodeName,
                    detailCodeName = command.detailCodeName,
                    seq = command.seq
                ).also { keyCache[externalKey] = it }
            } else {
                existing.companyCode = companyCode
                existing.groupCode = groupCode
                existing.detailCode = detailCode
                existing.groupCodeName = command.groupCodeName
                existing.detailCodeName = command.detailCodeName
                existing.seq = command.seq
                existing
            }
            toSave += entity
        }

        if (toSave.isNotEmpty()) {
            systemCodeMasterRepository.saveAll(toSave)
        }

        return SystemCodeMasterUpsertResult(
            successCount = toSave.size,
            failureCount = failures.size,
            failures = failures
        )
    }
}
