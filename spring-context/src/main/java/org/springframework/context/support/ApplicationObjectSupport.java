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

package org.springframework.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 适用于希望感知应用上下文的应用对象的便捷超类，例如用于自定义查找协作 Bean 或进行特定于上下文的资
 * 源访问。它保存应用上下文引用并提供初始化回调方法。此外，它还提供了许多用于消息查找的便捷方法。
 *
 * <p>并不要求继承此类：如果你需要访问上下文（例如访问文件资源或消息源），它只是让事情稍微简单一些。
 * 请注意，许多应用对象根本不需要感知应用上下文，因为它们可以通过 Bean 引用接收协作 Bean。
 *
 * <p>许多框架类都派生自此类，尤其是在 Web 支持中。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.web.context.support.WebApplicationObjectSupport
 */
public abstract class ApplicationObjectSupport implements ApplicationContextAware {

	/** 可供子类使用的日志记录器。 */
	protected final Log logger = LogFactory.getLog(getClass());

	/** 此对象运行所在的 ApplicationContext。 */
	@Nullable
	private ApplicationContext applicationContext;

	/** 用于便捷访问消息的 MessageSourceAccessor。 */
	@Nullable
	private MessageSourceAccessor messageSourceAccessor;


	@Override
	public final void setApplicationContext(@Nullable ApplicationContext context) throws BeansException {
		if (context == null && !isContextRequired()) {
			// Reset internal context state.
			this.applicationContext = null;
			this.messageSourceAccessor = null;
		}
		else if (this.applicationContext == null) {
			// Initialize with passed-in context.
			if (!requiredContextClass().isInstance(context)) {
				throw new ApplicationContextException(
						"Invalid application context: needs to be of type [" + requiredContextClass().getName() + "]");
			}
			this.applicationContext = context;
			this.messageSourceAccessor = new MessageSourceAccessor(context);
			initApplicationContext(context);
		}
		else {
			// Ignore reinitialization if same context passed in.
			if (this.applicationContext != context) {
				throw new ApplicationContextException(
						"Cannot reinitialize with different application context: current one is [" +
						this.applicationContext + "], passed-in one is [" + context + "]");
			}
		}
	}

	/**
	 * 确定此应用对象是否需要在 ApplicationContext 中运行。
	 *
	 * <p>默认值为 "false"。可以重写以强制在上下文中运行（即如果在上下文之外访问访问器，则抛出
	 * IllegalStateException）。
	 * @see #getApplicationContext
	 * @see #getMessageSourceAccessor
	 */
	protected boolean isContextRequired() {
		return false;
	}

	/**
	 * 确定传递给 {@code setApplicationContext} 的任何上下文必须属于哪个上下文类。可以在子类中重写。
	 * @see #setApplicationContext
	 */
	protected Class<?> requiredContextClass() {
		return ApplicationContext.class;
	}

	/**
	 * 子类可以重写此方法以实现自定义初始化行为。在设置上下文实例后由 {@code setApplicationContext} 调
	 * 用。
	 *
	 * <p>注意：在重新初始化上下文时<i>不会</i>调用，而只在此对象的上下文引用首次初始化时调用。
	 *
	 * <p>默认实现调用不带 ApplicationContext 引用的重载 {@link #initApplicationContext()} 方法。
	 * @param context 包含它的 ApplicationContext
	 * @throws ApplicationContextException 如果发生初始化错误
	 * @throws BeansException 如果由 ApplicationContext 的方法抛出
	 * @see #setApplicationContext
	 */
	protected void initApplicationContext(ApplicationContext context) throws BeansException {
		initApplicationContext();
	}

	/**
	 * 子类可以重写此方法以实现自定义初始化行为。
	 *
	 * <p>默认实现为空。由
	 * {@link #initApplicationContext(org.springframework.context.ApplicationContext)} 调用。
	 * @throws ApplicationContextException 如果发生初始化错误
	 * @throws BeansException 如果由 ApplicationContext 的方法抛出
	 * @see #setApplicationContext
	 */
	protected void initApplicationContext() throws BeansException {
	}


	/**
	 * 返回此对象所关联的 ApplicationContext。
	 * @throws IllegalStateException 如果未运行在 ApplicationContext 中
	 */
	@Nullable
	public final ApplicationContext getApplicationContext() throws IllegalStateException {
		if (this.applicationContext == null && isContextRequired()) {
			throw new IllegalStateException(
					"ApplicationObjectSupport instance [" + this + "] does not run in an ApplicationContext");
		}
		return this.applicationContext;
	}

	/**
	 * 获取供实际使用的 ApplicationContext。
	 * @return ApplicationContext（永不为 {@code null}）
	 * @throws IllegalStateException 如果未设置 ApplicationContext
	 * @since 5.0
	 */
	protected final ApplicationContext obtainApplicationContext() {
		ApplicationContext applicationContext = getApplicationContext();
		Assert.state(applicationContext != null, "No ApplicationContext");
		return applicationContext;
	}

	/**
	 * 返回此对象使用的应用上下文的 MessageSourceAccessor，以便轻松访问消息。
	 * @throws IllegalStateException 如果未运行在 ApplicationContext 中
	 */
	@Nullable
	protected final MessageSourceAccessor getMessageSourceAccessor() throws IllegalStateException {
		if (this.messageSourceAccessor == null && isContextRequired()) {
			throw new IllegalStateException(
					"ApplicationObjectSupport instance [" + this + "] does not run in an ApplicationContext");
		}
		return this.messageSourceAccessor;
	}

}
