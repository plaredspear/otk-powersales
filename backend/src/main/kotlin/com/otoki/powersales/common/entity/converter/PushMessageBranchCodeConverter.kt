package com.otoki.powersales.common.entity.converter

import com.otoki.powersales.common.enums.PushMessageBranchCode
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class PushMessageBranchCodeConverter : AttributeConverter<PushMessageBranchCode, String> {

    override fun convertToDatabaseColumn(attribute: PushMessageBranchCode?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): PushMessageBranchCode? =
        PushMessageBranchCode.fromDisplayNameOrNull(dbData)
}
