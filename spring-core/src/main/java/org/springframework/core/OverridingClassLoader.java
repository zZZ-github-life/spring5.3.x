/*
 * Copyright 2002-2018 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;

import org.springframework.lang.Nullable;
import org.springframework.util.FileCopyUtils;

/**
 * {@code ClassLoader}，它<em>不</em>像普通类加载器那样总是委托给父加载器。例如，这使得可以在覆盖
 * ClassLoader 中强制进行插桩，或者实现“一次性”类加载行为：为内省目的，在覆盖的 {@code ClassLoader}
 * 中临时加载选定的应用类，最终再在给定的父 {@code ClassLoader} 中加载该类的插桩版本。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0.1
 */
public class OverridingClassLoader extends DecoratingClassLoader {

	/** 默认排除的包。 */
	public static final String[] DEFAULT_EXCLUDED_PACKAGES = new String[]
			{"java.", "javax.", "sun.", "oracle.", "javassist.", "org.aspectj.", "net.sf.cglib."};

	private static final String CLASS_FILE_SUFFIX = ".class";

	static {
		ClassLoader.registerAsParallelCapable();
	}


	@Nullable
	private final ClassLoader overrideDelegate;


	/**
	 * 为给定的 ClassLoader 创建一个新的 OverridingClassLoader。
	 * @param parent 要为其构建覆盖 ClassLoader 的父 ClassLoader
	 */
	public OverridingClassLoader(@Nullable ClassLoader parent) {
		this(parent, null);
	}

	/**
	 * 为给定的 ClassLoader 创建一个新的 OverridingClassLoader。
	 * @param parent 要为其构建覆盖 ClassLoader 的父 ClassLoader
	 * @param overrideDelegate 用于覆盖时委托的 ClassLoader
	 * @since 4.3
	 */
	public OverridingClassLoader(@Nullable ClassLoader parent, @Nullable ClassLoader overrideDelegate) {
		super(parent);
		this.overrideDelegate = overrideDelegate;
		for (String packageName : DEFAULT_EXCLUDED_PACKAGES) {
			excludePackage(packageName);
		}
	}


	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		if (this.overrideDelegate != null && isEligibleForOverriding(name)) {
			return this.overrideDelegate.loadClass(name);
		}
		return super.loadClass(name);
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if (isEligibleForOverriding(name)) {
			Class<?> result = loadClassForOverriding(name);
			if (result != null) {
				if (resolve) {
					resolveClass(result);
				}
				return result;
			}
		}
		return super.loadClass(name, resolve);
	}

	/**
	 * 判断指定的类是否允许由该类加载器覆盖。
	 * @param className 要检查的类名
	 * @return 指定的类是否符合条件
	 * @see #isExcluded
	 */
	protected boolean isEligibleForOverriding(String className) {
		return !isExcluded(className);
	}

	/**
	 * 为此 ClassLoader 中的覆盖目的加载指定的类。
	 *
	 * <p>默认实现委托给 {@link #findLoadedClass}、{@link #loadBytesForClass} 和 {@link #defineClass}。
	 * @param name 类的名称
	 * @return Class 对象，如果该名称没有定义类则为 {@code null}
	 * @throws ClassNotFoundException 如果无法加载给定名称的类
	 */
	@Nullable
	protected Class<?> loadClassForOverriding(String name) throws ClassNotFoundException {
		Class<?> result = findLoadedClass(name);
		if (result == null) {
			byte[] bytes = loadBytesForClass(name);
			if (bytes != null) {
				result = defineClass(name, bytes, 0, bytes.length);
			}
		}
		return result;
	}

	/**
	 * 加载给定类的定义字节，以便通过 {@link #defineClass} 调用转换为 Class 对象。
	 *
	 * <p>默认实现委托给 {@link #openStreamForClass} 和 {@link #transformIfNecessary}。
	 * @param name 类的名称
	 * @return 字节内容（已应用转换器），如果该名称没有定义类则为 {@code null}
	 * @throws ClassNotFoundException 如果无法加载给定名称的类
	 */
	@Nullable
	protected byte[] loadBytesForClass(String name) throws ClassNotFoundException {
		InputStream is = openStreamForClass(name);
		if (is == null) {
			return null;
		}
		try {
			// Load the raw bytes.
			byte[] bytes = FileCopyUtils.copyToByteArray(is);
			// Transform if necessary and use the potentially transformed bytes.
			return transformIfNecessary(name, bytes);
		}
		catch (IOException ex) {
			throw new ClassNotFoundException("Cannot load resource for class [" + name + "]", ex);
		}
	}

	/**
	 * 为指定的类打开一个 InputStream。
	 *
	 * <p>默认实现通过父 ClassLoader 的 {@code getResourceAsStream} 方法加载标准类文件。
	 * @param name 类的名称
	 * @return 包含指定类字节码的 InputStream
	 */
	@Nullable
	protected InputStream openStreamForClass(String name) {
		String internalName = name.replace('.', '/') + CLASS_FILE_SUFFIX;
		return getParent().getResourceAsStream(internalName);
	}


	/**
	 * 由子类实现的转换钩子。
	 *
	 * <p>默认实现只是原样返回给定的字节。
	 * @param name 正在被转换的类的完全限定名
	 * @param bytes 类的原始字节
	 * @return 转换后的字节（永不为 {@code null}；如果转换未产生变化，则与输入字节相同）
	 */
	protected byte[] transformIfNecessary(String name, byte[] bytes) {
		return bytes;
	}

}
