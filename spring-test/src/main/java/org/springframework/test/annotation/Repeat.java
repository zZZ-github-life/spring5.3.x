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

package org.springframework.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于 JUnit 4 的测试注解，指示测试方法应
 * 重复调用。
 *
 * <p>请注意，重复执行的范围包括测试方法本身的执行
 * 以及测试夹具的任何<em>设置</em>或<em>清理</em>。当与
 * {@link org.springframework.test.context.junit4.rules.SpringMethodRule
 * SpringMethodRule} 一起使用时，该范围还包括
 * {@linkplain org.springframework.test.context.TestExecutionListener#prepareTestInstance
 * 测试实例的准备工作}。
 *
 * <p>此注解可用作<em>元注解</em>以创建自定义的
 * <em>组合注解</em>。
 *
 * @author Rod Johnson
 * @author Sam Brannen
 * @since 2.0
 * @see org.springframework.test.annotation.Timed
 * @see org.springframework.test.context.junit4.SpringJUnit4ClassRunner
 * @see org.springframework.test.context.junit4.rules.SpringMethodRule
 * @see org.springframework.test.context.junit4.statements.SpringRepeat
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Repeat {

	/**
	 * 带注解的测试方法应重复的次数。
	 */
	int value() default 1;

}
