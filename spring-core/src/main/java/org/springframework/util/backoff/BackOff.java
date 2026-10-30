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
 * 提供一个 {@link BackOffExecution}，用于指示操作应重试的速率。
 *
 * <p>此接口的使用者应按如下方式使用它：
 *
 * <pre class="code">
 * BackOffExecution exec = backOff.start();
 *
 * // In the operation recovery/retry loop:
 * long waitInterval = exec.nextBackOff();
 * if (waitInterval == BackOffExecution.STOP) {
 *     // do not retry operation
 * }
 * else {
 *     // sleep, e.g. Thread.sleep(waitInterval)
 *     // retry operation
 * }
 * }</pre>
 *
 * 一旦底层操作成功完成，执行实例就可以直接丢弃。
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see BackOffExecution
 */
@FunctionalInterface
public interface BackOff {

	/**
	 * 开始一次新的退避执行。
	 * @return 一个可直接使用的全新 {@link BackOffExecution}
	 */
	BackOffExecution start();

}
