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

import org.springframework.core.Ordered;

/**
 * 由能够提供按 AspectJ 优先级规则排序通知/Advisor 所需信息的类型实现的接口。
 *
 * @author Adrian Colyer
 * @since 2.0
 * @see org.springframework.aop.aspectj.autoproxy.AspectJPrecedenceComparator
 */
public interface AspectJPrecedenceInformation extends Ordered {

	// Implementation note:
	// We need the level of indirection this interface provides as otherwise the
	// AspectJPrecedenceComparator must ask an Advisor for its Advice in all cases
	// in order to sort advisors. This causes problems with the
	// InstantiationModelAwarePointcutAdvisor which needs to delay creating
	// its advice for aspects with non-singleton instantiation models.

	/**
	 * 返回声明该通知的切面（Bean）的名称。
	 */
	String getAspectName();

	/**
	 * 返回通知成员在切面中的声明顺序。
	 */
	int getDeclarationOrder();

	/**
	 * 返回这是否为前置通知。
	 */
	boolean isBeforeAdvice();

	/**
	 * 返回这是否为后置通知。
	 */
	boolean isAfterAdvice();

}
