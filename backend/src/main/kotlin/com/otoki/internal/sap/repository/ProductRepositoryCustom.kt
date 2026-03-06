package com.otoki.internal.sap.repository

import com.otoki.internal.sap.entity.Product
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ProductRepositoryCustom {

    fun searchByText(query: String, pageable: Pageable): Page<Product>

    fun searchByTextIncludingBarcode(query: String, pageable: Pageable): Page<Product>
}
