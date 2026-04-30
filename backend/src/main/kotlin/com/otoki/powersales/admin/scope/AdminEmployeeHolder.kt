package com.otoki.powersales.admin.scope

import com.otoki.powersales.employee.entity.Employee
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

@Component
@RequestScope
class AdminEmployeeHolder {

    var employee: Employee? = null

    fun require(): Employee {
        return employee ?: throw IllegalStateException("AdminEmployee가 설정되지 않았습니다")
    }
}
