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

package org.springframework.beans.factory.xml;

import org.springframework.beans.factory.parsing.DefaultsDefinition;
import org.springframework.lang.Nullable;

/**
 * 简单的 JavaBean，用于保存在标准 Spring XML Bean 定义文档中 {@code <beans>} 级别指定的默认值：
 * {@code default-lazy-init}、{@code default-autowire} 等。
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 */
public class DocumentDefaultsDefinition implements DefaultsDefinition {

	@Nullable
	private String lazyInit;

	@Nullable
	private String merge;

	@Nullable
	private String autowire;

	@Nullable
	private String autowireCandidates;

	@Nullable
	private String initMethod;

	@Nullable
	private String destroyMethod;

	@Nullable
	private Object source;


	/**
	 * 为当前正在解析的文档设置默认的 lazy-init 标志。
	 */
	public void setLazyInit(@Nullable String lazyInit) {
		this.lazyInit = lazyInit;
	}

	/**
	 * 返回当前正在解析的文档的默认 lazy-init 标志。
	 */
	@Nullable
	public String getLazyInit() {
		return this.lazyInit;
	}

	/**
	 * 为当前正在解析的文档设置默认的 merge 设置。
	 */
	public void setMerge(@Nullable String merge) {
		this.merge = merge;
	}

	/**
	 * 返回当前正在解析的文档的默认 merge 设置。
	 */
	@Nullable
	public String getMerge() {
		return this.merge;
	}

	/**
	 * 为当前正在解析的文档设置默认的 autowire 设置。
	 */
	public void setAutowire(@Nullable String autowire) {
		this.autowire = autowire;
	}

	/**
	 * 返回当前正在解析的文档的默认 autowire 设置。
	 */
	@Nullable
	public String getAutowire() {
		return this.autowire;
	}

	/**
	 * 为当前正在解析的文档设置默认的 autowire-candidate 模式。也接受逗号分隔的模式列表。
	 */
	public void setAutowireCandidates(@Nullable String autowireCandidates) {
		this.autowireCandidates = autowireCandidates;
	}

	/**
	 * 返回当前正在解析的文档的默认 autowire-candidate 模式。也可能返回逗号分隔的模式列表。
	 */
	@Nullable
	public String getAutowireCandidates() {
		return this.autowireCandidates;
	}

	/**
	 * 为当前正在解析的文档设置默认的 init-method 设置。
	 */
	public void setInitMethod(@Nullable String initMethod) {
		this.initMethod = initMethod;
	}

	/**
	 * 返回当前正在解析的文档的默认 init-method 设置。
	 */
	@Nullable
	public String getInitMethod() {
		return this.initMethod;
	}

	/**
	 * 为当前正在解析的文档设置默认的 destroy-method 设置。
	 */
	public void setDestroyMethod(@Nullable String destroyMethod) {
		this.destroyMethod = destroyMethod;
	}

	/**
	 * 返回当前正在解析的文档的默认 destroy-method 设置。
	 */
	@Nullable
	public String getDestroyMethod() {
		return this.destroyMethod;
	}

	/**
	 * 为此元数据元素设置配置源 {@code Object}。<p>该对象的确切类型取决于所使用的配置机制。
	 */
	public void setSource(@Nullable Object source) {
		this.source = source;
	}

	@Override
	@Nullable
	public Object getSource() {
		return this.source;
	}

}
