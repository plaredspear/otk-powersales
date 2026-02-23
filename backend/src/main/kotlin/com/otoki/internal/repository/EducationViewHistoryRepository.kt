package com.otoki.internal.repository

import com.otoki.internal.entity.EducationViewHistory
import com.otoki.internal.entity.EducationViewHistoryId
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 교육 조회 이력 Repository
 */
interface EducationViewHistoryRepository : JpaRepository<EducationViewHistory, EducationViewHistoryId>
