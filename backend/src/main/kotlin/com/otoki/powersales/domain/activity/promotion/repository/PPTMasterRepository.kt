package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.ProfessionalPromotionTeamMaster
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PPTMasterRepository : JpaRepository<ProfessionalPromotionTeamMaster, Long>, PPTMasterRepositoryCustom {

    fun findByEmployeeIdAndEndDateIsNull(employeeId: Long): List<ProfessionalPromotionTeamMaster>

    fun findByEmployeeId(employeeId: Long): List<ProfessionalPromotionTeamMaster>

    /**
     * name(전문행사조 마스터 번호) 채번 — SF AutoNumber(displayFormat PM{0000000}) 재현.
     * 시퀀스 nextval 단독.
     *
     * MAX 보정은 [syncNameSeq] 가 담당하며, 번호가 외부에서 주입될 수 있는 시점
     * (부팅 1회 / SF 마이그레이션 직후)에만 실행한다 — `NameSequenceSyncService`.
     * 벌크 업로드는 item 마다 본 채번을 호출하므로 hot path 다.
     */
    @Query(
        value = "SELECT nextval('powersales.professional_promotion_team_master_name_seq')",
        nativeQuery = true
    )
    fun getNextNameSeq(): Long

    /**
     * name 시퀀스를 기존 데이터 최대 번호 위로 끌어올린다 (멱등).
     * MAX 대상 표현식에는 부분 인덱스(`idx_ppt_master_name_seq_num`)가 있다.
     */
    @Query(
        value = """
            SELECT setval(
                'powersales.professional_promotion_team_master_name_seq',
                GREATEST(
                    nextval('powersales.professional_promotion_team_master_name_seq'),
                    COALESCE(
                        (SELECT MAX(NULLIF(regexp_replace(name, '\D', '', 'g'), '')::bigint)
                           FROM powersales.professional_promotion_team_master
                          WHERE name ~ '^PM[0-9]+$'),
                        0
                    ) + 1
                )
            )
        """,
        nativeQuery = true
    )
    fun syncNameSeq(): Long
}
