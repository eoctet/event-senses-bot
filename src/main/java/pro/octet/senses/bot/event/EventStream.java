/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.event;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.cep.CEP;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.contrib.streaming.state.EmbeddedRocksDBStateBackend;
import org.apache.flink.runtime.state.storage.FileSystemCheckpointStorage;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import pro.octet.senses.bot.channel.MessageDeserializationSchema;
import pro.octet.senses.bot.channel.MessageFilterFunction;
import pro.octet.senses.bot.core.ApplicationConfig;
import pro.octet.senses.bot.core.ExecutionContext;
import pro.octet.senses.bot.core.cep.PatternGenerator;
import pro.octet.senses.bot.core.data.ClickHouseSink;
import pro.octet.senses.bot.core.entity.ChannelMessage;
import pro.octet.senses.bot.core.enums.ConfigOption;
import pro.octet.senses.bot.core.enums.Constant;
import pro.octet.senses.bot.core.eventtime.EventWatermarkStrategy;
import pro.octet.senses.bot.utils.CommonUtils;

import java.time.Duration;

@Slf4j
public class EventStream {

    private StreamExecutionEnvironment env;
    private final ApplicationConfig appConfig;

    public EventStream(ApplicationConfig appConfig) {
        this.appConfig = appConfig;
    }

    protected void initExecutionEnvironment() {
        env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(Integer.MAX_VALUE, appConfig.getLong(ConfigOption.JOB_DELAY_BETWEEN_ATTEMPTS)));
        env.setParallelism(appConfig.getInt(ConfigOption.JOB_PARALLELISM_DEFAULT));
        // checkpoint config
        env.enableCheckpointing(appConfig.getLong(ConfigOption.JOB_CHECKPOINT_INTERVAL), CheckpointingMode.EXACTLY_ONCE);
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(appConfig.getLong(ConfigOption.JOB_CHECKPOINT_MIN_PAUSE_BETWEEN));
        env.getCheckpointConfig().setMaxConcurrentCheckpoints(appConfig.getInt(ConfigOption.JOB_CHECKPOINT_MAX_CONCURRENT));
        env.getCheckpointConfig().setCheckpointTimeout(appConfig.getLong(ConfigOption.JOB_CHECKPOINT_TIMEOUT));
        env.setStateBackend(new EmbeddedRocksDBStateBackend());
        env.getCheckpointConfig().setCheckpointStorage(new FileSystemCheckpointStorage(StringUtils.join("file://", appConfig.getString(ConfigOption.JOB_CHECKPOINT_STORAGE))));
        //TODO Only for 1.14.4: env.getCheckpointConfig().setExternalizedCheckpointCleanup(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
        //hard limited
        env.setMaxParallelism(appConfig.getInt(ConfigOption.JOB_PARALLELISM_MAX));
        //
        env.registerCachedFile(appConfig.getString(ConfigOption.APP_YAML), Constant.YAML_FILE);
        env.getConfig().setAutoWatermarkInterval(100);
    }

    protected KafkaSource<ChannelMessage> initKafkaSource(ExecutionContext context) {
        KafkaSource<ChannelMessage> kafkaSource = KafkaSource.<ChannelMessage>builder()
                .setBootstrapServers(appConfig.getString(ConfigOption.SOURCE_KAFKA_BOOTSTRAP))
                .setTopics(context.getChannel().getTopic())
                .setGroupId(context.getChannel().getGroupId())
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new MessageDeserializationSchema(context.getChannel()))
                .setProperty("partition.discovery.interval.ms", "10000")
                .setProperty("enable.auto.commit", "true")
                .build();

        log.info("Create kafka source success, Channel code: {}, Topic: {}, Group id: {}", context.getChannel().getCode(), context.getChannel().getTopic(), context.getChannel().getGroupId());
        return kafkaSource;
    }

    public void execute() throws Exception {
        initExecutionEnvironment();
        ExecutionContext context = new ExecutionContext(appConfig);
        KafkaSource<ChannelMessage> kafkaSource = initKafkaSource(context);
        DataStreamSource<ChannelMessage> stream = env.fromSource(kafkaSource,
                new EventWatermarkStrategy().withIdleness(Duration.ofMillis(appConfig.getLong(ConfigOption.JOB_IDLE_TIMEOUT))),
                context.getChannel().getCode());

        //execute message filter
        SingleOutputStreamOperator<ChannelMessage> streamOperator = stream.filter(new MessageFilterFunction(appConfig)).name(appConfig.getString(ConfigOption.JOB_FILTER_NAME));

        ClickHouseSink clickHouseSink = new ClickHouseSink(appConfig);
        if (context.isPatternMode()) {
            //generate event pattern
            Pattern<ChannelMessage, ChannelMessage> pattern = PatternGenerator.generate(context.getEvent().getPatternAction());
            //execute event pattern
            CEP.pattern(streamOperator, pattern).inEventTime()
                    .process(new EventPatternProcessFunction()).name("Event-Pattern-Task")
                    .map(new ComplexEventProcessFunction(context))
                    .name(appConfig.getString(ConfigOption.JOB_EVENT_NAME))
                    .addSink(clickHouseSink)
                    .setParallelism(appConfig.getInt(ConfigOption.SINK_PARALLELISM))
                    .name(appConfig.getString(ConfigOption.SINK_NAME));

        } else {
            streamOperator.map(new SimpleEventProcessFunction(context))
                    .name(appConfig.getString(ConfigOption.JOB_EVENT_NAME))
                    .addSink(clickHouseSink)
                    .setParallelism(appConfig.getInt(ConfigOption.SINK_PARALLELISM))
                    .name(appConfig.getString(ConfigOption.SINK_NAME));
        }

        log.info("Create data stream source success, Stream execution config:\n{}", CommonUtils.toJson(stream.getExecutionConfig()));
        log.info("Now start running the job, Job name: {}.", context.getEvent().getName());
        env.execute(context.getEvent().getName());
    }

}