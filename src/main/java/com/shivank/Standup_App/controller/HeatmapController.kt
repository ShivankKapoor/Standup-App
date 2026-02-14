package com.shivank.Standup_App.controller

import com.shivank.Standup_App.service.HeatmapService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class HeatmapController {

    @Autowired
    private lateinit var heatmapService: HeatmapService

    @GetMapping("/heatmap")
    fun getHeatmapData(
        @RequestParam(defaultValue = "365") daysBack: Int,
        request: HttpServletRequest
    ): ResponseEntity<HeatmapService.HeatmapData> {
        // Session is already validated by AuthenticationInterceptor
        // currentUser is available in request attributes if needed
        
        val heatmapData = heatmapService.getHeatmapData(daysBack)
        return ResponseEntity.ok(heatmapData)
    }
}
