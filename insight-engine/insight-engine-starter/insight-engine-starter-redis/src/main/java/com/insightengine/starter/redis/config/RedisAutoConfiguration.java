package com.insightengine.starter.redis.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * starter-redis 自动配置类。
 *
 * <p>为业务服务统一装配「可读、可序列化」的 {@link RedisTemplate}（TD §6.1）。</p>
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>key 用 {@link StringRedisSerializer}，保证 key 可读且带统一前缀（{@code ie:...}）；</li>
 *   <li>value 用 {@link GenericJackson2JsonRedisSerializer}，序列化为 JSON 而非
 *       JDK 默认的二进制序列化（二进制既不可读，还要求实体实现 Serializable 且不可跨语言）；</li>
 *   <li>为 JSON 反序列化保留类型信息（{@code @class} 字段），
 *       否则反序列化只会得到 {@code LinkedHashMap}，强转会抛 ClassCastException。</li>
 * </ul>
 *
 * <p>说明：本 starter 只提供序列化底座；分布式锁、缓存穿透/击穿/雪崩防护（TD §6.2~§6.4）
 * 留待 billing/model 等真正有缓存需求的阶段再实现，避免骨架阶段过度设计。</p>
 */
@Configuration
public class RedisAutoConfiguration {

    /**
     * 通用 RedisTemplate：key 为 String，value 为 JSON。
     *
     * @param connectionFactory Redis 连接工厂（由 spring-boot-starter-data-redis 自动装配）
     */
    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // key / hashKey 统一用字符串序列化，可读且便于按前缀扫描
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // value 用 JSON 序列化；开启默认类型，使反序列化能还原具体 POJO 类型
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
