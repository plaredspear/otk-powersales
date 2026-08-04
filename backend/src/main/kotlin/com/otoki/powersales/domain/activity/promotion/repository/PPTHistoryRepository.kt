package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.ProfessionalPromotionTeamHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PPTHistoryRepository : JpaRepository<ProfessionalPromotionTeamHistory, Long>, PPTHistoryRepositoryCustom {

    /**
     * name(전문행사조 이력 번호) 채번 — SF AutoNumber(displayFormat PH{0000000}) 재현.
     * 시퀀스 nextval 단독. 마스터(PM) 채번과 동일 패턴.
     *
     * MAX 보정은 [syncNameSeq] 가 담당하며, 번호가 외부에서 주입될 수 있는 시점
     * (부팅 1회 / SF 마이그레이션 직후)에만 실행한다 — `NameSequenceSyncService`.
     * 전문행사조 sync 배치(매일 01:00)가 사원마다 본 채번을 호출하므로 hot path 다.
     */
    @Query(
        value = "SELECT nextval('powersales.professional_promotion_team_history_name_seq')",
        nativeQuery = true
    )
    fun getNextNameSeq(): Long

    /**
     * name 시퀀스를 기존 데이터 최대 번호 위로 끌어올린다 (멱등).
     * MAX 대상 표현식에는 부분 인덱스(`idx_ppt_history_name_seq_num`)가 있다.
     */
    @Query(
        value = """
            SELECT setval(
                'powersales.professional_promotion_team_history_name_seq',
                GREATEST(
                    nextval('powersales.professional_promotion_team_history_name_seq'),
                    COALESCE(
                        (SELECT MAX(NULLIF(regexp_replace(name, '\D', '', 'g'), '')::bigint)
                           FROM powersales.professional_promotion_team_history
                          WHERE name ~ '^PH[0-9]+$'),
                        0
                    ) + 1
                )
            )
        """,
        nativeQuery = true
    )
    fun syncNameSeq(): Long
}
