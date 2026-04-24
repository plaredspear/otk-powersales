package com.otoki.powersales.inspection.repository

import com.otoki.powersales.inspection.entity.InspectionTheme
import java.time.LocalDate

interface InspectionThemeRepositoryCustom {

    fun findActiveThemesByDate(targetDate: LocalDate): List<InspectionTheme>
}
