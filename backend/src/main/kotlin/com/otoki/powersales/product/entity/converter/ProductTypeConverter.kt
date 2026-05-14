package com.otoki.powersales.product.entity.converter

import com.otoki.powersales.product.enums.ProductType
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class ProductTypeConverter : AttributeConverter<ProductType, String> {

    override fun convertToDatabaseColumn(attribute: ProductType?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): ProductType? =
        ProductType.fromDisplayNameOrNull(dbData)
}
