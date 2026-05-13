package com.otoki.powersales.leave.service

import com.otoki.powersales.leave.dto.request.HolidayMasterCreateRequest
import com.otoki.powersales.leave.dto.request.HolidayMasterUpdateRequest
import com.otoki.powersales.leave.dto.response.HolidayMasterResponse
import com.otoki.powersales.leave.entity.HolidayMaster
import com.otoki.powersales.leave.entity.HolidayType
import com.otoki.powersales.leave.exception.HolidayDateDuplicateException
import com.otoki.powersales.leave.exception.HolidayNotFoundException
import com.otoki.powersales.leave.exception.InvalidHolidayTypeException
import com.otoki.powersales.leave.repository.HolidayMasterRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminHolidayMasterService(
    private val holidayMasterRepository: HolidayMasterRepository
) {

    fun getHolidayMasters(year: Int): List<HolidayMasterResponse> {
        return holidayMasterRepository.findByYearOrderByHolidayDateAsc(year)
            .map { HolidayMasterResponse.Companion.from(it) }
    }

    @Transactional
    fun createHolidayMaster(request: HolidayMasterCreateRequest): HolidayMasterResponse {
        val type = parseType(request.type)

        if (holidayMasterRepository.existsByHolidayDate(request.holidayDate)) {
            throw HolidayDateDuplicateException()
        }

        val holidayMaster = holidayMasterRepository.save(
            HolidayMaster(
                holidayDate = request.holidayDate,
                name = request.name,
                type = type,
                year = request.holidayDate.year
            )
        )

        return HolidayMasterResponse.Companion.from(holidayMaster)
    }

    @Transactional
    fun updateHolidayMaster(id: Long, request: HolidayMasterUpdateRequest): HolidayMasterResponse {
        val holidayMaster = holidayMasterRepository.findById(id)
            .orElseThrow { HolidayNotFoundException() }

        val type = parseType(request.type)

        if (holidayMasterRepository.existsByHolidayDateAndIdNot(request.holidayDate, id)) {
            throw HolidayDateDuplicateException()
        }

        holidayMaster.update(
            holidayDate = request.holidayDate,
            name = request.name,
            type = type
        )

        return HolidayMasterResponse.Companion.from(holidayMaster)
    }

    @Transactional
    fun deleteHolidayMaster(id: Long) {
        val holidayMaster = holidayMasterRepository.findById(id)
            .orElseThrow { HolidayNotFoundException() }

        holidayMasterRepository.delete(holidayMaster)
    }

    private fun parseType(value: String): HolidayType =
        HolidayType.fromDisplayNameOrNull(value) ?: throw InvalidHolidayTypeException()
}
