package com.otoki.powersales.platform.common.repository

import com.otoki.powersales.platform.common.entity.AgreementHistory
import org.springframework.data.jpa.repository.JpaRepository

interface AgreementHistoryRepository : JpaRepository<AgreementHistory, Long> {

    /**
     * 사번별 최신 동의 이력 1건 조회 (스펙 #585 GPS 거리 검증에서 사용 예정).
     * 동일 일자에 여러 row 존재 시 id 큰 row 를 선택 (가장 최근 INSERT).
     */
    fun findFirstByEmployeeIdAndIsDeletedFalseOrderByAgreementDateDescIdDesc(
        employeeId: Long,
    ): AgreementHistory?
}
