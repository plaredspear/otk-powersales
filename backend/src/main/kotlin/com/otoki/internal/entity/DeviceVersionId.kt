package com.otoki.internal.entity

import java.io.Serializable

/**
 * DeviceVersion 복합 키
 *
 * device_version_mng 테이블의 (version, device) 복합 PK.
 */
class DeviceVersionId(
    val version: String = "",
    val device: String = ""
) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeviceVersionId) return false
        return version == other.version &&
            device == other.device
    }

    override fun hashCode(): Int {
        var result = version.hashCode()
        result = 31 * result + device.hashCode()
        return result
    }
}
