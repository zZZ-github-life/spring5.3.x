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

package org.springframework.aop.framework;

import java.io.Serializable;

import org.springframework.util.Assert;

/**
 * 用于创建代理的配置的便捷超类，可确保所有代理创建者拥有一致的属性。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see AdvisedSupport
 */
public class ProxyConfig implements Serializable {

	/** 使用 Spring 1.2 的 serialVersionUID 以实现互操作性。 */
	private static final long serialVersionUID = -8409359707199703185L;


	private boolean proxyTargetClass = false;

	private boolean optimize = false;

	boolean opaque = false;

	boolean exposeProxy = false;

	private boolean frozen = false;


	/**
	 * 设置是否直接代理目标类，而不是仅代理特定的接口。默认值为 "false"。
	 * <p>将其设置为 "true" 可强制代理 TargetSource 所暴露的目标类。如果该目标类是接口，则将为给定接口
	 * 创建 JDK 代理。如果该目标类是其他任何类，则将为给定类创建 CGLIB 代理。
	 * <p>注意：取决于具体代理工厂的配置，如果未指定任何接口（且未激活接口自动检测），也会应用
	 * proxy-target-class 行为。
	 * @see org.springframework.aop.TargetSource#getTargetClass()
	 */
	public void setProxyTargetClass(boolean proxyTargetClass) {
		this.proxyTargetClass = proxyTargetClass;
	}

	/**
	 * 返回是否除了任何接口之外还直接代理目标类。
	 */
	public boolean isProxyTargetClass() {
		return this.proxyTargetClass;
	}

	/**
	 * 设置代理是否应执行积极的优化。"aggressive optimizations" 的确切含义因代理而异，不过通常需要做出
	 * 某种权衡。默认值为 "false"。
	 * <p>在当前 Spring 的代理选项下，此标志实际上会强制使用 CGLIB 代理（类似于
	 * {@link #setProxyTargetClass}），但不做任何类校验检查（例如针对 final 方法）。
	 */
	public void setOptimize(boolean optimize) {
		this.optimize = optimize;
	}

	/**
	 * 返回代理是否应执行积极的优化。
	 */
	public boolean isOptimize() {
		return this.optimize;
	}

	/**
	 * 设置是否应阻止由此配置创建的代理被强制转换为 {@link Advised} 以查询代理状态。
	 * <p>默认值为 "false"，表示任何 AOP 代理都可以强制转换为 {@link Advised}。
	 */
	public void setOpaque(boolean opaque) {
		this.opaque = opaque;
	}

	/**
	 * 返回是否应阻止由此配置创建的代理被强制转换为 {@link Advised}。
	 */
	public boolean isOpaque() {
		return this.opaque;
	}

	/**
	 * 设置 AOP 框架是否应将代理解析为 ThreadLocal 暴露，以便通过 AopContext 类获取。当被通知的对象需要
	 * 在自身上调用另一个被通知的方法时，这很有用。（如果它使用 {@code this}，该调用不会被通知。）
	 * <p>默认值为 "false"，以避免不必要的额外拦截。这意味着不保证在被通知对象的任何方法内 AopContext
	 * 访问都能一致地工作。
	 */
	public void setExposeProxy(boolean exposeProxy) {
		this.exposeProxy = exposeProxy;
	}

	/**
	 * 返回 AOP 代理是否会在每次调用时暴露该 AOP 代理。
	 */
	public boolean isExposeProxy() {
		return this.exposeProxy;
	}

	/**
	 * 设置此配置是否应被冻结。
	 * <p>配置被冻结后，就无法再更改通知。这有助于优化，也适用于我们不希望调用方在强制转换为 Advised 后
	 * 还能操纵配置的情况。
	 */
	public void setFrozen(boolean frozen) {
		this.frozen = frozen;
	}

	/**
	 * 返回配置是否已冻结、是否已不能再更改通知。
	 */
	public boolean isFrozen() {
		return this.frozen;
	}


	/**
	 * 从另一个配置对象复制配置。
	 * @param other 要从中复制配置的对象
	 */
	public void copyFrom(ProxyConfig other) {
		Assert.notNull(other, "Other ProxyConfig object must not be null");
		this.proxyTargetClass = other.proxyTargetClass;
		this.optimize = other.optimize;
		this.exposeProxy = other.exposeProxy;
		this.frozen = other.frozen;
		this.opaque = other.opaque;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("proxyTargetClass=").append(this.proxyTargetClass).append("; ");
		sb.append("optimize=").append(this.optimize).append("; ");
		sb.append("opaque=").append(this.opaque).append("; ");
		sb.append("exposeProxy=").append(this.exposeProxy).append("; ");
		sb.append("frozen=").append(this.frozen);
		return sb.toString();
	}

}
