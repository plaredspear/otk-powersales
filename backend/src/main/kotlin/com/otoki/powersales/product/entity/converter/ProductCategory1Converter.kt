package com.otoki.powersales.product.entity.converter

import com.otoki.powersales.product.enums.ProductCategory1
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class ProductCategory1Converter : AttributeConverter<ProductCategory1, String> {

    override fun convertToDatabaseColumn(attribute: ProductCategory1?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): ProductCategory1? =
        ProductCategory1.fromDisplayNameOrNull(dbData)
}
