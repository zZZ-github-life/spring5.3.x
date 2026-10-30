/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.socket.sockjs.transport.handler;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.sockjs.transport.TransportHandler;
import org.springframework.web.socket.sockjs.transport.TransportHandlingSockJsService;

/**
 * {@link org.springframework.web.socket.sockjs.SockJsService} 的默认实现，
 * 预先注册了所有默认的 {@link TransportHandler} 实现。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public class DefaultSockJsService extends TransportHandlingSockJsService implements ServletContextAware {

	/**
	 * 使用默认 {@link TransportHandler handler} 类型创建 DefaultSockJsService。
	 * @param scheduler 用于心跳消息和移除超时会话的任务调度器；
	 * 提供的 TaskScheduler 应声明为 Spring Bean，
	 * 以确保它在启动时初始化，
	 * 并在应用停止时关闭。
	 */
	public DefaultSockJsService(TaskScheduler scheduler) {
		this(scheduler, getDefaultTransportHandlers(null));
	}

	/**
	 * 使用覆盖默认处理器实现的 {@link TransportHandler handler} 类型创建
	 * DefaultSockJsService。
	 * @param scheduler 用于心跳消息和移除超时会话的任务调度器；
	 * 提供的 TaskScheduler 应声明为 Spring Bean，
	 * 以确保它在启动时初始化并在应用停止时关闭
	 * @param handlerOverrides 对默认传输处理器类型的零个或多个覆盖
	 */
	public DefaultSockJsService(TaskScheduler scheduler, TransportHandler... handlerOverrides) {
		this(scheduler, Arrays.asList(handlerOverrides));
	}

	/**
	 * 使用覆盖默认处理器实现的 {@link TransportHandler handler} 类型创建
	 * DefaultSockJsService。
	 * @param scheduler 用于心跳消息和移除超时会话的任务调度器；
	 * 提供的 TaskScheduler 应声明为 Spring Bean，
	 * 以确保它在启动时初始化并在应用停止时关闭
	 * @param handlerOverrides 对默认传输处理器类型的零个或多个覆盖
	 */
	public DefaultSockJsService(TaskScheduler scheduler, Collection<TransportHandler> handlerOverrides) {
		super(scheduler, getDefaultTransportHandlers(handlerOverrides));
	}


	private static Set<TransportHandler> getDefaultTransportHandlers(@Nullable Collection<TransportHandler> overrides) {
		Set<TransportHandler> result = new LinkedHashSet<>(8);
		result.add(new XhrPollingTransportHandler());
		result.add(new XhrReceivingTransportHandler());
		result.add(new XhrStreamingTransportHandler());
		result.add(new EventSourceTransportHandler());
		result.add(new HtmlFileTransportHandler());
		try {
			result.add(new WebSocketTransportHandler(new DefaultHandshakeHandler()));
		}
		catch (Exception ex) {
			Log logger = LogFactory.getLog(DefaultSockJsService.class);
			if (logger.isWarnEnabled()) {
				logger.warn("Failed to create a default WebSocketTransportHandler", ex);
			}
		}
		if (overrides != null) {
			result.addAll(overrides);
		}
		return result;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		for (TransportHandler handler : getTransportHandlers().values()) {
			if (handler instanceof ServletContextAware) {
				((ServletContextAware) handler).setServletContext(servletContext);
			}
		}
	}
}
