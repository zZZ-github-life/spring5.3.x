/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.result.view;

import java.util.Locale;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

/**
 * 基于 URL 的视图的抽象基类。以 "url" Bean 属性的形式提供一种保存视图所包装 URL 的一致方式。
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractUrlBasedView extends AbstractView implements InitializingBean {

	@Nullable
	private String url;


	/**
	 * 用作 Bean 的构造函数。
	 */
	protected AbstractUrlBasedView() {
	}

	/**
	 * 使用给定 URL 创建新的 AbstractUrlBasedView。
	 */
	protected AbstractUrlBasedView(String url) {
		this.url = url;
	}


	/**
	 * 设置此视图所包装资源的 URL。该 URL 必须适合具体的 View 实现。
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
		if (getUrl() == null) {
			throw new IllegalArgumentException("Property 'url' is required");
		}
	}

	/**
	 * 检查配置的 URL 对应资源是否实际存在。
	 * @param locale the desired Locale that we're looking for
	 * @return {@code false} if the resource exists
	 * {@code false} if we know that it does not exist
	 * @throws Exception if the resource exists but is invalid (e.g. could not be parsed)
	 */
	public abstract boolean checkResourceExists(Locale locale) throws Exception;


	@Override
	public String toString() {
		return super.toString() + "; URL [" + getUrl() + "]";
	}

}
