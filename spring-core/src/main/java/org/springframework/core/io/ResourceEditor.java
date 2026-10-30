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

package org.springframework.core.io;

import java.beans.PropertyEditorSupport;
import java.io.IOException;

import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 用于 {@link Resource} 描述符的 {@link java.beans.PropertyEditor Editor}，可自动将 {@code String}
 * 位置（例如 {@code file:C:/myfile.txt} 或 {@code classpath:myfile.txt}）转换为 {@code Resource}
 * 属性，而不是使用 {@code String} 位置属性。
 *
 * <p>路径中可包含 {@code ${...}} 占位符，它们将被解析为
 * {@link org.springframework.core.env.Environment} 属性：例如 {@code ${user.dir}}。默认情况下会忽
 * 略无法解析的占位符。
 *
 * <p>委托给 {@link ResourceLoader} 完成主要工作，默认使用 {@link DefaultResourceLoader}。
 *
 * @author Juergen Hoeller
 * @author Dave Syer
 * @author Chris Beams
 * @since 28.12.2003
 * @see Resource
 * @see ResourceLoader
 * @see DefaultResourceLoader
 * @see PropertyResolver#resolvePlaceholders
 */
public class ResourceEditor extends PropertyEditorSupport {

	private final ResourceLoader resourceLoader;

	@Nullable
	private PropertyResolver propertyResolver;

	private final boolean ignoreUnresolvablePlaceholders;


	/**
	 * 使用 {@link DefaultResourceLoader} 和 {@link StandardEnvironment} 创建 {@link ResourceEditor} 类
	 * 的新实例。
	 */
	public ResourceEditor() {
		this(new DefaultResourceLoader(), null);
	}

	/**
	 * 使用给定的 {@link ResourceLoader} 和 {@link PropertyResolver} 创建 {@link ResourceEditor} 类的新
	 * 实例。
	 * @param resourceLoader 要使用的 {@code ResourceLoader}
	 * @param propertyResolver 要使用的 {@code PropertyResolver}
	 */
	public ResourceEditor(ResourceLoader resourceLoader, @Nullable PropertyResolver propertyResolver) {
		this(resourceLoader, propertyResolver, true);
	}

	/**
	 * 使用给定的 {@link ResourceLoader} 创建 {@link ResourceEditor} 类的新实例。
	 * @param resourceLoader 要使用的 {@code ResourceLoader}
	 * @param propertyResolver 要使用的 {@code PropertyResolver}
	 * @param ignoreUnresolvablePlaceholders 是否忽略无法解析的占位符，
	 * 如果在给定的 {@code propertyResolver} 中找不到对应的属性
	 */
	public ResourceEditor(ResourceLoader resourceLoader, @Nullable PropertyResolver propertyResolver,
			boolean ignoreUnresolvablePlaceholders) {

		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		this.resourceLoader = resourceLoader;
		this.propertyResolver = propertyResolver;
		this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
	}


	@Override
	public void setAsText(String text) {
		if (StringUtils.hasText(text)) {
			String locationToUse = resolvePath(text).trim();
			setValue(this.resourceLoader.getResource(locationToUse));
		}
		else {
			setValue(null);
		}
	}

	/**
	 * 解析给定路径，如有必要，用来自 {@code environment} 的相应属性值替换占位符。
	 * @param path 原始文件路径
	 * @return 解析后的文件路径
	 * @see PropertyResolver#resolvePlaceholders
	 * @see PropertyResolver#resolveRequiredPlaceholders
	 */
	protected String resolvePath(String path) {
		if (this.propertyResolver == null) {
			this.propertyResolver = new StandardEnvironment();
		}
		return (this.ignoreUnresolvablePlaceholders ? this.propertyResolver.resolvePlaceholders(path) :
				this.propertyResolver.resolveRequiredPlaceholders(path));
	}


	@Override
	@Nullable
	public String getAsText() {
		Resource value = (Resource) getValue();
		try {
			// Try to determine URL for resource.
			return (value != null ? value.getURL().toExternalForm() : "");
		}
		catch (IOException ex) {
			// Couldn't determine resource URL - return null to indicate
			// that there is no appropriate text representation.
			return null;
		}
	}

}
