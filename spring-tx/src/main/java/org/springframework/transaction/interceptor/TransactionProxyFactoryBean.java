/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.transaction.interceptor;

import java.util.Properties;

import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.AbstractSingletonProxyFactoryBean;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 用于简化声明式事务处理的代理工厂bean。
 * 这是标准AOP {@link org.springframework.aop.framework.ProxyFactoryBean}
 * 配合独立{@link TransactionInterceptor}定义的便捷替代方案。
 *
 * <p><strong>历史说明：</strong>此类最初旨在覆盖声明式事务划分的典型场景：即用一个事务代理包装单例目标对象，并代理该目标实现的所有接口。然而，从 Spring 2.0 及更高版本开始，此处提供的功能已被更方便的 {@code tx:} XML 命名空间取代。有关在 Spring 应用中管理事务的现代方案，请参见 Spring 参考文档中的<a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/data-access.html#transaction-declarative">声明式事务管理</a>一节。因此，<strong>用户应优先使用 {@code tx:} XML 命名空间，以及 @{@link org.springframework.transaction.annotation.Transactional Transactional} 和 @{@link org.springframework.transaction.annotation.EnableTransactionManagement EnableTransactionManagement} 注解。</strong>
 *
 * <p>需要指定三个主要属性：
 * <ul>
 * <li>"transactionManager"：要使用的 {@link PlatformTransactionManager} 实现（例如 {@link org.springframework.transaction.jta.JtaTransactionManager} 实例）
 * <li>"target"：应为其创建事务代理的目标对象
 * <li>"transactionAttributes"：针对每个目标方法名（或方法名模式）的事务属性（例如传播行为和 "readOnly" 标志）
 * </ul>
 *
 * <p>如果未显式设置 "transactionManager" 属性，并且此 {@link FactoryBean} 运行在 {@link ListableBeanFactory} 中，则会从 {@link BeanFactory} 中获取唯一匹配的 {@link PlatformTransactionManager} 类型 Bean。
 *
 * <p>与 {@link TransactionInterceptor} 不同，事务属性以属性形式指定，方法名作为键，事务属性描述符作为值。方法名始终应用于目标类。
 *
 * <p>内部会使用一个 {@link TransactionInterceptor} 实例，但此类的使用者无需关心。可以选择指定方法切点，以对底层 {@link TransactionInterceptor} 进行条件调用。
 *
 * <p>可以设置 "preInterceptors" 和 "postInterceptors" 属性，向其中添加额外的拦截器，例如 {@link org.springframework.aop.interceptor.PerformanceMonitorInterceptor}。
 *
 * <p><b>提示：</b>此类常与父子 Bean 定义一起使用。通常，你会在抽象的父 Bean 定义中定义事务管理器和默认事务属性（针对方法名模式），然后为特定目标对象派生具体的子 Bean 定义。这样可以将每个 Bean 的定义工作量降到最低。
 *
 * <pre class="code">
 * &lt;bean id="baseTransactionProxy" class="org.springframework.transaction.interceptor.TransactionProxyFactoryBean"
 *     abstract="true"&gt;
 *   &lt;property name="transactionManager" ref="transactionManager"/&gt;
 *   &lt;property name="transactionAttributes"&gt;
 *     &lt;props&gt;
 *       &lt;prop key="insert*"&gt;PROPAGATION_REQUIRED&lt;/prop&gt;
 *       &lt;prop key="update*"&gt;PROPAGATION_REQUIRED&lt;/prop&gt;
 *       &lt;prop key="*"&gt;PROPAGATION_REQUIRED,readOnly&lt;/prop&gt;
 *     &lt;/props&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="myProxy" parent="baseTransactionProxy"&gt;
 *   &lt;property name="target" ref="myTarget"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="yourProxy" parent="baseTransactionProxy"&gt;
 *   &lt;property name="target" ref="yourTarget"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * @author Juergen Hoeller
 * @author Dmitriy Kopylenko
 * @author Rod Johnson
 * @author Chris Beams
 * @since 21.08.2003
 * @see #setTransactionManager
 * @see #setTarget
 * @see #setTransactionAttributes
 * @see TransactionInterceptor
 * @see org.springframework.aop.framework.ProxyFactoryBean
 */
