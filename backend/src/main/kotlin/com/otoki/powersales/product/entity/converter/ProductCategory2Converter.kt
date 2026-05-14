package com.otoki.powersales.product.entity.converter

import com.otoki.powersales.product.enums.ProductCategory2
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class ProductCategory2Converter : AttributeConverter<ProductCategory2, String> {

    override fun convertToDatabaseColumn(attribute: ProductCategory2?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): ProductCategory2? =
        ProductCategory2.fromDisplayNameOrNull(dbData)
}
