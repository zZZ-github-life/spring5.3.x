/*
 * Copyright 2002-2020 the original author or authors.
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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Spring 对 JPA {@link javax.persistence.spi.PersistenceUnitInfo} 接口的
 * 基础实现，用于在容器中引导 {@code EntityManagerFactory}。
 *
 * <p>此实现在很大程度上是一个 JavaBean，为所有标准
 * {@code PersistenceUnitInfo} 属性提供了修改器。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Costin Leau
 * @since 2.0
 */
public class MutablePersistenceUnitInfo implements SmartPersistenceUnitInfo {

	@Nullable
	private String persistenceUnitName;

	@Nullable
	private String persistenceProviderClassName;

	@Nullable
	private PersistenceUnitTransactionType transactionType;

	@Nullable
	private DataSource nonJtaDataSource;

	@Nullable
	private DataSource jtaDataSource;

	private final List<String> mappingFileNames = new ArrayList<>();

	private final List<URL> jarFileUrls = new ArrayList<>();

	@Nullable
	private URL persistenceUnitRootUrl;

	private final List<String> managedClassNames = new ArrayList<>();

	private final List<String> managedPackages = new ArrayList<>();

	private boolean excludeUnlistedClasses = false;

	private SharedCacheMode sharedCacheMode = SharedCacheMode.UNSPECIFIED;

	private ValidationMode validationMode = ValidationMode.AUTO;

	private Properties properties = new Properties();

	private String persistenceXMLSchemaVersion = "2.0";

	@Nullable
	private String persistenceProviderPackageName;


	public void setPersistenceUnitName(@Nullable String persistenceUnitName) {
		this.persistenceUnitName = persistenceUnitName;
	}

	@Override
	@Nullable
	public String getPersistenceUnitName() {
		return this.persistenceUnitName;
	}

	public void setPersistenceProviderClassName(@Nullable String persistenceProviderClassName) {
		this.persistenceProviderClassName = persistenceProviderClassName;
	}

	@Override
	@Nullable
	public String getPersistenceProviderClassName() {
		return this.persistenceProviderClassName;
	}

	public void setTransactionType(PersistenceUnitTransactionType transactionType) {
		this.transactionType = transactionType;
	}

	@Override
	public PersistenceUnitTransactionType getTransactionType() {
		if (this.transactionType != null) {
			return this.transactionType;
		}
		else {
			return (this.jtaDataSource != null ?
					PersistenceUnitTransactionType.JTA : PersistenceUnitTransactionType.RESOURCE_LOCAL);
		}
	}

	public void setJtaDataSource(@Nullable DataSource jtaDataSource) {
		this.jtaDataSource = jtaDataSource;
	}

	@Override
	@Nullable
	public DataSource getJtaDataSource() {
		return this.jtaDataSource;
	}

	public void setNonJtaDataSource(@Nullable DataSource nonJtaDataSource) {
		this.nonJtaDataSource = nonJtaDataSource;
	}

	@Override
	@Nullable
	public DataSource getNonJtaDataSource() {
		return this.nonJtaDataSource;
	}

	public void addMappingFileName(String mappingFileName) {
		this.mappingFileNames.add(mappingFileName);
	}

	@Override
	public List<String> getMappingFileNames() {
		return this.mappingFileNames;
	}

	public void addJarFileUrl(URL jarFileUrl) {
		this.jarFileUrls.add(jarFileUrl);
	}

	@Override
	public List<URL> getJarFileUrls() {
		return this.jarFileUrls;
	}

	public void setPersistenceUnitRootUrl(@Nullable URL persistenceUnitRootUrl) {
		this.persistenceUnitRootUrl = persistenceUnitRootUrl;
	}

	@Override
	@Nullable
	public URL getPersistenceUnitRootUrl() {
		return this.persistenceUnitRootUrl;
	}

	/**
	 * 向持久化提供者的元数据中添加一个受管类名。
	 * @see javax.persistence.spi.PersistenceUnitInfo#getManagedClassNames()
	 * @see #addManagedPackage
	 */
	public void addManagedClassName(String managedClassName) {
		this.managedClassNames.add(managedClassName);
	}

	@Override
	public List<String> getManagedClassNames() {
		return this.managedClassNames;
	}

	/**
	 * 向持久化提供者的元数据中添加一个受管包。
	 * <p>注意：这指的是带有注解的 {@code package-info.java} 文件。
	 * 它<i>不会</i>触发对指定包中实体的扫描；后者是
	 * {@link DefaultPersistenceUnitManager#setPackagesToScan} 的职责。
	 * @since 4.1
	 * @see SmartPersistenceUnitInfo#getManagedPackages()
	 * @see #addManagedClassName
	 */
	public void addManagedPackage(String packageName) {
		this.managedPackages.add(packageName);
	}

	@Override
	public List<String> getManagedPackages() {
		return this.managedPackages;
	}

	public void setExcludeUnlistedClasses(boolean excludeUnlistedClasses) {
		this.excludeUnlistedClasses = excludeUnlistedClasses;
	}

	@Override
	public boolean excludeUnlistedClasses() {
		return this.excludeUnlistedClasses;
	}

	public void setSharedCacheMode(SharedCacheMode sharedCacheMode) {
		this.sharedCacheMode = sharedCacheMode;
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return this.sharedCacheMode;
	}

	public void setValidationMode(ValidationMode validationMode) {
		this.validationMode = validationMode;
	}

	@Override
	public ValidationMode getValidationMode() {
		return this.validationMode;
	}

	public void addProperty(String name, String value) {
		this.properties.setProperty(name, value);
	}

	public void setProperties(Properties properties) {
		Assert.notNull(properties, "Properties must not be null");
		this.properties = properties;
	}

	@Override
	public Properties getProperties() {
		return this.properties;
	}

	public void setPersistenceXMLSchemaVersion(String persistenceXMLSchemaVersion) {
		this.persistenceXMLSchemaVersion = persistenceXMLSchemaVersion;
	}

	@Override
	public String getPersistenceXMLSchemaVersion() {
		return this.persistenceXMLSchemaVersion;
	}

	@Override
	public void setPersistenceProviderPackageName(@Nullable String persistenceProviderPackageName) {
		this.persistenceProviderPackageName = persistenceProviderPackageName;
	}

	@Nullable
	public String getPersistenceProviderPackageName() {
		return this.persistenceProviderPackageName;
	}


	/**
	 * 此实现返回默认的 ClassLoader。
	 * @see org.springframework.util.ClassUtils#getDefaultClassLoader()
	 */
	@Override
	@Nullable
	public ClassLoader getClassLoader() {
		return ClassUtils.getDefaultClassLoader();
	}

	/**
	 * 此实现抛出 UnsupportedOperationException。
	 */
	@Override
	public void addTransformer(ClassTransformer classTransformer) {
		throw new UnsupportedOperationException("addTransformer not supported");
	}

	/**
	 * 此实现抛出 UnsupportedOperationException。
	 */
	@Override
	public ClassLoader getNewTempClassLoader() {
		throw new UnsupportedOperationException("getNewTempClassLoader not supported");
	}


	@Override
	public String toString() {
		return "PersistenceUnitInfo: name '" + this.persistenceUnitName +
				"', root URL [" + this.persistenceUnitRootUrl + "]";
	}

}