@SuppressWarnings("serial")
public class TransactionProxyFactoryBean extends AbstractSingletonProxyFactoryBean
		implements BeanFactoryAware {

	private final TransactionInterceptor transactionInterceptor = new TransactionInterceptor();

	@Nullable
	private Pointcut pointcut;


	/**
	 * 设置默认事务管理器。这将执行实际的事务管理: 此类只是一种调用它的方式。
	 * @see TransactionInterceptor#setTransactionManager
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionInterceptor.setTransactionManager(transactionManager);
	}

	/**
	 * 设置属性, 以方法名为键, 以事务属性描述符(通过TransactionAttributeEditor解析)为值:
	 * 例如 key = "myMethod", value = "PROPAGATION_REQUIRED,readOnly"。
	 * <p>注意: 方法名始终应用于目标类, 无论在接口中还是类本身中定义。
	 * <p>内部将根据给定属性创建NameMatchTransactionAttributeSource。
	 * @see #setTransactionAttributeSource
	 * @see TransactionInterceptor#setTransactionAttributes
	 * @see TransactionAttributeEditor
	 * @see NameMatchTransactionAttributeSource
	 */
	public void setTransactionAttributes(Properties transactionAttributes) {
		this.transactionInterceptor.setTransactionAttributes(transactionAttributes);
	}

	/**
	 * 设置用于查找事务属性的事务属性源。如果指定字符串属性值,
	 * PropertyEditor将从该值创建MethodMapTransactionAttributeSource。
	 * @see #setTransactionAttributes
	 * @see TransactionInterceptor#setTransactionAttributeSource
	 * @see TransactionAttributeSourceEditor
	 * @see MethodMapTransactionAttributeSource
	 * @see NameMatchTransactionAttributeSource
	 * @see org.springframework.transaction.annotation.AnnotationTransactionAttributeSource
	 */
	public void setTransactionAttributeSource(TransactionAttributeSource transactionAttributeSource) {
		this.transactionInterceptor.setTransactionAttributeSource(transactionAttributeSource);
	}

	/**
	 * 设置切点, 即一个bean, 可以根据传递的方法和属性有条件地调用TransactionInterceptor。
	 * 注意: 额外的拦截器始终会被调用。
	 * @see #setPreInterceptors
	 * @see #setPostInterceptors
	 */
	public void setPointcut(Pointcut pointcut) {
		this.pointcut = pointcut;
	}

	/**
	 * 此回调是可选的: 如果在BeanFactory中运行且未显式设置事务管理器,
	 * 将从BeanFactory中获取单个匹配的{@link PlatformTransactionManager}类型的bean。
	 * @see org.springframework.beans.factory.BeanFactory#getBean(Class)
	 * @see org.springframework.transaction.PlatformTransactionManager
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.transactionInterceptor.setBeanFactory(beanFactory);
	}


	/**
	 * 为此FactoryBean的TransactionInterceptor创建一个advisor。
	 */
	@Override
	protected Object createMainInterceptor() {
		this.transactionInterceptor.afterPropertiesSet();
		if (this.pointcut != null) {
			return new DefaultPointcutAdvisor(this.pointcut, this.transactionInterceptor);
		}
		else {
			// Rely on default pointcut.
			return new TransactionAttributeSourceAdvisor(this.transactionInterceptor);
		}
	}

	/**
	 * 从4.2开始, 此方法将{@link TransactionalProxy}添加到代理接口集合中,
	 * 以避免重新处理事务元数据。
	 */
	@Override
	protected void postProcessProxyFactory(ProxyFactory proxyFactory) {
		proxyFactory.addInterface(TransactionalProxy.class);
	}

}
