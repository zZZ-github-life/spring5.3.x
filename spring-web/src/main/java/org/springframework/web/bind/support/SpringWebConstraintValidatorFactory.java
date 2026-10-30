/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.bind.support;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;

import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

/**
 * JSR-303 {@link ConstraintValidatorFactory} 实现，委托给当前的 Spring
 * {@link WebApplicationContext} 来创建自动装配的 {@link ConstraintValidator} 实例。
 *
 * <p>与
 * {@link org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory}
 * 不同，此变体旨在用于标准 {@code validation.xml} 文件中的声明式使用，
 * 例如与 JAX-RS 或 JAX-WS 结合使用。
 *
 * @author Juergen Hoeller
 * @since 4.2.1
 * @see ContextLoader#getCurrentWebApplicationContext()
 * @see org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory
 */
public class SpringWebConstraintValidatorFactory implements ConstraintValidatorFactory {

	@Override
	public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
		return getWebApplicationContext().getAutowireCapableBeanFactory().createBean(key);
	}

	// Bean Validation 1.1 releaseInstance method
	@Override
	public void releaseInstance(ConstraintValidator<?, ?> instance) {
		getWebApplicationContext().getAutowireCapableBeanFactory().destroyBean(instance);
	}


	/**
	 * 检索要使用的 Spring {@link WebApplicationContext}。
	 * 默认实现返回为线程上下文类加载器注册的当前 {@link WebApplicationContext}。
	 * @return 当前的 WebApplicationContext（永不为 {@code null}）
	 * @see ContextLoader#getCurrentWebApplicationContext()
	 */
	protected WebApplicationContext getWebApplicationContext() {
		WebApplicationContext wac = ContextLoader.getCurrentWebApplicationContext();
		if (wac == null) {
			throw new IllegalStateException("No WebApplicationContext registered for current thread - " +
					"consider overriding SpringWebConstraintValidatorFactory.getWebApplicationContext()");
		}
		return wac;
	}

}
