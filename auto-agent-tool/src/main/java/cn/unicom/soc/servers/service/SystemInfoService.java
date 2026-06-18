package cn.unicom.soc.servers.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.text.DecimalFormat;
import java.util.Date;

public class SystemInfoService {

    private ObjectMapper objectMapper = new ObjectMapper();
    private Logger logger = LoggerFactory.getLogger(SystemInfoService.class);

    /**
     * 获取系统信息，包括操作系统、系统时间、磁盘、CPU和内存状态
     */
    @Tool(description = "获取系统信息，包括操作系统、系统时间、磁盘空间、CPU和内存使用状态")
    public String getSystemInfo() {
        logger.info("开始获取系统信息");

        try {
            ObjectNode result = objectMapper.createObjectNode();

            // 获取操作系统信息
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            result.put("operating_system", osBean.getName());
            result.put("os_version", osBean.getVersion());
            result.put("os_architecture", osBean.getArch());
            result.put("available_processors", osBean.getAvailableProcessors());

            // 获取系统时间
            Date currentTime = new Date();
            result.put("system_time", currentTime.toString());

            // 获取内存信息
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapMemoryUsage = memoryBean.getHeapMemoryUsage();
            MemoryUsage nonHeapMemoryUsage = memoryBean.getNonHeapMemoryUsage();

            ObjectNode memoryInfo = objectMapper.createObjectNode();
            memoryInfo.put("heap_memory_max", formatBytes(heapMemoryUsage.getMax()));
            memoryInfo.put("heap_memory_used", formatBytes(heapMemoryUsage.getUsed()));
            memoryInfo.put("heap_memory_committed", formatBytes(heapMemoryUsage.getCommitted()));
            memoryInfo.put("non_heap_memory_max", formatBytes(nonHeapMemoryUsage.getMax()));
            memoryInfo.put("non_heap_memory_used", formatBytes(nonHeapMemoryUsage.getUsed()));
            memoryInfo.put("non_heap_memory_committed", formatBytes(nonHeapMemoryUsage.getCommitted()));

            // 计算内存使用百分比
            double heapMemoryPercent = (double) heapMemoryUsage.getUsed() / heapMemoryUsage.getMax() * 100;
            double nonHeapMemoryPercent = (double) nonHeapMemoryUsage.getUsed() / nonHeapMemoryUsage.getMax() * 100;
            DecimalFormat df = new DecimalFormat("#.##");
            memoryInfo.put("heap_memory_percent", df.format(heapMemoryPercent) + "%");
            memoryInfo.put("non_heap_memory_percent", df.format(nonHeapMemoryPercent) + "%");

            result.set("memory_info", memoryInfo);

            // 获取CPU使用率（近似计算）
            ObjectNode cpuInfo = objectMapper.createObjectNode();
            double systemLoadAverage = osBean.getSystemLoadAverage();
            cpuInfo.put("system_load_average", systemLoadAverage);
            if (systemLoadAverage != -1) {
                double cpuUsage = Math.min(100.0, (systemLoadAverage / osBean.getAvailableProcessors()) * 100);
                cpuInfo.put("cpu_usage_percent", df.format(cpuUsage) + "%");
            } else {
                cpuInfo.put("cpu_usage_percent", "N/A");
            }
            result.set("cpu_info", cpuInfo);

            // 获取磁盘信息
            ObjectNode diskInfo = objectMapper.createObjectNode();
            File[] roots = File.listRoots();
            for (File root : roots) {
                ObjectNode disk = objectMapper.createObjectNode();
                long totalSpace = root.getTotalSpace();
                long freeSpace = root.getFreeSpace();
                long usableSpace = root.getUsableSpace();
                long usedSpace = totalSpace - freeSpace;

                disk.put("total_space", formatBytes(totalSpace));
                disk.put("free_space", formatBytes(freeSpace));
                disk.put("usable_space", formatBytes(usableSpace));
                disk.put("used_space", formatBytes(usedSpace));

                // 计算磁盘使用百分比
                double diskUsagePercent = (double) usedSpace / totalSpace * 100;
                disk.put("usage_percent", df.format(diskUsagePercent) + "%");

                diskInfo.set(root.getAbsolutePath(), disk);
            }
            result.set("disk_info", diskInfo);

            logger.info("系统信息获取成功");
            return result.toString();

        } catch (Exception e) {
            logger.error("获取系统信息失败: {}", e.getMessage(), e);
            ObjectNode errorResult = objectMapper.createObjectNode();
            errorResult.put("isError", true);
            errorResult.put("error", e.getMessage());
            return errorResult.toString();
        }
    }

    /**
     * 获取系统基本信息
     */
    @Tool(description = "获取系统基本信息，包括操作系统名称、版本、架构和可用处理器数量")
    public String getBasicSystemInfo() {
        logger.info("开始获取系统基本信息");

        try {
            ObjectNode result = objectMapper.createObjectNode();

            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            result.put("operating_system", osBean.getName());
            result.put("os_version", osBean.getVersion());
            result.put("os_architecture", osBean.getArch());
            result.put("available_processors", osBean.getAvailableProcessors());
            result.put("system_time", new Date().toString());

            logger.info("系统基本信息获取成功");
            return result.toString();

        } catch (Exception e) {
            logger.error("获取系统基本信息失败: {}", e.getMessage(), e);
            ObjectNode errorResult = objectMapper.createObjectNode();
            errorResult.put("isError", true);
            errorResult.put("error", e.getMessage());
            return errorResult.toString();
        }
    }

    /**
     * 格式化字节数为人类可读格式
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
}