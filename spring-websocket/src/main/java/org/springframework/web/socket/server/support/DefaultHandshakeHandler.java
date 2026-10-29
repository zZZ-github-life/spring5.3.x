/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.socket.server.support;

import javax.servlet.ServletContext;

import org.springframework.web.context.ServletContextAware;
import org.springframework.web.socket.server.RequestUpgradeStrategy;

/**
 * 默认的 {@link org.springframework.web.socket.server.HandshakeHandler} 实现，
 * 通过 {@link AbstractHandshakeHandler} 扩展了特定于 Servlet 的初始化支持。
 * 有关支持的服务器等详细信息，请参见 {@link AbstractHandshakeHandler} 的 javadoc。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public class DefaultHandshakeHandler extends AbstractHandshakeHandler implements ServletContextAware {

	public DefaultHandshakeHandler() {
	}

	public DefaultHandshakeHandler(RequestUpgradeStrategy requestUpgradeStrategy) {
		super(requestUpgradeStrategy);
	}


	@Override
	public void setServletContext(ServletContext servletContext) {
		RequestUpgradeStrategy strategy = getRequestUpgradeStrategy();
		if (strategy instanceof ServletContextAware) {
			((ServletContextAware) strategy).setServletContext(servletContext);
		}
	}

}
