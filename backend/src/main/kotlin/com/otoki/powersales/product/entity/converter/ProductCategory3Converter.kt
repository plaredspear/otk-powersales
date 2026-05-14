package com.otoki.powersales.product.entity.converter

import com.otoki.powersales.product.entity.ProductCategory3
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class ProductCategory3Converter : AttributeConverter<ProductCategory3, String> {

    override fun convertToDatabaseColumn(attribute: ProductCategory3?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): ProductCategory3? =
        ProductCategory3.fromDisplayNameOrNull(dbData)
}
