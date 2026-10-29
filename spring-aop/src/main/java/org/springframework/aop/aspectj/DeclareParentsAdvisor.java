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

package org.springframework.aop.aspectj;

import org.aopalliance.aop.Advice;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.IntroductionInterceptor;
import org.springframework.aop.support.ClassFilters;
import org.springframework.aop.support.DelegatePerTargetObjectIntroductionInterceptor;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;

/**
 * 委托给给定对象的引入 Advisor。为 DeclareParents 注解实现 AspectJ 注解风格的行为。
 *
 * @author Rod Johnson
 * @author Ramnivas Laddad
 * @since 2.0
 */
public class DeclareParentsAdvisor implements IntroductionAdvisor {

	private final Advice advice;

	private final Class<?> introducedInterface;

	private final ClassFilter typePatternClassFilter;


	/**
	 * 为这个 DeclareParents 字段创建一个新的 Advisor。
	 * @param interfaceType static field defining the introduction
	 * @param typePattern type pattern the introduction is restricted to
	 * @param defaultImpl the default implementation class
	 */
	public DeclareParentsAdvisor(Class<?> interfaceType, String typePattern, Class<?> defaultImpl) {
		this(interfaceType, typePattern,
				new DelegatePerTargetObjectIntroductionInterceptor(defaultImpl, interfaceType));
	}

	/**
	 * 为这个 DeclareParents 字段创建一个新的 Advisor。
	 * @param interfaceType static field defining the introduction
	 * @param typePattern type pattern the introduction is restricted to
	 * @param delegateRef the delegate implementation object
	 */
	public DeclareParentsAdvisor(Class<?> interfaceType, String typePattern, Object delegateRef) {
		this(interfaceType, typePattern, new DelegatingIntroductionInterceptor(delegateRef));
	}

	/**
	 * 私有构造函数，用于在基于 impl 的委托和基于引用的委托之间共享公共代码（由于使用了 final 字段，无
	 * 法使用 init() 之类的方法来共享公共代码）。
	 * @param interfaceType static field defining the introduction
	 * @param typePattern type pattern the introduction is restricted to
	 * @param interceptor the delegation advice as {@link IntroductionInterceptor}
	 */
	private DeclareParentsAdvisor(Class<?> interfaceType, String typePattern, IntroductionInterceptor interceptor) {
		this.advice = interceptor;
		this.introducedInterface = interfaceType;

		// Excludes methods implemented.
		ClassFilter typePatternFilter = new TypePatternClassFilter(typePattern);
		ClassFilter exclusion = (clazz -> !this.introducedInterface.isAssignableFrom(clazz));
		this.typePatternClassFilter = ClassFilters.intersection(typePatternFilter, exclusion);
	}


	@Override
	public ClassFilter getClassFilter() {
		return this.typePatternClassFilter;
	}

	@Override
	public void validateInterfaces() throws IllegalArgumentException {
		// Do nothing
	}

	@Override
	public boolean isPerInstance() {
		return true;
	}

	@Override
	public Advice getAdvice() {
		return this.advice;
	}

	@Override
	public Class<?>[] getInterfaces() {
		return new Class<?>[] {this.introducedInterface};
	}

}
