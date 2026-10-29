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

package org.springframework.beans.factory.support;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.lang.Nullable;

/**
 * 在没有可用注解支持时使用的 {@link AutowireCandidateResolver} 实现。此实现仅检查 Bean 定义。
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 2.5
 */
public class SimpleAutowireCandidateResolver implements AutowireCandidateResolver {

	/**
	 * {@code SimpleAutowireCandidateResolver} 的共享实例。
	 * @since 5.2.7
	 */
	public static final SimpleAutowireCandidateResolver INSTANCE = new SimpleAutowireCandidateResolver();


	@Override
	public boolean isAutowireCandidate(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
		return bdHolder.getBeanDefinition().isAutowireCandidate();
	}

	@Override
	public boolean isRequired(DependencyDescriptor descriptor) {
		return descriptor.isRequired();
	}

	@Override
	public boolean hasQualifier(DependencyDescriptor descriptor) {
		return false;
	}

	@Override
	@Nullable
	public Object getSuggestedValue(DependencyDescriptor descriptor) {
		return null;
	}

	@Override
	@Nullable
	public Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor, @Nullable String beanName) {
		return null;
	}

	/**
	 * 此实现按原样返回 {@code this}。
	 * @see #INSTANCE
	 */
	@Override
	public AutowireCandidateResolver cloneIfNecessary() {
		return this;
	}

}
