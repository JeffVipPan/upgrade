package com.eis.upgrade.config;

import com.eis.core.redis.RedisService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedisPool;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RedisConfig {

    @Value("${redis.uri.0}")
    private String url;
    @Value("${redis.maxTotal}")
    private int maxTotal;
    @Value("${redis.maxIdle}")
    private int maxIdle;
    @Value("${redis.maxWaitMillis}")
    private long maxWaitMillis;
    @Value("${redis.testOnBorrow}")
    private boolean testOnBorrow;
    @Value("${redis.testOnReturn}")
    private boolean testOnReturn;


    @Bean
    public ShardedJedisPool shardedJedisPool() {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(maxTotal);
        jedisPoolConfig.setMaxIdle(maxIdle);
        jedisPoolConfig.setMaxWaitMillis(maxWaitMillis);
        jedisPoolConfig.setTestOnBorrow(testOnBorrow);
        jedisPoolConfig.setTestOnReturn(testOnReturn);
        List<JedisShardInfo> jedisShardInfos = new ArrayList<>();
        jedisShardInfos.add(new JedisShardInfo(url));

        return new ShardedJedisPool(jedisPoolConfig, jedisShardInfos);
    }

    @Bean
    public ShardedJedisPool getShardedJedisPool() {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        List<JedisShardInfo> jedisShardInfos = new ArrayList<>();
        ShardedJedisPool shardedJedisPool = new ShardedJedisPool(jedisPoolConfig, jedisShardInfos);
        return shardedJedisPool;
    }

}


