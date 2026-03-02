package com.otoki.internal.product.repository

import com.otoki.internal.product.entity.Product
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ProductRepositoryCustom {

    fun searchByText(query: String, pageable: Pageable): Page<Product>

    fun searchByTextIncludingBarcode(query: String, pageable: Pageable): Page<Product>
}
