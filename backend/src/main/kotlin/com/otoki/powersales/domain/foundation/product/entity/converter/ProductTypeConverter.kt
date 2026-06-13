package com.otoki.powersales.domain.foundation.product.entity.converter

import com.otoki.powersales.domain.foundation.product.enums.ProductType
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class ProductTypeConverter : AttributeConverter<ProductType, String> {

    override fun convertToDatabaseColumn(attribute: ProductType?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): ProductType? =
        ProductType.Companion.fromDisplayNameOrNull(dbData)
}
