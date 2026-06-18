package cn.unicom.soc.servers.service;

import cn.unicom.soc.servers.entity.ToolCallLogEntity;
import cn.unicom.soc.servers.entity.ToolEntity;
import cn.unicom.soc.servers.entity.ToolSetEntity;
import cn.unicom.soc.servers.repository.ToolCallLogRepository;
import cn.unicom.soc.servers.repository.ToolRepository;
import cn.unicom.soc.servers.repository.ToolSetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class MonitorService {

    @Autowired
    private ToolSetRepository toolSetRepository;

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private ToolCallLogRepository toolCallLogRepository;

    /**
     * 获取仪表盘统计数据
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSets", toolSetRepository.count());
        stats.put("totalTools", toolRepository.count());
        stats.put("todayCalls", countTodayCalls());
        return stats;
    }

    /**
     * 获取调用趋势数据（按小时）
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCallTrend() {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        List<ToolCallLogEntity> logs = toolCallLogRepository.findTodayAllLogs(startOfDay);

        int[] hourlyCounts = new int[24];
        for (ToolCallLogEntity log : logs) {
            if (log.getCreatedAt() != null) {
                int hour = log.getCreatedAt().getHour();
                if (hour >= 0 && hour < 24) {
                    hourlyCounts[hour]++;
                }
            }
        }

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("hour", i + ":00");
            point.put("count", hourlyCounts[i]);
            trend.add(point);
        }
        return trend;
    }

    /**
     * 获取工具类型分布
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getToolTypeDistribution() {
        List<Object[]> results = toolCallLogRepository.countByToolType();
        List<Map<String, Object>> distribution = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> item = new HashMap<>();
            item.put("type", row[0] != null ? row[0] : "unknown");
            item.put("count", row[1]);
            distribution.add(item);
        }
        return distribution;
    }

    /**
     * 获取最近调用日志
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecentLogs() {
        List<ToolCallLogEntity> logs = toolCallLogRepository.findTop15ByOrderByCreatedAtDesc();
        List<Map<String, Object>> logList = new ArrayList<>();
        for (ToolCallLogEntity log : logs) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", log.getId());
            item.put("toolName", log.getToolName());
            item.put("status", log.getStatus());
            item.put("duration", log.getDurationMs());
            item.put("message", log.getErrorMessage() != null ? log.getErrorMessage() : "执行成功");
            item.put("time", log.getCreatedAt() != null ? log.getCreatedAt().toString() : "");
            logList.add(item);
        }
        return logList;
    }

    /**
     * 获取系统状态
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSystemStatus() {
        List<Map<String, Object>> statuses = new ArrayList<>();

        // API服务状态
        statuses.add(createStatusItem("API服务", "online", "运行中"));

        // 数据库连接状态
        boolean dbConnected = checkDatabaseConnection();
        statuses.add(createStatusItem("数据库连接", dbConnected ? "online" : "offline", dbConnected ? "正常" : "连接失败"));

        // 今日调用成功率
        double successRate = calculateTodaySuccessRate();
        String successStatus = successRate >= 95 ? "online" : (successRate >= 80 ? "warning" : "offline");
        statuses.add(createStatusItem("今日调用成功率", successStatus, String.format("%.1f%%", successRate)));

        // 平均响应时间
        double avgDuration = calculateAvgDuration();
        String durationStatus = avgDuration < 200 ? "online" : (avgDuration < 500 ? "warning" : "offline");
        statuses.add(createStatusItem("平均响应时间", durationStatus, String.format("%.0fms", avgDuration)));

        return statuses;
    }

    /**
     * 记录工具调用日志
     */
    @Transactional
    public void saveCallLog(ToolCallLogEntity log) {
        toolCallLogRepository.save(log);
    }

    private long countTodayCalls() {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        return toolCallLogRepository.countTodayCalls(startOfDay);
    }

    private double calculateTodaySuccessRate() {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        List<Object[]> results = toolCallLogRepository.countTodayCallsByStatus(startOfDay);
        long total = 0;
        long success = 0;
        for (Object[] row : results) {
            String status = (String) row[0];
            Long count = (Long) row[1];
            total += count;
            if ("success".equals(status)) {
                success += count;
            }
        }
        return total > 0 ? (success * 100.0 / total) : 100.0;
    }

    private double calculateAvgDuration() {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        List<ToolCallLogEntity> logs = toolCallLogRepository.findTodayLogs(startOfDay);
        if (logs.isEmpty()) return 0;
        long total = logs.stream().filter(l -> l.getDurationMs() != null).mapToInt(ToolCallLogEntity::getDurationMs).sum();
        long count = logs.stream().filter(l -> l.getDurationMs() != null).count();
        return count > 0 ? (double) total / count : 0;
    }

    private boolean checkDatabaseConnection() {
        try {
            toolSetRepository.count();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, Object> createStatusItem(String name, String status, String value) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("status", status);
        item.put("value", value);
        return item;
    }
}
