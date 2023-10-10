/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.core.data;

import com.clickhouse.client.config.ClickHouseClientOption;
import com.clickhouse.client.config.ClickHouseDefaults;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.util.concurrent.ExecutorThreadFactory;
import pro.octet.senses.bot.core.ApplicationConfig;
import pro.octet.senses.bot.core.entity.ResultSet;
import pro.octet.senses.bot.core.entity.Session;
import pro.octet.senses.bot.core.enums.ConfigOption;
import pro.octet.senses.bot.core.enums.Constant;
import pro.octet.senses.bot.exception.ServerException;
import pro.octet.senses.bot.utils.CommonUtils;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ClickHouse Sink
 *
 * @author William
 * @since 22.0816.2.6
 */
@Slf4j
public class ClickHouseSink extends RichSinkFunction<Session> implements AutoCloseable {

    private int cacheMaxQueue;
    private int cacheIntervalMillis;
    private AtomicLong numWritten;
    private final AtomicReference<Throwable> failureThrowable = new AtomicReference<>();
    private BlockingQueue<ResultSet> cacheQueue;
    private transient ScheduledThreadPoolExecutor executor;
    private transient ScheduledFuture<?> scheduledFuture;
    private volatile transient boolean closed = false;
    private transient ClickHouseManager clickHouseManager;
    private final ApplicationConfig appConfig;
    private String tableName;
    private String taskName;

