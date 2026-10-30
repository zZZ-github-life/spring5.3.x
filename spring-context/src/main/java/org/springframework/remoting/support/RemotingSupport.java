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

package org.springframework.remoting.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * 远程访问器和导出器的通用支持基类，提供通用的 Bean ClassLoader 处理。
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
public abstract class RemotingSupport implements BeanClassLoaderAware {

	/** 可供子类使用的日志记录器。 */
	protected final Log logger = LogFactory.getLog(getClass());

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();


	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	/**
	 * 返回此访问器运行所在的 ClassLoader，用于反序列化和生成代理。
	 */
	protected ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}


	/**
	 * 如有必要，即当 Bean ClassLoader 与线程上下文 ClassLoader 尚不一致时，用环境中的 Bean ClassLoader
	 * 覆盖线程上下文 ClassLoader。
	 * @return 原始的线程上下文 ClassLoader；
	 * 如果未被覆盖则为 {@code null}
	 */
	@Nullable
	protected ClassLoader overrideThreadContextClassLoader() {
		return ClassUtils.overrideThreadContextClassLoader(getBeanClassLoader());
	}

	/**
	 * 如有必要，重置原始的线程上下文 ClassLoader。
	 * @param original 原始线程上下文 ClassLoader；
	 * 如果未被覆盖则为 {@code null}（此时无需重置）
	 */
	protected void resetThreadContextClassLoader(@Nullable ClassLoader original) {
		if (original != null) {
			Thread.currentThread().setContextClassLoader(original);
		}
	}

}
