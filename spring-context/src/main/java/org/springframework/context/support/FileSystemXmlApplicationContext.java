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

package org.springframework.context.support;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

/**
 * 独立的 XML 应用上下文，从文件系统或 URL 获取上下文定义文件，将普通路径解释为相对文件系统位置（例
 * 如 "mydir/myfile.txt"）。既适用于测试装置，也适用于独立环境。
 *
 * <p><b>注意：</b>普通路径始终会被解释为相对于当前 VM 工作目录，即使它们以斜杠开头也是如此。（这与
 * Servlet 容器中的语义一致。）<b>请使用显式的 "file:" 前缀来强制使用绝对文件路径。</b>
 *
 * <p>默认配置位置可通过 {@link #getConfigLocations} 覆盖。配置位置既可以是具体文件，如
 * "/myfiles/context.xml"，也可以是 Ant 风格的匹配模式，如 "/myfiles/*-context.xml"（模式详情请参阅
 * {@link org.springframework.util.AntPathMatcher} 的 javadoc）。
 *
 * <p>注意：存在多个配置位置时，后加载文件中的 Bean 定义会覆盖先加载文件中的定义。可以利用这一点，
 * 通过额外的 XML 文件故意覆盖某些 Bean 定义。
 *
 * <p><b>这是一个简单的一站式便捷 ApplicationContext。如需更灵活的上下文配置，可考虑将
 * {@link GenericApplicationContext} 类与
 * {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader} 结合使用。</b>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #getResource
 * @see #getResourceByPath
 * @see GenericApplicationContext
 */
public class FileSystemXmlApplicationContext extends AbstractXmlApplicationContext {

	/**
	 * 为 Bean 风格的配置创建一个新的 FileSystemXmlApplicationContext。
	 * @see #setConfigLocation
	 * @see #setConfigLocations
	 * @see #afterPropertiesSet()
	 */
	public FileSystemXmlApplicationContext() {
	}

	/**
	 * 为 Bean 风格的配置创建一个新的 FileSystemXmlApplicationContext。
	 * @param parent the parent context
	 * @see #setConfigLocation
	 * @see #setConfigLocations
	 * @see #afterPropertiesSet()
	 */
	public FileSystemXmlApplicationContext(ApplicationContext parent) {
		super(parent);
	}

	/**
	 * 创建一个新的 FileSystemXmlApplicationContext，从给定的 XML 文件加载定义并自动刷新上下文。
	 * @param configLocation file path
	 * @throws BeansException if context creation failed
	 */
	public FileSystemXmlApplicationContext(String configLocation) throws BeansException {
		this(new String[] {configLocation}, true, null);
	}

	/**
	 * 创建一个新的 FileSystemXmlApplicationContext，从给定的 XML 文件加载定义并自动刷新上下文。
	 * @param configLocations array of file paths
	 * @throws BeansException if context creation failed
	 */
	public FileSystemXmlApplicationContext(String... configLocations) throws BeansException {
		this(configLocations, true, null);
	}

	/**
	 * 使用给定的父上下文创建一个新的 FileSystemXmlApplicationContext，从给定的 XML 文件加载定义并自动
	 * 刷新上下文。
	 * @param configLocations array of file paths
	 * @param parent the parent context
	 * @throws BeansException if context creation failed
	 */
	public FileSystemXmlApplicationContext(String[] configLocations, ApplicationContext parent) throws BeansException {
		this(configLocations, true, parent);
	}

	/**
	 * 创建一个新的 FileSystemXmlApplicationContext，从给定的 XML 文件加载定义。
	 * @param configLocations array of file paths
	 * @param refresh whether to automatically refresh the context,
	 * loading all bean definitions and creating all singletons.
	 * Alternatively, call refresh manually after further configuring the context.
	 * @throws BeansException if context creation failed
	 * @see #refresh()
	 */
	public FileSystemXmlApplicationContext(String[] configLocations, boolean refresh) throws BeansException {
		this(configLocations, refresh, null);
	}

	/**
	 * 使用给定的父上下文创建一个新的 FileSystemXmlApplicationContext，从给定的 XML 文件加载定义。
	 * @param configLocations array of file paths
	 * @param refresh whether to automatically refresh the context,
	 * loading all bean definitions and creating all singletons.
	 * Alternatively, call refresh manually after further configuring the context.
	 * @param parent the parent context
	 * @throws BeansException if context creation failed
	 * @see #refresh()
	 */
	public FileSystemXmlApplicationContext(
			String[] configLocations, boolean refresh, @Nullable ApplicationContext parent)
			throws BeansException {

		super(parent);
		setConfigLocations(configLocations);
		if (refresh) {
			refresh();
		}
	}


	/**
	 * 将资源路径解析为文件系统路径。<p>注意：即使给定路径以斜杠开头，它也会被解释为相对于当前 VM 工作
	 * 目录。这与 Servlet 容器中的语义一致。
	 * @param path the path to the resource
	 * @return the Resource handle
	 * @see org.springframework.web.context.support.XmlWebApplicationContext#getResourceByPath
	 */
	@Override
	protected Resource getResourceByPath(String path) {
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		return new FileSystemResource(path);
	}

}
