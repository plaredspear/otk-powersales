package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import org.springframework.stereotype.Component

/**
 * team_member_schedule.name 채번기 — SF AutoNumber(`DKRetail__TeamMemberSchedule__c.Name`,
 * displayFormat `TS{00000000}`) 재현.
 *
 * SF 원본은 `TS` + 8자리 zero-padded 순번(날짜 토큰 없음). 신규 INSERT 경로가 여러 곳이라
 * 포맷/채번 로직을 본 컴포넌트에 단일화한다. 시퀀스 충돌 회피(setval 보정)는 채번 쿼리
 * ([TeamMemberScheduleRepository.getNextNameSeq]) 가 담당한다.
 *
 * 벌크 생성(연차 전개 / 행사 확정)은 [nextBatch] 로 필요한 개수를 한 번에 발급받는다 — 건별
 * [next] 반복은 채번 쿼리의 전체 스캔(`MAX(regexp_replace(name, ...))`) 이 건수만큼 반복돼 느리다.
 * 두 경로 모두 시퀀스를 발급 구간 끝까지 밀어두므로 동시 요청·SF sync 추월에 안전하다.
 */
@Component
class TeamMemberScheduleNameGenerator(
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
) {

    /** 다음 name 값(`TS` + 8자리 zero-padded 순번, 예: `TS00012345`) 을 발급한다. */
    fun next(): String {
        val seq = teamMemberScheduleRepository.getNextNameSeq()
        return format(seq)
    }

    /**
     * name 값 [count] 개를 **채번 쿼리 1회**로 발급한다 (벌크 생성 경로용). 반환 순서는 발급 순번 오름차순.
     * [count] 가 0 이하면 채번하지 않고 빈 리스트를 반환한다.
     */
    fun nextBatch(count: Int): List<String> {
        if (count <= 0) return emptyList()
        val lastSeq = teamMemberScheduleRepository.allocateNameSeqBlock(count.toLong())
        val firstSeq = lastSeq - count + 1
        return (firstSeq..lastSeq).map { format(it) }
    }

    private fun format(seq: Long): String = PREFIX + String.format("%08d", seq)

    companion object {
        /** SF AutoNumber displayFormat `TS{00000000}` 의 prefix. */
        const val PREFIX: String = "TS"
    }
}
