package cn.unicom.soc.servers.repository;

import cn.unicom.soc.servers.entity.ToolCallLogEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ToolCallLogRepository extends JpaRepository<ToolCallLogEntity, String> {

    /**
     * 查询今日调用日志
     */
    @Query("SELECT l FROM ToolCallLogEntity l WHERE l.createdAt >= :startOfDay ORDER BY l.createdAt DESC")
    List<ToolCallLogEntity> findTodayLogs(@Param("startOfDay") LocalDateTime startOfDay);

    /**
     * 查询最近的调用日志
     */
    List<ToolCallLogEntity> findTop15ByOrderByCreatedAtDesc();

    /**
     * 统计今日调用次数
     */
    @Query("SELECT COUNT(l) FROM ToolCallLogEntity l WHERE l.createdAt >= :startOfDay")
    long countTodayCalls(@Param("startOfDay") LocalDateTime startOfDay);

    /**
     * 查询今日所有调用日志
     */
    @Query("SELECT l FROM ToolCallLogEntity l WHERE l.createdAt >= :startOfDay ORDER BY l.createdAt DESC")
    List<ToolCallLogEntity> findTodayAllLogs(@Param("startOfDay") LocalDateTime startOfDay);

    /**
     * 按状态统计今日调用次数
     */
    @Query("SELECT l.status, COUNT(l) FROM ToolCallLogEntity l WHERE l.createdAt >= :startOfDay GROUP BY l.status")
    List<Object[]> countTodayCallsByStatus(@Param("startOfDay") LocalDateTime startOfDay);

    /**
     * 按工具类型统计调用次数
     */
    @Query("SELECT l.toolType, COUNT(l) FROM ToolCallLogEntity l GROUP BY l.toolType")
    List<Object[]> countByToolType();
}
