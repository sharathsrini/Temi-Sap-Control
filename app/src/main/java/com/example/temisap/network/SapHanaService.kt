package com.example.temisap.network

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.GET

data class SapCommand(
    @SerializedName("ID") val id: String,
    @SerializedName("Action") val action: String,
    @SerializedName("Target") val target: String,
    @SerializedName("Status") val status: String
)

data class ODataResponse(
    @SerializedName("@odata.context") val context: String,
    @SerializedName("value") val value: List<SapCommand>
)

interface SapHanaService {
    @GET("CommandSet")
    fun getPendingCommands(): Call<ODataResponse>
}
