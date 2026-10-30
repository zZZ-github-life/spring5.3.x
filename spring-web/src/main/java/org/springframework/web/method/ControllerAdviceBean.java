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

package org.springframework.web.method;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.OrderUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * 封装 {@link ControllerAdvice @ControllerAdvice} 所标注的 Spring 托管 Bean
 * 的相关信息，而不必要求该 Bean 已被实例化。
 *
 * <p>可以使用 {@link #findAnnotatedBeans(ApplicationContext)} 方法发现此类 Bean。
 * 不过，{@code ControllerAdviceBean} 也可以由任意对象创建，
 * 包括未标注 {@code @ControllerAdvice} 的对象。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.2
 */
public class ControllerAdviceBean implements Ordered {

	/**
	 * 对实际 Bean 实例的引用，或表示 Bean 名称的 {@code String}。
	 */
	private final Object beanOrName;

	private final boolean isSingleton;

	/**
	 * 对已解析 Bean 实例的引用，可通过 {@code BeanFactory} 延迟获取。
	 */
	@Nullable
	private Object resolvedBean;

	@Nullable
	private final Class<?> beanType;

	private final HandlerTypePredicate beanTypePredicate;

	@Nullable
	private final BeanFactory beanFactory;

	@Nullable
	private Integer order;


	/**
	 * 使用给定的 Bean 实例创建 {@code ControllerAdviceBean}。
	 * @param bean Bean 实例
	 */
	public ControllerAdviceBean(Object bean) {
		Assert.notNull(bean, "Bean must not be null");
		this.beanOrName = bean;
		this.isSingleton = true;
		this.resolvedBean = bean;
		this.beanType = ClassUtils.getUserClass(bean.getClass());
		this.beanTypePredicate = createBeanTypePredicate(this.beanType);
		this.beanFactory = null;
	}

	/**
	 * 使用给定的 Bean 名称和 {@code BeanFactory} 创建 {@code ControllerAdviceBean}。
	 * @param beanName Bean 的名称
	 * @param beanFactory 用于最初获取 Bean 类型、
	 * 之后解析实际 Bean 的 {@code BeanFactory}
	 */
	public ControllerAdviceBean(String beanName, BeanFactory beanFactory) {
		this(beanName, beanFactory, null);
	}

	/**
	 * 使用给定的 Bean 名称、{@code BeanFactory} 以及
	 * {@link ControllerAdvice @ControllerAdvice} 注解创建 {@code ControllerAdviceBean}。
	 * @param beanName Bean 的名称
	 * @param beanFactory 用于最初获取 Bean 类型、
	 * 之后解析实际 Bean 的 {@code BeanFactory}
	 * @param controllerAdvice 该 Bean 的 {@code @ControllerAdvice} 注解，
	 * 如果尚未获取则为 {@code null}
	 * @since 5.2
	 */
	public ControllerAdviceBean(String beanName, BeanFactory beanFactory, @Nullable ControllerAdvice controllerAdvice) {
		Assert.hasText(beanName, "Bean name must contain text");
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		Assert.isTrue(beanFactory.containsBean(beanName), () -> "BeanFactory [" + beanFactory +
				"] does not contain specified controller advice bean '" + beanName + "'");

		this.beanOrName = beanName;
		this.isSingleton = beanFactory.isSingleton(beanName);
		this.beanType = getBeanType(beanName, beanFactory);
		this.beanTypePredicate = (controllerAdvice != null ? createBeanTypePredicate(controllerAdvice) :
				createBeanTypePredicate(this.beanType));
		this.beanFactory = beanFactory;
	}


	/**
	 * 获取所包含 Bean 的排序值。
	 * <p>自 Spring Framework 5.3 起，排序值按以下算法延迟获取并缓存。但请注意，
	 * 配置为作用域 Bean（例如请求作用域或会话作用域 Bean）的
	 * {@link ControllerAdvice @ControllerAdvice} Bean 不会被提前解析。
	 * 因此，对于作用域 {@code @ControllerAdvice} Bean，不会遵循 {@link Ordered} 的排序。
	 * <ul>
	 * <li>如果{@linkplain #resolveBean 已解析的 Bean}实现了 {@link Ordered}，
	 * 则使用 {@link Ordered#getOrder()} 返回的值。</li>
	 * <li>如果已知{@linkplain org.springframework.context.annotation.Bean 工厂方法}，
	 * 则使用 {@link OrderUtils#getOrder(AnnotatedElement)} 返回的值。
	 * <li>如果已知{@linkplain #getBeanType() Bean 类型}，则使用
	 * {@link OrderUtils#getOrder(Class, int)} 返回的值，并以
	 * {@link Ordered#LOWEST_PRECEDENCE} 作为默认排序值。</li>
	 * <li>否则使用 {@link Ordered#LOWEST_PRECEDENCE} 作为默认的后备排序值。</li>
	 * </ul>
	 * @see #resolveBean()
	 */
	@Override
	public int getOrder() {
		if (this.order == null) {
			String beanName = null;
			Object resolvedBean = null;
			if (this.beanFactory != null && this.beanOrName instanceof String) {
				beanName = (String) this.beanOrName;
				String targetBeanName = ScopedProxyUtils.getTargetBeanName(beanName);
				boolean isScopedProxy = this.beanFactory.containsBean(targetBeanName);
				// Avoid eager @ControllerAdvice bean resolution for scoped proxies,
				// since attempting to do so during context initialization would result
				// in an exception due to the current absence of the scope. For example,
				// an HTTP request or session scope is not active during initialization.
				if (!isScopedProxy && !ScopedProxyUtils.isScopedTarget(beanName)) {
					resolvedBean = resolveBean();
				}
			}
			else {
				resolvedBean = resolveBean();
			}

			if (resolvedBean instanceof Ordered) {
				this.order = ((Ordered) resolvedBean).getOrder();
			}
			else {
				if (beanName != null && this.beanFactory instanceof ConfigurableBeanFactory) {
					ConfigurableBeanFactory cbf = (ConfigurableBeanFactory) this.beanFactory;
					try {
						BeanDefinition bd = cbf.getMergedBeanDefinition(beanName);
						if (bd instanceof RootBeanDefinition) {
							Method factoryMethod = ((RootBeanDefinition) bd).getResolvedFactoryMethod();
							if (factoryMethod != null) {
								this.order = OrderUtils.getOrder(factoryMethod);
							}
						}
					}
					catch (NoSuchBeanDefinitionException ex) {
						// ignore -> probably a manually registered singleton
					}
				}
				if (this.order == null) {
					if (this.beanType != null) {
						this.order = OrderUtils.getOrder(this.beanType, Ordered.LOWEST_PRECEDENCE);
					}
					else {
						this.order = Ordered.LOWEST_PRECEDENCE;
					}
				}
			}
		}
		return this.order;
	}

	/**
	 * 返回所包含 Bean 的类型。
	 * <p>如果 Bean 类型是 CGLIB 生成的类，则返回原始的用户定义类。
	 */
	@Nullable
	public Class<?> getBeanType() {
		return this.beanType;
	}

	/**
	 * 获取此 {@code ControllerAdviceBean} 的 Bean 实例，必要时通过
	 * {@link BeanFactory} 解析 Bean 名称。
	 * <p>自 Spring Framework 5.2 起，Bean 实例一旦解析完成，若是单例则会被缓存，
	 * 从而避免在 {@code BeanFactory} 中重复查找。
	 */
	public Object resolveBean() {
		if (this.resolvedBean == null) {
			// this.beanOrName must be a String representing the bean name if
			// this.resolvedBean is null.
			Object resolvedBean = obtainBeanFactory().getBean((String) this.beanOrName);
			// Don't cache non-singletons (e.g., prototypes).
			if (!this.isSingleton) {
				return resolvedBean;
			}
			this.resolvedBean = resolvedBean;
		}
		return this.resolvedBean;
	}

	private BeanFactory obtainBeanFactory() {
		Assert.state(this.beanFactory != null, "No BeanFactory set");
		return this.beanFactory;
	}

	/**
	 * 检查给定的 Bean 类型是否应由此 {@code ControllerAdviceBean} 提供通知。
	 * @param beanType 要检查的 Bean 类型
	 * @since 4.0
	 * @see ControllerAdvice
	 */
	public boolean isApplicableToBeanType(@Nullable Class<?> beanType) {
		return this.beanTypePredicate.test(beanType);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ControllerAdviceBean)) {
			return false;
		}
		ControllerAdviceBean otherAdvice = (ControllerAdviceBean) other;
		return (this.beanOrName.equals(otherAdvice.beanOrName) && this.beanFactory == otherAdvice.beanFactory);
	}

	@Override
	public int hashCode() {
		return this.beanOrName.hashCode();
	}

	@Override
	public String toString() {
		return this.beanOrName.toString();
	}


	/**
	 * 在给定的 {@link ApplicationContext} 中查找标注了
	 * {@link ControllerAdvice @ControllerAdvice} 的 Bean，并将它们包装为
	 * {@code ControllerAdviceBean} 实例。
	 * <p>自 Spring Framework 5.2 起，返回列表中的 {@code ControllerAdviceBean}
	 * 实例会使用 {@link OrderComparator#sort(List)} 进行排序。
	 * @see #getOrder()
	 * @see OrderComparator
	 * @see Ordered
	 */
	public static List<ControllerAdviceBean> findAnnotatedBeans(ApplicationContext context) {
		ListableBeanFactory beanFactory = context;
		if (context instanceof ConfigurableApplicationContext) {
			// Use internal BeanFactory for potential downcast to ConfigurableBeanFactory above
			beanFactory = ((ConfigurableApplicationContext) context).getBeanFactory();
		}
		List<ControllerAdviceBean> adviceBeans = new ArrayList<>();
		for (String name : BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, Object.class)) {
			if (!ScopedProxyUtils.isScopedTarget(name)) {
				ControllerAdvice controllerAdvice = beanFactory.findAnnotationOnBean(name, ControllerAdvice.class);
				if (controllerAdvice != null) {
					// Use the @ControllerAdvice annotation found by findAnnotationOnBean()
					// in order to avoid a subsequent lookup of the same annotation.
					adviceBeans.add(new ControllerAdviceBean(name, beanFactory, controllerAdvice));
				}
			}
		}
		OrderComparator.sort(adviceBeans);
		return adviceBeans;
	}

	@Nullable
	private static Class<?> getBeanType(String beanName, BeanFactory beanFactory) {
		Class<?> beanType = beanFactory.getType(beanName);
		return (beanType != null ? ClassUtils.getUserClass(beanType) : null);
	}

	private static HandlerTypePredicate createBeanTypePredicate(@Nullable Class<?> beanType) {
		ControllerAdvice controllerAdvice = (beanType != null ?
				AnnotatedElementUtils.findMergedAnnotation(beanType, ControllerAdvice.class) : null);
		return createBeanTypePredicate(controllerAdvice);
	}

	private static HandlerTypePredicate createBeanTypePredicate(@Nullable ControllerAdvice controllerAdvice) {
		if (controllerAdvice != null) {
			return HandlerTypePredicate.builder()
					.basePackage(controllerAdvice.basePackages())
					.basePackageClass(controllerAdvice.basePackageClasses())
					.assignableType(controllerAdvice.assignableTypes())
					.annotation(controllerAdvice.annotations())
					.build();
		}
		return HandlerTypePredicate.forAnyHandlerType();
	}

}
