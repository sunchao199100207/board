# WebSocket 多玩家测试指南

## 概述

本项目提供了两种方式来测试10个玩家同时在线游戏的场景：

1. **集成测试**：使用 JUnit 5 编写的自动化测试，模拟10个WebSocket客户端
2. **手动测试**：通过浏览器打开多个标签页进行手动测试

## 方法一：运行集成测试（推荐）

### 前置条件

1. **确保Redis正在运行**
   ```bash
   # 检查Redis是否运行
   redis-cli ping
   # 应该返回 PONG
   ```

2. **确保项目依赖已安装**
   ```bash
   mvn clean install
   ```

### 运行测试

```bash
# 运行多玩家完整流程测试
mvn test -Dtest=MultiPlayerWebSocketIntegrationTest#testTenPlayersFullGameFlow

# 运行并发加入测试
mvn test -Dtest=MultiPlayerWebSocketIntegrationTest#testConcurrentJoinRoom

# 运行所有WebSocket测试
mvn test -Dtest=MultiPlayerWebSocketIntegrationTest
```

### 测试内容

测试会模拟以下完整流程：

1. ✅ **创建10个WebSocket客户端连接**
2. ✅ **第一个玩家创建房间**（房间号：9999，10人局）
3. ✅ **其他9个玩家加入房间**
4. ✅ **所有玩家选择座位**（1-10号座位）
5. ✅ **所有玩家准备**
6. ✅ **验证游戏自动开始**
7. ✅ **验证所有玩家收到身份信息**

### 测试输出

测试会输出详细的日志，包括：
- 每个玩家的连接状态
- 房间创建和加入过程
- 座位选择过程
- 准备状态变化
- 游戏开始和身份分发

## 方法二：手动测试（浏览器多标签页）

### 步骤

1. **启动应用**
   ```bash
   mvn spring-boot:run
   ```

2. **打开浏览器访问**
   ```
   http://localhost:8089/board/awlfj.html
   ```

3. **创建10个浏览器标签页**
   - 每个标签页代表一个玩家
   - 或者使用无痕模式（更方便）

4. **第一个玩家（标签页1）创建房间**
   - 输入房间号：`9999`
   - 设置人数：`10`
   - 点击"创建房间"

5. **其他9个玩家（标签页2-10）加入房间**
   - 输入房间号：`9999`
   - 点击"加入房间"

6. **所有玩家选择座位**
   - 每个玩家点击一个空座位（1-10号）

7. **所有玩家准备**
   - 每个玩家点击"准备"按钮

8. **验证游戏开始**
   - 所有玩家应该看到自己的身份信息
   - 检查身份是否正确分发

### 技巧

- **使用浏览器开发者工具**查看WebSocket消息
- **使用无痕模式**可以快速创建多个独立的会话
- **使用不同浏览器**（Chrome、Firefox、Edge等）可以更真实地模拟不同用户

## 方法三：使用自动化脚本（可选）

可以创建一个简单的HTML页面，使用JavaScript自动创建多个WebSocket连接：

```html
<!DOCTYPE html>
<html>
<head>
    <title>多玩家测试工具</title>
</head>
<body>
    <h1>多玩家测试工具</h1>
    <button onclick="createPlayers(10)">创建10个玩家</button>
    <div id="players"></div>
    
    <script>
        // 创建多个玩家客户端
        function createPlayers(count) {
            for (let i = 1; i <= count; i++) {
                createPlayer(i);
            }
        }
        
        function createPlayer(index) {
            // 这里可以使用iframe加载awlfj.html，或者直接创建WebSocket连接
            // 实现细节...
        }
    </script>
</body>
</html>
```

## 常见问题

### 1. Redis连接失败

**错误信息**：`Unable to connect to Redis`

**解决方案**：
- 确保Redis服务正在运行
- 检查`application.properties`中的Redis配置
- 如果Redis有密码，需要在配置文件中添加密码

### 2. WebSocket连接失败

**错误信息**：`WebSocket connection failed`

**解决方案**：
- 检查应用是否正常启动
- 检查端口是否正确（默认8089）
- 检查上下文路径是否正确（/board）
- 确保防火墙没有阻止连接

### 3. 测试超时

**错误信息**：`Test timeout`

**解决方案**：
- 增加测试超时时间（在测试类中修改`TIMEOUT_SECONDS`）
- 检查网络延迟
- 检查Redis性能

### 4. 玩家无法收到消息

**可能原因**：
- SessionId不匹配
- 订阅路径错误
- 消息序列化问题

**解决方案**：
- 检查WebSocket连接日志
- 验证消息格式是否正确
- 检查订阅路径是否匹配

## 测试配置

测试使用`application-test.properties`配置文件，主要配置：

- **随机端口**：`server.port=0`（由Spring Boot自动分配）
- **超时时间**：`app.game.afk.seat-select-timeout-ms=60000`（测试时延长）

## 性能测试

如果需要测试更高并发，可以修改测试类中的`TOTAL_PLAYERS`常量：

```java
private static final int TOTAL_PLAYERS = 20; // 改为20个玩家
```

## 日志级别

测试会输出详细的日志，包括：
- `INFO`：正常流程信息
- `WARN`：警告信息（如错误消息）
- `ERROR`：错误信息
- `DEBUG`：调试信息（房间状态更新等）

## 参考

- [Spring WebSocket文档](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#websocket)
- [STOMP协议文档](https://stomp.github.io/)
- [JUnit 5文档](https://junit.org/junit5/docs/current/user-guide/)


