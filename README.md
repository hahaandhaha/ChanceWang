# Smart Customer Service

一个最小可运行的 Java Spring Boot 智能客服项目：

- 后端：Spring Boot 3.5 + WebFlux
- 大模型：Qwen OpenAI-compatible API
- 编排框架：LangChain4j AI Service
- 地图工具：百度地图 MCP，本地 `stdio` 拉起
- 前端：原生 HTML/CSS/JS
- 输出方式：浏览器实时流式渲染

## 当前架构

这版已经迁移到 LangChain4j，并改成后端会话记忆模式：

1. 前端先调用 `POST /api/chat/sessions` 创建 `conversationId`
2. 后续聊天只发送当前一条 `message`
3. LangChain4j AI Service 使用 `@MemoryId` 绑定后端内存会话
4. 涉及路线、地点、天气、交通等问题时，按关键词启用百度地图 MCP 工具
5. 最终回答继续通过 `application/x-ndjson` 流式返回给前端

## 运行前准备

需要两个环境变量：

- `DASHSCOPE_API_KEY`：阿里云 Model Studio 的 API Key
- `BAIDU_MAPS_AK`：百度地图服务端 AK

PowerShell 示例：

```powershell
$env:DASHSCOPE_API_KEY="你的DashScopeKey"
$env:BAIDU_MAPS_AK="你的BaiduAK"
```

本地还需要：

- `Node.js`
- `npm / npx`

Windows 下项目会通过下面的命令自动拉起百度地图 MCP Server：

```powershell
cmd.exe /c npx -y @baidumap/mcp-server-baidu-map
```

## 启动

```powershell
mvn spring-boot:run
```

启动后打开：

[http://localhost:8080](http://localhost:8080)

## 默认配置

配置文件在 `src/main/resources/application.yml`：

- Qwen 默认模型：`qwen3.5-plus`
- 会话记忆窗口：`20` 条消息
- 百度地图 MCP：本地 `stdio`
- Windows MCP 启动命令：`cmd.exe /c npx -y @baidumap/mcp-server-baidu-map`

系统提示词放在：

- `src/main/resources/prompts/customer-support-system-prompt.txt`

## 接口

### 创建会话

`POST /api/chat/sessions`

返回示例：

```json
{
  "conversationId": "8d4d1f72-b9aa-4c35-a4c9-2f25c0f7f8f2"
}
```

### 流式聊天

`POST /api/chat/stream`

请求体示例：

```json
{
  "conversationId": "8d4d1f72-b9aa-4c35-a4c9-2f25c0f7f8f2",
  "message": "帮我推荐上海适合亲子周末去的景点"
}
```

返回内容类型：

`application/x-ndjson`

流中会持续返回：

- `delta`：增量文本
- `done`：结束事件，附带 `responseId` 和 token 统计
- `error`：错误事件

### 删除会话

`DELETE /api/chat/sessions/{conversationId}`

成功返回：

`204 No Content`

### 健康检查

`GET /api/chat/health`

返回：

```json
{
  "status": "ok"
}
```
