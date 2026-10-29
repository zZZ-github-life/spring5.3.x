/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.servlet.view.tiles3;

import org.apache.tiles.TilesException;
import org.apache.tiles.preparer.ViewPreparer;

import org.springframework.web.context.WebApplicationContext;

/**
 * Tiles {@link org.apache.tiles.preparer.factory.PreparerFactory} 实现，
 * 它期望使用 preparer bean 名称，并从
 * Spring ApplicationContext 中获取 preparer bean。在这种情况下，
 * 完整的 bean 创建过程将由 Spring 应用上下文控制，
 * 从而允许使用作用域 bean 等。
 *
 * @author Juergen Hoeller
 * @since 3.2
 * @see SimpleSpringPreparerFactory
 */
public class SpringBeanPreparerFactory extends AbstractSpringPreparerFactory {

	@Override
	protected ViewPreparer getPreparer(String name, WebApplicationContext context) throws TilesException {
		return context.getBean(name, ViewPreparer.class);
	}

}
