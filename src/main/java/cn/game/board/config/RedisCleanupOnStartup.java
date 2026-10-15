package cn.game.board.config;

import cn.game.board.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 应用启动时清除 Redis 中的游戏数据
 * 可通过配置 app.redis.cleanup-on-startup=true/false 控制是否启用
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisCleanupOnStartup implements CommandLineRunner {

    private final RedisTemplate<String, ?> gameRoomRedisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.redis.cleanup-on-startup:true}")
    private boolean cleanupOnStartup;

    @Override
    public void run(String... args) {
        if (!cleanupOnStartup) {
            log.info("Redis 启动清理功能已禁用");
            return;
        }

        log.info("开始清理 Redis 中的游戏数据");

        try {
            // 先测试 Redis 连接是否可用
            var connection = stringRedisTemplate.getConnectionFactory().getConnection();
            try {
                connection.ping();
            } finally {
                if (connection != null) {
                    connection.close();
                }
            }
        } catch (Exception e) {
            log.warn("Redis 连接不可用，跳过数据清理。请确保 Redis 服务已启动并配置正确。错误: {}", e.getMessage());
            log.debug("Redis 连接异常详情", e);
            return;
        }

        try {
            Set<String> roomKeys = gameRoomRedisTemplate.keys(RoomService.ROOM_KEY_PREFIX + "*");
            if (roomKeys != null && !roomKeys.isEmpty()) {
                gameRoomRedisTemplate.delete(roomKeys);
                log.info("已清除 {} 个房间数据", roomKeys.size());
            }

            Set<String> sessionKeys = stringRedisTemplate.keys("avalon:session:*");
            if (sessionKeys != null && !sessionKeys.isEmpty()) {
                stringRedisTemplate.delete(sessionKeys);
                log.info("已清除 {} 个 Session 映射", sessionKeys.size());
            }

            Set<String> playerKeys = stringRedisTemplate.keys("avalon:player:*");
            if (playerKeys != null && !playerKeys.isEmpty()) {
                stringRedisTemplate.delete(playerKeys);
                log.info("已清除 {} 个 Player 映射", playerKeys.size());
            }

            Set<String> gameKeys = stringRedisTemplate.keys("avalon:game:*");
            if (gameKeys != null && !gameKeys.isEmpty()) {
                stringRedisTemplate.delete(gameKeys);
                log.info("已清除 {} 个游戏相关键", gameKeys.size());
            }

            log.info("Redis 数据清理完成");
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 连接失败，跳过数据清理。请确保 Redis 服务已启动并配置正确。错误: {}", e.getMessage());
            log.debug("Redis 连接异常详情", e);
        } catch (Exception e) {
            log.warn("清理 Redis 数据时发生错误，但不影响应用启动: {}", e.getMessage());
            log.debug("清理异常详情", e);
        }
    }
}
