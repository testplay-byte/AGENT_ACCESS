package com.anitrack.app.data.api.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    val data: T? = null,
    @Json(name = "errors") val errors: List<ApiError>? = null
)

@JsonClass(generateAdapter = true)
data class ApiError(
    val message: String? = null,
    val status: Int? = null,
    val locations: List<ErrorLocation>? = null
)

@JsonClass(generateAdapter = true)
data class ErrorLocation(
    val line: Int? = null,
    val column: Int? = null
)

// Page wrapper for paginated responses
@JsonClass(generateAdapter = true)
data class PageResponse<T>(
    @Json(name = "Page") val page: PageInfo<T>? = null
)

@JsonClass(generateAdapter = true)
data class PageInfo<T>(
    @Json(name = "pageInfo") val pageInfo: PageMeta? = null,
    @Json(name = "media") val media: List<T>? = null
)

@JsonClass(generateAdapter = true)
data class PageMeta(
    val total: Int? = null,
    @Json(name = "currentPage") val currentPage: Int? = null,
    @Json(name = "lastPage") val lastPage: Int? = null,
    @Json(name = "hasNextPage") val hasNextPage: Boolean? = false,
    @Json(name = "perPage") val perPage: Int? = null
)

// GraphQL Request/Response wrappers
@JsonClass(generateAdapter = true)
data class GraphQLRequest(
    val query: String,
    val variables: Map<String, Any?>? = emptyMap()
)

@JsonClass(generateAdapter = true)
data class GraphQLResponse<T>(
    val data: T? = null,
    val errors: List<GraphQLError>? = null
)

@JsonClass(generateAdapter = true)
data class GraphQLError(
    val message: String? = null,
    val locations: List<ErrorLocation>? = null,
    val path: List<String>? = null
)
