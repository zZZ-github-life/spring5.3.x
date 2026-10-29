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

package org.springframework.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于 JUnit 4 的测试注解，指示测试方法必须在
 * {@linkplain #millis() 指定的时间段}内完成执行。
 *
 * <p>如果测试执行时间超过指定的时间段，则
 * 测试被视为失败。
 *
 * <p>请注意，该时间段包括测试方法本身的执行、
 * 测试的任何 {@linkplain Repeat 重复}以及测试夹具的任何<em>设置</em>或
 * <em>清理</em>。
 *
 * <p>此注解可用作<em>元注解</em>以创建自定义的
 * <em>组合注解</em>。
 *
 * @author Rod Johnson
 * @author Sam Brannen
 * @since 2.0
 * @see org.springframework.test.annotation.Repeat
 * @see org.springframework.test.context.junit4.SpringJUnit4ClassRunner
 * @see org.springframework.test.context.junit4.rules.SpringMethodRule
 * @see org.springframework.test.context.junit4.statements.SpringFailOnTimeout
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Timed {

	/**
	 * 测试执行在不因耗时过长而被标记为失败的情况下
	 * 可以花费的最长时间（以毫秒为单位）。
	 */
	long millis();

}
