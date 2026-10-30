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

package org.springframework.web.jsf;

import java.util.Collection;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.web.context.WebApplicationContext;

/**
 * JSF PhaseListener 实现，委托给来自 Spring 根 WebApplicationContext 的一个或多个
 * Spring 管理的 PhaseListener Bean。
 *
 * <p>在 {@code faces-config.xml} 文件中按如下方式配置此监听器多播器：
 *
 * <pre class="code">
 * &lt;application&gt;
 *   ...
 *   &lt;phase-listener&gt;
 *     org.springframework.web.jsf.DelegatingPhaseListenerMulticaster
 *   &lt;/phase-listener&gt;
 *   ...
 * &lt;/application&gt;</pre>
 *
 * 该多播器会将所有 {@code beforePhase} 和 {@code afterPhase} 事件委托给所有目标
 * PhaseListener Bean。默认情况下，这些 Bean 只是按类型获取：Spring 根
 * WebApplicationContext 中所有实现 PhaseListener 接口的 Bean 都会被获取并调用。
 *
 * <p>注意：此多播器的 {@code getPhaseId()} 方法将始终返回 {@code ANY_PHASE}。
 * <b>目标监听器 Bean 所暴露的阶段 id 将被忽略；所有事件都会传播给所有监听器。</b>
 *
 * <p>可以对此多播器进行子类化，以更改获取监听器 Bean 的策略，
 * 或更改访问 ApplicationContext 的策略（通常通过
 * {@link FacesContextUtils#getWebApplicationContext(FacesContext)} 获取）。
 *
 * @author Juergen Hoeller
 * @author Colin Sampaleanu
 * @since 1.2.7
 */
@SuppressWarnings("serial")
public class DelegatingPhaseListenerMulticaster implements PhaseListener {

	@Override
	public PhaseId getPhaseId() {
		return PhaseId.ANY_PHASE;
	}

	@Override
	public void beforePhase(PhaseEvent event) {
		for (PhaseListener listener : getDelegates(event.getFacesContext())) {
			listener.beforePhase(event);
		}
	}

	@Override
	public void afterPhase(PhaseEvent event) {
		for (PhaseListener listener : getDelegates(event.getFacesContext())) {
			listener.afterPhase(event);
		}
	}


	/**
	 * 从 Spring 根 WebApplicationContext 获取委托的 PhaseListener Bean。
	 * @param facesContext 当前的 JSF 上下文
	 * @return PhaseListener 对象的 Collection
	 * @see #getBeanFactory
	 * @see org.springframework.beans.factory.ListableBeanFactory#getBeansOfType(Class)
	 */
	protected Collection<PhaseListener> getDelegates(FacesContext facesContext) {
		ListableBeanFactory bf = getBeanFactory(facesContext);
		return BeanFactoryUtils.beansOfTypeIncludingAncestors(bf, PhaseListener.class, true, false).values();
	}

	/**
	 * 获取用于委托 Bean 名称解析的 Spring BeanFactory。
	 * <p>默认实现委托给 {@code getWebApplicationContext}。
	 * 可以重写此方法以提供一个任意的 ListableBeanFactory 引用用于解析；
	 * 通常这将是一个完整的 Spring ApplicationContext。
	 * @param facesContext 当前的 JSF 上下文
	 * @return Spring ListableBeanFactory（永不为 {@code null}）
	 * @see #getWebApplicationContext
	 */
	protected ListableBeanFactory getBeanFactory(FacesContext facesContext) {
		return getWebApplicationContext(facesContext);
	}

	/**
	 * 获取用于委托 Bean 名称解析的 Web 应用上下文。
	 * <p>默认实现委托给 FacesContextUtils。
	 * @param facesContext 当前的 JSF 上下文
	 * @return Spring Web 应用上下文（永不为 {@code null}）
	 * @see FacesContextUtils#getRequiredWebApplicationContext
	 */
	protected WebApplicationContext getWebApplicationContext(FacesContext facesContext) {
		return FacesContextUtils.getRequiredWebApplicationContext(facesContext);
	}

}
