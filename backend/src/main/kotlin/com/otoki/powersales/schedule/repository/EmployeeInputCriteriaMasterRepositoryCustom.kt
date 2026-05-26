package com.otoki.powersales.schedule.repository

import com.otoki.powersales.schedule.entity.EmployeeInputCriteriaMaster
import com.otoki.powersales.schedule.enums.TypeOfWork1
import java.time.LocalDate

interface EmployeeInputCriteriaMasterRepositoryCustom {

    fun findAllNotDeleted(): List<EmployeeInputCriteriaMaster>

    /**
     * SF Trigger duplicateCheck() 정합 — 동일 거래처유형 + 동일 근무형태1 의 기간 겹침 존재 여부.
     * 종료일 null = 무기한.
     * [excludeId] 는 update 시 자기 자신 제외용 (생성 시 -1 같은 값 전달).
     */
    fun existsOverlapping(
        categoryId: Long,
        typeOfWork1: TypeOfWork1?,
        startDate: LocalDate,
        endDate: LocalDate?,
        excludeId: Long,
    ): Boolean

    /**
     * MFEIS self-trigger 3필드 set (spec #680 §5.3) — refreshIntegration 에서 호출.
     *
     * legacy `MonthlyEmpIntegrationSchTriggerHandler.cls:73-80` SOQL 동등 —
     * 동일 거래처유형 (categoryId) + 동일 근무유형1 (typeOfWork1) + 활성 기간
     * (`StartDate <= referenceDate AND (EndDate IS NULL OR EndDate >= referenceDate)`) +
     * `Confirmed = TRUE` + `IsDeleted != true` 만족하는 첫 번째 row.
     *
     * 중복 기간 row 가 존재하면 (실데이터 보장 부재 — `existsOverlapping` 가드는
     * Admin CRUD 시점만 적용) startDate 내림차순 → id 내림차순으로 우선순위 결정.
     */
    fun findActiveByCategoryAndTypeOfWork1(
        categoryId: Long,
        typeOfWork1: TypeOfWork1,
        referenceDate: LocalDate,
    ): EmployeeInputCriteriaMaster?
}
