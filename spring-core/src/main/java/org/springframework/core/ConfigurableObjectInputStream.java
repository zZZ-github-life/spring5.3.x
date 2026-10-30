/*
 * Copyright 2002-2016 the original author or authors.
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
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * 特殊的 ObjectInputStream 子类，它针对特定的 ClassLoader 解析类名。
 * 作为 {@link org.springframework.remoting.rmi.CodebaseAwareObjectInputStream} 的基类。
 *
 * @author Juergen Hoeller
 * @since 2.5.5
 */
public class ConfigurableObjectInputStream extends ObjectInputStream {

	@Nullable
	private final ClassLoader classLoader;

	private final boolean acceptProxyClasses;


	/**
	 * 为给定的 InputStream 和 ClassLoader 创建新的 ConfigurableObjectInputStream。
	 * @param in 要读取的 InputStream
	 * @param classLoader 用于加载本地类的 ClassLoader
	 * @see java.io.ObjectInputStream#ObjectInputStream(java.io.InputStream)
	 */
	public ConfigurableObjectInputStream(InputStream in, @Nullable ClassLoader classLoader) throws IOException {
		this(in, classLoader, true);
	}

	/**
	 * 为给定的 InputStream 和 ClassLoader 创建新的 ConfigurableObjectInputStream。
	 * @param in 要读取的 InputStream
	 * @param classLoader 用于加载本地类的 ClassLoader
	 * @param acceptProxyClasses 是否接受代理类的反序列化（可作为安全措施禁用）
	 * @see java.io.ObjectInputStream#ObjectInputStream(java.io.InputStream)
	 */
	public ConfigurableObjectInputStream(
			InputStream in, @Nullable ClassLoader classLoader, boolean acceptProxyClasses) throws IOException {

		super(in);
		this.classLoader = classLoader;
		this.acceptProxyClasses = acceptProxyClasses;
	}


	@Override
	protected Class<?> resolveClass(ObjectStreamClass classDesc) throws IOException, ClassNotFoundException {
		try {
			if (this.classLoader != null) {
				// Use the specified ClassLoader to resolve local classes.
				return ClassUtils.forName(classDesc.getName(), this.classLoader);
			}
			else {
				// Use the default ClassLoader...
				return super.resolveClass(classDesc);
			}
		}
		catch (ClassNotFoundException ex) {
			return resolveFallbackIfPossible(classDesc.getName(), ex);
		}
	}

	@Override
	protected Class<?> resolveProxyClass(String[] interfaces) throws IOException, ClassNotFoundException {
		if (!this.acceptProxyClasses) {
			throw new NotSerializableException("Not allowed to accept serialized proxy classes");
		}
		if (this.classLoader != null) {
			// Use the specified ClassLoader to resolve local proxy classes.
			Class<?>[] resolvedInterfaces = new Class<?>[interfaces.length];
			for (int i = 0; i < interfaces.length; i++) {
				try {
					resolvedInterfaces[i] = ClassUtils.forName(interfaces[i], this.classLoader);
				}
				catch (ClassNotFoundException ex) {
					resolvedInterfaces[i] = resolveFallbackIfPossible(interfaces[i], ex);
				}
			}
			try {
				return ClassUtils.createCompositeInterface(resolvedInterfaces, this.classLoader);
			}
			catch (IllegalArgumentException ex) {
				throw new ClassNotFoundException(null, ex);
			}
		}
		else {
			// Use ObjectInputStream's default ClassLoader...
			try {
				return super.resolveProxyClass(interfaces);
			}
			catch (ClassNotFoundException ex) {
				Class<?>[] resolvedInterfaces = new Class<?>[interfaces.length];
				for (int i = 0; i < interfaces.length; i++) {
					resolvedInterfaces[i] = resolveFallbackIfPossible(interfaces[i], ex);
				}
				return ClassUtils.createCompositeInterface(resolvedInterfaces, getFallbackClassLoader());
			}
		}
	}


	/**
	 * 针对回退类加载器解析给定的类名。
	 * <p>默认实现只是重新抛出原始异常，因为没有可用的回退方案。
	 * @param className 要解析的类名
	 * @param ex 尝试加载该类时抛出的原始异常
	 * @return 新解析出的类（永不为 {@code null}）
	 */
	protected Class<?> resolveFallbackIfPossible(String className, ClassNotFoundException ex)
			throws IOException, ClassNotFoundException{

		throw ex;
	}

	/**
	 * 返回在未指定 ClassLoader 且 ObjectInputStream 自身的默认类加载器失败时要使用的回退
	 * ClassLoader。
	 * <p>默认实现只是返回 {@code null}，表示没有可用的特定回退方案。
	 */
	@Nullable
	protected ClassLoader getFallbackClassLoader() throws IOException {
		return null;
	}

}
