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

package org.springframework.web.servlet.view;

import java.util.Locale;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

/**
 * 基于 URL 的视图的抽象基类。以 "url" bean 属性的形式，
 * 提供了一种保持一致的方式来保存视图所包装的 URL。
 *
 * @author Juergen Hoeller
 * @since 13.12.2003
 */
public abstract class AbstractUrlBasedView extends AbstractView implements InitializingBean {

	@Nullable
	private String url;


	/**
	 * 作为 bean 使用的构造函数。
	 */
	protected AbstractUrlBasedView() {
	}

	/**
	 * 使用给定的 URL 创建一个新的 AbstractUrlBasedView。
	 * @param url 要转发到的 URL
	 */
	protected AbstractUrlBasedView(String url) {
		this.url = url;
	}


	/**
	 * 设置此视图所包装资源的 URL。
	 * 该 URL 必须适合具体的 View 实现。
	 */
	public void setUrl(@Nullable String url) {
		this.url = url;
	}

	/**
	 * 返回此视图所包装资源的 URL。
	 */
	@Nullable
	public String getUrl() {
		return this.url;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (isUrlRequired() && getUrl() == null) {
			throw new IllegalArgumentException("Property 'url' is required");
		}
	}

	/**
	 * 返回 'url' 属性是否为必需。
	 * <p>默认实现返回 {@code true}。
	 * 可在子类中重写。
	 */
	protected boolean isUrlRequired() {
		return true;
	}

	/**
	 * 检查所配置的 URL 指向的底层资源是否
	 * 实际存在。
	 * @param locale 我们要查找的目标 Locale
	 * @return 如果资源存在（或被假定存在）则为 {@code true}；
	 * 如果我们已知其不存在则为 {@code false}
	 * @throws Exception 如果资源存在但无效（例如无法解析）
	 */
	public boolean checkResource(Locale locale) throws Exception {
		return true;
	}

	@Override
	public String toString() {
		return super.toString() + "; URL [" + getUrl() + "]";
	}

}
