/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.data;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import pro.octet.senses.bot.core.ApplicationConfig;
import pro.octet.senses.bot.core.enums.CacheKey;
import pro.octet.senses.bot.core.enums.ConfigOption;
import pro.octet.senses.bot.core.enums.Constant;
import pro.octet.senses.bot.core.handler.KryoRedisSerializer;
import pro.octet.senses.bot.exception.ServerException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.commands.JedisCommands;
import redis.clients.jedis.params.SetParams;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Cache manager
 *
 * @author William
 * @since 22.0816.2.6
 */
@Slf4j
public final class CacheManager {

    private static final String LUA_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
    private static final Joiner JOINER = Joiner.on(Constant.SEPARATOR).skipNulls();
    private static RedisTemplate<String, Object> redisTemplate;
    private static volatile CacheManager manager;

    private CacheManager() {
    }

    /**
     * Get an instance of redis
     *
     * @return CacheManager
     */
    public static CacheManager getInstance() {
        if (manager == null) {
            synchronized (CacheManager.class) {
                if (manager == null) {
                    manager = new CacheManager();
                }
            }
        }
        return manager;
    }

    public RedisTemplate<String, Object> getRedisTemplate() {
        if (redisTemplate == null) {
            throw new ServerException("Please init RedisTemplate.");
        }
        return redisTemplate;
    }

    public void build(ApplicationConfig appConfig) {
        Preconditions.checkNotNull(appConfig, "appConfig cannot be null.");
        try {
            if (redisTemplate != null) {
                return;
            }
            synchronized (this) {
                redisTemplate = new RedisTemplate<>();
                JedisConnectionFactory factory;
                if (appConfig.getBoolean(ConfigOption.DB_REDIS_CLUSTER_ENABLED)) {
                    factory = initClusterConnFactory(appConfig);
                } else {
                    factory = initStandaloneConnFactory(appConfig);
                }
                redisTemplate.setConnectionFactory(factory);
                // key & value serializer
                redisTemplate.setKeySerializer(new StringRedisSerializer());
                redisTemplate.setValueSerializer(new KryoRedisSerializer<>(Object.class));
                // hash serializer
                redisTemplate.setHashKeySerializer(new StringRedisSerializer());
                redisTemplate.setHashValueSerializer(new KryoRedisSerializer<>(Object.class));
                //
                redisTemplate.afterPropertiesSet();
            }
        } catch (Exception e) {
            throw new ServerException("Init Redis manager error", e);
        }
        log.info("Init Redis manager success.");
    }

    private void setConnPoolConfig(JedisConnectionFactory factory, ApplicationConfig appConfig) {
        factory.getPoolConfig().setMaxIdle(appConfig.getInt(ConfigOption.DB_REDIS_POOL_MAX_IDLE));
        factory.getPoolConfig().setMaxTotal(appConfig.getInt(ConfigOption.DB_REDIS_POOL_MAX_TOTAL));
        factory.getPoolConfig().setMinIdle(appConfig.getInt(ConfigOption.DB_REDIS_POOL_MIN_IDLE));
        factory.getPoolConfig().setMaxWait(Duration.ofMillis(appConfig.getInt(ConfigOption.DB_REDIS_POOL_MAX_WAIT_MILLIS)));
        factory.afterPropertiesSet();
    }

    private JedisConnectionFactory initStandaloneConnFactory(ApplicationConfig appConfig) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(appConfig.getString(ConfigOption.DB_REDIS_HOST));
        config.setPort(appConfig.getInt(ConfigOption.DB_REDIS_PORT));
        config.setUsername(appConfig.getString(ConfigOption.DB_REDIS_USER));
        config.setPassword(appConfig.getString(ConfigOption.DB_REDIS_PWD));
        config.setDatabase(appConfig.getInt(ConfigOption.DB_REDIS_DATABASE));
        JedisConnectionFactory factory = new JedisConnectionFactory(config);
        setConnPoolConfig(factory, appConfig);
        return factory;
    }

    private JedisConnectionFactory initClusterConnFactory(ApplicationConfig appConfig) {
        RedisClusterConfiguration config = new RedisClusterConfiguration();
        config.setUsername(appConfig.getString(ConfigOption.DB_REDIS_USER));
        config.setPassword(appConfig.getString(ConfigOption.DB_REDIS_PWD));
        String nodesConfig = appConfig.getString(ConfigOption.DB_REDIS_CLUSTER_NODES);
        if (StringUtils.isBlank(nodesConfig)) {
            throw new ServerException("Redis cluster nodes is empty");
        }
        List<String> nodesList = Arrays.asList(StringUtils.split(nodesConfig, ","));
        List<RedisNode> nodes = Lists.newArrayList();
        nodesList.forEach(e -> {
            String[] node = StringUtils.split(StringUtils.trim(e), ":");
            nodes.add(new RedisNode(node[0], Integer.parseInt(String.valueOf(node[1]))));
        });
        config.setClusterNodes(nodes);
        JedisConnectionFactory factory = new JedisConnectionFactory(config);
        setConnPoolConfig(factory, appConfig);
        return factory;
    }

    public boolean setLock(String name, String id, long timeout) {
        boolean flag = false;
        String key = JOINER.join(CacheKey.LOCK, name);
        try {
            RedisCallback<String> callback = (connection) -> {
                JedisCommands commands = (JedisCommands) connection.getNativeConnection();
                return commands.set(key, id, SetParams.setParams().nx().ex(timeout));
            };
            String result = getRedisTemplate().execute(callback);
            flag = "OK".equalsIgnoreCase(Optional.ofNullable(result).orElse(StringUtils.EMPTY));
        } catch (Exception e) {
            log.error("Setting Redis lock error", e);
        }
        log.debug("Setting Redis lock, Key {} State {}", key, flag);
        return flag;
    }

    public boolean setLock(String name, String id) {
        // Release the lock in 5 minutes
        return setLock(name, id, 300);
    }

    public boolean releaseLock(String name, String id) {
        boolean flag = false;
        String key = JOINER.join(CacheKey.LOCK, name);
        try {
            RedisCallback<Long> callback = (connection) -> {
                Object nativeConnection = connection.getNativeConnection();
                // for Redis Cluster
                if (nativeConnection instanceof JedisCluster) {
                    return (Long) ((JedisCluster) nativeConnection).eval(LUA_SCRIPT, 1, key, id);
                }
                // for Single Redis
                if (nativeConnection instanceof Jedis) {
                    return (Long) ((Jedis) nativeConnection).eval(LUA_SCRIPT, 1, key, id);
                }
                return 0L;
            };
            flag = Optional.ofNullable(getRedisTemplate().execute(callback)).orElse(0L) > 0;
        } catch (Exception e) {
            log.error("Releasing Redis lock error", e);
        }
        log.debug("Releasing Redis lock, Key {} State {}", key, flag);
        return flag;
    }

}
