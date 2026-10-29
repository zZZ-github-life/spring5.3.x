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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指示要导入的一个或多个<em>组件类</em> &mdash; 通常是 {@link Configuration @Configuration} 类。
 *
 * <p>提供与 Spring XML 中 {@code <import/>} 元素等价的功能。可用于导入 {@code @Configuration} 类、
 * {@link ImportSelector} 和 {@link ImportBeanDefinitionRegistrar} 实现，以及常规组件类（自 4.2 起；
 * 类似于 {@link AnnotationConfigApplicationContext#register}）。
 *
 * <p>对于在导入的 {@code @Configuration} 类中声明的 {@code @Bean} 定义，应通过
 * {@link org.springframework.beans.factory.annotation.Autowired @Autowired} 注入来访问。既可以自动
 * 注入 Bean 本身，也可以自动注入声明该 Bean 的配置类实例。后一种方式可以在 {@code @Configuration}
 * 类的方法之间进行显式且对 IDE 友好的导航。
 *
 * <p>可以在类级别声明，也可以作为元注解声明。
 *
 * <p>如果需要导入 XML 或其他非 {@code @Configuration} 的 Bean 定义资源，请改用
 * {@link ImportResource @ImportResource} 注解。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see Configuration
 * @see ImportSelector
 * @see ImportBeanDefinitionRegistrar
 * @see ImportResource
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Import {

	/**
	 * 要导入的 {@link Configuration @Configuration}、{@link ImportSelector}、
	 * {@link ImportBeanDefinitionRegistrar} 或常规组件类。
	 */
	Class<?>[] value();

}
