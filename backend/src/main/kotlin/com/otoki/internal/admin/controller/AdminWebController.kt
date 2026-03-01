package com.otoki.internal.admin.controller

import com.otoki.internal.admin.dto.AdminUserDetails
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/admin")
class AdminWebController {

    @GetMapping("/login")
    fun loginPage(
        @RequestParam(required = false) error: String?,
        @RequestParam(required = false) logout: String?,
        @RequestParam(required = false) denied: String?,
        model: Model
    ): String {
        if (error != null) {
            model.addAttribute("errorMessage", "사번 또는 비밀번호가 올바르지 않습니다")
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "로그아웃되었습니다")
        }
        if (denied != null) {
            model.addAttribute("deniedMessage", "조장 이상 권한이 필요합니다")
        }
        return "admin/login"
    }

    @GetMapping("/dashboard")
    fun dashboard(
        @AuthenticationPrincipal principal: AdminUserDetails,
        model: Model
    ): String {
        model.addAttribute("userName", principal.displayName)
        model.addAttribute("userRole", principal.role.name)
        return "admin/dashboard"
    }
}
