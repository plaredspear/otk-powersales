package com.otoki.powersales.domain.foundation.product.entity.converter

import com.otoki.powersales.domain.foundation.product.enums.ProductStatus
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class ProductStatusConverter : AttributeConverter<ProductStatus, String> {

    override fun convertToDatabaseColumn(attribute: ProductStatus?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): ProductStatus? =
        ProductStatus.Companion.fromDisplayNameOrNull(dbData)
}
