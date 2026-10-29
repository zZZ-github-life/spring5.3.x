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

package org.springframework.test.web.servlet.result;

import org.hamcrest.Matcher;

import org.springframework.lang.Nullable;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertNotNull;

/**
 * “输出”flash 属性断言的工厂。
 *
 * <p>此类的实例通常通过 {@link MockMvcResultMatchers#flash} 访问。
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class FlashAttributeResultMatchers {

	/**
	 * 受保护的构造函数。
	 * 请使用 {@link MockMvcResultMatchers#flash()}。
	 */
	protected FlashAttributeResultMatchers() {
	}


	/**
	 * 使用给定的 Hamcrest {@link Matcher} 断言 flash 属性的值。
	 */
	@SuppressWarnings("unchecked")
	public <T> ResultMatcher attribute(String name, Matcher<? super T> matcher) {
		return result -> assertThat("Flash attribute '" + name + "'", (T) result.getFlashMap().get(name), matcher);
	}

	/**
	 * 断言 flash 属性的值。
	 */
	public ResultMatcher attribute(String name, @Nullable Object value) {
		return result -> assertEquals("Flash attribute '" + name + "'", value, result.getFlashMap().get(name));
	}

	/**
	 * 断言给定 flash 属性存在。
	 */
	public ResultMatcher attributeExists(String... names) {
		return result -> {
			for (String name : names) {
				assertNotNull("Flash attribute '" + name + "' does not exist", result.getFlashMap().get(name));
			}
		};
	}

	/**
	 * 断言 flash 属性的数量。
	 */
	public ResultMatcher attributeCount(int count) {
		return result -> assertEquals("FlashMap size", count, result.getFlashMap().size());
	}

}
