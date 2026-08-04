package com.otoki.powersales.platform.common.sequence

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * 부팅 시 name/번호 시퀀스 1회 보정 — 앱이 내려간 사이의 SF 이관 적재 / DB 복원으로 시퀀스가
 * 뒤처진 상태를 기동 시점에 흡수한다. 상세 배경은 [NameSequenceSyncService] 참조.
 *
 * `test` 프로파일에서는 제외 — 테스트는 H2 + `ddl-auto: create-drop` 이라 PostgreSQL 시퀀스와
 * native `setval` 이 존재하지 않는다. 그 외(local/dev/prod)는 모두 PostgreSQL 이라 그대로 실행한다.
 * 보정 실패가 기동을 막지 않도록 [NameSequenceSyncService] 가 시퀀스별로 예외를 흡수한다.
 */
@Component
@Profile("!test")
class NameSequenceSyncRunner(
    private val nameSequenceSyncService: NameSequenceSyncService,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        try {
            nameSequenceSyncService.syncAll(reason = "boot")
        } catch (e: Exception) {
            log.warn("[name-seq-sync] 부팅 보정 실행 실패: {}", e.message)
        }
    }
}
