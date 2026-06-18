package cn.unicom.soc.servers.controller;

import cn.unicom.soc.servers.dto.HttpToolConfigDto;
import cn.unicom.soc.servers.dto.ToolDto;
import cn.unicom.soc.servers.dto.ToolSetDto;
import cn.unicom.soc.servers.service.ToolSetService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tool-sets")
@RequiredArgsConstructor
public class ToolSetController {
    
    @Autowired
    private final ToolSetService toolSetService;
    
    /**
     * 获取所有工具集
     */
    @GetMapping
    public ResponseEntity<List<ToolSetDto>> getAllToolSets() {
        List<ToolSetDto> toolSets = toolSetService.getAllToolSets();
        return ResponseEntity.ok(toolSets);
    }
    
    /**
     * 根据ID获取工具集
     */
    @GetMapping("/{id}")
    public ResponseEntity<ToolSetDto> getToolSetById(@PathVariable String id) {
        ToolSetDto toolSet = toolSetService.getToolSetById(id);
        return ResponseEntity.ok(toolSet);
    }
    
    /**
     * 根据类型获取工具集
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<ToolSetDto>> getToolSetsByType(@PathVariable String type) {
        List<ToolSetDto> toolSets = toolSetService.getToolSetsByType(type);
        return ResponseEntity.ok(toolSets);
    }
    
    /**
     * 根据状态获取工具集
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ToolSetDto>> getToolSetsByStatus(@PathVariable Integer status) {
        List<ToolSetDto> toolSets = toolSetService.getToolSetsByStatus(status);
        return ResponseEntity.ok(toolSets);
    }
    
    /**
     * 创建工具集
     */
    @PostMapping
    public ResponseEntity<ToolSetDto> createToolSet(@RequestBody ToolSetDto toolSetDto) {
        ToolSetDto createdToolSet = toolSetService.createToolSet(toolSetDto);
        return ResponseEntity.ok(createdToolSet);
    }
    
    /**
     * 更新工具集
     */
    @PutMapping("/{id}")
    public ResponseEntity<ToolSetDto> updateToolSet(@PathVariable String id, @RequestBody ToolSetDto toolSetDto) {
        ToolSetDto updatedToolSet = toolSetService.updateToolSet(id, toolSetDto);
        return ResponseEntity.ok(updatedToolSet);
    }
    
    /**
     * 删除工具集
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteToolSet(@PathVariable String id) {
        toolSetService.deleteToolSet(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 启用/禁用工具集
     */
    @PatchMapping("/{id}/status/{status}")
    public ResponseEntity<ToolSetDto> toggleToolSetStatus(@PathVariable String id, @PathVariable Integer status) {
        ToolSetDto updatedToolSet = toolSetService.toggleToolSetStatus(id, status);
        return ResponseEntity.ok(updatedToolSet);
    }
    
    /**
     * 添加工具到工具集
     */
    @PostMapping("/{toolSetId}/tools")
    public ResponseEntity<ToolDto> addToolToSet(@PathVariable String toolSetId, @RequestBody ToolDto toolDto) {
        ToolDto addedTool = toolSetService.addToolToSet(toolSetId, toolDto);
        return ResponseEntity.ok(addedTool);
    }
    
    /**
     * 添加HTTP工具到工具集
     */
    @PostMapping("/{toolSetId}/http-tools")
    public ResponseEntity<HttpToolConfigDto> addHttpToolToSet(@PathVariable String toolSetId, @RequestBody HttpToolConfigDto httpToolDto) {
        HttpToolConfigDto addedHttpTool = toolSetService.addHttpToolToSet(toolSetId, httpToolDto);
        return ResponseEntity.ok(addedHttpTool);
    }
}