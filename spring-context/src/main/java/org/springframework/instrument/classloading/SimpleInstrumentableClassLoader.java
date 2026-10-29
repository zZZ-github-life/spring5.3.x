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

package org.springframework.instrument.classloading;

import java.lang.instrument.ClassFileTransformer;

import org.springframework.core.OverridingClassLoader;
import org.springframework.lang.Nullable;

/**
 * 可插桩的 {@code ClassLoader} 的简化实现。
 *
 * <p>可用于测试和独立环境。
 *
 * @author Rod Johnson
 * @author Costin Leau
 * @since 2.0
 */
public class SimpleInstrumentableClassLoader extends OverridingClassLoader {

	static {
		ClassLoader.registerAsParallelCapable();
	}


	private final WeavingTransformer weavingTransformer;


	/**
	 * 为给定的 ClassLoader 创建一个新的 SimpleInstrumentableClassLoader。
	 * @param parent the ClassLoader to build an instrumentable ClassLoader for
	 */
	public SimpleInstrumentableClassLoader(@Nullable ClassLoader parent) {
		super(parent);
		this.weavingTransformer = new WeavingTransformer(parent);
	}


	/**
	 * 添加一个由该 ClassLoader 应用的 {@link ClassFileTransformer}。
	 * @param transformer the {@link ClassFileTransformer} to register
	 */
	public void addTransformer(ClassFileTransformer transformer) {
		this.weavingTransformer.addTransformer(transformer);
	}


	@Override
	protected byte[] transformIfNecessary(String name, byte[] bytes) {
		return this.weavingTransformer.transformIfNecessary(name, bytes);
	}

}
