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

package org.springframework.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.Ordered;

/**
 * {@code @Order} 定义了被注解组件的排序顺序。
 *
 * <p>{@link #value} 是可选的，表示 {@link Ordered} 接口中定义的顺序值。值越低优先级越高。默认值为
 * {@code Ordered.LOWEST_PRECEDENCE}，表示最低优先级（会输给任何其他指定的顺序值）。
 *
 * <p><b>注意：</b>自 Spring 4.0 起，Spring 中的许多类型组件都支持基于注解的排序，甚至对于集合注入
 * 也是如此，此时会考虑目标组件的顺序值（来自其目标类或其 {@code @Bean} 方法）。虽然这些顺序值可能
 * 影响注入点的优先级，但请注意它们不影响单例启动顺序，后者是一个正交的关注点，由依赖关系和
 * {@code @DependsOn} 声明决定（影响运行时确定的依赖图）。
 *
 * <p>自 Spring 4.1 起，在排序场景中，标准 {@link javax.annotation.Priority} 注解可用作此注解的直接
 * 替代。请注意，当需要挑选单个元素时，{@code @Priority} 可能具有额外的语义（请参见
 * {@link AnnotationAwareOrderComparator#getPriority}）。
 *
 * <p>或者，也可以通过 {@link Ordered} 接口按实例确定顺序值，从而使用由配置确定的实例值，而不是附加
 * 到特定类的硬编码值。
 *
 * <p>有关非有序对象的排序语义的详细信息，请参阅
 * {@link org.springframework.core.OrderComparator OrderComparator} 的 Javadoc。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.core.Ordered
 * @see AnnotationAwareOrderComparator
 * @see OrderUtils
 * @see javax.annotation.Priority
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Documented
public @interface Order {

	/**
	 * 顺序值。<p>默认为 {@link Ordered#LOWEST_PRECEDENCE}。
	 * @see Ordered#getOrder()
	 */
	int value() default Ordered.LOWEST_PRECEDENCE;

}
