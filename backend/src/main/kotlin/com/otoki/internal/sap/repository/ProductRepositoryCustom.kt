package com.otoki.internal.sap.repository

import com.otoki.internal.sap.entity.Product
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ProductRepositoryCustom {

    fun searchByText(query: String, pageable: Pageable): Page<Product>

    fun searchByTextIncludingBarcode(query: String, pageable: Pageable): Page<Product>

    fun searchForAdmin(
        keyword: String?,
        category1: String?,
        category2: String?,
        category3: String?,
        productStatus: String?,
        pageable: Pageable
    ): Page<Product>

    fun findDistinctCategories(): List<CategoryRow>
}

data class CategoryRow(
    val category1: String,
    val category2: String,
    val category3: String
)
