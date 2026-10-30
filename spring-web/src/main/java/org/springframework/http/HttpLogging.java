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

package org.springframework.http;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.log.LogDelegateFactory;

/**
 * 持有名为 "org.springframework.web.HttpLogging" 的共享日志记录器，用于在未启用 "org.springframework.http" 但启用了 "org.springframework.web" 时进行 HTTP 相关日志记录。
 * <p>这意味着 "org.springframework.web" 会启用所有 Web 日志记录，包括来自 "org.springframework.http" 等较低层包以及来自 {@literal "spring-core"} 等模块的编解码器日志，只要这些编解码器被 {@link org.springframework.http.codec.EncoderHttpMessageWriter EncoderHttpMessageWriter} 或 {@link org.springframework.http.codec.DecoderHttpMessageReader DecoderHttpMessageReader} 包装。
 * <p>若要查看主类日志记录器的日志，只需为 "org.springframework.http" 和 "org.springframework.codec" 启用日志记录。
 *
 * @author Rossen Stoyanchev
 * @since 5.1
 * @see LogDelegateFactory
 */
public abstract class HttpLogging {

	private static final Log fallbackLogger =
			LogFactory.getLog("org.springframework.web." + HttpLogging.class.getSimpleName());


	/**
	 * 为给定类创建主日志记录器，并用组合日志记录器包装它；如果主日志记录器未启用，则委托给该日志记录器或后备日志记录器 "org.springframework.web.HttpLogging"。
	 * @param primaryLoggerClass 主日志记录器名称所对应的类
	 * @return 生成的复合日志记录器
	 */
	public static Log forLogName(Class<?> primaryLoggerClass) {
		Log primaryLogger = LogFactory.getLog(primaryLoggerClass);
		return forLog(primaryLogger);
	}

	/**
	 * 用组合日志记录器包装给定的主日志记录器；如果主日志记录器未启用，则委托给该日志记录器或后备日志记录器 "org.springframework.web.HttpLogging"。
	 * @param primaryLogger 要使用的主日志记录器
	 * @return 生成的复合日志记录器
	 */
	public static Log forLog(Log primaryLogger) {
		return LogDelegateFactory.getCompositeLog(primaryLogger, fallbackLogger);
	}

}
