package com.otoki.powersales.domain.org.employee.repository

import com.otoki.powersales.domain.org.employee.enums.DismissalPolicy
import com.otoki.powersales.domain.org.employee.enums.EmploymentStatus
import com.otoki.powersales.domain.org.employee.entity.QEmployee.Companion.employee
import com.querydsl.core.BooleanBuilder

/**
 * 「재직상태」 조회 필터 술어 — 사원 원본 `employee.status` 축 + 발령명 '면직' 보정 ([DismissalPolicy]).
 *
 * 여사원 현황 목록/엑셀([EmployeeRepositoryCustom.findEmployees] 의 `treatDismissalAsResigned = true`) 과
 * 전문행사조 마스터 목록/엑셀([com.otoki.powersales.domain.activity.promotion.repository.PPTMasterRepositoryCustom.searchMasters])
 * 이 **같은 술어**를 공유한다 — 두 화면의 재직상태 조회 결과가 어긋나지 않도록 SQL 을 한 곳에 둔다.
 *
 * - 퇴직 조회: `status = '퇴직' OR ord_detail_node = '면직'` (상태가 아직 재직인 면직자 포함)
 * - 재직/휴직 조회: `status = ? AND (ord_detail_node IS NULL OR ord_detail_node <> '면직')`
 *   — SQL 3값 논리상 `<>` 만 쓰면 발령명 NULL 행이 통째로 탈락하므로 IS NULL 을 함께 허용한다.
 *
 * 사원 필드는 QueryDSL 정적 alias([employee]) 로 참조하므로, 호출 쿼리는 이 alias 로 사원을
 * 조인해야 한다 (경로 표현식으로 조인하면 implicit inner join 이 생겨 목록/count 모수가 갈린다).
 */
object EmploymentStatusPredicate {

    /**
     * @param status 재직상태 원본 값 (재직 / 휴직 / 퇴직 — [EmploymentStatus.code]).
     *   도메인 밖 문자열이면 `status = ?` 가 매칭되지 않아 빈 결과가 된다 (여사원 현황과 동일).
     */
    fun matching(status: String): BooleanBuilder {
        val dismissed = employee.ordDetailNode.eq(DismissalPolicy.ORD_DETAIL_NODE)
        return if (status == EmploymentStatus.RESIGNED.code) {
            BooleanBuilder().and(employee.status.eq(status).or(dismissed))
        } else {
            BooleanBuilder().and(employee.status.eq(status))
                .and(employee.ordDetailNode.isNull.or(dismissed.not()))
        }
    }
}
