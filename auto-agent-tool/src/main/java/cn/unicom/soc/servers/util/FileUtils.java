package cn.unicom.soc.servers.util;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Description
 * @Author chenss
 * @CreateTime 2025-07-11 11:23:16
 * @ModifyTime
 */
public class FileUtils {
    /**
     * 将通配符路径转换为正则表达式
     * @param pattern 用户输入的通配符路径
     * @return 转换后的正则表达式
     */
    private static String convertToRegex(String pattern) {
        // 处理路径分隔符（兼容Windows和Unix）
        String normalized = pattern.replace("\\", "/");
        // 转义正则特殊字符（保留通配符*和?）
        String escaped = normalized.replace(".", "\\.")
                .replace("+", "\\+")
                .replace("$", "\\$")
                .replace("^", "\\^")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)");
        // 转换通配符为正则表达式
        return escaped.replace("*", ".*")
                .replace("?", ".");
    }

    /**
     * 获取匹配指定模式的所有文件
     * @param pattern 支持通配符的路径模式
     * @return 匹配的文件列表
     */
    public static List<String> matchFiles(String pattern) {
        try {
            // 分离目录部分和文件名部分
            int lastSlash = pattern.replace("\\", "/").lastIndexOf("/");
            String dirPath = lastSlash > 0 ? pattern.substring(0, lastSlash) : ".";
            String filePattern = lastSlash > 0 ? pattern.substring(lastSlash + 1) : pattern;

            // 构建正则表达式
            String regex = convertToRegex(filePattern);
            Pattern compiledPattern = Pattern.compile(regex);

            // 遍历目录查找匹配文件
            return Files.walk(Paths.get(dirPath))
                    .filter(Files::isRegularFile)
                    .filter(p -> compiledPattern.matcher(p.getFileName().toString()).matches())
                    .map(x->x.toFile().getAbsolutePath())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("文件匹配失败: " + e.getMessage(), e);
        }
    }
}
