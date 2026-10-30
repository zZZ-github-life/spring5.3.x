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

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.lang.Nullable;

/**
 * 用于确定特定 Bean 定义是否符合特定依赖的自动装配候选条件的策略接口。
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 2.5
 */
public interface AutowireCandidateResolver {

	/**
	 * 确定给定 Bean 定义是否符合给定依赖的自动装配候选条件。<p>默认实现检查
	 * {@link org.springframework.beans.factory.config.BeanDefinition#isAutowireCandidate()}。
	 * @param bdHolder 包含 Bean 名称和别名的 Bean 定义
	 * @param descriptor 目标方法参数或字段的描述符
	 * @return 该 Bean 定义是否符合自动装配候选条件
	 * @see org.springframework.beans.factory.config.BeanDefinition#isAutowireCandidate()
	 */
	default boolean isAutowireCandidate(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
		return bdHolder.getBeanDefinition().isAutowireCandidate();
	}

	/**
	 * 确定给定描述符是否实际上是必需的。<p>默认实现检查 {@link DependencyDescriptor#isRequired()}。
	 * @param descriptor 目标方法参数或字段的描述符
	 * @return 该描述符是否标记为必需，
	 * 或以其他方式表示非必需状态（例如通过参数注解）
	 * @since 5.0
	 * @see DependencyDescriptor#isRequired()
	 */
	default boolean isRequired(DependencyDescriptor descriptor) {
		return descriptor.isRequired();
	}

	/**
	 * 确定给定描述符是否声明了类型之外的限定符（通常——但不一定——是特定类型的注解）。<p>默认实现返回
	 * {@code false}。
	 * @param descriptor 目标方法参数或字段的描述符
	 * @return 该描述符是否声明了限定符，
	 * 从而在类型匹配之外进一步收窄候选状态
	 * @since 5.1
	 * @see org.springframework.beans.factory.annotation.QualifierAnnotationAutowireCandidateResolver#hasQualifier
	 */
	default boolean hasQualifier(DependencyDescriptor descriptor) {
		return false;
	}

	/**
	 * 确定是否为给定依赖建议了默认值。<p>默认实现直接返回 {@code null}。
	 * @param descriptor 目标方法参数或字段的描述符
	 * @return 建议的值（通常是表达式 String），
	 * 若没有则为 {@code null}
	 * @since 3.0
	 */
	@Nullable
	default Object getSuggestedValue(DependencyDescriptor descriptor) {
		return null;
	}

	/**
	 * 如果注入点要求，构建用于延迟解析实际依赖目标的代理。<p>默认实现直接返回 {@code null}。
	 * @param descriptor 目标方法参数或字段的描述符
	 * @param beanName 包含该注入点的 Bean 的名称
	 * @return 实际依赖目标的延迟解析代理；
	 * 如果要执行直接解析则为 {@code null}
	 * @since 4.0
	 */
	@Nullable
	default Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor, @Nullable String beanName) {
		return null;
	}

	/**
	 * 如有必要，返回此解析器实例的克隆，保留其本地配置并允许克隆实例关联到新的 Bean 工厂；如果没有此类
	 * 状态，则返回此原始实例。<p>默认实现通过默认类构造函数创建一个单独的实例，假定没有需要复制的特定
	 * 配置状态。子类可以覆盖此方法，使用自定义配置状态处理或标准的 {@link Cloneable} 支持（如 Spring
	 * 自身的可配置 {@code AutowireCandidateResolver} 变体所实现的那样），或者直接返回 {@code this}（如
	 * {@link SimpleAutowireCandidateResolver} 中那样）。
	 * @since 5.2.7
	 * @see GenericTypeAwareAutowireCandidateResolver#cloneIfNecessary()
	 * @see DefaultListableBeanFactory#copyConfigurationFrom
	 */
	default AutowireCandidateResolver cloneIfNecessary() {
		return BeanUtils.instantiateClass(getClass());
	}

}
