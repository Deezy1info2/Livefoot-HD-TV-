package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class LiveScoreViewModel : ViewModel() {
    private val _liveScores = MutableStateFlow<List<MatchData>>(emptyList())
    val liveScores: StateFlow<List<MatchData>> = _liveScores

    private val service = Retrofit.Builder()
        .baseUrl("https://livescore-api2.p.rapidapi.com/")
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(LiveScoreService::class.java)

    fun fetchLiveScores(apiKey: String) {
        viewModelScope.launch {
            try {
                val response = service.getLiveMatches(apiKey)
                _liveScores.value = response.data
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
