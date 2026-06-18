package cn.unicom.soc.servers.controller;

import cn.unicom.soc.servers.service.MonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    @Autowired
    private MonitorService monitorService;

    /**
     * 获取仪表盘统计数据
     */
    @GetMapping("/dashboard-stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(monitorService.getDashboardStats());
    }

    /**
     * 获取调用趋势（按小时）
     */
    @GetMapping("/call-trend")
    public ResponseEntity<List<Map<String, Object>>> getCallTrend() {
        return ResponseEntity.ok(monitorService.getCallTrend());
    }

    /**
     * 获取工具类型分布
     */
    @GetMapping("/tool-distribution")
    public ResponseEntity<List<Map<String, Object>>> getToolTypeDistribution() {
        return ResponseEntity.ok(monitorService.getToolTypeDistribution());
    }

    /**
     * 获取最近调用日志
     */
    @GetMapping("/recent-logs")
    public ResponseEntity<List<Map<String, Object>>> getRecentLogs() {
        return ResponseEntity.ok(monitorService.getRecentLogs());
    }

    /**
     * 获取系统状态
     */
    @GetMapping("/system-status")
    public ResponseEntity<List<Map<String, Object>>> getSystemStatus() {
        return ResponseEntity.ok(monitorService.getSystemStatus());
    }
}
