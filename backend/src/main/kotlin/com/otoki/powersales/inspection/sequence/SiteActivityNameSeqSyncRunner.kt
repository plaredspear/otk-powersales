package com.otoki.powersales.inspection.sequence

import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

/**
 * 부팅 시 `site_activity_name_seq` 가 `site_activity.name` 의 실제 max(suffix) 보다 작으면
 * `setval` 로 추월시켜 신규 채번이 기존 row 와 충돌하지 않도록 보정.
 *
 * ## 도입 배경
 *
 * `DKRetail__SiteAcitivity__c.Name` 은 SF `autoNumber` (`SA{00000000}`) 로 발행되는데, 운영 SF 의
 * 마이그레이션 row (SA00001764 등) 가 DB 에 적재되면 신규 환경의 sequence 가 그 채번값보다
 * 뒤처져 신규 INSERT 시 동일 Name 의 silent duplicate 가 발생할 수 있다 (UNIQUE 제약 없음 — SF 정합).
 *
 * Flyway `V202606130200__create_site_activity_name_seq.sql` 의 setval 이 1차 방어, 본 Runner 가
 * 부팅 시 2차 안전망. 멱등 — 이미 큰 값이면 no-op. PromotionProductNameSeqSyncRunner 동형.
 */
@Component
@Profile("!test")
class SiteActivityNameSeqSyncRunner(
    private val entityManager: EntityManager,
    private val transactionTemplate: TransactionTemplate,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        transactionTemplate.executeWithoutResult {
            sync()
        }
    }

    private fun sync() {
        // Native query 라 JPA 의 hibernate.default_schema 가 적용되지 않으므로 schema prefix 명시.
        // SA{8자리 숫자} 형식만 후보. 그 외 (수동 입력된 Name 등) 무시.
        val maxSuffix = (
            entityManager.createNativeQuery(
                """
                SELECT COALESCE(MAX(SUBSTRING(name FROM 3)::bigint), 0)
                FROM powersales.site_activity
                WHERE name ~ '^SA[0-9]+$'
                """.trimIndent()
            ).singleResult as Number
            ).toLong()

        val currentSeq = (
            entityManager.createNativeQuery(
                "SELECT last_value FROM powersales.site_activity_name_seq"
            ).singleResult as Number
            ).toLong()

        if (maxSuffix <= currentSeq) {
            // sequence 가 이미 충분히 앞서 있음 — 보정 불필요.
            log.info(
                "[SiteActivityNameSeqSyncRunner] no-op — seq={} >= maxNameSuffix={}",
                currentSeq, maxSuffix,
            )
            return
        }

        // setval(seq, value, true) 는 다음 nextval 이 value+1 부터 시작.
        entityManager.createNativeQuery(
            "SELECT setval('powersales.site_activity_name_seq', :v, true)"
        ).setParameter("v", maxSuffix).singleResult

        log.warn(
            "[SiteActivityNameSeqSyncRunner] sequence 추월 — seq={} → {} (maxNameSuffix={})",
            currentSeq, maxSuffix, maxSuffix,
        )
    }
}
