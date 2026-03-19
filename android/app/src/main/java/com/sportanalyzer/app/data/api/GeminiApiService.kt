package com.sportanalyzer.app.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun analyzeVideo(
        @Header("x-goog-api-key") apiKey: String,
        @Path("model") model: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
}

data class GeminiRequest(
    val contents: List<ContentPart>
)

data class ContentPart(
    val parts: List<Part>
)

data class Part(
    val text: String? = null,
    @SerializedName("inline_data") val inlineData: InlineData? = null
)

data class InlineData(
    @SerializedName("mime_type") val mimeType: String,
    val data: String
)

data class GeminiResponse(
    val candidates: List<Candidate>? = null,
    val error: GeminiError? = null
)

data class GeminiError(
    val code: Int,
    val message: String,
    val status: String
)

data class Candidate(
    val content: Content,
    val finishReason: String? = null,
    val index: Int? = null,
    val safetyRatings: List<SafetyRating>? = null
)

data class Content(
    val parts: List<ResponsePart>,
    val role: String? = null
)

data class ResponsePart(
    val text: String? = null
)

data class SafetyRating(
    val category: String,
    val probability: String
)
