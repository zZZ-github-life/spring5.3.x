/*
 * Copyright 2002-2007 the original author or authors.
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
 * 用于 AOP 代理接口（特别是引入接口）的标记，这些接口明确打算返回原始目标对象（通常从方法调用返回
 * 时会被替换为代理对象）。
 *
 * <p>请注意，这是一个类似 {@link java.io.Serializable} 的标记接口，它在语义上适用于某个声明的接口，
 * 而不是具体对象的整个类。换句话说，此标记仅适用于某个特定接口（通常是不作为 AOP 代理主接口的引
 * 入接口），因此不会影响具体 AOP 代理可能实现的其他接口。
 *
 * @author Juergen Hoeller
 * @since 2.0.5
 * @see org.springframework.aop.scope.ScopedObject
 */
public interface RawTargetAccess {

}
