package cn.game.board.config;

import cn.game.board.model.avalon.dto.GameRoom;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 序列化配置
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, GameRoom> gameRoomRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, GameRoom> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        ObjectMapper om = new ObjectMapper();
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);

        Jackson2JsonRedisSerializer<GameRoom> jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(GameRoom.class);
        jackson2JsonRedisSerializer.setObjectMapper(om);

        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
