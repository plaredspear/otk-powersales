package com.otoki.internal.inspection.repository

import com.otoki.internal.inspection.entity.InspectionTheme
import java.time.LocalDate

interface InspectionThemeRepositoryCustom {

    fun findActiveThemesByDate(targetDate: LocalDate): List<InspectionTheme>
}
