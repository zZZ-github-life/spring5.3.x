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

package org.springframework.context.event;

import java.util.concurrent.Executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.ErrorHandler;

/**
 * {@link ApplicationEventMulticaster} 接口的简单实现。
 *
 * <p>将所有事件多播给所有已注册的监听器，由监听器自行忽略其不感兴趣的事件。监听器通常会对传入的事
 * 件对象执行相应的 {@code instanceof} 检查。
 *
 * <p>默认情况下，所有监听器都在调用线程中调用。这会带来一个风险：某个不守规矩的监听器可能阻塞整个
 * 应用，但额外开销最小。可以指定其他任务执行器，让监听器在不同线程中执行，例如从线程池中执行。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @see #setTaskExecutor
 */
public class SimpleApplicationEventMulticaster extends AbstractApplicationEventMulticaster {

	@Nullable
	private Executor taskExecutor;

	@Nullable
	private ErrorHandler errorHandler;

	@Nullable
	private volatile Log lazyLogger;


	/**
	 * 创建一个新的 SimpleApplicationEventMulticaster。
	 */
	public SimpleApplicationEventMulticaster() {
	}

	/**
	 * 为给定的 BeanFactory 创建一个新的 SimpleApplicationEventMulticaster。
	 */
	public SimpleApplicationEventMulticaster(BeanFactory beanFactory) {
		setBeanFactory(beanFactory);
	}


	/**
	 * 设置用于调用每个监听器的自定义执行器（通常是 {@link org.springframework.core.task.TaskExecutor}）。
	 * <p>默认值等同于 {@link org.springframework.core.task.SyncTaskExecutor}，会在调用线程中同步执
	 * 行所有监听器。<p>可以考虑在此指定异步任务执行器，以免在所有监听器执行完毕前阻塞调用方。但请注意，
	 * 除非 TaskExecutor 显式支持，否则异步执行不会参与调用方的线程上下文（类加载器、事务关联）。
	 * @see org.springframework.core.task.SyncTaskExecutor
	 * @see org.springframework.core.task.SimpleAsyncTaskExecutor
	 */
	public void setTaskExecutor(@Nullable Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * 返回此多播器当前的任务执行器。
	 */
	@Nullable
	protected Executor getTaskExecutor() {
		return this.taskExecutor;
	}

	/**
	 * 设置当监听器抛出异常时要调用的 {@link ErrorHandler}。<p>默认没有，监听器异常会停止当前多播并传播
	 * 到当前事件的发布器。如果指定了 {@linkplain #setTaskExecutor task executor}，每个监听器异常都会传
	 * 播到该执行器，但不一定会停止其他监听器的执行。<p>考虑设置一个捕获并记录异常的
	 * {@link ErrorHandler} 实现（类似
	 * {@link org.springframework.scheduling.support.TaskUtils#LOG_AND_SUPPRESS_ERROR_HANDLER}），或者
	 * 一个记录异常但仍将其传播的实现（例如
	 * {@link org.springframework.scheduling.support.TaskUtils#LOG_AND_PROPAGATE_ERROR_HANDLER}）。
	 * @since 4.1
	 */
	public void setErrorHandler(@Nullable ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	/**
	 * 返回此多播器当前的错误处理器。
	 * @since 4.1
	 */
	@Nullable
	protected ErrorHandler getErrorHandler() {
		return this.errorHandler;
	}

	@Override
	public void multicastEvent(ApplicationEvent event) {
		multicastEvent(event, resolveDefaultEventType(event));
	}

	@Override
	public void multicastEvent(final ApplicationEvent event, @Nullable ResolvableType eventType) {
		ResolvableType type = (eventType != null ? eventType : resolveDefaultEventType(event));
		Executor executor = getTaskExecutor();
		for (ApplicationListener<?> listener : getApplicationListeners(event, type)) {
			if (executor != null) {
				executor.execute(() -> invokeListener(listener, event));
			}
			else {
				invokeListener(listener, event);
			}
		}
	}

	private ResolvableType resolveDefaultEventType(ApplicationEvent event) {
		return ResolvableType.forInstance(event);
	}

	/**
	 * 使用给定事件调用给定监听器。
	 * @param listener the ApplicationListener to invoke
	 * @param event the current event to propagate
	 * @since 4.1
	 */
	protected void invokeListener(ApplicationListener<?> listener, ApplicationEvent event) {
		ErrorHandler errorHandler = getErrorHandler();
		if (errorHandler != null) {
			try {
				doInvokeListener(listener, event);
			}
			catch (Throwable err) {
				errorHandler.handleError(err);
			}
		}
		else {
			doInvokeListener(listener, event);
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void doInvokeListener(ApplicationListener listener, ApplicationEvent event) {
		try {
			listener.onApplicationEvent(event);
		}
		catch (ClassCastException ex) {
			String msg = ex.getMessage();
			if (msg == null || matchesClassCastMessage(msg, event.getClass()) ||
					(event instanceof PayloadApplicationEvent &&
							matchesClassCastMessage(msg, ((PayloadApplicationEvent) event).getPayload().getClass()))) {
				// Possibly a lambda-defined listener which we could not resolve the generic event type for
				// -> let's suppress the exception.
				Log loggerToUse = this.lazyLogger;
				if (loggerToUse == null) {
					loggerToUse = LogFactory.getLog(getClass());
					this.lazyLogger = loggerToUse;
				}
				if (loggerToUse.isTraceEnabled()) {
					loggerToUse.trace("Non-matching event type for listener: " + listener, ex);
				}
			}
			else {
				throw ex;
			}
		}
	}

	private boolean matchesClassCastMessage(String classCastMessage, Class<?> eventClass) {
		// On Java 8, the message starts with the class name: "java.lang.String cannot be cast..."
		if (classCastMessage.startsWith(eventClass.getName())) {
			return true;
		}
		// On Java 11, the message starts with "class ..." a.k.a. Class.toString()
		if (classCastMessage.startsWith(eventClass.toString())) {
			return true;
		}
		// On Java 9, the message used to contain the module name: "java.base/java.lang.String cannot be cast..."
		int moduleSeparatorIndex = classCastMessage.indexOf('/');
		if (moduleSeparatorIndex != -1 && classCastMessage.startsWith(eventClass.getName(), moduleSeparatorIndex + 1)) {
			return true;
		}
		// Assuming an unrelated class cast failure...
		return false;
	}

}
