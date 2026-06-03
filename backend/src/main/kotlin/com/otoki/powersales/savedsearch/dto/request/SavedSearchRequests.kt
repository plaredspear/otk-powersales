package com.otoki.powersales.savedsearch.dto.request

import com.otoki.powersales.savedsearch.entity.SavedSearchScope
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class SavedSearchCreateRequest(
    @field:NotBlank(message = "resourceKey 는 필수입니다")
    @field:Size(max = 50, message = "resourceKey 는 50자 이하여야 합니다")
    val resourceKey: String,

    @field:NotBlank(message = "검색 이름은 필수입니다")
    @field:Size(max = 100, message = "검색 이름은 100자 이하여야 합니다")
    val name: String,

    @field:NotNull(message = "scope 는 필수입니다")
    val scope: SavedSearchScope,

    @field:NotNull(message = "filters 는 필수입니다")
    val filters: Map<String, Any?>,

    val sortOrder: Int = 0,
)

data class SavedSearchUpdateRequest(
    @field:NotBlank(message = "검색 이름은 필수입니다")
    @field:Size(max = 100, message = "검색 이름은 100자 이하여야 합니다")
    val name: String,

    @field:NotNull(message = "filters 는 필수입니다")
    val filters: Map<String, Any?>,

    val sortOrder: Int = 0,
)
