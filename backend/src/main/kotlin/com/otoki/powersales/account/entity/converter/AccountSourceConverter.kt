package com.otoki.powersales.account.entity.converter

import com.otoki.powersales.account.entity.AccountSource
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class AccountSourceConverter : AttributeConverter<AccountSource, String> {

    override fun convertToDatabaseColumn(attribute: AccountSource?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): AccountSource? =
        AccountSource.fromDisplayNameOrNull(dbData)
}
