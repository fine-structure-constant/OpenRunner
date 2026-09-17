package cn.edu.pku.openrunner.core.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface PkuNewYouthApi {
    @FormUrlEncoded
    @POST("user")
    suspend fun exchangeIaaaToken(
        @Field("access_token") iaaaAccessToken: String
    ): ApiResponse<UserDto>

    @GET("badge/user/{userId}")
    suspend fun getTasks(
        @Path("userId") userId: String
    ): ApiResponse<Map<String, TaskDto>>

    @GET("record/{userId}")
    suspend fun getRecords(
        @Path("userId") userId: String
    ): ApiResponse<List<RunRecordDto>>

    @GET("record/{userId}/{recordId}")
    suspend fun getRecord(
        @Path("userId") userId: String,
        @Path("recordId") recordId: String
    ): ApiResponse<RunRecordDto>

    @GET("record/status/{userId}")
    suspend fun getUserStatus(
        @Path("userId") userId: String
    ): ApiResponse<UserStatusDto>

    @Multipart
    @POST("record/{userId}")
    suspend fun uploadRecord(
        @Path("userId") userId: String,
        @Part("duration") duration: RequestBody,
        @Part("distance") distance: RequestBody,
        @Part("date") date: RequestBody,
        @Part("detail") detail: RequestBody,
        @Part("misc") misc: RequestBody,
        @Part("step") step: RequestBody,
        @Part("abstract") abstract: RequestBody,
        @Part photo: MultipartBody.Part? = null
    ): ApiResponse<RunRecordDto>

    @GET("record2/{userId}")
    suspend fun getGymRecords(
        @Path("userId") userId: String
    ): ApiResponse<List<RunRecordDto>>

    @FormUrlEncoded
    @POST("record2/{userId}/{recordId}")
    suspend fun verifyGymRecord(
        @Path("userId") userId: String,
        @Path("recordId") recordId: String,
        @Field("token") token: String
    ): ApiResponse<RunRecordDto>

    @POST("activity/{activityId}/user/{userId}/team/{color}")
    suspend fun signUpActivity(
        @Path("activityId") activityId: Int,
        @Path("userId") userId: String,
        @Path("color") color: String
    ): ApiResponse<Any?>

    @POST("activity/20180420/user/{userId}/team/purple")
    suspend fun clearLegacyActivity(
        @Path("userId") userId: String
    ): ApiResponse<Any?>

    @GET("public/client/android/curr_version")
    suspend fun getCurrentVersion(): VersionDto

    @GET("public/client/android/min_version")
    suspend fun getMinimumVersion(): VersionDto
}
