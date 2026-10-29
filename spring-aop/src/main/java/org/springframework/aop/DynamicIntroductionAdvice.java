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

package org.springframework.aop;

import org.aopalliance.aop.Advice;

/**
 * AOP Alliance Advice 的子接口，允许通知实现额外的接口，并通过使用该拦截器的代理提供这些接口。这是
 * 一个称为<b>引入</b>的基本 AOP 概念。
 *
 * <p>引入通常是<b>mixin</b>，能够构建复合对象，从而实现 Java 中多重继承的许多目标。
 *
 * <p>与 {@link IntroductionInfo} 相比，此接口允许通知实现一组不必事先已知的接口。因此，可以使用
 * {@link IntroductionAdvisor} 来指定将在被通知对象中暴露哪些接口。
 *
 * @author Rod Johnson
 * @since 1.1.1
 * @see IntroductionInfo
 * @see IntroductionAdvisor
 */
public interface DynamicIntroductionAdvice extends Advice {

	/**
	 * 此引入通知是否实现了给定接口？
	 * @param intf the interface to check
	 * @return whether the advice implements the specified interface
	 */
	boolean implementsInterface(Class<?> intf);

}
