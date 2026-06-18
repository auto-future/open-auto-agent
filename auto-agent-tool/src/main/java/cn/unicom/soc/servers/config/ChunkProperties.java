package cn.unicom.soc.servers.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "chunk")
@Data
public class ChunkProperties {
    private int size = 2000;                    // 分块大小
    private double overlapRatio = 0.1;          // 重叠比例
    private int threshold = 2000;               // 自动分块阈值
    private boolean enableOverlap = true;       // 是否启用重叠
    private String boundaryChars = ".!?;，。！？；\n\r"; // 断句字符
}