/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.scheduling.support;

import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.ErrorHandler;
import org.springframework.util.ReflectionUtils;

/**
 * 用于为任务添加错误处理装饰的工具方法。
 *
 * <p><b>NOTE:</b> 此类仅供 Spring 的调度器实现内部使用。它之所以是 public，只是为了让其他包中的实
 * 现类能够访问它。它<i>并非</i>面向一般用途。
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 3.0
 */
public abstract class TaskUtils {

	/**
	 * 一种 ErrorHandler 策略：记录异常，但不执行任何进一步处理。这会抑制错误，从而不会阻止任务的后续执
	 * 行。
	 */
	public static final ErrorHandler LOG_AND_SUPPRESS_ERROR_HANDLER = new LoggingErrorHandler();

	/**
	 * 一种 ErrorHandler 策略：以 error 级别记录日志，然后重新抛出异常。注意：这通常会阻止计划任务的后
	 * 续执行。
	 */
	public static final ErrorHandler LOG_AND_PROPAGATE_ERROR_HANDLER = new PropagatingErrorHandler();


	/**
	 * 为任务添加错误处理装饰。如果提供的 {@link ErrorHandler} 不为 {@code null}，则使用它。否则，重复
	 * 任务默认会抑制错误，而一次性任务默认会传播错误，因为这些错误可能通过返回的 {@link Future} 预期到。
	 * 两种情况下都会记录错误。
	 */
	public static DelegatingErrorHandlingRunnable decorateTaskWithErrorHandler(
			Runnable task, @Nullable ErrorHandler errorHandler, boolean isRepeatingTask) {

		if (task instanceof DelegatingErrorHandlingRunnable) {
			return (DelegatingErrorHandlingRunnable) task;
		}
		ErrorHandler eh = (errorHandler != null ? errorHandler : getDefaultErrorHandler(isRepeatingTask));
		return new DelegatingErrorHandlingRunnable(task, eh);
	}

	/**
	 * 根据表示任务是否会重复执行的布尔值，返回默认的 {@link ErrorHandler} 实现。对于重复任务，它会抑制
	 * 错误；对于一次性任务，它会传播错误。两种情况下都会记录错误。
	 */
	public static ErrorHandler getDefaultErrorHandler(boolean isRepeatingTask) {
		return (isRepeatingTask ? LOG_AND_SUPPRESS_ERROR_HANDLER : LOG_AND_PROPAGATE_ERROR_HANDLER);
	}


	/**
	 * 一种 {@link ErrorHandler} 实现：以 error 级别记录 Throwable。它不执行任何额外的错误处理。当有意
	 * 抑制错误时，这会很有用。
	 */
	private static class LoggingErrorHandler implements ErrorHandler {

		private final Log logger = LogFactory.getLog(LoggingErrorHandler.class);

		@Override
		public void handleError(Throwable t) {
			logger.error("Unexpected error occurred in scheduled task", t);
		}
	}


	/**
	 * 一种 {@link ErrorHandler} 实现：以 error 级别记录 Throwable，然后将其传播。
	 */
	private static class PropagatingErrorHandler extends LoggingErrorHandler {

		@Override
		public void handleError(Throwable t) {
			super.handleError(t);
			ReflectionUtils.rethrowRuntimeException(t);
		}
	}

}
