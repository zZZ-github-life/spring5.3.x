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

package org.springframework.test.web.client;

import org.springframework.util.Assert;

/**
 * 表示预期调用次数范围的简单类型。
 *
 * <p>示例：
 * <pre>
 * import static org.springframework.test.web.client.ExpectedCount.*
 *
 * once()
 * twice()
 * manyTimes()
 * times(5)
 * min(2)
 * max(4)
 * between(2, 4)
 * never()
 * </pre>
 * @author Rossen Stoyanchev
 * @since 4.3
 */
public final class ExpectedCount {

	private final int minCount;

	private final int maxCount;


	/**
	 * 私有构造函数。
	 * 请参见此类中的静态工厂方法。
	 */
	private ExpectedCount(int minCount, int maxCount) {
		Assert.isTrue(minCount >= 0, "minCount >= 0 is required");
		Assert.isTrue(maxCount >= minCount, "maxCount >= minCount is required");
		this.minCount = minCount;
		this.maxCount = maxCount;
	}


	/**
	 * 返回预期次数范围的下界 {@code min}。
	 */
	public int getMinCount() {
		return this.minCount;
	}

	/**
	 * 返回预期次数范围的上界 {@code max}。
	 */
	public int getMaxCount() {
		return this.maxCount;
	}


	/**
	 * 恰好一次。
	 */
	public static ExpectedCount once() {
		return new ExpectedCount(1, 1);
	}

	/**
	 * 恰好两次。
	 */
	public static ExpectedCount twice() {
		return new ExpectedCount(2, 2);
	}

	/**
	 * 多次（范围为 1..Integer.MAX_VALUE）。
	 */
	public static ExpectedCount manyTimes() {
		return new ExpectedCount(1, Integer.MAX_VALUE);
	}

	/**
	 * 恰好 N 次。
	 */
	public static ExpectedCount times(int count) {
		Assert.isTrue(count >= 1, "'count' must be >= 1");
		return new ExpectedCount(count, count);
	}

	/**
	 * 至少 {@code min} 次。
	 */
	public static ExpectedCount min(int min) {
		Assert.isTrue(min >= 1, "'min' must be >= 1");
		return new ExpectedCount(min, Integer.MAX_VALUE);
	}

	/**
	 * 至多 {@code max} 次。
	 */
	public static ExpectedCount max(int max) {
		Assert.isTrue(max >= 1, "'max' must be >= 1");
		return new ExpectedCount(1, max);
	}

	/**
	 * 完全不期望调用，即 min=0 且 max=0。
	 * @since 4.3.6
	 */
	public static ExpectedCount never() {
		return new ExpectedCount(0, 0);
	}

	/**
	 * 介于 {@code min} 与 {@code max} 次之间。
	 */
	public static ExpectedCount between(int min, int max) {
		return new ExpectedCount(min, max);
	}

}
