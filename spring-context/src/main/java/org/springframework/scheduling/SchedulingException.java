/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.scheduling;

import org.springframework.core.NestedRuntimeException;

/**
 * 在调度失败时抛出的通用异常，例如调度器已经关闭。由于调度失败通常是致命的，因此该异常为非受检异常。
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class SchedulingException extends NestedRuntimeException {

	/**
	 * SchedulingException 的构造方法。
	 * @param msg the detail message
	 */
	public SchedulingException(String msg) {
		super(msg);
	}

	/**
	 * SchedulingException 的构造方法。
	 * @param msg the detail message
	 * @param cause the root cause (usually from using an underlying
	 * scheduling API such as Quartz)
	 */
	public SchedulingException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
