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
}
