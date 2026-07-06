package com.example

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface LiveScoreService {
    @GET("fixtures")
    suspend fun getLiveMatches(
        @Header("x-rapidapi-key") apiKey: String,
        @Header("x-rapidapi-host") host: String = "livescore-api2.p.rapidapi.com",
        @Query("live") live: String = "1"
    ): LiveScoreResponse
}

data class LiveScoreResponse(
    val data: List<MatchData>
)

data class MatchData(
    val fixture: Fixture,
    val teams: Teams
)

data class Fixture(
    val id: Int,
    val status: Status
)

data class Status(
    val long: String,
    val short: String
)

data class Teams(
    val home: Team,
    val away: Team
)

data class Team(
    val name: String,
    val logo: String
)
