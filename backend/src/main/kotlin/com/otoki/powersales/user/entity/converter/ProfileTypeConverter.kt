package com.otoki.powersales.user.entity.converter

import com.otoki.powersales.user.entity.ProfileType
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class ProfileTypeConverter : AttributeConverter<ProfileType?, String?> {

    override fun convertToDatabaseColumn(attribute: ProfileType?): String? =
        attribute?.value

    override fun convertToEntityAttribute(dbData: String?): ProfileType? =
        ProfileType.fromValue(dbData)
}
