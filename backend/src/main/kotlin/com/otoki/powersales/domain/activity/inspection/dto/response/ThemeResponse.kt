package com.otoki.powersales.domain.activity.inspection.dto.response

import com.otoki.powersales.domain.activity.inspection.entity.InspectionTheme

/**
 * 현장 점검 테마 응답 DTO
 */
data class ThemeResponse(
    val id: Long,
    /**
     * 화면 표시명. 레거시(fieldTalk write.jsp) 의 `title__c [department__c]` 표시 정합.
     * 엔티티 `name` 은 테마번호(TM00000001 코드)이므로 표시에 쓰지 않고, 테마명(title)에
     * 부서(department)를 덧붙여 내려준다. 제출은 id 로만 하므로 코드값은 노출 불필요.
     */
    val name: String,
    val startDate: String,
    val endDate: String
) {
    companion object {
        fun from(theme: InspectionTheme): ThemeResponse {
            val title = theme.title ?: ""
            val department = theme.department
            val displayName =
                if (!department.isNullOrBlank()) "$title [$department]" else title
            return ThemeResponse(
                id = theme.id,
                name = displayName,
                startDate = theme.startDate?.toString() ?: "",
                endDate = theme.endDate?.toString() ?: ""
            )
        }
    }
}
