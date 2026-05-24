package com.otoki.powersales.organization.branchmapping

import com.otoki.powersales.organization.branchmapping.entity.BranchMapping
import com.otoki.powersales.organization.branchmapping.repository.BranchMappingRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

/**
 * Spec #810 — [BranchMappingMatrix] 74개 SoT → DB `branch_mapping` 부팅 sync.
 *
 * CLAUDE.md §4 reference data 정책 정합 (Flyway 가 아니라 Kotlin SoT + ApplicationRunner).
 *
 * ## 책임
 * - SoT 의 각 Entry 가 DB 에 없으면 INSERT, 있으면 included_branch_codes / label 갱신
 * - DB-only row (SoT 에 없는 branch_code) 는 **보존** — DELETE 하지 않음 (운영자 admin 수정 보호)
 *
 * ## 위험
 * - SoT 변경 후 재배포가 곧 DB 갱신. 운영자 admin UI 도입 전까지는 SoT 수정만 권위 채널.
 */
@Component
class BranchMappingSyncRunner(
    private val repository: BranchMappingRepository,
    private val transactionTemplate: TransactionTemplate,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        transactionTemplate.executeWithoutResult {
            sync()
        }
    }

    private fun sync() {
        var inserted = 0
        var updated = 0
        for (entry in BranchMappingMatrix.ALL) {
            val existing = repository.findById(entry.branchCode).orElse(null)
            if (existing == null) {
                repository.save(
                    BranchMapping(
                        branchCode = entry.branchCode,
                        includedBranchCodes = entry.includedBranchCodes,
                        label = entry.label,
                    ),
                )
                inserted++
            } else if (existing.includedBranchCodes != entry.includedBranchCodes || existing.label != entry.label) {
                existing.includedBranchCodes = entry.includedBranchCodes
                existing.label = entry.label
                repository.save(existing)
                updated++
            }
        }
        log.info(
            "[BranchMappingSyncRunner] sync 완료 — SoT={} INSERT={} UPDATE={} (DB-only row 는 보존)",
            BranchMappingMatrix.ALL.size, inserted, updated,
        )
    }
}
