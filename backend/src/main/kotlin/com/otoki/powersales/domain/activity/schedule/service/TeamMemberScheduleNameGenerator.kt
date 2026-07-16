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
 * 벌크 생성(연차 전개 / 행사 확정)은 건별로 [next] 를 반복 호출한다 — 각 호출이 서로 다른
 * 채번값을 원자적으로 발급받으므로 동시 요청·SF sync 추월 모두 안전하다.
 */
@Component
class TeamMemberScheduleNameGenerator(
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
) {

    /** 다음 name 값(`TS` + 8자리 zero-padded 순번, 예: `TS00012345`) 을 발급한다. */
    fun next(): String {
        val seq = teamMemberScheduleRepository.getNextNameSeq()
        return PREFIX + String.format("%08d", seq)
    }

    companion object {
        /** SF AutoNumber displayFormat `TS{00000000}` 의 prefix. */
        const val PREFIX: String = "TS"
    }
}
