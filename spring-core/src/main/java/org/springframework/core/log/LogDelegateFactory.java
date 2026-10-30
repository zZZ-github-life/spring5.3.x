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

package org.springframework.core.log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 用于 Spring 日志约定的通用 {@link Log} 委托的工厂。
 *
 * <p>主要供框架内部与 Apache Commons Logging 配合使用，
 * 通常以 {@code spring-jcl} 桥接的形式出现，但也兼容
 * 其他 Commons Logging 桥接。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 5.1
 * @see org.apache.commons.logging.LogFactory
 */
public final class LogDelegateFactory {

	private LogDelegateFactory() {
	}


	/**
	 * 创建一个复合日志记录器，它会委托给主日志记录器，
	 * 如果主日志记录器的日志未启用，则回退到次级日志记录器。
	 * <p>这可用于来自较低层包的备用日志记录，这些包
	 * 在逻辑上应与某些较高层包一起记录日志，但两者
	 * 并未恰好共享合适的父包（例如 web 包以及
	 * 较低层 http 和 codec 包的日志记录）。对于这种情况，可以将主（基于类的）
	 * 日志记录器包装在一个共享的回退日志记录器周围。
	 * @param primaryLogger 首先尝试的主日志记录器
	 * @param secondaryLogger 次级日志记录器
	 * @param tertiaryLoggers 可选的变长参数，用于指定更多回退日志记录器
	 * @return 相关类别对应的组合日志记录器
	 */
	public static Log getCompositeLog(Log primaryLogger, Log secondaryLogger, Log... tertiaryLoggers) {
		List<Log> loggers = new ArrayList<>(2 + tertiaryLoggers.length);
		loggers.add(primaryLogger);
		loggers.add(secondaryLogger);
		Collections.addAll(loggers, tertiaryLoggers);
		return new CompositeLog(loggers);
	}

	/**
	 * 创建一个类别名以 "_" 为前缀的“隐藏”日志记录器，从而
	 * 使其无法与同一包中的其他日志类别一起启用。
	 * 这对于过于冗长、或者在其他方面属于可选
	 * 或无需始终查看的专门输出很有用。
	 * @param clazz 要为其创建日志记录器的类
	 * @return 类别为 {@code "_" + fully-qualified class name} 的 Log
	 */
	public static Log getHiddenLog(Class<?> clazz) {
		return getHiddenLog(clazz.getName());
	}

	/**
	 * 创建一个类别名以 "_" 为前缀的“隐藏”日志记录器，从而
	 * 使其无法与同一包中的其他日志类别一起启用。
	 * 这对于过于冗长、或者在其他方面属于可选
	 * 或无需始终查看的专门输出很有用。
	 * @param category 要使用的日志类别
	 * @return 类别为 {@code "_" + category} 的 Log
	 * @since 5.3.5
	 */
	public static Log getHiddenLog(String category) {
		return LogFactory.getLog("_" + category);
	}

}
