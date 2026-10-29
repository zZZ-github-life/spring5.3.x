/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.util;

/**
 * 用于处理错误的策略。对于处理提交给 TaskScheduler 的任务在异步执行期间发生的错误尤其有用。在这种
 * 情况下，可能无法将错误抛给最初的调用者。
 *
 * @author Mark Fisher
 * @since 3.0
 */
@FunctionalInterface
public interface ErrorHandler {

	/**
	 * 处理给定的错误，可以将其作为致命异常重新抛出。
	 */
	void handleError(Throwable t);

}
