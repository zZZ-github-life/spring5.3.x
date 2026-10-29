/*
 * Copyright 2002-2017 the original author or authors.
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

import com.jamonapi.MonKey;
import com.jamonapi.MonKeyImp;
import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import com.jamonapi.utils.Misc;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;

/**
 * 使用 <b>JAMon</b> 库对被拦截方法进行性能测量并输出统计信息的性能监控拦截器。此外，它还会跟踪/统
 * 计被拦截方法抛出的异常。堆栈信息可在 JAMon Web 应用中查看。
 *
 * <p>此代码受 Thierry Templier 的博客启发。
 *
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Steve Souza
 * @since 1.1.3
 * @see com.jamonapi.MonitorFactory
 * @see PerformanceMonitorInterceptor
 */
@SuppressWarnings("serial")
public class JamonPerformanceMonitorInterceptor extends AbstractMonitoringInterceptor {

	private boolean trackAllInvocations = false;


	/**
	 * 使用静态日志器创建一个新的 JamonPerformanceMonitorInterceptor。
	 */
	public JamonPerformanceMonitorInterceptor() {
	}

	/**
	 * 根据给定标志，使用动态或静态日志器创建一个新的 JamonPerformanceMonitorInterceptor。
	 * @param useDynamicLogger whether to use a dynamic logger or a static logger
	 * @see #setUseDynamicLogger
	 */
	public JamonPerformanceMonitorInterceptor(boolean useDynamicLogger) {
		setUseDynamicLogger(useDynamicLogger);
	}

	/**
	 * 根据给定标志，使用动态或静态日志器创建一个新的 JamonPerformanceMonitorInterceptor。
	 * @param useDynamicLogger whether to use a dynamic logger or a static logger
	 * @param trackAllInvocations whether to track all invocations that go through
	 * this interceptor, or just invocations with trace logging enabled
	 * @see #setUseDynamicLogger
	 */
	public JamonPerformanceMonitorInterceptor(boolean useDynamicLogger, boolean trackAllInvocations) {
		setUseDynamicLogger(useDynamicLogger);
		setTrackAllInvocations(trackAllInvocations);
	}


	/**
	 * 设置是跟踪经过此拦截器的所有调用，还是仅跟踪启用了跟踪日志的调用。
	 * <p>默认值为 “false”：仅监控启用了跟踪日志的调用。指定为 “true” 可让 JAMon 跟踪所有调用，即使禁用
	 * 了跟踪日志也会收集统计数据。
	 */
	public void setTrackAllInvocations(boolean trackAllInvocations) {
		this.trackAllInvocations = trackAllInvocations;
	}


	/**
	 * 如果已设置 “trackAllInvocations” 标志，则始终应用该拦截器；否则仅在日志启用时才生效。
	 * @see #setTrackAllInvocations
	 * @see #isLogEnabled
	 */
	@Override
	protected boolean isInterceptorEnabled(MethodInvocation invocation, Log logger) {
		return (this.trackAllInvocations || isLogEnabled(logger));
	}

	/**
	 * 用 JAMon Monitor 包装调用，并将当前性能统计信息写入日志（如果已启用）。
	 * @see com.jamonapi.MonitorFactory#start
	 * @see com.jamonapi.Monitor#stop
	 */
	@Override
	protected Object invokeUnderTrace(MethodInvocation invocation, Log logger) throws Throwable {
		String name = createInvocationTraceName(invocation);
		MonKey key = new MonKeyImp(name, name, "ms.");

		Monitor monitor = MonitorFactory.start(key);
		try {
			return invocation.proceed();
		}
		catch (Throwable ex) {
			trackException(key, ex);
			throw ex;
		}
		finally {
			monitor.stop();
			if (!this.trackAllInvocations || isLogEnabled(logger)) {
				writeToLog(logger, "JAMon performance statistics for method [" + name + "]:\n" + monitor);
			}
		}
	}

	/**
	 * 统计抛出的异常，并将堆栈信息放入键的明细部分。这样便可在 JAMon Web 应用中查看堆栈信息。
	 */
	protected void trackException(MonKey key, Throwable ex) {
		String stackTrace = "stackTrace=" + Misc.getExceptionTrace(ex);
		key.setDetails(stackTrace);

		// Specific exception counter. Example: java.lang.RuntimeException
		MonitorFactory.add(new MonKeyImp(ex.getClass().getName(), stackTrace, "Exception"), 1);

		// General exception counter which is a total for all exceptions thrown
		MonitorFactory.add(new MonKeyImp(MonitorFactory.EXCEPTIONS_LABEL, stackTrace, "Exception"), 1);
	}

}
