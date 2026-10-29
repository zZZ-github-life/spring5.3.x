/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.test.context.web;

import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.test.context.support.DefaultTestContextBootstrapper;

/**
 * {@link TestContextBootstrapper} SPI 的 Web 特定实现。
 * <ul>
 * <li>如果测试类标注了 {@link WebAppConfiguration @WebAppConfiguration}，则使用 {@link WebDelegatingSmartContextLoader} 作为默认 {@link ContextLoader}，否则委托给超类。
 * <li>如果测试类标注了 {@link WebAppConfiguration @WebAppConfiguration}，则构建 {@link WebMergedContextConfiguration}。
 * </ul>
 *
 * @author Sam Brannen
 * @since 4.1
 */
public class WebTestContextBootstrapper extends DefaultTestContextBootstrapper {

	/**
	 * 如果所提供的类标注了 {@link WebAppConfiguration @WebAppConfiguration}，则返回 {@link WebDelegatingSmartContextLoader}，否则委托给超类。
	 */
	@Override
	protected Class<? extends ContextLoader> getDefaultContextLoaderClass(Class<?> testClass) {
		if (getWebAppConfiguration(testClass) != null) {
			return WebDelegatingSmartContextLoader.class;
		}
		else {
			return super.getDefaultContextLoaderClass(testClass);
		}
	}

	/**
	 * 如果所提供的 {@code MergedContextConfiguration} 中的测试类标注了 {@link WebAppConfiguration @WebAppConfiguration}，则返回 {@link WebMergedContextConfiguration}，否则原样返回所提供的实例。
	 */
	@Override
	protected MergedContextConfiguration processMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
		WebAppConfiguration webAppConfiguration = getWebAppConfiguration(mergedConfig.getTestClass());
		if (webAppConfiguration != null) {
			return new WebMergedContextConfiguration(mergedConfig, webAppConfiguration.value());
		}
		else {
			return mergedConfig;
		}
	}

	@Nullable
	private static WebAppConfiguration getWebAppConfiguration(Class<?> testClass) {
		return TestContextAnnotationUtils.findMergedAnnotation(testClass, WebAppConfiguration.class);
	}

}
