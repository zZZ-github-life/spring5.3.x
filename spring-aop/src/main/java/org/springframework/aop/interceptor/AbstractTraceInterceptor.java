/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.aop.interceptor;

import java.io.Serializable;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.support.AopUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于跟踪的基础 {@code MethodInterceptor} 实现。
 *
 * <p>默认情况下，日志消息写入拦截器类对应的日志，而不是被拦截的类。将 {@code useDynamicLogger}
 * Bean 属性设为 {@code true} 会使所有日志消息写入被拦截目标类的 {@code Log}。
 *
 * <p>子类必须实现 {@code invokeUnderTrace} 方法，仅当特定调用应当被跟踪时，本类才会调用该方法。子
 * 类应写入所提供的 {@code Log} 实例。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see #setUseDynamicLogger
 * @see #invokeUnderTrace(org.aopalliance.intercept.MethodInvocation, org.apache.commons.logging.Log)
 */
@SuppressWarnings("serial")
public abstract class AbstractTraceInterceptor implements MethodInterceptor, Serializable {

	/**
	 * 用于写入跟踪消息的默认 {@code Log} 实例。此实例映射到实现类 {@code Class}。
	 */
	@Nullable
	protected transient Log defaultLogger = LogFactory.getLog(getClass());

	/**
	 * 指示在使用动态日志记录器时是否应隐藏代理类名。
	 * @see #setUseDynamicLogger
	 */
	private boolean hideProxyClassNames = false;

	/**
	 * 指示是否将异常传递给日志记录器。
	 * @see #writeToLog(Log, String, Throwable)
	 */
	private boolean logExceptionStackTrace = true;


	/**
	 * 设置使用动态日志记录器还是静态日志记录器。默认为此跟踪拦截器使用静态日志记录器。
	 * <p>用于确定应为特定方法调用使用哪个 {@code Log} 实例来写入日志消息：被调用 {@code Class} 的动态
	 * 实例，还是跟踪拦截器 {@code Class} 的静态实例。
	 * <p><b>NOTE:</b> 只能指定此属性或 "loggerName" 之一，不能同时指定。
	 * @see #getLoggerForInvocation(org.aopalliance.intercept.MethodInvocation)
	 */
	public void setUseDynamicLogger(boolean useDynamicLogger) {
		// Release default logger if it is not being used.
		this.defaultLogger = (useDynamicLogger ? null : LogFactory.getLog(getClass()));
	}

	/**
	 * 设置要使用的日志记录器名称。该名称将通过 Commons Logging 传递给底层日志记录器实现，并根据日志记
	 * 录器的配置被解释为日志类别。
	 * <p>可以指定此项，以便不记录到某个类的类别（无论是此拦截器的类还是被调用的类），而是记录到特定的
	 * 命名类别。
	 * <p><b>NOTE:</b> 只能指定此属性或 "useDynamicLogger" 之一，不能同时指定。
	 * @see org.apache.commons.logging.LogFactory#getLog(String)
	 * @see java.util.logging.Logger#getLogger(String)
	 */
	public void setLoggerName(String loggerName) {
		this.defaultLogger = LogFactory.getLog(loggerName);
	}

	/**
	 * 设为 "true" 可使 {@link #setUseDynamicLogger 动态日志记录器} 尽可能隐藏代理类名。默认值为
	 * "false"。
	 */
	public void setHideProxyClassNames(boolean hideProxyClassNames) {
		this.hideProxyClassNames = hideProxyClassNames;
	}

	/**
	 * 设置是否将异常传递给日志记录器，以建议将其堆栈跟踪包含到日志中。默认值为 "true"；将其设为
	 * "false" 可将日志输出缩减为仅包含跟踪消息（如果适用，其中可能包含异常类名和异常消息）。
	 * @since 4.3.10
	 */
	public void setLogExceptionStackTrace(boolean logExceptionStackTrace) {
		this.logExceptionStackTrace = logExceptionStackTrace;
	}


