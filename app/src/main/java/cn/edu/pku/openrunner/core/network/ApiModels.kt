package cn.edu.pku.openrunner.core.network

import com.google.gson.annotations.SerializedName
import java.util.Date

data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("code") val code: Int = 0,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: T? = null,
    @SerializedName("version") val version: Int? = null
) {
    fun requireData(): T {
        if (!success || data == null) {
            throw ApiException(code, message ?: "服务器返回了空数据")
        }
        return data
    }
}

class ApiException(val code: Int, override val message: String) : Exception(message)

data class UserDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String? = null,
    @SerializedName("department") val department: String? = null,
    @SerializedName("sex") val sex: String? = null,
    @SerializedName("isPESpecialty") val isPeSpecialty: Boolean? = null,
    @SerializedName("access_token") val accessToken: String? = null
)

data class TaskDto(
    @SerializedName("id") val id: Int,
    @SerializedName("activityId") val activityId: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("requirement") val requirement: String = "",
    @SerializedName("status") val status: Int = 0
) {
    val isVisible: Boolean get() = status != HIDDEN
    val isAcquired: Boolean get() = status == ACQUIRED

    companion object {
        const val HIDDEN = 0
        const val NOT_ACQUIRED = 1
        const val ACQUIRED = 2
    }
}

data class RunRecordDto(
    @SerializedName("id") val id: Int = -1,
    @SerializedName("recordId") val recordId: Int = -1,
    @SerializedName("distance") val distance: Int = 0,
    @SerializedName("duration") val duration: Double = 0.0,
    @SerializedName("date") val date: Date? = null,
    @SerializedName("step") val step: Int = 0,
    // The official API returns detail as [[longitude, latitude, status], ...].
    @SerializedName("detail") val track: List<List<Double>>? = null,
    @SerializedName("verified") val verified: Boolean = false,
    @SerializedName("uploaded") val uploaded: Boolean = true,
    @SerializedName("invalidReason") val invalidReason: Int = 0,
    @SerializedName("morningBonus") val morningBonus: Boolean = false,
    @SerializedName("photoPath") val photoPath: String? = null,
    /** Only set for a run saved on this device and still waiting to be uploaded. */
    @Transient val localId: String? = null
)

data class UserStatusDto(
    @SerializedName("beginDate") val beginDate: Date? = null,
    @SerializedName("endDate") val endDate: Date? = null,
    @SerializedName("current") val current: Int = 0,
    @SerializedName("bonus") val bonus: Int = 0,
    @SerializedName("target") val target: Int = 0,
    @SerializedName("validCount") val validCount: Int = 0,
    @SerializedName("isPassed") val isPassed: Boolean = false
)

data class VersionDto(
    @SerializedName("version") val version: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("force") val force: Boolean? = null
)
