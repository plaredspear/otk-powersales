package com.otoki.powersales.product.entity.converter

import com.otoki.powersales.product.entity.ProductStatus
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class ProductStatusConverter : AttributeConverter<ProductStatus, String> {

    override fun convertToDatabaseColumn(attribute: ProductStatus?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): ProductStatus? =
        ProductStatus.fromDisplayNameOrNull(dbData)
}
