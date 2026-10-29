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

package org.springframework.test.context.web.socket;

import javax.servlet.ServletContext;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.web.context.WebApplicationContext;

/**
 * {@link ContextCustomizer}，实例化一个新的 {@link MockServerContainer} 并将其以 {@code "javax.websocket.server.ServerContainer"} 为属性名存储到 {@code ServletContext} 中。
 *
 * @author Sam Brannen
 * @since 4.3.1
 */
class MockServerContainerContextCustomizer implements ContextCustomizer {

	@Override
	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		if (context instanceof WebApplicationContext) {
			WebApplicationContext wac = (WebApplicationContext) context;
			ServletContext sc = wac.getServletContext();
			if (sc != null) {
				sc.setAttribute("javax.websocket.server.ServerContainer", new MockServerContainer());
			}
		}
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other != null && getClass() == other.getClass()));
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

}
