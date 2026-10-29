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

package org.springframework.core.type;

/**
 * 定义对特定方法注解的抽象访问的接口，其形式不要求该方法的类已被加载。
 *
 * @author Juergen Hoeller
 * @author Mark Pollack
 * @author Chris Beams
 * @author Phillip Webb
 * @since 3.0
 * @see StandardMethodMetadata
 * @see AnnotationMetadata#getAnnotatedMethods
 * @see AnnotatedTypeMetadata
 */
public interface MethodMetadata extends AnnotatedTypeMetadata {

	/**
	 * 获取底层方法的名称。
	 */
	String getMethodName();

	/**
	 * 获取声明底层方法的类的完全限定名。
	 */
	String getDeclaringClassName();

	/**
	 * 获取底层方法所声明返回类型的完全限定名。
	 * @since 4.2
	 */
	String getReturnTypeName();

	/**
	 * 确定底层方法是否实际上是抽象的：即在类中被标记为 abstract，或在接口中声明为常规的非默认方法。
	 * @since 4.2
	 */
	boolean isAbstract();

	/**
	 * 确定底层方法是否声明为“static”。
	 */
	boolean isStatic();

	/**
	 * 确定底层方法是否标记为“final”。
	 */
	boolean isFinal();

	/**
	 * 确定底层方法是否可覆盖，即未标记为 static、final 或 private。
	 */
	boolean isOverridable();

}
