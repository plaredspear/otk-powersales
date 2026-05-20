package com.otoki.powersales.sfmigration.stage1

import com.otoki.powersales.common.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * SF migration Stage 1 — S3 의 SF export CSV 를 backend 가 직접 PostgreSQL 에 COPY 적재.
 *
 * 권한: SYSTEM_ADMIN (= ROLE_ADMIN GrantedAuthority).
 *
 * 사전 조건:
 *  - 사용자가 extract-csv.sh 산출 CSV 를 S3 에 업로드해둔 상태.
 *  - DB 는 비어있거나 (첫 적재) ON CONFLICT DO NOTHING 으로 멱등 가능 (재적재).
 *
 * POC 한계:
 *  - 동기 호출 — 큰 entity 는 HTTP timeout 가능. ALB / EB 의 idle timeout 설정 확인 필요.
 *  - 정식 구축 시 비동기 job + 진행 상태 endpoint 로 전환.
 */
@RestController
class Stage1CopyController(
    private val service: Stage1S3CopyService,
) {

    @PostMapping("/api/v1/admin/sf-migration/stage1/copy-from-s3")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    fun copyFromS3(
        @Valid @RequestBody req: Stage1CopyRequest,
    ): ResponseEntity<ApiResponse<Stage1CopyResult>> {
        val result = service.copyFromS3(req.targetName, req.s3Bucket, req.s3Key)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @GetMapping("/api/v1/admin/sf-migration/stage1/targets")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    fun listTargets(): ResponseEntity<ApiResponse<List<String>>> {
        return ResponseEntity.ok(ApiResponse.success(Stage1Targets.list()))
    }
}
