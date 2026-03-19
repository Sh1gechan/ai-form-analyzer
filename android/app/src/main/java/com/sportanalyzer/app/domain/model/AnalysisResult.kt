package com.sportanalyzer.app.domain.model

data class AnalysisResult(
    val id: String,
    val videoUri: String,
    val timestamp: Long,
    val analysisText: String,
    val overallStats: OverallStats,
    val recommendations: List<String>
)

data class OverallStats(
    val averageFormScore: Float,
    val improvementAreas: List<String>
)
