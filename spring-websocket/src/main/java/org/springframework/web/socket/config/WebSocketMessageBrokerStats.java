/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.config;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.task.TaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;

/**
 * 用于汇总配置中关键基础设施组件内部状态和计数器信息的核心类，这些配置
 * 来自 Java 配置的 {@code @EnableWebSocketMessageBroker} 或 XML 的
 * {@code <websocket:message-broker>}。
 *
 * <p>默认情况下，汇总信息每 30 分钟以 INFO 级别记录一次日志。
 * 可以通过 {@link #setLoggingPeriod(long)} 更改日志记录频率。
 *
 * <p>上述配置会将此类声明为名为 "webSocketMessageBrokerStats" 的 Spring Bean，
 * 并且可以很容易地导出到 JMX，例如通过
 * {@link org.springframework.jmx.export.MBeanExporter MBeanExporter}。
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.1
 */
public class WebSocketMessageBrokerStats {

	private static final Log logger = LogFactory.getLog(WebSocketMessageBrokerStats.class);


	@Nullable
	private SubProtocolWebSocketHandler webSocketHandler;

	@Nullable
	private StompSubProtocolHandler stompSubProtocolHandler;

	@Nullable
	private StompBrokerRelayMessageHandler stompBrokerRelay;

	@Nullable
	private TaskExecutor inboundChannelExecutor;

	@Nullable
	private TaskExecutor outboundChannelExecutor;

	@Nullable
	private TaskScheduler sockJsTaskScheduler;

	@Nullable
	private ScheduledFuture<?> loggingTask;

	private long loggingPeriod = TimeUnit.MINUTES.toMillis(30);


	public void setSubProtocolWebSocketHandler(SubProtocolWebSocketHandler webSocketHandler) {
		this.webSocketHandler = webSocketHandler;
		this.stompSubProtocolHandler = initStompSubProtocolHandler();
	}

	@Nullable
	private StompSubProtocolHandler initStompSubProtocolHandler() {
		if (this.webSocketHandler == null) {
			return null;
		}
		for (SubProtocolHandler handler : this.webSocketHandler.getProtocolHandlers()) {
			if (handler instanceof StompSubProtocolHandler) {
				return (StompSubProtocolHandler) handler;
			}
		}
		SubProtocolHandler defaultHandler = this.webSocketHandler.getDefaultProtocolHandler();
		if (defaultHandler instanceof StompSubProtocolHandler) {
			return (StompSubProtocolHandler) defaultHandler;
		}
		return null;
	}

	public void setStompBrokerRelay(StompBrokerRelayMessageHandler stompBrokerRelay) {
		this.stompBrokerRelay = stompBrokerRelay;
	}

	public void setInboundChannelExecutor(TaskExecutor inboundChannelExecutor) {
		this.inboundChannelExecutor = inboundChannelExecutor;
	}

	public void setOutboundChannelExecutor(TaskExecutor outboundChannelExecutor) {
		this.outboundChannelExecutor = outboundChannelExecutor;
	}

	public void setSockJsTaskScheduler(TaskScheduler sockJsTaskScheduler) {
		this.sockJsTaskScheduler = sockJsTaskScheduler;
		this.loggingTask = initLoggingTask(TimeUnit.MINUTES.toMillis(1));
	}

	@Nullable
	private ScheduledFuture<?> initLoggingTask(long initialDelay) {
		if (this.sockJsTaskScheduler != null && this.loggingPeriod > 0 && logger.isInfoEnabled()) {
			return this.sockJsTaskScheduler.scheduleWithFixedDelay(
					() -> logger.info(WebSocketMessageBrokerStats.this.toString()),
					Instant.now().plusMillis(initialDelay), Duration.ofMillis(this.loggingPeriod));
		}
		return null;
	}

	/**
	 * 设置以 INFO 级别记录信息的频率（毫秒）。
	 * 如果设置为 0 或小于 0，则取消日志记录任务。
	 * <p>默认情况下，此属性设置为 30 分钟（30 * 60 * 1000）。
	 */
	public void setLoggingPeriod(long period) {
		if (this.loggingTask != null) {
			this.loggingTask.cancel(true);
		}
		this.loggingPeriod = period;
		this.loggingTask = initLoggingTask(0);
	}

	/**
	 * 返回配置的日志记录周期频率（毫秒）。
	 */
	public long getLoggingPeriod() {
		return this.loggingPeriod;
	}

	/**
	 * 获取关于 WebSocket 会话的统计信息。
	 */
	public String getWebSocketSessionStatsInfo() {
		return (this.webSocketHandler != null ? this.webSocketHandler.getStatsInfo() : "null");
	}

	/**
	 * 获取关于与 STOMP 相关的 WebSocket 消息处理的统计信息。
	 */
	public String getStompSubProtocolStatsInfo() {
		return (this.stompSubProtocolHandler != null ? this.stompSubProtocolHandler.getStatsInfo() : "null");
	}

	/**
	 * 获取关于 STOMP 代理中继的统计信息（当使用功能完整的 STOMP 代理时）。
	 */
	public String getStompBrokerRelayStatsInfo() {
		return (this.stompBrokerRelay != null ? this.stompBrokerRelay.getStatsInfo() : "null");
	}

	/**
	 * 获取关于处理来自 WebSocket 客户端的传入消息的执行器的统计信息。
	 */
	public String getClientInboundExecutorStatsInfo() {
		return getExecutorStatsInfo(this.inboundChannelExecutor);
	}

	/**
	 * 获取关于处理发往 WebSocket 客户端的传出消息的执行器的统计信息。
	 */
	public String getClientOutboundExecutorStatsInfo() {
		return getExecutorStatsInfo(this.outboundChannelExecutor);
	}

	/**
	 * 获取关于 SockJS 任务调度器的统计信息。
	 */
	public String getSockJsTaskSchedulerStatsInfo() {
		if (this.sockJsTaskScheduler == null) {
			return "null";
		}
		if (this.sockJsTaskScheduler instanceof ThreadPoolTaskScheduler) {
			return getExecutorStatsInfo(((ThreadPoolTaskScheduler) this.sockJsTaskScheduler)
					.getScheduledThreadPoolExecutor());
		}
		return "unknown";
	}

	private String getExecutorStatsInfo(@Nullable Executor executor) {
		if (executor == null) {
			return "null";
		}

		if (executor instanceof ThreadPoolTaskExecutor) {
			executor = ((ThreadPoolTaskExecutor) executor).getThreadPoolExecutor();
		}

		if (executor instanceof ThreadPoolExecutor) {
			// It is assumed that the implementation of toString() in ThreadPoolExecutor
			// generates text that ends similar to the following:
			// pool size = #, active threads = #, queued tasks = #, completed tasks = #]
			String str = executor.toString();
			int indexOfPool = str.indexOf("pool");
			if (indexOfPool != -1) {
				// (length - 1) omits the trailing "]"
				return str.substring(indexOfPool, str.length() - 1);
			}
		}

		return "unknown";
	}

	@Override
	public String toString() {
		return "WebSocketSession[" + getWebSocketSessionStatsInfo() + "]" +
				", stompSubProtocol[" + getStompSubProtocolStatsInfo() + "]" +
				", stompBrokerRelay[" + getStompBrokerRelayStatsInfo() + "]" +
				", inboundChannel[" + getClientInboundExecutorStatsInfo() + "]" +
				", outboundChannel[" + getClientOutboundExecutorStatsInfo() + "]" +
				", sockJsScheduler[" + getSockJsTaskSchedulerStatsInfo() + "]";
	}

}
