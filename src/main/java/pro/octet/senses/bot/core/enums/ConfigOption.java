/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.enums;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import pro.octet.senses.bot.core.ApplicationConfig;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * Application global config options
 *
 * @author William
 * @see ApplicationConfig
 * @since 22.0816.2.6
 */
public enum ConfigOption {

    /**
     * APP CONFIG
     */
    APP_LOGGER("app.logger", "INFO"),
    APP_LANG("app.lang", "zh-CN"),
    APP_PUBLISH_TIME("app.publish-time", -1L),
    APP_YAML("app.yaml", StringUtils.EMPTY),
    /**
     * SOURCE CONFIG
     */
    SOURCE_KAFKA_BOOTSTRAP("app.source.kafka-bootstrap", "localhost:9093"),
    /**
     * JOB CONFIG
     */
    JOB_DELAY_BETWEEN_ATTEMPTS("app.job.delay-between-attempts", 5 * 1000),
    JOB_CHECKPOINT_MIN_PAUSE_BETWEEN("app.job.checkpoint.min-pause-between", 500),
    JOB_PARALLELISM_DEFAULT("app.job.parallelism.default", 2),
    JOB_PARALLELISM_MAX("app.job.parallelism.max", 100),
    JOB_CHECKPOINT_INTERVAL("app.job.checkpoint.interval", 6 * 1000),
    JOB_CHECKPOINT_MAX_CONCURRENT("app.job.checkpoint.max-concurrent", 1),
    JOB_CHECKPOINT_TIMEOUT("app.job.checkpoint.timeout", 60 * 1000),
    JOB_CHECKPOINT_STORAGE("app.job.checkpoint.storage", "/tmp/event/checkpoint"),
    JOB_FILTER_MAX_CACHE_EXPIRE("app.job.filter.max-cache-expire", 3600),
    JOB_FILTER_ENABLED("app.job.filter.enabled", true),
    JOB_FILTER_NAME("app.job.filter-name", "Message-Filter-Task"),
    JOB_EVENT_NAME("app.job.event-name", "Event-Calc-Task"),
    JOB_PATTERN_MODE("app.job.pattern-mode", false),
    JOB_IDLE_TIMEOUT("app.job.idle-timeout", 5000),
    /**
     * SINK CONFIG
     */
    SINK_NAME("app.sink.name", "Data-Storage-Task"),
    SINK_PARALLELISM("app.sink.parallelism", 1),
    SINK_BACKUP("app.sink.backup", "/tmp/event/backup"),
    SINK_CACHE_MAX_QUEUE("app.sink.cache.max-queue", 5 * 1000),
    SINK_CACHE_INTERVAL_MILLIS("app.sink.cache.interval-millis", 60 * 1000),
    /**
     * DB CONFIG
     */
    DB_REDIS_DATABASE("app.db.redis.database", 0),
    DB_REDIS_PORT("app.db.redis.port", 6379),
    DB_REDIS_HOST("app.db.redis.host", "localhost"),
    DB_REDIS_USER("app.db.redis.username", StringUtils.EMPTY),
    DB_REDIS_PWD("app.db.redis.password", StringUtils.EMPTY),
    DB_REDIS_POOL_MAX_IDLE("app.db.redis.pool.max-idle", 0),
    DB_REDIS_POOL_MIN_IDLE("app.db.redis.pool.min-idle", 0),
    DB_REDIS_POOL_MAX_TOTAL("app.db.redis.pool.max-total", 8),
    DB_REDIS_POOL_MAX_WAIT_MILLIS("app.db.redis.pool.max-wait-millis", 5 * 1000),
    DB_REDIS_CLUSTER_ENABLED("app.db.redis.cluster-enabled", false),
    DB_REDIS_CLUSTER_NODES("app.db.redis.cluster-nodes", StringUtils.EMPTY),
    DB_CH_JDBC("app.db.clickhouse.jdbc", "jdbc:clickhouse://localhost:8123/event_senses_bot"),
    DB_CH_USERNAME("app.db.clickhouse.username", StringUtils.EMPTY),
    DB_CH_PASSWORD("app.db.clickhouse.password", StringUtils.EMPTY),
    DB_CH_TABLE("app.db.clickhouse.table", "event_metrics"),
    DB_CH_ERROR_RETRY("app.db.clickhouse.retry", 3),
    DB_CH_MAX_THREADS_PER_CLIENT("app.db.clickhouse.max-threads", "10");

    private static final Map<String, ConfigOption> OPTIONS;

    static {
        Map<String, ConfigOption> map = Maps.newHashMap();

        for (ConfigOption o : values()) {
            if (map.put(o.getKey(), o) != null) {
                throw new IllegalStateException("Duplicated key found: " + o.getKey());
            }
        }
        OPTIONS = Collections.unmodifiableMap(map);
    }

    private final String key;
    private final Serializable defaultValue;
    private final Class<? extends Serializable> clazz;

    public String getKey() {
        return key;
    }

    public Serializable getDefaultValue() {
        return defaultValue;
    }

    public static ConfigOption valueOfKey(String key) {
        return OPTIONS.get(key);
    }

    public Class<? extends Serializable> getValueType() {
        return clazz;
    }

    <T extends Serializable> ConfigOption(String key, T defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.clazz = defaultValue.getClass();
    }

}
