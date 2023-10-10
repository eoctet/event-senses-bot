CREATE DATABASE IF NOT EXISTS event_senses_bot;

DROP TABLE IF EXISTS event_senses_bot.event_metrics;
CREATE TABLE IF NOT EXISTS event_senses_bot.event_metrics (
	`id` String comment '记录主键',
	`event_log` String comment '日志编号',
	`event_name` String comment '事件名称',
	`event_code` String comment '事件代码',
	`channel_name` String comment '事件渠道名称',
	`channel_code` String comment '事件渠道代码',
	`event_publish_time` DateTime64(3, 'Asia/Shanghai') comment '事件发布时间',
	`channel_receive_time` DateTime64(3, 'Asia/Shanghai') comment '事件渠道消息接收时间',
	`channel_message` String comment '事件渠道消息内容',
	`channel_message_count` Int32 comment '事件渠道消息数量',
	`channel_process_status` String comment '事件渠道消息处理状态',
	`event_execute_start` DateTime64(3, 'Asia/Shanghai') comment '事件处理开始时间',
	`event_execute_end` DateTime64(3, 'Asia/Shanghai') comment '事件处理结束时间',
	`event_summary_log` String comment '格式化的日志摘要',
	`event_stack_log` String comment '详细错误堆栈日志',
	`event_execute_status` Int8 comment '事件执行状态，0正常，1发生异常终止，2部分异常结束，-1未知',
	`event_pattern_matched` UInt8 comment '模式匹配累计',
	`action_error_count` UInt8 comment '错误动作累计',
	`action_success_count` UInt8 comment '成功动作累计',
	`event_retry_count` UInt8 comment '重试次数累计',
	`total_process_time` Int32 comment '事件处理总耗时',
	`channel_process_time` Int32 comment '渠道消息处理耗时',
	`event_process_time` Int32 comment '事件计算耗时',
	`createtime` DateTime64(3, 'Asia/Shanghai') comment '创建时间'
)
ENGINE=MergeTree()
ORDER BY id
PRIMARY KEY id
PARTITION BY toYYYYMMDD(createtime);