	/**
	 * 确定是否对特定 {@code MethodInvocation} 启用日志记录。如果未启用，方法调用将正常继续；否则方法调
	 * 用会被传递给 {@code invokeUnderTrace} 方法处理。
	 * @see #invokeUnderTrace(org.aopalliance.intercept.MethodInvocation, org.apache.commons.logging.Log)
	 */
	@Override
	@Nullable
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Log logger = getLoggerForInvocation(invocation);
		if (isInterceptorEnabled(invocation, logger)) {
			return invokeUnderTrace(invocation, logger);
		}
		else {
			return invocation.proceed();
		}
	}

	/**
	 * 返回用于给定 {@code MethodInvocation} 的适当 {@code Log} 实例。如果设置了
	 * {@code useDynamicLogger} 标志，则该 {@code Log} 实例对应 {@code MethodInvocation} 的目标类；否则
	 * {@code Log} 将是默认的静态日志记录器。
	 * @param invocation 正在跟踪的 {@code MethodInvocation}
	 * @return 要使用的 {@code Log} 实例
	 * @see #setUseDynamicLogger
	 */
	protected Log getLoggerForInvocation(MethodInvocation invocation) {
		if (this.defaultLogger != null) {
			return this.defaultLogger;
		}
		else {
			Object target = invocation.getThis();
			Assert.state(target != null, "Target must not be null");
			return LogFactory.getLog(getClassForLogging(target));
		}
	}

	/**
	 * 确定为日志记录目的而使用的类。
	 * @param target 要内省的目标对象
	 * @return 给定对象的目标类
	 * @see #setHideProxyClassNames
	 */
	protected Class<?> getClassForLogging(Object target) {
		return (this.hideProxyClassNames ? AopUtils.getTargetClass(target) : target.getClass());
	}

	/**
	 * 确定拦截器是否应生效，即是否应调用 {@code invokeUnderTrace} 方法。
	 * <p>默认行为是检查给定的 {@code Log} 实例是否已启用。子类可以覆盖此方法，以便在其他情况下也应用拦
	 * 截器。
	 * @param invocation 正在跟踪的 {@code MethodInvocation}
	 * @param logger 要检查的 {@code Log} 实例
	 * @see #invokeUnderTrace
	 * @see #isLogEnabled
	 */
	protected boolean isInterceptorEnabled(MethodInvocation invocation, Log logger) {
		return isLogEnabled(logger);
	}

	/**
	 * 确定给定的 {@link Log} 实例是否已启用。
	 * <p>当启用 "trace" 级别时，默认返回 {@code true}。子类可以覆盖此方法，以更改发生“跟踪”的级别。
	 * @param logger 要检查的 {@code Log} 实例
	 */
	protected boolean isLogEnabled(Log logger) {
		return logger.isTraceEnabled();
	}

	/**
	 * 将提供的跟踪消息写入提供的 {@code Log} 实例。
	 * <p>供 {@link #invokeUnderTrace} 在进入/退出消息时调用。
	 * <p>最终委托给 {@link #writeToLog(Log, String, Throwable)}，由它控制对底层日志记录器的调用。
	 * @since 4.3.10
	 * @see #writeToLog(Log, String, Throwable)
	 */
	protected void writeToLog(Log logger, String message) {
		writeToLog(logger, message, null);
	}

	/**
	 * 将提供的跟踪消息和 {@link Throwable} 写入提供的 {@code Log} 实例。
	 * <p>供 {@link #invokeUnderTrace} 在进入/退出结果（可能包含异常）时调用。请注意，当
	 * {@link #setLogExceptionStackTrace} 为 "false" 时，异常的堆栈跟踪不会被记录。
	 * <p>默认情况下，消息以 {@code TRACE} 级别写入。子类可以覆盖此方法以控制消息写入的级别，通常也会相
	 * 应地覆盖 {@link #isLogEnabled}。
	 * @since 4.3.10
	 * @see #setLogExceptionStackTrace
	 * @see #isLogEnabled
	 */
	protected void writeToLog(Log logger, String message, @Nullable Throwable ex) {
		if (ex != null && this.logExceptionStackTrace) {
			logger.trace(message, ex);
		}
		else {
			logger.trace(message);
		}
	}


	/**
	 * 子类必须覆盖此方法，以围绕提供的 {@code MethodInvocation} 执行跟踪。子类负责通过调用
	 * {@code MethodInvocation.proceed()} 确保 {@code MethodInvocation} 实际执行。
	 * <p>默认情况下，传入的 {@code Log} 实例将启用 "trace" 日志级别。子类无需再次检查这一点，除非它们
	 * 覆盖 {@code isInterceptorEnabled} 方法以修改默认行为，并且可以将实际要写入的消息委托给
	 * {@code writeToLog}。
	 * @param logger 用于写入跟踪消息的 {@code Log}
	 * @return 调用 {@code MethodInvocation.proceed()} 的结果
	 * @throws Throwable 如果调用 {@code MethodInvocation.proceed()} 时遇到任何错误
	 * @see #isLogEnabled
	 * @see #writeToLog(Log, String)
	 * @see #writeToLog(Log, String, Throwable)
	 */
	@Nullable
	protected abstract Object invokeUnderTrace(MethodInvocation invocation, Log logger) throws Throwable;

}
