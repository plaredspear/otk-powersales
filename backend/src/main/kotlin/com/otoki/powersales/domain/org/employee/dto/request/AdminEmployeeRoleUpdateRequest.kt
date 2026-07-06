package com.otoki.powersales.domain.org.employee.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

/**
 * 사원 권한(role) 전용 수정 요청 DTO.
 *
 * 일반 사원 수정([AdminEmployeeUpdateRequest]) 은 origin=SAP 사원을 차단하지만, 권한(role) 필드는
 * SAP 인입([EmployeeUpsertService.applyMutableFields]) 이 갱신하지 않는 컬럼이라 SAP 인입과 경합하지
 * 않는다. 따라서 권한만은 origin 과 무관하게 web admin 에서 수정할 수 있도록 별도 경로로 분리한다.
 *
 * 특히 AccountViewAll(전체 거래처 조회 권한) 은 SAP 발령으로 산출되지 않아, 이 경로 없이는 SAP 원천
 * 사원에게 부여할 방법이 없다.
 */
data class AdminEmployeeRoleUpdateRequest(

    /** SF DKRetail__AppAuthority__c picklist 4종 raw value. */
    @field:NotBlank(message = "권한(role) 은 필수입니다")
    @field:Pattern(
        regexp = "^(여사원|조장|지점장|AccountViewAll)$",
        message = "AppAuthority 는 '여사원' / '조장' / '지점장' / 'AccountViewAll' 중 하나여야 합니다"
    )
    val role: String? = null
)
