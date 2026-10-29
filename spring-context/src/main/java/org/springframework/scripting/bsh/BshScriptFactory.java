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

package org.springframework.scripting.bsh;

import java.io.IOException;

import bsh.EvalError;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.lang.Nullable;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.scripting.ScriptFactory;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link org.springframework.scripting.ScriptFactory} 的 BeanShell 脚本实现。
 *
 * <p>通常与 {@link org.springframework.scripting.support.ScriptFactoryPostProcessor} 结合使用；有
 * 关配置示例，请参见后者的 javadoc。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 2.0
 * @see BshScriptUtils
 * @see org.springframework.scripting.support.ScriptFactoryPostProcessor
 */
public class BshScriptFactory implements ScriptFactory, BeanClassLoaderAware {

	private final String scriptSourceLocator;

	@Nullable
	private final Class<?>[] scriptInterfaces;

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	@Nullable
	private Class<?> scriptClass;

	private final Object scriptClassMonitor = new Object();

	private boolean wasModifiedForTypeCheck = false;


	/**
	 * 为给定的脚本源创建一个新的 BshScriptFactory。
	 * <p>使用此 {@code BshScriptFactory} 变体时，脚本需要声明完整的类，或返回脚本化对象的实际实例。
	 * @param scriptSourceLocator a locator that points to the source of the script.
	 * Interpreted by the post-processor that actually creates the script.
	 */
	public BshScriptFactory(String scriptSourceLocator) {
		Assert.hasText(scriptSourceLocator, "'scriptSourceLocator' must not be empty");
		this.scriptSourceLocator = scriptSourceLocator;
		this.scriptInterfaces = null;
	}

	/**
	 * 为给定的脚本源创建一个新的 BshScriptFactory。
	 * <p>脚本既可以是一个简单脚本（需要生成相应的代理，实现指定的接口），也可以声明完整的类，或返回脚
	 * 本化对象的实际实例（此时，指定的接口（如有）需要由该类/实例实现）。
	 * @param scriptSourceLocator a locator that points to the source of the script.
	 * Interpreted by the post-processor that actually creates the script.
	 * @param scriptInterfaces the Java interfaces that the scripted object
	 * is supposed to implement (may be {@code null})
	 */
	public BshScriptFactory(String scriptSourceLocator, @Nullable Class<?>... scriptInterfaces) {
		Assert.hasText(scriptSourceLocator, "'scriptSourceLocator' must not be empty");
		this.scriptSourceLocator = scriptSourceLocator;
		this.scriptInterfaces = scriptInterfaces;
	}


	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}


	@Override
	public String getScriptSourceLocator() {
		return this.scriptSourceLocator;
	}

	@Override
	@Nullable
	public Class<?>[] getScriptInterfaces() {
		return this.scriptInterfaces;
	}

	/**
	 * BeanShell 脚本确实需要配置接口。
	 */
	@Override
	public boolean requiresConfigInterface() {
		return true;
	}

	/**
	 * 通过 {@link BshScriptUtils} 加载并解析 BeanShell 脚本。
	 * @see BshScriptUtils#createBshObject(String, Class[], ClassLoader)
	 */
	@Override
	@Nullable
	public Object getScriptedObject(ScriptSource scriptSource, @Nullable Class<?>... actualInterfaces)
			throws IOException, ScriptCompilationException {

		Class<?> clazz;

		try {
			synchronized (this.scriptClassMonitor) {
				boolean requiresScriptEvaluation = (this.wasModifiedForTypeCheck && this.scriptClass == null);
				this.wasModifiedForTypeCheck = false;

				if (scriptSource.isModified() || requiresScriptEvaluation) {
					// New script content: Let's check whether it evaluates to a Class.
					Object result = BshScriptUtils.evaluateBshScript(
							scriptSource.getScriptAsString(), actualInterfaces, this.beanClassLoader);
					if (result instanceof Class) {
						// A Class: We'll cache the Class here and create an instance
						// outside the synchronized block.
						this.scriptClass = (Class<?>) result;
					}
					else {
						// Not a Class: OK, we'll simply create BeanShell objects
						// through evaluating the script for every call later on.
						// For this first-time check, let's simply return the
						// already evaluated object.
						return result;
					}
				}
				clazz = this.scriptClass;
			}
		}
		catch (EvalError ex) {
			this.scriptClass = null;
			throw new ScriptCompilationException(scriptSource, ex);
		}

		if (clazz != null) {
			// A Class: We need to create an instance for every call.
			try {
				return ReflectionUtils.accessibleConstructor(clazz).newInstance();
			}
			catch (Throwable ex) {
				throw new ScriptCompilationException(
						scriptSource, "Could not instantiate script class: " + clazz.getName(), ex);
			}
		}
		else {
			// Not a Class: We need to evaluate the script for every call.
			try {
				return BshScriptUtils.createBshObject(
						scriptSource.getScriptAsString(), actualInterfaces, this.beanClassLoader);
			}
			catch (EvalError ex) {
				throw new ScriptCompilationException(scriptSource, ex);
			}
		}
	}

	@Override
	@Nullable
	public Class<?> getScriptedObjectType(ScriptSource scriptSource)
			throws IOException, ScriptCompilationException {

		synchronized (this.scriptClassMonitor) {
			try {
				if (scriptSource.isModified()) {
					// New script content: Let's check whether it evaluates to a Class.
					this.wasModifiedForTypeCheck = true;
					this.scriptClass = BshScriptUtils.determineBshObjectType(
							scriptSource.getScriptAsString(), this.beanClassLoader);
				}
				return this.scriptClass;
			}
			catch (EvalError ex) {
				this.scriptClass = null;
				throw new ScriptCompilationException(scriptSource, ex);
			}
		}
	}

	@Override
	public boolean requiresScriptedObjectRefresh(ScriptSource scriptSource) {
		synchronized (this.scriptClassMonitor) {
			return (scriptSource.isModified() || this.wasModifiedForTypeCheck);
		}
	}


	@Override
	public String toString() {
		return "BshScriptFactory: script source locator [" + this.scriptSourceLocator + "]";
	}

}
