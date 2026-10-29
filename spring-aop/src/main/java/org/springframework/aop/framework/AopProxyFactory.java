/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.aop.framework;

/**
 * 由能够基于 {@link AdvisedSupport} 配置对象创建 AOP 代理的工厂实现的接口。
 *
 * <p>代理应遵守以下约定：
 * <ul>
 * <li>它们应实现配置所指示应代理的所有接口。</li>
 * <li>它们应实现 {@link Advised} 接口。</li>
 * <li>它们应实现 equals 方法，以比较被代理的接口、通知和目标。</li>
 * <li>如果所有 Advisor 和目标都可序列化，则它们应可序列化。</li>
 * <li>如果 Advisor 和目标都是线程安全的，则它们应线程安全。</li>
 * </ul>
 *
 * <p>代理可能允许也可能不允许更改通知。如果代理不允许更改通知（例如因为配置已被冻结），那么在尝试
 * 更改通知时应抛出 {@link AopConfigException}。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public interface AopProxyFactory {

	/**
	 * 为给定的 AOP 配置创建 {@link AopProxy}。
	 * @param config the AOP configuration in the form of an
	 * AdvisedSupport object
	 * @return the corresponding AOP proxy
	 * @throws AopConfigException if the configuration is invalid
	 */
	AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException;

}
