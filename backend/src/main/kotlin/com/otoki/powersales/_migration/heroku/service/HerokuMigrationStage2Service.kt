package com.otoki.powersales._migration.heroku.service

import com.otoki.powersales._migration.sf.dto.SfMigrationStage2Response
import com.otoki.powersales._migration.sf.dto.SubstepResult
import com.otoki.powersales._migration.sf.service.SfMigrationStage2Service
import com.otoki.powersales.platform.auth.policy.TemporaryPasswordPolicy
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Heroku 데이터 마이그레이션 Stage 2 — Logical 변환 (1회성 cut-over, 런칭 후 폐기).
 *
 * SF [SfMigrationStage2Service] 의 logical 변환 substep 과 동형. FK resolve 는 별도 클래스
 * ([HerokuFkResolveService]) 로 분리돼 있으므로 본 클래스는 값 변환 substep 만 담는다.
 *
 * 구현 substep:
 * - password : EmployeeInfo(mobile) 초기 비밀번호를 BCrypt hash 로 채움. Heroku Stage 1 은
 *   레거시 평문(emp_pwd)을 적재하지 않고 password NULL 로 두므로 (HerokuStage1Targets.EXCLUDED_COLUMNS),
 *   본 substep 이 SF User.password 재해시와 동일한 초기 비밀번호 정책을 적용한다.
 * - education-category : 안전교육(c00002) 으로 등록돼 있던 앱 사용법 게시물을 신설 카테고리
 *   APP 매뉴얼(c00005) 로 재분류. Stage 1 은 edu_code 를 원본 그대로 복사하므로 적재 후 보정한다.
 */
@Service
class HerokuMigrationStage2Service(
    @PersistenceContext private val em: EntityManager,
    private val passwordEncoder: PasswordEncoder,
) {

    /**
     * password — 마이그레이션 대상 EmployeeInfo 의 초기 비밀번호를 사번 기반
     * `"{사번}@pwrs"` ([TemporaryPasswordPolicy]) 의 BCrypt hash 로 채운다.
     *
     * 대상: `password IS NULL OR password = ''` 인 employee_info row (Heroku Stage 1 적재분).
     * 멱등 (이미 채워진 row skip). `password_change_required = TRUE` 로 최초 로그인 시 강제 변경 유도.
     *
     * SF User.password 와 동일 규칙을 공유하므로 같은 사번이면 web / mobile 초기 비밀번호가 일치한다.
     * 평문이 사번마다 다르므로 row 별로 encode 한다. 사번이 없는 row 는 정책상 고정 fallback 이 적용된다.
     */
    @Transactional
    fun runPasswordHash(): SfMigrationStage2Response {
        val rowsQuery = em.createNativeQuery(
            "SELECT i.employee_id, e.employee_code FROM powersales.employee_info i " +
                "JOIN powersales.employee e ON e.employee_id = i.employee_id " +
                "WHERE i.password IS NULL OR i.password = ''"
        )
        @Suppress("UNCHECKED_CAST")
        val rows = rowsQuery.resultList as List<Array<Any?>>

        var totalUpdated = 0
        for (row in rows) {
            val id = (row[0] as Number).toLong()
            val employeeCode = row[1] as String?
            val hash = passwordEncoder.encode(TemporaryPasswordPolicy.forEmployeeCode(employeeCode))
            val updated = em.createNativeQuery(
                "UPDATE powersales.employee_info SET password = :hash, password_change_required = TRUE " +
                    "WHERE employee_id = :id AND (password IS NULL OR password = '')"
            )
                .setParameter("hash", hash)
                .setParameter("id", id)
                .executeUpdate()
            totalUpdated += updated
        }

        return SfMigrationStage2Response(
            substep = "password",
            results = listOf(SubstepResult(label = "EmployeeInfo.password (BCrypt)", rowsAffected = totalUpdated)),
            totalRowsAffected = totalUpdated,
        )
    }

    /**
     * education-category — 안전교육(c00002) 게시물 중 앱 사용법 안내 5건을 APP 매뉴얼(c00005) 로 옮긴다.
     *
     * 교육 메인이 APP 매뉴얼 카테고리를 신설하면서, 레거시에서 안전교육에 섞여 있던 앱 조작 매뉴얼
     * (매출현황 조회 / 물류클레임 등록 / 출근등록 변경 안내) 을 분리한다. 제목 패턴으로 일반화할 수
     * 없어 대상 edu_id 를 명시한다 ([EDUCATION_APP_MANUAL_EDU_IDS]). 나머지 안전교육 게시물은 유지.
     *
     * 멱등 — `education_code = 'c00002'` 가드로 이미 옮긴 row 는 재실행해도 0 건.
     */
    @Transactional
    fun runEducationCategoryRemap(): SfMigrationStage2Response {
        val updated = em.createNativeQuery(
            // 컬럼명 주의 — Heroku 원본 edu_code 는 신규 스키마에서 education_post.education_code 다
            // (EducationPost.eduCode @Column(name = "education_code")). edu_id 만 이름이 같다.
            "UPDATE powersales.education_post SET education_code = :target, updated_at = now() " +
                "WHERE edu_id IN (:eduIds) AND education_code = :source"
        )
            .setParameter("target", APP_MANUAL_CODE)
            .setParameter("source", SAFETY_CODE)
            .setParameter("eduIds", EDUCATION_APP_MANUAL_EDU_IDS)
            .executeUpdate()

        return SfMigrationStage2Response(
            substep = "education-category",
            results = listOf(
                SubstepResult(
                    label = "EducationPost.education_code 안전교육(c00002) → APP 매뉴얼(c00005)",
                    rowsAffected = updated,
                )
            ),
            totalRowsAffected = updated,
        )
    }

    companion object {
        private const val SAFETY_CODE = "c00002"
        private const val APP_MANUAL_CODE = "c00005"

        /**
         * APP 매뉴얼로 옮길 안전교육 게시물 (레거시 education_post.edu_id).
         *
         * - edu20230915112030 : 출근등록 방식 변경 안내 + 사용법 영상
         * - edu20240416084907 : 물류클레임 등록 매뉴얼
         * - edu20260331173038 : 매출현황-POS매출 조회
         * - edu20260331173623 : 매출현황-월 매출 조회(전산)
         * - edu20260331173907 : 매출현황-월 매출 조회(물류)
         */
        val EDUCATION_APP_MANUAL_EDU_IDS: List<String> = listOf(
            "edu20230915112030",
            "edu20240416084907",
            "edu20260331173038",
            "edu20260331173623",
            "edu20260331173907",
        )
    }
}
