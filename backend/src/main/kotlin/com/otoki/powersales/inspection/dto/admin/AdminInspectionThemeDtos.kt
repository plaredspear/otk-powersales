package com.otoki.powersales.inspection.dto.admin

import com.otoki.powersales.inspection.entity.InspectionTheme

/**
 * admin 현장점검(등록) 테마 DTO 모음.
 *
 * 레거시 매핑: SF `현장점검(등록)` = `Theme__c` 표준 레코드 페이지 (목록/등록/수정/삭제 + 엑셀다운로드).
 */

/** 테마 목록 항목 — SF ListView `All` 컬럼 (테마번호/테마이름/부서/시작일/종료일) + 소유자/점검결과 수. */
data class AdminThemeListItem(
    val id: Long,
    val name: String?,
    val title: String?,
    val department: String?,
    val branchCode: String?,
    val startDate: String?,
    val endDate: String?,
    val ownerName: String?,
    val siteActivityCount: Long,
    val createdAt: String?,
)

data class AdminThemeListResponse(
    val content: List<AdminThemeListItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

/** 테마 상세 — 헤더 + 하위 현장점검 결과 관련목록. */
data class AdminThemeDetailResponse(
    val id: Long,
    val name: String?,
    val title: String?,
    val department: String?,
    val branchCode: String?,
    val startDate: String?,
    val endDate: String?,
    val ownerName: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val siteActivities: List<AdminThemeSiteActivityItem>,
)

/** 테마 상세 하단 관련목록 (SF Theme 레이아웃의 SiteActivity__r related list 컬럼 대응). */
data class AdminThemeSiteActivityItem(
    val id: Long,
    val name: String?,
    val branchName: String?,
    val accountName: String?,
    val productName: String?,
    val orgName: String?,
    val employeeName: String?,
    val category: String?,
    val activityDate: String?,
)

/** 테마 생성 요청 — SF 편집 가능 필드 (테마이름/시작일/종료일). 테마번호·부서·지점은 서버 자동 부여. */
data class CreateThemeRequest(
    val title: String,
    val startDate: String?,
    val endDate: String?,
)

/** 테마 수정 요청. */
data class UpdateThemeRequest(
    val title: String,
    val startDate: String?,
    val endDate: String?,
)

/** 테마 생성/수정 응답. */
data class ThemeMutationResponse(
    val id: Long,
    val name: String?,
) {
    companion object {
        fun from(theme: InspectionTheme) = ThemeMutationResponse(id = theme.id, name = theme.name)
    }
}
