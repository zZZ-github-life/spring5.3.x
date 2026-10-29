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

package org.springframework.validation.beanvalidation;

import java.lang.annotation.Annotation;

import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

/**
 * 一个便捷的 {@link BeanPostProcessor} 实现，委托给 JSR-303 提供者对带注解的方法执行方法级校验。
 *
 * <p>适用的方法在其参数和/或返回值上带有 JSR-303 约束注解（后者在方法级别指定，通常以内联注解形式
 * 给出），例如：
 *
 * <pre class="code">
 * public @NotNull Object myValidMethod(@NotNull String arg1, @Max(10) int arg2)
 * </pre>
 *
 * <p>包含此类带注解方法的目标类需要在类型级别使用 Spring 的 {@link Validated} 注解标注，其方法才会
 * 被搜索内联约束注解。校验分组同样可以通过 {@code @Validated} 指定。默认情况下，JSR-303 仅针对其默
 * 认分组进行校验。
 *
 * <p>从 Spring 5.0 起，此功能需要 Bean Validation 1.1+ 提供者。
 *
 * @author Juergen Hoeller
 * @since 3.1
 * @see MethodValidationInterceptor
 * @see javax.validation.executable.ExecutableValidator
 */
@SuppressWarnings("serial")
public class MethodValidationPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor
		implements InitializingBean {

	private Class<? extends Annotation> validatedAnnotationType = Validated.class;

	@Nullable
	private Validator validator;


	/**
	 * 设置 'validated' 注解类型。默认的 validated 注解类型是 {@link Validated} 注解。
	 * <p>此 setter 属性之所以存在，是为了让开发者可以提供自己的（非 Spring 特有的）注解类型，用以表明
	 * 某个类应按照应用方法校验的方式接受校验。
	 * @param validatedAnnotationType the desired annotation type
	 */
	public void setValidatedAnnotationType(Class<? extends Annotation> validatedAnnotationType) {
		Assert.notNull(validatedAnnotationType, "'validatedAnnotationType' must not be null");
		this.validatedAnnotationType = validatedAnnotationType;
	}

	/**
	 * 设置用于校验方法的 JSR-303 Validator 委托对象。
	 * <p>默认是默认 ValidatorFactory 的默认 Validator。
	 */
	public void setValidator(Validator validator) {
		// Unwrap to the native Validator with forExecutables support
		if (validator instanceof LocalValidatorFactoryBean) {
			this.validator = ((LocalValidatorFactoryBean) validator).getValidator();
		}
		else if (validator instanceof SpringValidatorAdapter) {
			this.validator = validator.unwrap(Validator.class);
		}
		else {
			this.validator = validator;
		}
	}

	/**
	 * 设置用于校验方法的 JSR-303 ValidatorFactory 委托对象，使用其默认 Validator。
	 * <p>默认是默认 ValidatorFactory 的默认 Validator。
	 * @see javax.validation.ValidatorFactory#getValidator()
	 */
	public void setValidatorFactory(ValidatorFactory validatorFactory) {
		this.validator = validatorFactory.getValidator();
	}


	@Override
	public void afterPropertiesSet() {
		Pointcut pointcut = new AnnotationMatchingPointcut(this.validatedAnnotationType, true);
		this.advisor = new DefaultPointcutAdvisor(pointcut, createMethodValidationAdvice(this.validator));
	}

	/**
	 * 创建用于方法校验目的的 AOP 通知，以便通过针对指定 'validated' 注解的切点来应用。
	 * @param validator the JSR-303 Validator to delegate to
	 * @return the interceptor to use (typically, but not necessarily,
	 * a {@link MethodValidationInterceptor} or subclass thereof)
	 * @since 4.2
	 */
	protected Advice createMethodValidationAdvice(@Nullable Validator validator) {
		return (validator != null ? new MethodValidationInterceptor(validator) : new MethodValidationInterceptor());
	}

}
