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

package org.springframework.util.backoff;

/**
 * 表示一次特定的退避执行。
 *
 * <p>实现无需是线程安全的。
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see BackOff
 */
@FunctionalInterface
public interface BackOffExecution {

	/**
	 * 返回 {@link #nextBackOff()} 的值，该值表示不应重试该操作。
	 */
	long STOP = -1;

	/**
	 * 返回重试操作前需要等待的毫秒数，或返回 {@link #STOP}（{@value #STOP}）以表示不应再对该操作进行尝
	 * 试。
	 */
	long nextBackOff();

}
