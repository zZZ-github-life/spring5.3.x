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

package org.springframework.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @BootstrapWith} 定义类级元数据，用于确定
 * 如何引导 <em>Spring TestContext Framework</em>。
 *
 * <p>此注解还可用作<em>元注解</em>以创建
 * 自定义的<em>组合注解</em>。从 Spring Framework 5.1 开始，本地
 * 声明的 {@code @BootstrapWith} 注解（即<em>直接存在</em>于
 * 当前测试类上的注解）将覆盖任何元存在的
 * {@code @BootstrapWith} 声明。
 *
 * <p>从 Spring Framework 5.3 开始，此注解默认将从
 * 外层测试类继承。详情请参见
 * {@link NestedTestConfiguration @NestedTestConfiguration}。
 *
 * @author Sam Brannen
 * @since 4.1
 * @see BootstrapContext
 * @see TestContextBootstrapper
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface BootstrapWith {

	/**
	 * 用于引导 <em>Spring TestContext Framework</em> 的
	 * {@link TestContextBootstrapper}。
	 */
	Class<? extends TestContextBootstrapper> value() default TestContextBootstrapper.class;

}
