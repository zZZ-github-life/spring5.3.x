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

package org.springframework.context.support;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link AbstractRefreshableApplicationContext} 的子类，增加了对指定配置位置的通用处理。用作基于
 * XML 的应用上下文实现的基类，例如 {@link ClassPathXmlApplicationContext} 和
 * {@link FileSystemXmlApplicationContext}，以及
 * {@link org.springframework.web.context.support.XmlWebApplicationContext}。
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 * @see #setConfigLocation
 * @see #setConfigLocations
 * @see #getDefaultConfigLocations
 */
public abstract class AbstractRefreshableConfigApplicationContext extends AbstractRefreshableApplicationContext
		implements BeanNameAware, InitializingBean {

	@Nullable
	private String[] configLocations;

	private boolean setIdCalled = false;


	/**
	 * 创建一个没有父上下文的新 AbstractRefreshableConfigApplicationContext。
	 */
	public AbstractRefreshableConfigApplicationContext() {
	}

	/**
	 * 使用给定的父上下文创建一个新的 AbstractRefreshableConfigApplicationContext。
	 * @param parent 父上下文
	 */
	public AbstractRefreshableConfigApplicationContext(@Nullable ApplicationContext parent) {
		super(parent);
	}


	/**
	 * 以 init-param 风格为此应用上下文设置配置位置，即用逗号、分号或空白分隔不同的位置。
	 *
	 * <p>如果未设置，实现可以根据需要采用默认值。
	 */
	public void setConfigLocation(String location) {
		setConfigLocations(StringUtils.tokenizeToStringArray(location, CONFIG_LOCATION_DELIMITERS));
	}

	/**
	 * 为此应用上下文设置配置位置。
	 *
	 * <p>如果未设置，实现可以根据需要采用默认值。
	 */
	public void setConfigLocations(@Nullable String... locations) {
		if (locations != null) {
			Assert.noNullElements(locations, "Config locations must not be null");
			this.configLocations = new String[locations.length];
			for (int i = 0; i < locations.length; i++) {
				this.configLocations[i] = resolvePath(locations[i]).trim();
			}
		}
		else {
			this.configLocations = null;
		}
	}

	/**
	 * 返回资源位置数组，这些位置指向构建此上下文应使用的 XML Bean 定义文件。也可以包含位置模式，这些模
	 * 式将通过 ResourcePatternResolver 解析。
	 *
	 * <p>默认实现返回 {@code null}。子类可以重写此方法，提供一组用于加载 Bean 定义的资源位置。
	 * @return 资源位置数组；如果没有则为 {@code null}
	 * @see #getResources
	 * @see #getResourcePatternResolver
	 */
	@Nullable
	protected String[] getConfigLocations() {
		return (this.configLocations != null ? this.configLocations : getDefaultConfigLocations());
	}

	/**
	 * 返回要使用的默认配置位置，用于未指定显式配置位置的情况。
	 *
	 * <p>默认实现返回 {@code null}，要求提供显式配置位置。
	 * @return 默认配置位置数组（如果有）
	 * @see #setConfigLocations
	 */
	@Nullable
	protected String[] getDefaultConfigLocations() {
		return null;
	}

	/**
	 * 解析给定路径，必要时用相应的环境属性值替换占位符。应用于配置位置。
	 * @param path 原始文件路径
	 * @return 解析后的文件路径
	 * @see org.springframework.core.env.Environment#resolveRequiredPlaceholders(String)
	 */
	protected String resolvePath(String path) {
		return getEnvironment().resolveRequiredPlaceholders(path);
	}


	@Override
	public void setId(String id) {
		super.setId(id);
		this.setIdCalled = true;
	}

	/**
	 * 默认情况下将此上下文的 id 设置为 Bean 名称，适用于上下文实例本身被定义为 Bean 的情况。
	 */
	@Override
	public void setBeanName(String name) {
		if (!this.setIdCalled) {
			super.setId(name);
			setDisplayName("ApplicationContext '" + name + "'");
		}
	}

	/**
	 * 如果具体上下文的构造函数中尚未刷新，则触发 {@link #refresh()}。
	 */
	@Override
	public void afterPropertiesSet() {
		if (!isActive()) {
			refresh();
		}
	}

}
