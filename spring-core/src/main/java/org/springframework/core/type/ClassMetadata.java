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

package org.springframework.core.type;

import org.springframework.lang.Nullable;

/**
 * 定义特定类抽象元数据的接口，其形式不要求该类已被加载。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see StandardClassMetadata
 * @see org.springframework.core.type.classreading.MetadataReader#getClassMetadata()
 * @see AnnotationMetadata
 */
public interface ClassMetadata {

	/**
	 * 返回底层类的名称。
	 */
	String getClassName();

	/**
	 * 返回底层类是否表示接口。
	 */
	boolean isInterface();

	/**
	 * 返回底层类是否表示注解。
	 * @since 4.1
	 */
	boolean isAnnotation();

	/**
	 * 返回底层类是否被标记为抽象。
	 */
	boolean isAbstract();

	/**
	 * 返回底层类是否表示具体类，即既不是接口也不是抽象类。
	 */
	default boolean isConcrete() {
		return !(isInterface() || isAbstract());
	}

	/**
	 * 返回底层类是否被标记为“final”。
	 */
	boolean isFinal();

	/**
	 * 确定底层类是否独立，即它是顶级类还是可以独立于外围类构造的嵌套类（静态内部类）。
	 */
	boolean isIndependent();

	/**
	 * 返回底层类是否声明在外围类中（即底层类是内部/嵌套类或方法中的局部类）。
	 * <p>如果此方法返回 {@code false}，则底层类是顶级类。
	 */
	default boolean hasEnclosingClass() {
		return (getEnclosingClassName() != null);
	}

	/**
	 * 返回底层类的外围类的名称；如果底层类是顶级类，则返回 {@code null}。
	 */
	@Nullable
	String getEnclosingClassName();

	/**
	 * 返回底层类是否有超类。
	 */
	default boolean hasSuperClass() {
		return (getSuperClassName() != null);
	}

	/**
	 * 返回底层类的超类的名称；如果未定义超类，则返回 {@code null}。
	 */
	@Nullable
	String getSuperClassName();

	/**
	 * 返回底层类实现的所有接口的名称；如果没有，则返回空数组。
	 */
	String[] getInterfaceNames();

	/**
	 * 返回声明为此 ClassMetadata 对象所表示类的成员的所有类的名称。这包括该类声明的 public、protected、
	 * 默认（包）访问以及 private 的类和接口，但不包括继承的类和接口。如果不存在成员类或接口，则返回
	 * 空数组。
	 * @since 3.1
	 */
	String[] getMemberClassNames();

}
