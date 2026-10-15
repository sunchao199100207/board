/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  cn.game.board.config.RedisConfig
 *  cn.game.board.model.avalon.dto.GameRoom
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.fasterxml.jackson.databind.ObjectMapper$DefaultTyping
 *  com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator
 *  com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 *  org.springframework.data.redis.connection.RedisConnectionFactory
 *  org.springframework.data.redis.core.RedisTemplate
 *  org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
 *  org.springframework.data.redis.serializer.RedisSerializer
 *  org.springframework.data.redis.serializer.StringRedisSerializer
 */
package cn.game.board.config;

import cn.game.board.model.avalon.dto.GameRoom;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, GameRoom> gameRoomRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate template = new RedisTemplate();
        template.setConnectionFactory(connectionFactory);
        ObjectMapper om = new ObjectMapper();
        om.activateDefaultTyping((PolymorphicTypeValidator)LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(GameRoom.class);
        jackson2JsonRedisSerializer.setObjectMapper(om);
        template.setKeySerializer((RedisSerializer)new StringRedisSerializer());
        template.setHashKeySerializer((RedisSerializer)new StringRedisSerializer());
        template.setValueSerializer((RedisSerializer)jackson2JsonRedisSerializer);
        template.setHashValueSerializer((RedisSerializer)jackson2JsonRedisSerializer);
        template.afterPropertiesSet();
        return template;
    }
}

