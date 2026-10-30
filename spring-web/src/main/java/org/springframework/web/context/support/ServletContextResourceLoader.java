/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.web.context.support;

import javax.servlet.ServletContext;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

/**
 * ResourceLoader 实现，将路径解析为 ServletContext 资源，供 WebApplicationContext 之外使用（例如
 * 在 HttpServletBean 或 GenericFilterBean 子类中）。
 *
 * <p>在 WebApplicationContext 内部，资源路径会由上下文实现自动解析为 ServletContext 资源。
 *
 * @author Juergen Hoeller
 * @since 1.0.2
 * @see #getResourceByPath
 * @see ServletContextResource
 * @see org.springframework.web.context.WebApplicationContext
 * @see org.springframework.web.servlet.HttpServletBean
 * @see org.springframework.web.filter.GenericFilterBean
 */
public class ServletContextResourceLoader extends DefaultResourceLoader {

	private final ServletContext servletContext;


	/**
	 * 创建一个新的 ServletContextResourceLoader。
	 * @param servletContext 用于加载资源的 ServletContext
	 */
	public ServletContextResourceLoader(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	/**
	 * 此实现支持 Web 应用根路径下的文件路径。
	 * @see ServletContextResource
	 */
	@Override
	protected Resource getResourceByPath(String path) {
		return new ServletContextResource(this.servletContext, path);
	}

}
