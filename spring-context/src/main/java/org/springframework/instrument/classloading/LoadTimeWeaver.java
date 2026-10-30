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

package org.springframework.instrument.classloading;

import java.lang.instrument.ClassFileTransformer;

/**
 * 定义了向 {@link ClassLoader} 添加一个或多个 {@link ClassFileTransformer ClassFileTransformers}
 * 的契约。
 *
 * <p>实现可以作用于当前上下文 {@code ClassLoader}，也可以公开自己的可插桩 {@code ClassLoader}。
 *
 * @author Rod Johnson
 * @author Costin Leau
 * @since 2.0
 * @see java.lang.instrument.ClassFileTransformer
 */
public interface LoadTimeWeaver {

	/**
	 * 添加一个由此 {@code LoadTimeWeaver} 应用的 {@code ClassFileTransformer}。
	 * @param transformer 要添加的 {@code ClassFileTransformer}
	 */
	void addTransformer(ClassFileTransformer transformer);

	/**
	 * 返回一个 {@code ClassLoader}，它基于用户定义的
	 * {@link ClassFileTransformer ClassFileTransformers} 通过 AspectJ 风格的加载时织入支持插桩。
	 * <p>可以是当前 {@code ClassLoader}，也可以是由此 {@link LoadTimeWeaver} 实例创建的
	 * {@code ClassLoader}。
	 * @return 将根据已注册的转换器公开插桩后类的
	 * {@code ClassLoader}
	 */
	ClassLoader getInstrumentableClassLoader();

	/**
	 * 返回一个一次性 {@code ClassLoader}，使类能够被加载和检查而不影响父 {@code ClassLoader}。
	 * <p><i>不应</i>返回与调用 {@link #getInstrumentableClassLoader()} 所返回的 {@link ClassLoader} 相
	 * 同的实例。
	 * @return 临时的用完即弃 {@code ClassLoader}；每次调用都应返回
	 * 不带任何现有状态的新实例
	 */
	ClassLoader getThrowawayClassLoader();

}
