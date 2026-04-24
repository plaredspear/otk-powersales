package com.otoki.powersales.leave.entity

import com.otoki.powersales.common.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "holiday_master")
class HolidayMaster(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "holiday_date", nullable = false, unique = true)
    var holidayDate: LocalDate,

    @Column(name = "name", nullable = false, length = 50)
    var name: String,

    @Column(name = "type", nullable = false, length = 20)
    var type: String,

    @Column(name = "year", nullable = false)
    var year: Int
) : BaseEntity() {
    fun update(holidayDate: LocalDate, name: String, type: String) {
        this.holidayDate = holidayDate
        this.name = name
        this.type = type
        this.year = holidayDate.year

    }

    companion object {
        val VALID_TYPES = listOf("법정공휴일", "대체공휴일", "임시공휴일")
    }
}
