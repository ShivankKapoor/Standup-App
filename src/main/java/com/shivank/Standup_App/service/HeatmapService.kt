package com.shivank.Standup_App.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Service
class HeatmapService {

    @Autowired
    private lateinit var standupDataService: StandupDataService

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    data class HeatmapDay(
        val date: String,
        val intensity: Int,  // 0-4 scale
        val wordCount: Int,
        val dayOfWeek: Int   // 0 = Sunday, 6 = Saturday
    )

    data class HeatmapData(
        val days: List<HeatmapDay>,
        val stats: HeatmapStats
    )

    data class HeatmapStats(
        val totalEntries: Int,
        val longestStreak: Int,
        val currentStreak: Int,
        val averageWordCount: Double,
        val mostProductiveDay: String
    )

    fun getHeatmapData(daysBack: Int = 365): HeatmapData {
        val allStandups = standupDataService.loadAllStandups()
        val today = LocalDate.now()
        
        // If daysBack is 9999, find the earliest entry and calculate from there
        val actualStartDate = if (daysBack >= 9999) {
            val earliestDate = allStandups.keys
                .mapNotNull { dateStr ->
                    try {
                        LocalDate.parse(dateStr, DATE_FORMATTER)
                    } catch (e: Exception) {
                        null
                    }
                }
                .minOrNull()
            
            earliestDate ?: today.minusDays(365) // Fallback to 1 year if no entries
        } else {
            today.minusDays(daysBack.toLong())
        }
        
        val startDate = actualStartDate

        // Calculate word counts for each day
        val wordCounts = allStandups.mapValues { (_, content) ->
            content.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
        }

        // Determine intensity thresholds
        val nonZeroCounts = wordCounts.values.filter { it > 0 }
        val maxCount = nonZeroCounts.maxOrNull() ?: 1
        val avgCount = if (nonZeroCounts.isNotEmpty()) {
            nonZeroCounts.average()
        } else {
            0.0
        }

        // Generate heatmap days
        val heatmapDays = mutableListOf<HeatmapDay>()
        var currentDate = startDate

        while (!currentDate.isAfter(today)) {
            val dateStr = currentDate.format(DATE_FORMATTER)
            val wordCount = wordCounts[dateStr] ?: 0
            val intensity = calculateIntensity(wordCount, avgCount, maxCount)
            val dayOfWeek = currentDate.dayOfWeek.value % 7 // Convert to 0=Sunday

            heatmapDays.add(HeatmapDay(dateStr, intensity, wordCount, dayOfWeek))
            currentDate = currentDate.plusDays(1)
        }

        // Calculate statistics
        val stats = calculateStats(heatmapDays, allStandups)

        return HeatmapData(heatmapDays, stats)
    }

    private fun calculateIntensity(wordCount: Int, avgCount: Double, maxCount: Int): Int {
        return when {
            wordCount == 0 -> 0
            wordCount < avgCount * 0.5 -> 1
            wordCount < avgCount -> 2
            wordCount < avgCount * 1.5 -> 3
            else -> 4
        }
    }

    private fun calculateStats(days: List<HeatmapDay>, allStandups: Map<String, String>): HeatmapStats {
        val entriesWithContent = days.filter { it.wordCount > 0 }
        val totalEntries = entriesWithContent.size

        // Calculate streaks
        val sortedDays = days.sortedBy { it.date }
        val (currentStreak, longestStreak) = calculateStreaks(sortedDays)

        // Average word count
        val avgWordCount = if (entriesWithContent.isNotEmpty()) {
            entriesWithContent.map { it.wordCount }.average()
        } else {
            0.0
        }

        // Most productive day (by total word count)
        val dayProductivity = entriesWithContent.groupBy { it.dayOfWeek }
            .mapValues { (_, daysList) -> daysList.sumOf { it.wordCount } }
        
        val mostProductiveDayNum = dayProductivity.maxByOrNull { it.value }?.key ?: 0
        val mostProductiveDay = getDayName(mostProductiveDayNum)

        return HeatmapStats(
            totalEntries = totalEntries,
            longestStreak = longestStreak,
            currentStreak = currentStreak,
            averageWordCount = avgWordCount,
            mostProductiveDay = mostProductiveDay
        )
    }

    private fun calculateStreaks(sortedDays: List<HeatmapDay>): Pair<Int, Int> {
        var currentStreak = 0
        var longestStreak = 0
        var tempStreak = 0

        val today = LocalDate.now().format(DATE_FORMATTER)
        var isCurrentStreakActive = false

        for (i in sortedDays.indices.reversed()) {
            val day = sortedDays[i]
            
            if (day.wordCount > 0) {
                tempStreak++
                if (day.date == today || (i < sortedDays.size - 1 && 
                    ChronoUnit.DAYS.between(
                        LocalDate.parse(day.date, DATE_FORMATTER),
                        LocalDate.parse(sortedDays[i + 1].date, DATE_FORMATTER)
                    ) == 1L)) {
                    if (day.date == today || !isCurrentStreakActive) {
                        isCurrentStreakActive = true
                    }
                    if (isCurrentStreakActive && i == sortedDays.size - 1) {
                        currentStreak = tempStreak
                    } else if (isCurrentStreakActive && sortedDays[i + 1].wordCount > 0) {
                        currentStreak = tempStreak
                    }
                }
                longestStreak = maxOf(longestStreak, tempStreak)
            } else {
                if (isCurrentStreakActive && currentStreak == 0) {
                    currentStreak = tempStreak
                }
                tempStreak = 0
                if (day.date != today) {
                    isCurrentStreakActive = false
                }
            }
        }

        // Simplified current streak calculation
        currentStreak = 0
        for (i in sortedDays.indices.reversed()) {
            val day = sortedDays[i]
            if (day.wordCount > 0) {
                currentStreak++
            } else if (day.date <= today) {
                break
            }
        }

        return Pair(currentStreak, longestStreak)
    }

    private fun getDayName(dayNum: Int): String {
        return when (dayNum) {
            0 -> "Sunday"
            1 -> "Monday"
            2 -> "Tuesday"
            3 -> "Wednesday"
            4 -> "Thursday"
            5 -> "Friday"
            6 -> "Saturday"
            else -> "Unknown"
        }
    }
}
