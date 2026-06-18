package cn.unicom.soc.servers.repository;

import cn.unicom.soc.servers.entity.ToolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Repository
public interface ToolRepository extends JpaRepository<ToolEntity, String> {
    
    /**
     * 根据名称和工具集ID查询工具
     */
    Optional<ToolEntity> findByNameAndToolSetId(@Param("name") String name, @Param("toolSetId") String toolSetId);
    
    /**
     * 根据工具集ID查询工具
     */
    List<ToolEntity> findByToolSetId(@Param("toolSetId") String toolSetId);
    
    /**
     * 根据工具集ID和状态查询工具
     */
    List<ToolEntity> findByToolSetIdAndStatus(@Param("toolSetId") String toolSetId, @Param("status") Integer status);
    
    /**
     * 根据类型查询工具
     */
    List<ToolEntity> findByType(@Param("type") String type);
    
    /**
     * 根据类型和状态查询工具
     */
    List<ToolEntity> findByTypeAndStatus(@Param("type") String type, @Param("status") Integer status);
    
    /**
     * 根据名称模糊查询工具
     */
    List<ToolEntity> findByNameContainingIgnoreCase(@Param("name") String name);
    
    /**
     * 根据工具集ID和名称查询工具
     */
    List<ToolEntity> findByToolSetIdAndName(@Param("toolSetId") String toolSetId, @Param("name") String name);
    
    /**
     * 查询启用状态的工具
     */
    List<ToolEntity> findByStatus(@Param("status") Integer status);

    /**
     * 分页条件查询工具
     */
    @Query("SELECT t FROM ToolEntity t WHERE " +
           "(:type IS NULL OR :type = '' OR t.type = :type) AND " +
           "(:toolSetId IS NULL OR :toolSetId = '' OR t.toolSetId = :toolSetId) AND " +
           "(:name IS NULL OR :name = '' OR LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%')))" +
           "ORDER BY t.createdAt DESC")
    Page<ToolEntity> findByConditions(
            @Param("type") String type,
            @Param("toolSetId") String toolSetId,
            @Param("name") String name,
            Pageable pageable);
}