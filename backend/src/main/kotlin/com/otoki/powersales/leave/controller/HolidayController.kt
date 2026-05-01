package com.otoki.powersales.leave.controller

import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.leave.service.HolidayMasterService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/mobile/holidays")
class HolidayController(
    private val holidayMasterService: HolidayMasterService
) {

    @GetMapping
    fun getHolidays(
        @RequestParam year: Int
    ): ResponseEntity<ApiResponse<List<HolidayDateResponse>>> {
        val startDate = LocalDate.of(year, 1, 1)
        val endDate = LocalDate.of(year, 12, 31)
        val holidays = holidayMasterService.getHolidaysByDateRange(startDate, endDate)
        val response = holidays.map { HolidayDateResponse(it.holidayDate.toString(), it.name) }
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}

data class HolidayDateResponse(
    val date: String,
    val name: String
)
