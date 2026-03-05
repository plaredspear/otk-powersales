package com.otoki.internal.admin.controller

import com.otoki.internal.branch.dto.response.BranchResponse
import com.otoki.internal.branch.service.BranchService
import com.otoki.internal.common.dto.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/branches")
class AdminBranchController(
    private val branchService: BranchService
) {

    @GetMapping
    fun getBranches(): ResponseEntity<ApiResponse<List<BranchResponse>>> {
        val response = branchService.getBranches()
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
