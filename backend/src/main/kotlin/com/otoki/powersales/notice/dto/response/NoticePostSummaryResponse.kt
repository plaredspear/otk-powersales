package com.otoki.powersales.notice.dto.response

import java.time.LocalDateTime

/**
 * 공지사항 게시물 요약 Response (목록용)
 *
 * 컬럼 구성은 SF `DKRetail__Notice__c` 의 `DKRetail__All` ListView("모두" 뷰) 기준.
 * - scope    ← DKRetail__Scope__c (공개범위)
 * - branch   ← DKRetail__Jeejum__c (지점)
 * - department ← Department__c = EmployeeId__r.DKRetail__OrgName__c (부서, 작성 직원 소속 조직명)
 * - authorName ← OwnerName__c = Owner:User.LastName (작성자명)
 */
data class NoticePostSummaryResponse(
    val id: Long,
    val category: String,           // enum name (e.g., "COMPANY")
    val categoryName: String,       // enum displayName (e.g., "회사공지")
    val scope: String?,             // 공개범위 (e.g., "영업사원")
    val title: String,
    val branch: String?,            // 지점 (지점공지일 때만 값 존재)
    val department: String?,        // 부서 (작성 직원 소속 조직명)
    val authorName: String?,        // 작성자명
    val createdAt: LocalDateTime?
)
