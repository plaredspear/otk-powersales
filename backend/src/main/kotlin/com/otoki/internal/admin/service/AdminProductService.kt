package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.response.Category2Node
import com.otoki.internal.admin.dto.response.CategoryTree
import com.otoki.internal.admin.dto.response.ProductListItem
import com.otoki.internal.admin.dto.response.ProductListResponse
import com.otoki.internal.sap.repository.ProductRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminProductService(
    private val productRepository: ProductRepository
) {

    fun getProducts(
        keyword: String?,
        category1: String?,
        category2: String?,
        category3: String?,
        productStatus: String?,
        page: Int,
        size: Int
    ): ProductListResponse {
        val pageable = PageRequest.of(page, size, Sort.by("name").ascending())
        val productPage = productRepository.searchForAdmin(
            keyword = keyword,
            category1 = category1,
            category2 = category2,
            category3 = category3,
            productStatus = productStatus,
            pageable = pageable
        )

        return ProductListResponse(
            content = productPage.content.map { ProductListItem.from(it) },
            page = page,
            size = size,
            totalElements = productPage.totalElements,
            totalPages = productPage.totalPages
        )
    }

    fun getCategories(): List<CategoryTree> {
        val rows = productRepository.findDistinctCategories()

        return rows
            .groupBy { it.category1 }
            .map { (cat1, cat1Rows) ->
                CategoryTree(
                    category1 = cat1,
                    children = cat1Rows
                        .groupBy { it.category2 }
                        .map { (cat2, cat2Rows) ->
                            Category2Node(
                                category2 = cat2,
                                children = cat2Rows.map { it.category3 }.sorted()
                            )
                        }
                        .sortedBy { it.category2 }
                )
            }
            .sortedBy { it.category1 }
    }
}
