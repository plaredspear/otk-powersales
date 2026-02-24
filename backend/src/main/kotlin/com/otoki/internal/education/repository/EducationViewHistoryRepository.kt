package com.otoki.internal.education.repository

import com.otoki.internal.education.entity.EducationViewHistory
import com.otoki.internal.education.entity.EducationViewHistoryId
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 교육 조회 이력 Repository
 */
interface EducationViewHistoryRepository : JpaRepository<EducationViewHistory, EducationViewHistoryId>
