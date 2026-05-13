package com.otoki.powersales.promotion.entity.converter

import com.otoki.powersales.promotion.entity.ProductTemperatureType
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class ProductTemperatureTypeConverter : AttributeConverter<ProductTemperatureType, String> {

    override fun convertToDatabaseColumn(attribute: ProductTemperatureType?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): ProductTemperatureType? =
        ProductTemperatureType.fromDisplayNameOrNull(dbData)
}
