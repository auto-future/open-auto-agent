package cn.unicom.soc.servers.repository;

import cn.unicom.soc.servers.entity.ToolSetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ToolSetRepository extends JpaRepository<ToolSetEntity, String> {
    
    /**
     * 根据名称查询工具集
     */
    Optional<ToolSetEntity> findByName(@Param("name") String name);
    
    /**
     * 根据类型查询工具集
     */
    List<ToolSetEntity> findByType(@Param("type") String type);
    
    /**
     * 根据类型和状态查询工具集
     */
    List<ToolSetEntity> findByTypeAndStatus(@Param("type") String type, @Param("status") Integer status);
    
    /**
     * 根据名称模糊查询工具集
     */
    List<ToolSetEntity> findByNameContainingIgnoreCase(@Param("name") String name);
    
    /**
     * 查询启用状态的工具集
     */
    List<ToolSetEntity> findByStatus(@Param("status") Integer status);
    
    /**
     * 查询包含特定工具的工具集
     */
    @Query("SELECT DISTINCT ts FROM ToolSetEntity ts JOIN ts.tools t WHERE t.name LIKE %:toolName%")
    List<ToolSetEntity> findByToolNameContaining(@Param("toolName") String toolName);
}