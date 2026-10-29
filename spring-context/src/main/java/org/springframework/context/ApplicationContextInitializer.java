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

package org.springframework.context;

/**
 * 用于在 Spring {@link ConfigurableApplicationContext} 被
 * {@linkplain ConfigurableApplicationContext#refresh() 刷新} 之前对其进行初始化的回调接口。
 *
 * <p>通常用于需要对应用上下文进行某种编程式初始化的 Web 应用。例如，针对
 * {@linkplain ConfigurableApplicationContext#getEnvironment() 上下文的 Environment} 注册属性源或激
 * 活 profile。有关分别声明“contextInitializerClasses” context-param 和 init-param 的支持，请参见
 * {@code ContextLoader} 和 {@code FrameworkServlet}。
 *
 * <p>鼓励 {@code ApplicationContextInitializer} 处理器检测是否实现了 Spring 的
 * {@link org.springframework.core.Ordered Ordered} 接口，或者是否存在
 * {@link org.springframework.core.annotation.Order @Order} 注解，并据此在调用之前对实例进行排序。
 *
 * @author Chris Beams
 * @since 3.1
 * @param <C> the application context type
 * @see org.springframework.web.context.ContextLoader#customizeContext
 * @see org.springframework.web.context.ContextLoader#CONTEXT_INITIALIZER_CLASSES_PARAM
 * @see org.springframework.web.servlet.FrameworkServlet#setContextInitializerClasses
 * @see org.springframework.web.servlet.FrameworkServlet#applyInitializers
 */
@FunctionalInterface
public interface ApplicationContextInitializer<C extends ConfigurableApplicationContext> {

	/**
	 * 初始化给定的应用上下文。
	 * @param applicationContext the application to configure
	 */
	void initialize(C applicationContext);

}
