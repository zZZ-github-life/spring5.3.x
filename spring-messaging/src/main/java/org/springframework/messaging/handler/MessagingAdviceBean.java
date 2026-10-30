/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.messaging.handler;

import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;

/**
 * 表示一个 Spring 管理的 Bean，其横切功能将应用于一个或多个带有基于注解的
 * 消息处理方法的 Spring Bean。
 *
 * <p>带有注解处理器方法的组件构造型（例如
 * {@link org.springframework.stereotype.Controller @Controller}）
 * 通常需要在所有此类注解组件或其中的一部分上提供横切功能。
 * 一个主要示例是对“全局”注解异常处理器方法的需求，但这一概念的应用范围更广。
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public interface MessagingAdviceBean extends Ordered {

	/**
	 * 返回所包含通知 Bean 的类型。
	 * <p>如果 Bean 类型是由 CGLIB 生成的类，则返回原始的用户定义类。
	 */
	@Nullable
	Class<?> getBeanType();

	/**
	 * 返回通知 Bean 实例，如有必要，通过 BeanFactory 解析按名称指定的 Bean。
	 */
	Object resolveBean();

	/**
	 * 此 {@link MessagingAdviceBean} 是否适用于给定 Bean 类型。
	 * @param beanType 要检查的 Bean 类型
	 */
	boolean isApplicableToBeanType(Class<?> beanType);

}
