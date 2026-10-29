/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.core.env;

/**
 * 指示某个组件包含并暴露 {@link Environment} 引用的接口。
 *
 * <p>所有 Spring 应用上下文都是 EnvironmentCapable，该接口主要用于
 * 在框架方法中执行 {@code instanceof} 检查，这些方法接受 BeanFactory 实例，
 * 而这些实例实际上可能是也可能不是 ApplicationContext 实例，
 * 以便在环境确实可用时与其交互。
 *
 * <p>如前所述，{@link org.springframework.context.ApplicationContext ApplicationContext}
 * 扩展了 EnvironmentCapable，因而暴露了 {@link #getEnvironment()} 方法；然而，
 * {@link org.springframework.context.ConfigurableApplicationContext ConfigurableApplicationContext}
 * 重新定义了
 * {@link org.springframework.context.ConfigurableApplicationContext#getEnvironment getEnvironment()}
 * 并将签名收窄为返回 {@link ConfigurableEnvironment}。
 * 其效果是，Environment 对象在被从 ConfigurableApplicationContext 访问之前是“只读的”，
 * 而一旦从 ConfigurableApplicationContext 访问，它也可以被配置。
 *
 * @author Chris Beams
 * @since 3.1
 * @see Environment
 * @see ConfigurableEnvironment
 * @see org.springframework.context.ConfigurableApplicationContext#getEnvironment()
 */
public interface EnvironmentCapable {

	/**
	 * 返回与此组件关联的 {@link Environment}。
	 */
	Environment getEnvironment();

}
