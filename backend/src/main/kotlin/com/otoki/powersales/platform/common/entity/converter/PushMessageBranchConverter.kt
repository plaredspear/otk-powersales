package com.otoki.powersales.platform.common.entity.converter

import com.otoki.powersales.platform.common.enums.PushMessageBranch
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class PushMessageBranchConverter : AttributeConverter<PushMessageBranch, String> {

    override fun convertToDatabaseColumn(attribute: PushMessageBranch?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): PushMessageBranch? =
        PushMessageBranch.fromDisplayNameOrNull(dbData)
}
