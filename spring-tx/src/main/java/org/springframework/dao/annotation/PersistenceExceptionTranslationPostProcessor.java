/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.dao.annotation;

import java.lang.annotation.Annotation;

import org.springframework.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

/**
 * Bean 后处理器，自动将持久化异常翻译应用于任何标记了
 * Spring 的 @{@link org.springframework.stereotype.Repository Repository}
 * 注解的 Bean，将相应的 {@link PersistenceExceptionTranslationAdvisor}
 * 添加到暴露的代理中（无论是现有的 AOP 代理还是新生成的实现目标所有接口的代理）。
 *
 * <p>将原生资源异常翻译为 Spring 的
 * {@link org.springframework.dao.DataAccessException DataAccessException} 层次结构。
 * 自动检测实现了
 * {@link org.springframework.dao.support.PersistenceExceptionTranslator
 * PersistenceExceptionTranslator} 接口的 Bean，随后要求这些 Bean 翻译候选异常。
 *
 * <p>Spring 所有适用的资源工厂（例如
 * {@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean}）
 * 都开箱即用地实现了 {@code PersistenceExceptionTranslator} 接口。
 * 因此，启用自动异常翻译通常只需要在所有受影响的 Bean
 * （如 Repository 或 DAO）上标记 {@code @Repository} 注解，
 * 并在应用上下文中定义此后处理器为 Bean。
 *
 * <p>从 5.3 开始，{@code PersistenceExceptionTranslator} Bean 将按照
 * Spring 的依赖排序规则进行排序：参见 {@link org.springframework.core.Ordered}
 * 和 {@link org.springframework.core.annotation.Order}。请注意，从 5.3 版本开始，
 * 此类 Bean 将从任何作用域中检索，而不仅仅是单例作用域。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see PersistenceExceptionTranslationAdvisor
 * @see org.springframework.stereotype.Repository
 * @see org.springframework.dao.DataAccessException
 * @see org.springframework.dao.support.PersistenceExceptionTranslator
 */
@SuppressWarnings("serial")
public class PersistenceExceptionTranslationPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor {

	private Class<? extends Annotation> repositoryAnnotationType = Repository.class;


	/**
	 * 设置 'repository' 注解类型。
	 * 默认的 repository 注解类型是 {@link Repository} 注解。
	 * <p>此 setter 属性的存在使开发者可以提供自己的
	 * （非 Spring 特定的）注解类型来指示一个类具有仓储角色。
	 * @param repositoryAnnotationType 所需的注解类型
	 */
	public void setRepositoryAnnotationType(Class<? extends Annotation> repositoryAnnotationType) {
		Assert.notNull(repositoryAnnotationType, "'repositoryAnnotationType' must not be null");
		this.repositoryAnnotationType = repositoryAnnotationType;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		super.setBeanFactory(beanFactory);

		if (!(beanFactory instanceof ListableBeanFactory)) {
			throw new IllegalArgumentException(
					"Cannot use PersistenceExceptionTranslator autodetection without ListableBeanFactory");
		}
		this.advisor = new PersistenceExceptionTranslationAdvisor(
				(ListableBeanFactory) beanFactory, this.repositoryAnnotationType);
	}

}
