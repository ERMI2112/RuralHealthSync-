package com.example.ruralhealthsync.data.remote

import com.example.ruralhealthsync.data.local.PatientEntity
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Multipart
import retrofit2.http.Part
import okhttp3.RequestBody
import okhttp3.MultipartBody

interface ApiService {

    @POST("sync_patients.php")
    suspend fun syncPatientsFull(@Body request: SyncRequest): Response<SyncResponse>

    @POST("login.php")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("register.php")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("manage_users.php")
    suspend fun manageUsers(@Body request: ManageUsersRequest): Response<ManageUsersResponse>

    // Added to support old SyncWorker calls if necessary, but we should update SyncWorker
    @POST("sync_patients.php")
    suspend fun syncPatients(@Body patients: List<PatientEntity>): Response<SyncResponse>

    // Multipart upload for patient photo
    @Multipart
    @POST("upload_image.php")
    suspend fun uploadPatientPhoto(
        @Part("patientName") patientName: RequestBody,
        @Part("timestamp") timestamp: RequestBody,
        @Part("serverId") serverId: RequestBody?,
        @Part image: MultipartBody.Part
    ): Response<UploadResponse>

    data class UploadResponse(
        val success: Boolean,
        val message: String?,
        val photoUrl: String?
    )
}

data class SyncRequest(
    val workerId: Int,
    val patients: List<PatientEntity>,
    val deletedServerIds: List<Int> = emptyList()
)

data class SyncResponse(
    val success: Boolean = false,
    val message: String? = null,
    val syncedIds: List<Int>? = null,
    val syncedRecords: List<SyncedRecord>? = null,
    val patients: List<RemotePatientDto>? = null
)

data class SyncedRecord(
    val localId: Int? = null,
    val serverId: Int? = null
)

/** Gson-safe DTO — server JSON may omit or null fields; map to [PatientEntity] before Room. */
data class RemotePatientDto(
    val serverId: Int? = null,
    val workerId: Int? = null,
    val fullName: String? = null,
    val age: Int? = null,
    val gender: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val diagnosis: String? = null,
    val photoUrl: String? = null,
    val createdAt: Long? = null,
    val nextVisitDate: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    fun toEntity(): PatientEntity = PatientEntity(
        serverId = serverId ?: 0,
        workerId = workerId ?: 0,
        fullName = fullName.orEmpty(),
        age = age ?: 0,
        gender = gender.orEmpty(),
        phone = phone.orEmpty(),
        address = address.orEmpty(),
        diagnosis = diagnosis.orEmpty(),
        photoUrl = photoUrl.orEmpty(),
        createdAt = createdAt ?: System.currentTimeMillis(),
        nextVisitDate = nextVisitDate ?: 0L,
        latitude = latitude ?: 0.0,
        longitude = longitude ?: 0.0,
        isSynced = true
    )
}

data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val password: String,
    val fullName: String
)

data class AuthResponse(
    val success: Boolean,
    val message: String,
    val userId: Int? = null,
    val fullName: String? = null,
    val role: String? = null
)

data class ManageUsersRequest(
    val action: String,
    val adminId: Int? = null,
    val targetId: Int? = null,
    val username: String? = null,
    val password: String? = null,
    val full_name: String? = null,
    val role: String? = null
)

data class ManageUsersResponse(
    val success: Boolean,
    val message: String,
    val users: List<UserDto>? = null
)

data class UserDto(
    val id: Int = 0,
    val username: String,
    val full_name: String,
    val role: String,
    val password: String? = null
)
