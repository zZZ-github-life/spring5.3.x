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

package org.springframework.core;

import java.security.ProtectionDomain;

import org.springframework.lang.Nullable;

/**
 * 由具备重载感知能力的 ClassLoader（例如基于 Groovy 的 ClassLoader）实现的接口。例如，Spring 的
 * CGLIB 代理工厂会检测该接口，以便做出缓存决策。
 *
 * <p>如果某个 ClassLoader <i>没有</i>实现此接口，则从中获得的所有类都应被视为不可重载（即可以缓存）。
 *
 *
 * @author Juergen Hoeller
 * @since 2.5.1
 */
public interface SmartClassLoader {

	/**
	 * 判断给定类（在此 ClassLoader 中）是否可重载。
	 * <p>通常用于检查结果是否可以（针对此 ClassLoader）被缓存，或者是否每次都需要重新获取。默认实现始
	 * 终返回 {@code false}。
	 * @param clazz 要检查的类（通常从此 ClassLoader 加载）
	 * @return 该类是否预期稍后出现在重新加载的版本中（使用不同的 {@code Class} 对象）
	 */
	default boolean isClassReloadable(Class<?> clazz) {
		return false;
	}

	/**
	 * 返回此 SmartClassLoader 的原始 ClassLoader；如果当前加载器自身足够用，也可能返回当前加载器本身。
	 * <p>默认实现按原样返回本地 ClassLoader 引用。对于可重载的或其他有选择地覆盖的 ClassLoader，其通常
	 * 处理来自基础应用程序类加载器的不受影响的类，此时应实现该方法以返回当前加载器所源自的原始
	 * ClassLoader（例如通过 {@code return getParent();}）。
	 * <p>这特别用于 Spring 的 AOP 框架中，当目标类未在当前类加载器中定义时，用于确定特定代理的类加载器。
	 * 对于可重载的类加载器，对于未在可重载类加载器自身中定义的通用类，我们更倾向于使用基础应用程序类
	 * 加载器来进行代理。
	 * @return 原始的 ClassLoader（默认情况下为同一引用）
	 * @since 5.3.5
	 * @see ClassLoader#getParent()
	 * @see org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator
	 */
	default ClassLoader getOriginalClassLoader() {
		return (ClassLoader) this;
	}

	/**
	 * 在此类加载器中定义自定义类（通常是 CGLIB 代理类）。
	 * <p>这是 {@link ClassLoader} 中受保护的
	 * {@code defineClass(String, byte[], int, int, ProtectionDomain)} 方法的公共等价形式，该方法传统上
	 * 通过反射调用。自定义类加载器中的具体实现应简单地委托给该受保护方法，以便使特定于类加载器的定义在
	 * JDK 9+ 上公开可用而不出现“非法访问”警告：
	 * {@code return defineClass(name, b, 0, b.length, protectionDomain)}。请注意，JDK 9+ 的
	 * {@code Lookup#defineClass} 方法并不支持为新定义指定自定义目标类加载器；它始终在查找上下文类所在
	 * 的同一个类加载器中定义该类。
	 * @param name 类的名称
	 * @param b 定义该类的字节
	 * @param protectionDomain 该类的保护域（如果有）
	 * @return 新创建的类
	 * @throws LinkageError 类定义错误时
	 * @throws SecurityException 定义尝试无效时
	 * @throws UnsupportedOperationException 无法进行自定义定义时（由此接口中的默认实现抛出）
	 * @since 5.3.4
	 * @see ClassLoader#defineClass(String, byte[], int, int, ProtectionDomain)
	 */
	default Class<?> publicDefineClass(String name, byte[] b, @Nullable ProtectionDomain protectionDomain) {
		throw new UnsupportedOperationException();
	}

}
