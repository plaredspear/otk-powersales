package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.ProfessionalPromotionTeamMaster
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PPTMasterRepository : JpaRepository<ProfessionalPromotionTeamMaster, Long>, PPTMasterRepositoryCustom {

    fun findByEmployeeIdAndEndDateIsNull(employeeId: Long): List<ProfessionalPromotionTeamMaster>

    fun findByEmployeeId(employeeId: Long): List<ProfessionalPromotionTeamMaster>

    /**
     * name(전문행사조 마스터 번호) 채번.
     *
     * name 은 SF AutoNumber(ProfessionalPromotionTeamMaster__c.Name, displayFormat PM{0000000}) 와
     * 동일한 번호 공간(PM + 7자리)을 공유한다. SF 데이터 sync 가 신규 시스템 시퀀스보다 큰 번호를 적재하면
     * nextval 만으로는 unique(혹은 표시번호) 충돌이 발생한다. 또한 시퀀스 동기화를 특정 시점에 한 번만 하면
     * SF 데이터 마이그레이션과의 실행 순서에 의존해 다시 뒤처질 수 있다.
     *
     * 이를 시점 의존 없이 해소하기 위해, 채번 때마다 nextval 과 "현재 데이터 최대 번호 + 1" 중 큰 값을
     * setval 로 확정한다. setval 반환값이 곧 발급 번호이며, 항상 기존 데이터 최대값을 추월하므로 충돌이 없다.
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
    fun getNextNameSeq(): Long
}
