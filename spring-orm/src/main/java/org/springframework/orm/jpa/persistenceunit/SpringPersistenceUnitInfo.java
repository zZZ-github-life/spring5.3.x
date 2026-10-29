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

package org.springframework.orm.jpa.persistenceunit;

import javax.persistence.spi.ClassTransformer;

import org.springframework.core.DecoratingClassLoader;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.instrument.classloading.SimpleThrowawayClassLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link MutablePersistenceUnitInfo} 的子类，
 * 基于 Spring 的 {@link org.springframework.instrument.classloading.LoadTimeWeaver}
 * 抽象添加了插桩钩子。
 *
 * <p>与其超类不同，此类的可见性限制为包级。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Costin Leau
 * @since 2.0
 * @see PersistenceUnitManager
 */
class SpringPersistenceUnitInfo extends MutablePersistenceUnitInfo {

	@Nullable
	private LoadTimeWeaver loadTimeWeaver;

	@Nullable
	private ClassLoader classLoader;


	/**
	 * 使用 Spring 向当前类加载器添加插桩所用的 LoadTimeWeaver SPI 接口
	 * 初始化此 PersistenceUnitInfo。
	 */
	public void init(LoadTimeWeaver loadTimeWeaver) {
		Assert.notNull(loadTimeWeaver, "LoadTimeWeaver must not be null");
		this.loadTimeWeaver = loadTimeWeaver;
		this.classLoader = loadTimeWeaver.getInstrumentableClassLoader();
	}

	/**
	 * 使用当前类加载器初始化此 PersistenceUnitInfo
	 * （而不是使用 LoadTimeWeaver）。
	 */
	public void init(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
	}


	/**
	 * 此实现返回 LoadTimeWeaver 的可插桩 ClassLoader（如果已指定）。
	 */
	@Override
	@Nullable
	public ClassLoader getClassLoader() {
		return this.classLoader;
	}

	/**
	 * 此实现委托给 LoadTimeWeaver（如果已指定）。
	 */
	@Override
	public void addTransformer(ClassTransformer classTransformer) {
		if (this.loadTimeWeaver == null) {
			throw new IllegalStateException("Cannot apply class transformer without LoadTimeWeaver specified");
		}
		this.loadTimeWeaver.addTransformer(new ClassFileTransformerAdapter(classTransformer));
	}

	/**
	 * 此实现委托给 LoadTimeWeaver（如果已指定）。
	 */
	@Override
	public ClassLoader getNewTempClassLoader() {
		ClassLoader tcl = (this.loadTimeWeaver != null ? this.loadTimeWeaver.getThrowawayClassLoader() :
				new SimpleThrowawayClassLoader(this.classLoader));
		String packageToExclude = getPersistenceProviderPackageName();
		if (packageToExclude != null && tcl instanceof DecoratingClassLoader) {
			((DecoratingClassLoader) tcl).excludePackage(packageToExclude);
		}
		return tcl;
	}

}
