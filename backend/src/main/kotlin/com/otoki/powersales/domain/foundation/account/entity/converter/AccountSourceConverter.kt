package com.otoki.powersales.domain.foundation.account.entity.converter

import com.otoki.powersales.domain.foundation.account.entity.AccountSource
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class AccountSourceConverter : AttributeConverter<AccountSource, String> {

    override fun convertToDatabaseColumn(attribute: AccountSource?): String? =
        attribute?.displayName

    override fun convertToEntityAttribute(dbData: String?): AccountSource? =
        AccountSource.Companion.fromDisplayNameOrNull(dbData)
}
