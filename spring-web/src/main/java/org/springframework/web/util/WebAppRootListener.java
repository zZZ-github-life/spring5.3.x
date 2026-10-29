/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.util;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * 将系统属性设置为 Web 应用根目录的监听器。
 * 系统属性的键可以通过 Servlet 上下文级别的 "webAppRootKey" 初始化参数
 * （即 web.xml 中的 context-param）定义，默认键为 "webapp.root"。
 *
 * <p>可用于支持系统属性替换（即 System.getProperty 值）的工具包，
 * 例如 log4j 在日志文件位置中使用的 "${key}" 语法。
 *
 * <p>注意：此监听器应放在 {@code web.xml} 中 ContextLoaderListener 之前，
 * 至少在使用 log4j 时应如此。Log4jConfigListener 会隐式设置系统属性，
 * 因此无需在它之外再使用此监听器。
 *
 * <p><b>警告</b>：某些容器（例如 Tomcat）不会为每个 Web 应用分别维护
 * 系统属性。此时必须为每个 Web 应用使用唯一的 "webAppRootKey"
 * context-param，以避免冲突。其他容器（如 Resin）会隔离每个 Web 应用的
 * 系统属性：在这种情况下，可以放心使用默认键（即完全不设置
 * "webAppRootKey" context-param）。
 *
 * <p><b>警告</b>：包含 Web 应用的 WAR 文件需要展开，才能设置 Web 应用根
 * 系统属性。例如，将 WAR 文件部署到 WebLogic 时默认不会展开。
 * 请勿在这种环境中使用此监听器！
 *
 * @author Juergen Hoeller
 * @since 18.04.2003
 * @see WebUtils#setWebAppRootSystemProperty
 * @see System#getProperty
 */
public class WebAppRootListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent event) {
		WebUtils.setWebAppRootSystemProperty(event.getServletContext());
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		WebUtils.removeWebAppRootSystemProperty(event.getServletContext());
	}

}
