package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.request.HolidayMasterCreateRequest
import com.otoki.internal.admin.dto.request.HolidayMasterUpdateRequest
import com.otoki.internal.admin.dto.response.HolidayMasterResponse
import com.otoki.internal.leave.entity.HolidayMaster
import com.otoki.internal.leave.exception.HolidayDateDuplicateException
import com.otoki.internal.leave.exception.HolidayNotFoundException
import com.otoki.internal.leave.exception.InvalidHolidayTypeException
import com.otoki.internal.leave.repository.HolidayMasterRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminHolidayMasterService(
    private val holidayMasterRepository: HolidayMasterRepository
) {

    fun getHolidayMasters(year: Int): List<HolidayMasterResponse> {
        return holidayMasterRepository.findByYearOrderByHolidayDateAsc(year)
            .map { HolidayMasterResponse.from(it) }
    }

    @Transactional
    fun createHolidayMaster(request: HolidayMasterCreateRequest): HolidayMasterResponse {
        validateType(request.type)

        if (holidayMasterRepository.existsByHolidayDate(request.holidayDate)) {
            throw HolidayDateDuplicateException()
        }

        val holidayMaster = holidayMasterRepository.save(
            HolidayMaster(
                holidayDate = request.holidayDate,
                name = request.name,
                type = request.type,
                year = request.holidayDate.year
            )
        )

        return HolidayMasterResponse.from(holidayMaster)
    }

    @Transactional
    fun updateHolidayMaster(id: Long, request: HolidayMasterUpdateRequest): HolidayMasterResponse {
        val holidayMaster = holidayMasterRepository.findById(id)
            .orElseThrow { HolidayNotFoundException() }

        validateType(request.type)

        if (holidayMasterRepository.existsByHolidayDateAndIdNot(request.holidayDate, id)) {
            throw HolidayDateDuplicateException()
        }

        holidayMaster.update(
            holidayDate = request.holidayDate,
            name = request.name,
            type = request.type
        )

        return HolidayMasterResponse.from(holidayMaster)
    }

    @Transactional
    fun deleteHolidayMaster(id: Long) {
        val holidayMaster = holidayMasterRepository.findById(id)
            .orElseThrow { HolidayNotFoundException() }

        holidayMasterRepository.delete(holidayMaster)
    }

    private fun validateType(type: String) {
        if (type !in HolidayMaster.VALID_TYPES) {
            throw InvalidHolidayTypeException()
        }
    }
}
