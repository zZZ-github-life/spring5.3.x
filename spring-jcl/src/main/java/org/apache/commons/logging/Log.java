/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.logging;

/**
 * 一个简单的日志接口，用于抽象不同的日志 API。为了让 {@link LogFactory} 能成功实例化实现此接口的类
 * ，实现类必须提供一个接收单个 String 参数的构造方法，该参数表示此 Log 的“名称”。
 *
 * <p><code>Log</code> 使用的六个日志级别（按顺序）如下：
 * <ol>
 * <li>trace（最不严重）</li>
 * <li>debug</li>
 * <li>info</li>
 * <li>warn</li>
 * <li>error</li>
 * <li>fatal（最严重）</li>
 * </ol>
 *
 * 这些日志级别到底层日志系统所用概念的映射方式取决于具体实现。不过，实现应确保这一顺序的行为符合预
 * 期。
 *
 * <p>性能通常是日志记录关注的重点。通过检查相应的属性，组件可以避免昂贵的操作（即生成要记录的信息
 * ）。
 *
 * <p>例如，
 * <pre>
 *    if (log.isDebugEnabled()) {
 *        ... do something expensive ...
 *        log.debug(theResult);
 *    }
 * </pre>
 *
 * <p>底层日志系统的配置通常在 Logging API 之外完成，具体使用该系统所支持的机制。
 *
 * @author Juergen Hoeller (for the {@code spring-jcl} variant)
 * @since 5.0
 */
public interface Log {

	/**
	 * 当前是否启用了 fatal 级别日志？<p>当日志级别高于 fatal 时，可调用此方法以避免执行昂贵的操作（例
	 * 如 <code>String</code> 拼接）。
	 * @return 如果底层日志记录器启用了 fatal 级别，则返回 true。
	 */
	boolean isFatalEnabled();

	/**
	 * 当前是否启用了 error 级别日志？<p>当日志级别高于 error 时，可调用此方法以避免执行昂贵的操作（例
	 * 如 <code>String</code> 拼接）。
	 * @return 如果底层日志记录器启用了 error 级别，则返回 true。
	 */
	boolean isErrorEnabled();

	/**
	 * 当前是否启用了 warn 级别日志？<p>当日志级别高于 warn 时，可调用此方法以避免执行昂贵的操作（例如
	 * <code>String</code> 拼接）。
	 * @return 如果底层日志记录器启用了 warn 级别，则返回 true。
	 */
	boolean isWarnEnabled();

	/**
	 * 当前是否启用了 info 级别日志？<p>当日志级别高于 info 时，可调用此方法以避免执行昂贵的操作（例如
	 * <code>String</code> 拼接）。
	 * @return 如果底层日志记录器启用了 info 级别，则返回 true。
	 */
	boolean isInfoEnabled();

	/**
	 * 当前是否启用了 debug 级别日志？<p>当日志级别高于 debug 时，可调用此方法以避免执行昂贵的操作（例
	 * 如 <code>String</code> 拼接）。
	 * @return 如果底层日志记录器启用了 debug 级别，则返回 true。
	 */
	boolean isDebugEnabled();

	/**
	 * 当前是否启用了 trace 级别日志？<p>当日志级别高于 trace 时，可调用此方法以避免执行昂贵的操作（例
	 * 如 <code>String</code> 拼接）。
	 * @return 如果底层日志记录器启用了 trace 级别，则返回 true。
	 */
	boolean isTraceEnabled();


	/**
	 * 以 fatal 日志级别记录一条消息。
	 * @param message 记录此消息
	 */
	void fatal(Object message);

	/**
	 * 以 fatal 日志级别记录一个错误。
	 * @param message 记录此消息
	 * @param t 记录该异常
	 */
	void fatal(Object message, Throwable t);

	/**
	 * 以 error 日志级别记录一条消息。
	 * @param message 记录此消息
	 */
	void error(Object message);

	/**
	 * 以 error 日志级别记录一个错误。
	 * @param message 记录此消息
	 * @param t 记录该异常
	 */
	void error(Object message, Throwable t);

	/**
	 * 以 warn 日志级别记录一条消息。
	 * @param message 记录此消息
	 */
	void warn(Object message);

	/**
	 * 以 warn 日志级别记录一个错误。
	 * @param message 记录此消息
	 * @param t 记录该异常
	 */
	void warn(Object message, Throwable t);

	/**
	 * 以 info 日志级别记录一条消息。
	 * @param message 记录此消息
	 */
	void info(Object message);

	/**
	 * 以 info 日志级别记录一个错误。
	 * @param message 记录此消息
	 * @param t 记录该异常
	 */
	void info(Object message, Throwable t);

	/**
	 * 以 debug 日志级别记录一条消息。
	 * @param message 记录此消息
	 */
	void debug(Object message);

	/**
	 * 以 debug 日志级别记录一个错误。
	 * @param message 记录此消息
	 * @param t 记录该异常
	 */
	void debug(Object message, Throwable t);

	/**
	 * 以 trace 日志级别记录一条消息。
	 * @param message 记录此消息
	 */
	void trace(Object message);

	/**
	 * 以 trace 日志级别记录一个错误。
	 * @param message 记录此消息
	 * @param t 记录该异常
	 */
	void trace(Object message, Throwable t);

}
