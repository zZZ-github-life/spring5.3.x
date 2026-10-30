/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core;

/**
 * 由装饰代理实现的接口，特别是 Spring AOP 代理，但也可能是具有装饰器语义的自定义代理。
 *
 * <p>请注意，仅当被装饰类一开始就不在代理类的层次结构中时，才应实现此接口。特别是，
 * 像 Spring AOP CGLIB 代理这样的“目标类”代理不应实现它，
 * 因为对目标类的任何查找都可以直接在代理类上执行。
 *
 * <p>将其定义在 core 模块中，
 * 是为了让 {@link org.springframework.core.annotation.AnnotationAwareOrderComparator}（以
 * 及其他可能不依赖 spring-aop 的候选者）能够将其用于内省目的，
 * 特别是注解查找。
 *
 * @author Juergen Hoeller
 * @since 4.3
 */
public interface DecoratingProxy {

	/**
	 * 返回此代理背后的（最终）被装饰类。
	 * <p>对于 AOP 代理，这将是最终目标类，而不仅仅是直接目标（在存在多层嵌套代理的情况下）。
	 * @return 被装饰的类（永不为 {@code null}）
	 */
	Class<?> getDecoratedClass();

}
