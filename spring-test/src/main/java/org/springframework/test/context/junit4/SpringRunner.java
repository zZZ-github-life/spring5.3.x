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

package org.springframework.test.context.junit4;

import org.junit.runners.model.InitializationError;

/**
 * {@code SpringRunner} 是 {@link SpringJUnit4ClassRunner} 的<em>别名</em>。
 * <p>要使用此类，只需使用 {@code @RunWith(SpringRunner.class)} 标注基于 JUnit 4 的测试类。
 * <p>如果希望将 Spring TestContext Framework 与此运行器以外的运行器一起使用，请使用 {@link org.springframework.test.context.junit4.rules.SpringClassRule} 和 {@link org.springframework.test.context.junit4.rules.SpringMethodRule}。
 * <p><strong>注意：</strong>此类要求 JUnit 4.12 或更高版本。
 *
 * @author Sam Brannen
 * @since 4.3
 * @see SpringJUnit4ClassRunner
 * @see org.springframework.test.context.junit4.rules.SpringClassRule
 * @see org.springframework.test.context.junit4.rules.SpringMethodRule
 */
public final class SpringRunner extends SpringJUnit4ClassRunner {

	/**
	 * 构造一个新的 {@code SpringRunner}，并初始化一个 {@link org.springframework.test.context.TestContextManager TestContextManager}，以为标准 JUnit 4 测试提供 Spring 测试功能。
	 * @param clazz 要运行的测试类
	 * @see #createTestContextManager(Class)
	 */
	public SpringRunner(Class<?> clazz) throws InitializationError {
		super(clazz);
	}

}
