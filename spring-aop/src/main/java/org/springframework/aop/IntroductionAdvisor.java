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

package org.springframework.aop;

/**
 * 执行一个或多个 AOP <b>引入</b>的 Advisor 的父接口。
 *
 * <p>此接口不能直接实现；子接口必须提供实现引入的通知类型。
 *
 * <p>引入是通过 AOP 通知实现额外的接口（目标未实现的接口）。
 *
 * @author Rod Johnson
 * @since 04.04.2003
 * @see IntroductionInterceptor
 */
public interface IntroductionAdvisor extends Advisor, IntroductionInfo {

	/**
	 * 返回确定此引入应应用于哪些目标类的过滤器。
	 * <p>这表示切点的类部分。请注意，方法匹配对引入没有意义。
	 * @return the class filter
	 */
	ClassFilter getClassFilter();

	/**
	 * 被通知的接口能否由引入通知实现？
	 * 在添加 IntroductionAdvisor 之前调用。
	 * @throws IllegalArgumentException if the advised interfaces can't be
	 * implemented by the introduction advice
	 */
	void validateInterfaces() throws IllegalArgumentException;

}
