package cn.unicom.soc.servers.controller;

import cn.unicom.soc.servers.dto.ToolDto;
import cn.unicom.soc.servers.service.ToolSetService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tools")
@RequiredArgsConstructor
public class ToolController {
    
    @Autowired
    private final ToolSetService toolSetService;
    
    /**
     * 获取所有工具
     */
    @GetMapping
    public ResponseEntity<List<ToolDto>> getAllTools() {
        List<ToolDto> tools = toolSetService.getAllTools();
        return ResponseEntity.ok(tools);
    }

    /**
     * 分页条件查询工具
     */
    @GetMapping("/page")
    public ResponseEntity<Page<ToolDto>> getToolsByPage(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String toolSetId,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ToolDto> toolPage = toolSetService.getToolsByPage(type, toolSetId, name, page, size);
        return ResponseEntity.ok(toolPage);
    }
    
    /**
     * 根据ID获取工具
     */
    @GetMapping("/{id}")
    public ResponseEntity<ToolDto> getToolById(@PathVariable String id) {
        ToolDto tool = toolSetService.getToolById(id);
        return ResponseEntity.ok(tool);
    }
    
    /**
     * 根据工具集ID获取工具
     */
    @GetMapping("/set/{toolSetId}")
    public ResponseEntity<List<ToolDto>> getToolsBySetId(@PathVariable String toolSetId) {
        List<ToolDto> tools = toolSetService.getToolsBySetId(toolSetId);
        return ResponseEntity.ok(tools);
    }
    
    /**
     * 根据类型获取工具
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<ToolDto>> getToolsByType(@PathVariable String type) {
        List<ToolDto> tools = toolSetService.getToolsByType(type);
        return ResponseEntity.ok(tools);
    }
    
    /**
     * 更新工具
     */
    @PutMapping("/{id}")
    public ResponseEntity<ToolDto> updateTool(@PathVariable String id, @RequestBody ToolDto toolDto) {
        ToolDto updatedTool = toolSetService.updateTool(id, toolDto);
        return ResponseEntity.ok(updatedTool);
    }
    
    /**
     * 删除工具
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTool(@PathVariable String id) {
        toolSetService.deleteTool(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 启用/禁用工具
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ToolDto> toggleToolStatus(@PathVariable String id, @RequestBody Map<String, Integer> request) {
        Integer status = request.get("status");
        ToolDto updatedTool = toolSetService.toggleToolStatus(id, status);
        return ResponseEntity.ok(updatedTool);
    }
}
