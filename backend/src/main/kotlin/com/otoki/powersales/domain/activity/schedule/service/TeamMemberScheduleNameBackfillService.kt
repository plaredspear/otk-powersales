package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * team_member_schedule.name 백필 도구 (개발자 도구 전용).
 *
 * 자동 채번([TeamMemberScheduleNameGenerator])은 **신규 INSERT 부터** 적용되므로, 그 이전에
 * `name = null` 로 저장된 기존 일정은 소급 대상이 남는다. 본 서비스가 그 기존 row 에 SF AutoNumber
 * 재현값(`TS{00000000}`)을 소급 부여한다.
 *
 * - preview: 채번이 필요한(name 이 비어있는) 건수만 조회 (변경 없음).
 * - backfill: id 오래된 순으로 최대 [limit] 건을 채번해 native UPDATE. 채번은 건별로
 *   [TeamMemberScheduleNameGenerator.next] 를 호출하므로, 신규 INSERT 채번과 동일한 setval 보정
 *   번호 공간을 공유한다(중복 없음). 남은 대상이 있으면 여러 번 실행해 소진한다.
 */
@Service
class TeamMemberScheduleNameBackfillService(
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val teamMemberScheduleNameGenerator: TeamMemberScheduleNameGenerator,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /** 채번이 필요한(name 이 비어있는) 일정 건수. */
    @Transactional(readOnly = true)
    fun countMissing(): Long = teamMemberScheduleRepository.countMissingName()

    /**
     * name 이 비어있는 일정에 채번값을 소급 부여한다 (id 오래된 순, 최대 limit 건).
     * @return 실제로 갱신된 건수 (한 tx 안에서 처리).
     */
    @Transactional
    fun backfill(limit: Int): BackfillResult {
        val safeLimit = limit.coerceIn(1, MAX_LIMIT_PER_RUN)
        val ids = teamMemberScheduleRepository.findMissingNameIds(safeLimit)
        var updated = 0
        for (id in ids) {
            val name = teamMemberScheduleNameGenerator.next()
            // 조건부 UPDATE(name IS NULL) 라, 동시 실행/그새 채워짐이면 0 이 되어 해당 name 은 건너뛴다.
            updated += teamMemberScheduleRepository.updateNameById(id, name)
        }
        val remaining = teamMemberScheduleRepository.countMissingName()
        log.info("TeamMemberSchedule name 백필: 대상 {}건 요청, {}건 갱신, 잔여 {}건", ids.size, updated, remaining)
        return BackfillResult(processed = ids.size, updated = updated, remaining = remaining)
    }

    data class BackfillResult(
        /** 이번 실행에서 조회한 대상 건수. */
        val processed: Int,
        /** 실제 name 이 채워진 건수. */
        val updated: Int,
        /** 실행 후에도 아직 name 이 비어있는 잔여 건수. */
        val remaining: Long,
    )

    companion object {
        /** 한 번 실행에서 처리하는 상한 (과도한 UPDATE/락 시간 방지). 초과분은 재실행으로 소진. */
        const val MAX_LIMIT_PER_RUN = 5000
        const val DEFAULT_LIMIT = 1000
    }
}
