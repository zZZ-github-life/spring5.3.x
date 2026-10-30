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

package org.springframework.test.context;

import org.springframework.context.ApplicationContext;

/**
 * 用于为 Spring TestContext Framework 管理的集成测试加载
 * {@link ApplicationContext 应用程序上下文}的策略接口。
 *
 * <p><b>注意</b>：从 Spring 3.1 开始，请实现 {@link SmartContextLoader} 而不是
 * 此接口，以便支持注解类、活动
 * Bean 定义配置和应用程序上下文初始化器。
 *
 * <p>ContextLoader 的客户端应在
 * 调用 {@link #loadContext(String...) loadContext()} 之前调用
 * {@link #processLocations(Class, String...) processLocations()}，
 * 以防 ContextLoader 提供对修改或生成位置的自定义支持。
 * 然后应将 {@link #processLocations(Class, String...) processLocations()}
 * 的结果提供给 {@link #loadContext(String...) loadContext()}。
 *
 * <p>具体实现必须提供 {@code public} 无参构造函数。
 *
 * <p>Spring 提供以下开箱即用的实现：
 * <ul>
 * <li>{@link org.springframework.test.context.support.GenericXmlContextLoader GenericXmlContextLoader}</li>
 * <li>{@link org.springframework.test.context.support.GenericPropertiesContextLoader GenericPropertiesContextLoader}</li>
 * </ul>
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see SmartContextLoader
 * @see org.springframework.test.context.support.AnnotationConfigContextLoader AnnotationConfigContextLoader
 */
public interface ContextLoader {

	/**
	 * 处理指定类的应用程序上下文资源位置。
	 * <p>具体实现可以选择修改提供的位置、
	 * 生成新位置，或仅原样返回提供的位置。
	 * @param clazz 与位置关联的类：用于确定
	 * 如何处理所提供的位置
	 * @param locations 用于加载应用上下文的未修改位置
	 * （可为 {@code null} 或空）
	 * @return 应用上下文资源位置的数组
	 */
	String[] processLocations(Class<?> clazz, String... locations);

	/**
	 * 根据提供的
	 * {@code locations} 加载新的 {@link ApplicationContext 上下文}，配置该上下文，最后
	 * 以完全<em>刷新</em>的状态返回该上下文。
	 * <p>默认情况下，配置位置通常被视为类路径
	 * 资源。
	 * <p>具体实现应为此 ContextLoader 加载的 {@link ApplicationContext 应用程序
	 * 上下文}的 Bean 工厂注册注解配置
	 * 处理器。因此，Bean 将自动
	 * 成为使用
	 * {@link org.springframework.beans.factory.annotation.Autowired @Autowired}、
	 * {@link javax.annotation.Resource @Resource} 和
	 * {@link javax.inject.Inject @Inject} 进行基于注解的依赖注入的候选者。
	 * <p>ContextLoader 加载的任何 ApplicationContext <strong>必须</strong>
	 * 为其自身注册 JVM 关闭钩子。除非上下文提前关闭，
	 * 否则所有上下文实例将在 JVM 关闭时
	 * 自动关闭。这样可以释放上下文中 Bean 持有的外部资源，
	 * 例如临时文件。
	 * @param locations 用于加载应用上下文的资源位置
	 * @return 新的应用上下文
	 * @throws Exception 如果上下文加载失败
	 */
	ApplicationContext loadContext(String... locations) throws Exception;

}
