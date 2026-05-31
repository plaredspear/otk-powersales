package com.otoki.powersales.product.dto.response

import com.otoki.powersales.product.entity.Product
import com.otoki.powersales.product.entity.ProductBarcode

data class ProductListResponse(
    val content: List<ProductListItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class ProductListItem(
    val id: Long,
    val productCode: String?,
    val name: String?,
    val category1: String?,
    val category2: String?,
    val category3: String?,
    val standardUnitPrice: java.math.BigDecimal?,
    val unit: String?,
    val storageCondition: String?,
    val productStatus: String?,
    val launchDate: String?,
    val superTax: java.math.BigDecimal?,
    val shelfLife: String?,
    val shelfLifeUnit: String?,
    val tasteGift: String?,
    val lastModifiedAt: String?
) {
    companion object {
        fun from(product: Product): ProductListItem = ProductListItem(
            id = product.id,
            productCode = product.productCode,
            name = product.name,
            category1 = product.productCategory1,
            category2 = product.productCategory2,
            category3 = product.productCategory3,
            standardUnitPrice = product.standardUnitPrice,
            unit = product.unit,
            storageCondition = product.storageCondition?.displayName,
            productStatus = product.productStatus?.displayName,
            launchDate = product.launchDate?.toString(),
            superTax = product.superTax,
            shelfLife = product.shelfLife,
            shelfLifeUnit = product.shelfLifeUnit,
            tasteGift = product.tasteGift,
            lastModifiedAt = product.updatedAt.toString()
        )
    }
}

data class CategoryTree(
    val category1: String,
    val children: List<Category2Node>
)

data class Category2Node(
    val category2: String,
    val children: List<String>
)

data class InventorySearchResponse(
    val results: List<InventorySearchResultItem>
)

data class InventorySearchResultItem(
    val productCode: String,
    val productName: String?,
    val unit: String?,
    val conversionQuantity: Int,
    val supplyLimitQuantity: Int,
    val unitPrice: java.math.BigDecimal,
    val message: String?
)

data class ProductDetail(
    val id: Long,
    val productCode: String?,
    val name: String?,
    val barcode: String?,
    val logisticsBarcode: String?,
    val category1: String?,
    val category2: String?,
    val category3: String?,
    val categoryCode1: String?,
    val categoryCode2: String?,
    val categoryCode3: String?,
    val unit: String?,
    val orderingUnit: String?,
    val conversionQuantity: Double?,
    val boxReceivingQuantity: java.math.BigDecimal?,
    val standardUnitPrice: java.math.BigDecimal?,
    val superTax: java.math.BigDecimal?,
    val launchDate: String?,
    val storageCondition: String?,
    val productStatus: String?,
    val productType: String?,
    val shelfLife: String?,
    val shelfLifeUnit: String?,
    val tasteGift: String?,
    val productFeatures: String?,
    val sellingPoint: String?,
    val purpose: String?,
    val targetAccountType: String?,
    val allergen: String?,
    val crossContamination: String?,
    val imgRefPathFront: String?,
    val imgRefPathBack: String?,
    val pallet: java.math.BigDecimal?,
    val manufacture: String?,
    val manufactureDetail: String?,
    val claimManagement: String?,
    val createdAt: String,
    val lastModifiedAt: String,
    val barcodes: List<ProductBarcodeItem>
) {
    companion object {
        // SF 레거시 수식 `IMAGE(ImgRefPathTXT__c + ImgRefPath_*__c)` 와 동등.
        // 파일명에 공백/한글/괄호가 들어있어 path segment encoding 적용.
        private fun buildImageUrl(baseUrl: String?, relativePath: String?): String? {
            if (baseUrl.isNullOrBlank() || relativePath.isNullOrBlank()) return null
            val encodedPath = java.net.URI(null, null, relativePath, null).rawPath
            return baseUrl.trimEnd('/') + "/" + encodedPath.trimStart('/')
        }

        fun from(product: Product, barcodes: List<ProductBarcode> = emptyList()): ProductDetail = ProductDetail(
            id = product.id,
            productCode = product.productCode,
            name = product.name,
            barcode = product.barcode,
            logisticsBarcode = product.logisticsBarcode,
            category1 = product.productCategory1,
            category2 = product.productCategory2,
            category3 = product.productCategory3,
            categoryCode1 = product.categoryCode1,
            categoryCode2 = product.categoryCode2,
            categoryCode3 = product.categoryCode3,
            unit = product.unit,
            orderingUnit = product.orderingUnit,
            conversionQuantity = product.conversionQuantity,
            boxReceivingQuantity = product.boxReceivingQuantity,
            standardUnitPrice = product.standardUnitPrice,
            superTax = product.superTax,
            launchDate = product.launchDate?.toString(),
            storageCondition = product.storageCondition?.displayName,
            productStatus = product.productStatus?.displayName,
            productType = product.productType?.displayName,
            shelfLife = product.shelfLife,
            shelfLifeUnit = product.shelfLifeUnit,
            tasteGift = product.tasteGift,
            productFeatures = product.productFeatures,
            sellingPoint = product.sellingPoint,
            purpose = product.purpose,
            targetAccountType = product.targetAccountType,
            allergen = product.allergen,
            crossContamination = product.crossContamination,
            imgRefPathFront = buildImageUrl(product.imgRefPathTxt, product.imgRefPathFront),
            imgRefPathBack = buildImageUrl(product.imgRefPathTxt, product.imgRefPathBack),
            pallet = product.pallet,
            manufacture = product.manufacture,
            manufactureDetail = product.manufactureDetail,
            claimManagement = product.claimManagement,
            createdAt = product.createdAt.toString(),
            lastModifiedAt = product.updatedAt.toString(),
            barcodes = barcodes
                .filter { it.isDeleted != true }
                .sortedBy { it.sortOrder }
                .map { ProductBarcodeItem.from(it) }
        )
    }
}

/**
 * 제품 바코드 항목 (SF ProductBarcode__c related list 동등)
 */
data class ProductBarcodeItem(
    val id: Int,
    val barcode: String?,
    val unit: String?,
    val sortOrder: String?,
    val productName: String?
) {
    companion object {
        fun from(entity: ProductBarcode): ProductBarcodeItem = ProductBarcodeItem(
            id = entity.id,
            barcode = entity.barcode,
            unit = entity.unit,
            sortOrder = entity.sortOrder,
            productName = entity.productName
        )
    }
}
