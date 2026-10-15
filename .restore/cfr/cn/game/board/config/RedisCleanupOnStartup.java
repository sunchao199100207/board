/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cn.game.board.config.RedisCleanupOnStartup
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.boot.CommandLineRunner
 *  org.springframework.data.redis.RedisConnectionFailureException
 *  org.springframework.data.redis.connection.RedisConnection
 *  org.springframework.data.redis.core.RedisTemplate
 *  org.springframework.data.redis.core.StringRedisTemplate
 *  org.springframework.stereotype.Component
 */
package cn.game.board.config;

import java.util.Collection;
import java.util.Set;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisCleanupOnStartup
implements CommandLineRunner {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(RedisCleanupOnStartup.class);
    private final RedisTemplate<String, ?> gameRoomRedisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    @Value(value="${app.redis.cleanup-on-startup:true}")
    private boolean cleanupOnStartup;

    public void run(String ... args) {
        if (!this.cleanupOnStartup) {
            log.info("Redis \u542f\u52a8\u6e05\u7406\u529f\u80fd\u5df2\u7981\u7528");
            return;
        }
        log.info("\u5f00\u59cb\u6e05\u7406 Redis \u4e2d\u7684\u6e38\u620f\u6570\u636e");
        try (RedisConnection connection = this.stringRedisTemplate.getConnectionFactory().getConnection();){
            connection.ping();
        }
        catch (Exception e) {
            log.warn("Redis \u8fde\u63a5\u4e0d\u53ef\u7528\uff0c\u8df3\u8fc7\u6570\u636e\u6e05\u7406\u3002\u8bf7\u786e\u4fdd Redis \u670d\u52a1\u5df2\u542f\u52a8\u5e76\u914d\u7f6e\u6b63\u786e\u3002\u9519\u8bef: {}", (Object)e.getMessage());
            log.debug("Redis \u8fde\u63a5\u5f02\u5e38\u8be6\u60c5", (Throwable)e);
            return;
        }
        try {
            Set gameKeys;
            Set playerKeys;
            Set sessionKeys;
            Set roomKeys = this.gameRoomRedisTemplate.keys((Object)"avalon:room:*");
            if (roomKeys != null && !roomKeys.isEmpty()) {
                this.gameRoomRedisTemplate.delete((Collection)roomKeys);
                log.info("\u5df2\u6e05\u9664 {} \u4e2a\u623f\u95f4\u6570\u636e", (Object)roomKeys.size());
            }
            if ((sessionKeys = this.stringRedisTemplate.keys((Object)"avalon:session:*")) != null && !sessionKeys.isEmpty()) {
                this.stringRedisTemplate.delete((Collection)sessionKeys);
                log.info("\u5df2\u6e05\u9664 {} \u4e2a Session \u6620\u5c04", (Object)sessionKeys.size());
            }
            if ((playerKeys = this.stringRedisTemplate.keys((Object)"avalon:player:*")) != null && !playerKeys.isEmpty()) {
                this.stringRedisTemplate.delete((Collection)playerKeys);
                log.info("\u5df2\u6e05\u9664 {} \u4e2a Player \u6620\u5c04", (Object)playerKeys.size());
            }
            if ((gameKeys = this.stringRedisTemplate.keys((Object)"avalon:game:*")) != null && !gameKeys.isEmpty()) {
                this.stringRedisTemplate.delete((Collection)gameKeys);
                log.info("\u5df2\u6e05\u9664 {} \u4e2a\u6e38\u620f\u76f8\u5173\u952e", (Object)gameKeys.size());
            }
            log.info("Redis \u6570\u636e\u6e05\u7406\u5b8c\u6210");
        }
        catch (RedisConnectionFailureException e) {
            log.warn("Redis \u8fde\u63a5\u5931\u8d25\uff0c\u8df3\u8fc7\u6570\u636e\u6e05\u7406\u3002\u8bf7\u786e\u4fdd Redis \u670d\u52a1\u5df2\u542f\u52a8\u5e76\u914d\u7f6e\u6b63\u786e\u3002\u9519\u8bef: {}", (Object)e.getMessage());
            log.debug("Redis \u8fde\u63a5\u5f02\u5e38\u8be6\u60c5", (Throwable)e);
        }
        catch (Exception e) {
            log.warn("\u6e05\u7406 Redis \u6570\u636e\u65f6\u53d1\u751f\u9519\u8bef\uff0c\u4f46\u4e0d\u5f71\u54cd\u5e94\u7528\u542f\u52a8: {}", (Object)e.getMessage());
            log.debug("\u6e05\u7406\u5f02\u5e38\u8be6\u60c5", (Throwable)e);
        }
    }

    @Generated
    public RedisCleanupOnStartup(RedisTemplate<String, ?> gameRoomRedisTemplate, StringRedisTemplate stringRedisTemplate) {
        this.gameRoomRedisTemplate = gameRoomRedisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }
}

