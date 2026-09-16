package com.insightengine.starter.redis.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.insightengine.starter.redis.blacklist.RedisTokenBlacklistService;
import com.insightengine.starter.redis.session.RedisTokenSessionService;
import com.insightengine.starter.security.blacklist.TokenBlacklistService;
import com.insightengine.starter.security.session.TokenSessionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * starter-redis 自动配置类。
 *
 * <p>为业务服务统一装配「可读、可序列化」的 {@link RedisTemplate}（TD §6.1），
 * 以及 starter-security 两个可选安全接口的 Redis 实现（登录态校验 / 登出黑名单）。</p>
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>key 用 {@link StringRedisSerializer}，保证 key 可读且带统一前缀（{@code ie:...}）；</li>
 *   <li>value 用 {@link GenericJackson2JsonRedisSerializer}，序列化为 JSON 而非
 *       JDK 默认的二进制序列化（二进制既不可读，还要求实体实现 Serializable 且不可跨语言）；</li>
 *   <li>为 JSON 反序列化保留类型信息（{@code @class} 字段），
 *       否则反序列化只会得到 {@code LinkedHashMap}，强转会抛 ClassCastException；</li>
 *   <li>安全实现统一在此装配（{@code @ConditionalOnMissingBean} 允许业务侧覆盖）：
 *       登录态/黑名单是「全局一致」的能力，若各服务各写一份，改名漂移会造成静默失效
 *       （详见 {@link com.insightengine.common.constant.CacheKeyConstants}）。</li>
 * </ul>
 *
 * <p>说明：本 starter 的序列化底座与安全会话实现已就绪；分布式锁、缓存穿透/击穿/雪崩防护
 * （TD §6.2~§6.4）留待 billing/model 等真正有缓存需求的阶段再实现，避免过度设计。</p>
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

    /**
     * 登录态校验服务（Redis 实现）：认证过滤器据此实现「改密/禁用/登出/换签后旧 token 立即失效」。
     *
     * <p>未引入 Redis 的服务不会装配本 Bean，{@code JwtAuthFilter} 退化为纯无状态 JWT 校验。</p>
     */
    @Bean
    @ConditionalOnMissingBean(TokenSessionService.class)
    public TokenSessionService redisTokenSessionService(StringRedisTemplate stringRedisTemplate) {
        return new RedisTokenSessionService(stringRedisTemplate);
    }

    /**
     * 登出黑名单服务（Redis 实现）：登出后 token 立即失效，且在所有引入本 starter 的服务上一致生效。
     */
    @Bean
    @ConditionalOnMissingBean(TokenBlacklistService.class)
    public TokenBlacklistService redisTokenBlacklistService(StringRedisTemplate stringRedisTemplate) {
        return new RedisTokenBlacklistService(stringRedisTemplate);
    }
}