    public ClickHouseSink(ApplicationConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    public void open(Configuration parameters) {
        // initialize variables
        this.cacheMaxQueue = appConfig.getInt(ConfigOption.SINK_CACHE_MAX_QUEUE);
        this.cacheIntervalMillis = appConfig.getInt(ConfigOption.SINK_CACHE_INTERVAL_MILLIS);
        this.cacheQueue = new LinkedBlockingQueue<>();
        this.numWritten = new AtomicLong(0);
        this.tableName = appConfig.getString(ConfigOption.DB_CH_TABLE);
        this.taskName = CommonUtils.getTaskNameWithIndex(appConfig.getString(ConfigOption.SINK_NAME), this.getRuntimeContext());

        try {
            // init sink data backup directories
            Path path = Paths.get(appConfig.getString(ConfigOption.SINK_BACKUP));
            Files.createDirectories(path);
            // init clickhouse properties
            Properties props = new Properties();
            props.setProperty(ClickHouseClientOption.CLIENT_NAME.getKey(), Constant.JDBC_CLIENT_NAME);
            props.setProperty(ClickHouseClientOption.MAX_THREADS_PER_CLIENT.getKey(), appConfig.getString(ConfigOption.DB_CH_MAX_THREADS_PER_CLIENT));
            props.setProperty(ClickHouseDefaults.USER.getKey(), appConfig.getString(ConfigOption.DB_CH_USERNAME));
            props.setProperty(ClickHouseDefaults.PASSWORD.getKey(), appConfig.getString(ConfigOption.DB_CH_PASSWORD));
            // init clickhouse connection and statement
            this.clickHouseManager = new ClickHouseManager(props, appConfig.getString(ConfigOption.DB_CH_JDBC), taskName);
            initScheduledWorker();
        } catch (Exception e) {
            throw new ServerException(CommonUtils.format("{0} -> Open ClickHouse sink error", taskName), e);
        }
        log.info("{} -> Open ClickHouse sink success.", taskName);
    }

    private void initScheduledWorker() {
        if (cacheIntervalMillis > 0 && cacheMaxQueue != 1) {
            String poolName = StringUtils.join("clickhouse_sink_flusher_", RandomUtils.nextInt(0, 9));
            log.info("{} -> Create sink worker thread pool, name: {}, interval millis: {} ms, flush max size: {}.", taskName, poolName, cacheIntervalMillis, cacheMaxQueue);
            this.executor = new ScheduledThreadPoolExecutor(1, new ExecutorThreadFactory(poolName));
            this.scheduledFuture = this.executor.scheduleWithFixedDelay(() -> {
                if (closed) {
                    return;
                }
                try {
                    if (numWritten.get() > 0) {
                        log.info("{} -> Time is up, write data to ClickHouse. written num: {}.", taskName, numWritten.get());
                        flush();
                    }
                } catch (Exception e) {
                    failureThrowable.compareAndSet(null, e);
                }
            }, cacheIntervalMillis, cacheIntervalMillis, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void invoke(Session session, Context context) {
        try {
            checkErrorAndRethrow();
            // Put data to the data queue
            if (session.get(tableName) != null) {
                ResultSet resultSet = (ResultSet) session.get(tableName);
                boolean state = cacheQueue.offer(resultSet, 3, TimeUnit.MILLISECONDS);
                log.debug("{} -> Add result data to cache queue: {}.", taskName, state);
            }
            // If the upper limit of the queue is reached then flush the data to ClickHouse
            if (cacheMaxQueue > 0 && numWritten.incrementAndGet() >= cacheMaxQueue) {
                log.info("{} -> Cache queue is full, write data to ClickHouse. written num: {}", taskName, numWritten.get());
                flush();
            }
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * Check error and rethrow
     */
    private void checkErrorAndRethrow() {
        Throwable cause = failureThrowable.get();
        if (cause != null) {
            throw new ServerException(CommonUtils.format("{0} -> An error occurred in ClickHouse.", taskName), cause);
        }
    }

    /**
     * Batch execute write table operation
     */
    private void batchExecute() {
        synchronized (this) {
            if (cacheQueue.size() == 0) {
                return;
            }
            // drain to write list
            List<ResultSet> writeList = Lists.newArrayList();
            int rowCount = cacheQueue.drainTo(writeList, this.cacheMaxQueue);
            int maxErrorRetry = appConfig.getInt(ConfigOption.DB_CH_ERROR_RETRY);
            int retry = 0;
            boolean status = false;

            do {
                StringBuffer dataString = new StringBuffer();
                try {
                    // parse the data into a JSON string and append string
                    writeList.forEach(e -> dataString.append(CommonUtils.toJson(e)));
                    // check clickhouse status
                    clickHouseManager.checkClickHouseStatus();
                    // write data to table
                    status = clickHouseManager.write(tableName, dataString);
                    log.info("{} -> Write ClickHouse is done, status: {}, record row count: {}.", taskName, status, rowCount);
                    break;
                } catch (Exception e) {
                    retry++;
                    log.error("{} -> Write ClickHouse error, retry times: {}, record row count: {}.", taskName, retry, rowCount, e);
                    try {
                        Thread.sleep(1000L * retry);
                    } catch (InterruptedException ex) {
                        log.warn("Interrupted while doing another attempt.", ex);
                    }
                }
            } while (retry < maxErrorRetry);
            if (!status) {
                backupDataToFile(writeList);
            }
            numWritten.addAndGet(-rowCount);
        }
    }

    /**
     * Backup failed data to target file, the file format is JSON
     *
     * @param writeList write list
     * @see ConfigOption
     */
    private void backupDataToFile(List<ResultSet> writeList) {
        String filePath = CommonUtils.format("%s/%s", appConfig.getString(ConfigOption.SINK_BACKUP), System.currentTimeMillis());

        try (PrintWriter writer = new PrintWriter(filePath)) {
            writer.println(CommonUtils.toJson(writeList));
            writer.flush();
        } catch (Exception e) {
            log.error("{} -> Backup data error", taskName, e);
        }
        log.warn("{} -> Backup data on disk, path: {}, record row count: {}.", taskName, filePath, writeList.size());
    }

    /**
     * Flush data to ClickHouse
     */
    private void flush() {
        batchExecute();
        checkErrorAndRethrow();
    }

    @Override
    public void close() {
        closed = true;
        flush();
        if (clickHouseManager != null) {
            clickHouseManager.close();
        }
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            if (executor != null) {
                executor.shutdownNow();
                log.debug("{} -> Closed sink worker thread pool.", taskName);
            }
        }
    }

}
