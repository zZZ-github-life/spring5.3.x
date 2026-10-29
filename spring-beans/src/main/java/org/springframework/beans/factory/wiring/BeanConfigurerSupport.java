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

package org.springframework.beans.factory.wiring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 用于 Bean 配置器的便捷基类，可对对象执行依赖注入（无论它们如何创建）。通常由 AspectJ 切面继承。
 *
 * <p>子类可能还需要在 {@link BeanWiringInfoResolver} 接口中提供自定义的元数据解析策略。默认实现会
 * 查找与完全限定类名同名的 Bean。（如果未使用 '{@code id}' 属性，这就是 Spring XML 文件中 Bean 的
 * 默认名称。）

 * @author Rob Harrop
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Adrian Colyer
 * @since 2.0
 * @see #setBeanWiringInfoResolver
 * @see ClassNameBeanWiringInfoResolver
 */
public class BeanConfigurerSupport implements BeanFactoryAware, InitializingBean, DisposableBean {

	/** 可供子类使用的日志记录器。 */
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private volatile BeanWiringInfoResolver beanWiringInfoResolver;

	@Nullable
	private volatile ConfigurableListableBeanFactory beanFactory;


	/**
	 * 设置要使用的 {@link BeanWiringInfoResolver}。<p>默认行为是查找与类同名的 Bean。作为替代方案，可
	 * 以考虑使用注解驱动的 Bean 装配。
	 * @see ClassNameBeanWiringInfoResolver
	 * @see org.springframework.beans.factory.annotation.AnnotationBeanWiringInfoResolver
	 */
	public void setBeanWiringInfoResolver(BeanWiringInfoResolver beanWiringInfoResolver) {
		Assert.notNull(beanWiringInfoResolver, "BeanWiringInfoResolver must not be null");
		this.beanWiringInfoResolver = beanWiringInfoResolver;
	}

	/**
	 * 设置该切面必须配置 Bean 的 {@link BeanFactory}。
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
			throw new IllegalArgumentException(
				"Bean configurer aspect needs to run in a ConfigurableListableBeanFactory: " + beanFactory);
		}
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
		if (this.beanWiringInfoResolver == null) {
			this.beanWiringInfoResolver = createDefaultBeanWiringInfoResolver();
		}
	}

	/**
	 * 创建在未显式指定时使用的默认 BeanWiringInfoResolver。<p>默认实现会构建
	 * {@link ClassNameBeanWiringInfoResolver}。
	 * @return the default BeanWiringInfoResolver (never {@code null})
	 */
	@Nullable
	protected BeanWiringInfoResolver createDefaultBeanWiringInfoResolver() {
		return new ClassNameBeanWiringInfoResolver();
	}

	/**
	 * 检查是否已设置 {@link BeanFactory}。
	 */
	@Override
	public void afterPropertiesSet() {
		Assert.notNull(this.beanFactory, "BeanFactory must be set");
	}

	/**
	 * 在容器销毁时释放对 {@link BeanFactory} 和 {@link BeanWiringInfoResolver} 的引用。
	 */
	@Override
	public void destroy() {
		this.beanFactory = null;
		this.beanWiringInfoResolver = null;
	}


	/**
	 * 配置 Bean 实例。<p>子类可以覆盖此方法以提供自定义配置逻辑。通常由切面对切点匹配的所有 Bean 实例
	 * 调用。
	 * @param beanInstance the bean instance to configure (must <b>not</b> be {@code null})
	 */
	public void configureBean(Object beanInstance) {
		if (this.beanFactory == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("BeanFactory has not been set on " + ClassUtils.getShortName(getClass()) + ": " +
						"Make sure this configurer runs in a Spring container. Unable to configure bean of type [" +
						ClassUtils.getDescriptiveType(beanInstance) + "]. Proceeding without injection.");
			}
			return;
		}

		BeanWiringInfoResolver bwiResolver = this.beanWiringInfoResolver;
		Assert.state(bwiResolver != null, "No BeanWiringInfoResolver available");
		BeanWiringInfo bwi = bwiResolver.resolveWiringInfo(beanInstance);
		if (bwi == null) {
			// Skip the bean if no wiring info given.
			return;
		}


		ConfigurableListableBeanFactory beanFactory = this.beanFactory;
		Assert.state(beanFactory != null, "No BeanFactory available");
		try {
			String beanName = bwi.getBeanName();
			if (bwi.indicatesAutowiring() || (bwi.isDefaultBeanName() && beanName != null &&
					!beanFactory.containsBean(beanName))) {
				// Perform autowiring (also applying standard factory / post-processor callbacks).
				beanFactory.autowireBeanProperties(beanInstance, bwi.getAutowireMode(), bwi.getDependencyCheck());
				beanFactory.initializeBean(beanInstance, (beanName != null ? beanName : ""));
			}
			else {
				// Perform explicit wiring based on the specified bean definition.
				beanFactory.configureBean(beanInstance, (beanName != null ? beanName : ""));
			}
		}
		catch (BeanCreationException ex) {
			Throwable rootCause = ex.getMostSpecificCause();
			if (rootCause instanceof BeanCurrentlyInCreationException) {
				BeanCreationException bce = (BeanCreationException) rootCause;
				String bceBeanName = bce.getBeanName();
				if (bceBeanName != null && beanFactory.isCurrentlyInCreation(bceBeanName)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Failed to create target bean '" + bce.getBeanName() +
								"' while configuring object of type [" + beanInstance.getClass().getName() +
								"] - probably due to a circular reference. This is a common startup situation " +
								"and usually not fatal. Proceeding without injection. Original exception: " + ex);
					}
					return;
				}
			}
			throw ex;
		}
	}

}
