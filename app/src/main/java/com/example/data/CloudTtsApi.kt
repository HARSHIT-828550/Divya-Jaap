package com.example.data

import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

@JsonClass(generateAdapter = true)
data class CloudTtsRequest(
    val text: String,
    val languageCode: String = "hi-IN",
    val voiceGender: String = "FEMALE",
    val speakingRate: Float = 0.85f
)

@JsonClass(generateAdapter = true)
data class CloudTtsResponse(
    val audioBase64: String? = null,
    val audioUrl: String? = null,
    val mimeType: String = "audio/mpeg",
    val voiceName: String? = null
)

interface CloudTtsApiService {
    @POST
    suspend fun synthesize(
        @Url endpointUrl: String,
        @Body request: CloudTtsRequest
    ): Response<CloudTtsResponse>
}
