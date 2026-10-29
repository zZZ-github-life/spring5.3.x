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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指示 Bean 是否应延迟初始化。
 *
 * <p>可用于直接或间接标注了 {@link org.springframework.stereotype.Component @Component} 的任何类，
 * 或标注了 {@link Bean @Bean} 的方法。
 *
 * <p>如果 {@code @Component} 或 {@code @Bean} 定义上没有此注解，则会发生急切初始化。如果存在此注解
 * 并设置为 {@code true}，则 {@code @Bean} 或 {@code @Component} 在被另一个 Bean 引用或从所在
 * {@link org.springframework.beans.factory.BeanFactory BeanFactory} 中显式获取之前不会初始化。如果
 * 存在此注解并设置为 {@code false}，则对于执行单例急切初始化的 Bean 工厂，该 Bean 会在启动时实例化。
 *
 *
 * <p>如果 {@link Configuration @Configuration} 类上存在 Lazy，则表示该 {@code @Configuration} 中的
 * 所有 {@code @Bean} 方法都应延迟初始化。如果在标注了 {@code @Lazy} 的 {@code @Configuration} 类中
 * 的某个 {@code @Bean} 方法上存在设置为 false 的 {@code @Lazy}，则表示覆盖“默认延迟”行为，该 Bean
 * 应被急切初始化。
 *
 * <p>除了在组件初始化中的作用外，此注解还可以放在标注了
 * {@link org.springframework.beans.factory.annotation.Autowired} 或 {@link javax.inject.Inject} 的
 * 注入点上：在这种情况下，它会为所有受影响的依赖创建延迟解析代理，作为使用
 * {@link org.springframework.beans.factory.ObjectFactory} 或 {@link javax.inject.Provider} 的替代
 * 方案。请注意，此类延迟解析代理始终会被注入；如果目标依赖不存在，只能在调用时通过异常发现。因此，
 * 这样的注入点对于可选依赖会产生不符合直觉的行为。如需编程式的等价方案，以支持更精细的延迟引用，请
 * 考虑使用 {@link org.springframework.beans.factory.ObjectProvider}。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see Primary
 * @see Bean
 * @see Configuration
 * @see org.springframework.stereotype.Component
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Lazy {

	/**
	 * 是否应进行延迟初始化。
	 */
	boolean value() default true;

}
