package cn.unicom.soc.servers.controller;

import cn.unicom.soc.servers.dto.ToolDto;
import cn.unicom.soc.servers.service.ToolSetService;
import cn.unicom.soc.servers.service.ToolTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tools")
@RequiredArgsConstructor
public class ToolTestController {
    
    @Autowired
    private final ToolSetService toolSetService;
    
    @Autowired
    private final ToolTestService toolTestService;
    
    /**
     * 测试工具
     */
    @PostMapping("/{id}/test")
    public ResponseEntity<Map<String, Object>> testTool(
            @PathVariable String id, 
            @RequestBody Map<String, Object> params) {
        try {
            // 获取工具信息
            ToolDto tool = toolSetService.getToolById(id);
            
            // 执行工具测试
            Map<String, Object> result = toolTestService.executeTool(tool, params);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}
