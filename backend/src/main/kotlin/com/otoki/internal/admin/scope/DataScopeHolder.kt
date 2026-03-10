package com.otoki.internal.admin.scope

import com.otoki.internal.admin.dto.DataScope
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

@Component
@RequestScope
class DataScopeHolder {

    var dataScope: DataScope? = null

    fun require(): DataScope {
        return dataScope ?: throw IllegalStateException("DataScope가 설정되지 않았습니다")
    }
}
