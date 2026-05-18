package com.otoki.powersales.account.repository

import com.otoki.powersales.account.entity.Account
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface AccountRepositoryCustom {

    fun searchForAdmin(
        keyword: String?,
        abcType: String?,
        branchCodes: List<String>?,
        accountStatusName: String?,
        pageable: Pageable
    ): Page<Account>

    /**
     * 좌표 미수신 거래처 조회 — Naver Geocode batch (#637) 진입 조건.
     *
     * 조건:
     * - latitude IS NULL OR longitude IS NULL
     * - address1 IS NOT NULL
     * - external_key IS NOT NULL
     * - account_status_name = '거래'
     * - LIMIT [limit]
     *
     * 레거시 SOQL (`Batch_AccountLatLong.cls#start`) 동등.
     */
    fun findCoordinatesMissingAccounts(limit: Int): List<Account>

    /**
     * 동일 [name] + 활성(미삭제) 거래처 존재 여부.
     *
     * `is_deleted` 가 nullable Boolean 이므로 `IS NULL` 과 `= false` 두 케이스 모두 활성으로 간주.
     */
    fun existsActiveByName(name: String): Boolean

    /**
     * 활성(미삭제) 거래처 단건 조회.
     *
     * `is_deleted` 가 nullable Boolean 이므로 `IS NULL` 과 `= false` 두 케이스 모두 활성으로 간주.
     */
    fun findActiveById(id: Int): Account?

    /**
     * 동일 [name] + 활성(미삭제) + 자기 자신 ([id]) 제외 거래처 존재 여부.
     *
     * UPDATE 흐름에서 자기 자신은 중복 검증에서 제외 (`Trigger.oldMap` 비교 동등 효과).
     */
    fun existsActiveByNameAndIdNot(name: String, id: Int): Boolean
}
