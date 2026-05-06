package com.example.smartcustomerservice.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.baidu-map-mcp")
public record BaiduMapMcpProperties(
        boolean enabled,
        boolean logEvents,
        List<String> triggerKeywords,
        List<String> command
) {

    public BaiduMapMcpProperties {
        triggerKeywords = triggerKeywords == null || triggerKeywords.isEmpty()
                ? List.of(
                "地图", "地点", "地址", "景点", "周边", "位置", "poi", "POI", "天气", "路线",
                "路径", "导航", "交通", "拥堵", "出行", "公交", "地铁", "打车", "驾车", "步行",
                "骑行", "距离", "多远", "route", "directions", "location", "weather"
        )
                : List.copyOf(triggerKeywords);
        command = command == null || command.isEmpty()
                ? List.of("cmd.exe", "/c", "npx", "-y", "@baidumap/mcp-server-baidu-map")
                : List.copyOf(command);
    }
}
