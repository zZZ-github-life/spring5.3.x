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

package org.springframework.messaging.simp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.log.LogDelegateFactory;

/**
 * 持有名为 "org.springframework.web.SimpLogging" 的共享日志记录器，
 * 当 "org.springframework.messaging.simp" 的日志记录关闭而
 * "org.springframework.web" 的日志记录开启时，用于基于 WebSocket 的 STOMP 消息传递。
 *
 * <p>这样可以启用 "org.springframework.web" 下所有与 Web 相关的日志记录，
 * 包括来自 "org.springframework.messaging.simp" 等较低级别包的日志记录。
 *
 * <p>要查看日志消息主要来源类的日志，只需为 "org.springframework.messaging" 启用日志记录。
 *
 * @author Rossen Stoyanchev
 * @since 5.1
 * @see LogDelegateFactory
 */
public abstract class SimpLogging {

	private static final Log fallbackLogger =
			LogFactory.getLog("org.springframework.web." + SimpLogging.class.getSimpleName());


	/**
	 * 为给定类创建主日志记录器，并用组合日志记录器包装它，该组合日志记录器会委托给主日志记录器，
	 * 如果主日志记录器未启用，则委托给名为 "org.springframework.web.SimpLogging" 的回退日志记录器。
	 * @param primaryLoggerClass 用作主日志记录器名称的类
	 * @return 生成的复合日志记录器
	 */
	public static Log forLogName(Class<?> primaryLoggerClass) {
		Log primaryLogger = LogFactory.getLog(primaryLoggerClass);
		return forLog(primaryLogger);
	}

	/**
	 * 用组合日志记录器包装给定主日志记录器，该组合日志记录器会委托给主日志记录器，
	 * 如果主日志记录器未启用，则委托给共享回退日志记录器
	 * "org.springframework.web.HttpLogging"。
	 * @param primaryLogger 要使用的主日志记录器
	 * @return 生成的复合日志记录器
	 */
	public static Log forLog(Log primaryLogger) {
		return LogDelegateFactory.getCompositeLog(primaryLogger, fallbackLogger);
	}

}
