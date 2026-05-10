package com.otoki.powersales.account.dto.request

import com.otoki.powersales.account.entity.AccountType
import com.otoki.powersales.account.entity.FreezerType
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

/**
 * 관리자 웹 거래처 수정 요청 DTO. (Spec #643)
 *
 * ## PUT 부분 갱신 시맨틱 (Q-E)
 *
 * - 모든 필드 nullable. **null 인 필드는 갱신 skip (보존 시맨틱)** — 본 프로젝트의 첫 PUT 엔드포인트로,
 *   "미포함 vs null 명시" 구분 메커니즘(JsonNode/JsonNullable) 미도입. 운영 시나리오 상 "null 로 덮어쓰기"
 *   요건 발생 빈도 매우 낮아 단순화 (스펙 §3 Q-E 추가 단순화 결정 — 본 P1-B 구현 시점).
 * - 빈 페이로드 PUT (`{}`) 시 모든 필드 null → 변경 0건 + 200 OK.
 * - `employeeCode = ""` 는 null 동등 (변경 안 함 시맨틱) — service 단 trim 후 isBlank 분기.
 *
 * ## SAP 동기 키 필드 silent ignore (Q-C, 옵션 A)
 *
 * `id`/`sfid`/`external_key`/`account_group`/`werk*`/`werk*_tx`/`sales_dept_*`/`division_*`/
 * `branch_cost_center`/`logistics_*`/`is_deleted`/`latitude`/`longitude` 는 **DTO 정의 자체에서 제외** —
 * 클라이언트가 페이로드에 포함해도 Jackson deserialization 단에서 무시 (Spring 기본 동작).
 * 의도치 않은 SAP 동기 키 변경 차단 안전성 우선.
 */
data class AdminAccountUpdateRequest(

    @field:Size(max = 255, message = "거래처명은 255자 이하여야 합니다.")
    val name: String? = null,

    @field:Size(max = 120, message = "주소1은 120자 이하여야 합니다.")
    val address1: String? = null,

    @field:Size(max = 120, message = "주소2는 120자 이하여야 합니다.")
    val address2: String? = null,

    @field:Size(max = 40, message = "전화번호는 40자 이하여야 합니다.")
    val phone: String? = null,

    @field:Size(max = 40, message = "휴대전화는 40자 이하여야 합니다.")
    val mobilePhone: String? = null,

    @field:Size(max = 100, message = "대표자는 100자 이하여야 합니다.")
    val representative: String? = null,

    @field:Email(message = "이메일 형식이 올바르지 않습니다.")
    @field:Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
    val email: String? = null,

    @field:Size(max = 100, message = "우편번호는 100자 이하여야 합니다.")
    val zipCode: String? = null,

    @field:Size(max = 255, message = "업종은 255자 이하여야 합니다.")
    val industry: String? = null,

    val description: String? = null,

    @field:Size(max = 255, message = "웹사이트는 255자 이하여야 합니다.")
    val website: String? = null,

    @field:Size(max = 40, message = "팩스는 40자 이하여야 합니다.")
    val fax: String? = null,

    @field:Size(max = 50, message = "영업시간1은 50자 이하여야 합니다.")
    val closingTime1: String? = null,

    @field:Size(max = 50, message = "영업시간2는 50자 이하여야 합니다.")
    val closingTime2: String? = null,

    @field:Size(max = 50, message = "영업시간3은 50자 이하여야 합니다.")
    val closingTime3: String? = null,

    @field:Size(max = 40, message = "거래처번호는 40자 이하여야 합니다.")
    val accountNumber: String? = null,

    @field:Size(max = 80, message = "사이트는 80자 이하여야 합니다.")
    val site: String? = null,

    @field:Size(max = 40, message = "거래처 출처는 40자 이하여야 합니다.")
    val accountSource: String? = null,

    @field:Size(max = 40, message = "지도 좌표는 40자 이하여야 합니다.")
    val mapCoordinate: String? = null,

    @field:Size(max = 18, message = "상위 거래처 sfid 는 18자 이하여야 합니다.")
    val parentSfid: String? = null,

    @field:Size(max = 20, message = "등급은 20자 이하여야 합니다.")
    val rating: String? = null,

    @field:Size(max = 20, message = "소유 구분은 20자 이하여야 합니다.")
    val ownership: String? = null,

    @field:Size(max = 40, message = "거래처 상태명은 40자 이하여야 합니다.")
    val accountStatusName: String? = null,

    @field:Size(max = 20, message = "거래처 상태 코드는 20자 이하여야 합니다.")
    val accountStatusCode: String? = null,

    @field:Size(max = 50, message = "업태는 50자 이하여야 합니다.")
    val businessType: String? = null,

    @field:Size(max = 50, message = "업종 분류는 50자 이하여야 합니다.")
    val businessCategory: String? = null,

    @field:Size(max = 80, message = "사업자등록번호는 80자 이하여야 합니다.")
    val businessLicenseNumber: String? = null,

    @field:Size(max = 1, message = "위탁 거래처 여부는 1자 이하여야 합니다.")
    val consignmentAcc: String? = null,

    @field:Size(max = 1, message = "유통 구분은 1자 이하여야 합니다.")
    val distribution: String? = null,

    val accountType: AccountType? = null,

    val freezerType: FreezerType? = null,

    val freezerInstalled: Boolean? = null,

    val firstInstalled: LocalDate? = null,

    val orderEndTime: LocalTime? = null,

    val remainingCredit: BigDecimal? = null,

    val totalCredit: BigDecimal? = null,

    val annualRevenue: BigDecimal? = null,

    val numberOfEmployees: Int? = null,

    @field:Size(max = 20, message = "담당 영업사원 사번은 20자 이하여야 합니다.")
    val employeeCode: String? = null,

    @field:Size(max = 100, message = "지점 코드는 100자 이하여야 합니다.")
    val branchCode: String? = null,

    @field:Size(max = 250, message = "지점명은 250자 이하여야 합니다.")
    val branchName: String? = null,

    @field:Size(max = 20, message = "ABC 등급은 20자 이하여야 합니다.")
    val abcType: String? = null,

    @field:Size(max = 40, message = "ABC 등급 코드는 40자 이하여야 합니다.")
    val abcTypeCode: String? = null
)
