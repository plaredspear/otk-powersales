package com.otoki.powersales.admin.scope

import com.otoki.powersales.admin.security.AdminPermission
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

@Component
@RequestScope
class AdminPermissionHolder {

    var permissions: Set<AdminPermission> = emptySet()
}
