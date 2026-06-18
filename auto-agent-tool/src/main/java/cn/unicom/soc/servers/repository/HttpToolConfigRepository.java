package cn.unicom.soc.servers.repository;

import cn.unicom.soc.servers.entity.HttpToolConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HttpToolConfigRepository extends JpaRepository<HttpToolConfigEntity, String> {
    
    /**
     * 根据名称和工具集ID查询HTTP工具
     */
    Optional<HttpToolConfigEntity> findByNameAndToolSetId(@Param("name") String name, @Param("toolSetId") String toolSetId);
    
    /**
     * 根据工具集ID查询HTTP工具
     */
    List<HttpToolConfigEntity> findByToolSetId(@Param("toolSetId") String toolSetId);
    
    /**
     * 根据工具集ID和状态查询HTTP工具
     */
    List<HttpToolConfigEntity> findByToolSetIdAndStatus(@Param("toolSetId") String toolSetId, @Param("status") Integer status);
    
    /**
     * 根据名称模糊查询HTTP工具
     */
    List<HttpToolConfigEntity> findByNameContainingIgnoreCase(@Param("name") String name);
    
    /**
     * 根据方法类型查询HTTP工具
     */
    List<HttpToolConfigEntity> findByMethod(@Param("method") String method);
    
    /**
     * 根据状态查询HTTP工具
     */
    List<HttpToolConfigEntity> findByStatus(@Param("status") Integer status);
    
    /**
     * 根据URL模糊查询HTTP工具
     */
    List<HttpToolConfigEntity> findByUrlContainingIgnoreCase(@Param("url") String url);
}