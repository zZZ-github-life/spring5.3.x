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

package org.springframework.orm.jpa.persistenceunit;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Converter;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import javax.persistence.PersistenceException;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.index.CandidateComponentsIndex;
import org.springframework.context.index.CandidateComponentsIndexLoader;
import org.springframework.context.weaving.LoadTimeWeaverAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.jdbc.datasource.lookup.DataSourceLookup;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.jdbc.datasource.lookup.MapDataSourceLookup;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ResourceUtils;

/**
 * {@link PersistenceUnitManager} 接口的默认实现。
 * 由 {@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean}
 * 用作内部默认实现。
 *
 * <p>支持对 {@code persistence.xml} 文件的标准 JPA 扫描，
 * 并提供可配置的文件位置、JDBC DataSource 查找和加载时织入。
 *
 * <p>默认的 XML 文件位置是 {@code classpath*:META-INF/persistence.xml}，
 * 会在类路径中扫描所有匹配的文件（依据 JPA 规范的定义）。
 * DataSource 名称默认被解释为 JNDI 名称，并且不启用加载时织入
 * （启用加载时织入需要在持久化提供者中关闭织入）。
 *
 * <p><b>注意：自 Spring 5.0 起，Spring 的 JPA 支持要求 JPA 2.1 或更高版本。</b>
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 2.0
 * @see #setPersistenceXmlLocations
 * @see #setDataSourceLookup
 * @see #setLoadTimeWeaver
 * @see org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean#setPersistenceUnitManager
 */
public class DefaultPersistenceUnitManager
		implements PersistenceUnitManager, ResourceLoaderAware, LoadTimeWeaverAware, InitializingBean {

	private static final String CLASS_RESOURCE_PATTERN = "/**/*.class";

	private static final String PACKAGE_INFO_SUFFIX = ".package-info";

	private static final String DEFAULT_ORM_XML_RESOURCE = "META-INF/orm.xml";

	private static final String PERSISTENCE_XML_FILENAME = "persistence.xml";

	/**
	 * {@code persistence.xml} 文件的默认位置：
	 * "classpath*:META-INF/persistence.xml"。
	 */
	public static final String DEFAULT_PERSISTENCE_XML_LOCATION = "classpath*:META-INF/" + PERSISTENCE_XML_FILENAME;

	/**
	 * 持久化单元根 URL 的默认位置：
	 * "classpath:"，表示类路径的根。
	 */
	public static final String ORIGINAL_DEFAULT_PERSISTENCE_UNIT_ROOT_LOCATION = "classpath:";

	/**
	 * 默认的持久化单元名称。
	 */
	public static final String ORIGINAL_DEFAULT_PERSISTENCE_UNIT_NAME = "default";


	private static final Set<AnnotationTypeFilter> entityTypeFilters;

	static {
		entityTypeFilters = new LinkedHashSet<>(8);
		entityTypeFilters.add(new AnnotationTypeFilter(Entity.class, false));
		entityTypeFilters.add(new AnnotationTypeFilter(Embeddable.class, false));
		entityTypeFilters.add(new AnnotationTypeFilter(MappedSuperclass.class, false));
		entityTypeFilters.add(new AnnotationTypeFilter(Converter.class, false));
	}


	protected final Log logger = LogFactory.getLog(getClass());

	private String[] persistenceXmlLocations = new String[] {DEFAULT_PERSISTENCE_XML_LOCATION};

	@Nullable
	private String defaultPersistenceUnitRootLocation = ORIGINAL_DEFAULT_PERSISTENCE_UNIT_ROOT_LOCATION;

	@Nullable
	private String defaultPersistenceUnitName = ORIGINAL_DEFAULT_PERSISTENCE_UNIT_NAME;

	@Nullable
	private String[] packagesToScan;

	@Nullable
	private String[] mappingResources;

	@Nullable
	private SharedCacheMode sharedCacheMode;

	@Nullable
	private ValidationMode validationMode;

	private DataSourceLookup dataSourceLookup = new JndiDataSourceLookup();

	@Nullable
	private DataSource defaultDataSource;

	@Nullable
	private DataSource defaultJtaDataSource;

	@Nullable
	private PersistenceUnitPostProcessor[] persistenceUnitPostProcessors;

	@Nullable
	private LoadTimeWeaver loadTimeWeaver;

	private ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

	@Nullable
	private CandidateComponentsIndex componentsIndex;

	private final Set<String> persistenceUnitInfoNames = new HashSet<>();

	private final Map<String, PersistenceUnitInfo> persistenceUnitInfos = new HashMap<>();


	/**
	 * 指定要加载的 {@code persistence.xml} 文件的位置。
	 * 这些位置可以指定为 Spring 资源位置和/或位置模式。
	 * <p>默认是 "classpath*:META-INF/persistence.xml"。
	 */
	public void setPersistenceXmlLocation(String persistenceXmlLocation) {
		this.persistenceXmlLocations = new String[] {persistenceXmlLocation};
	}

	/**
	 * 指定要加载的 {@code persistence.xml} 文件的多个位置。
	 * 这些位置可以指定为 Spring 资源位置和/或位置模式。
	 * <p>默认是 "classpath*:META-INF/persistence.xml"。
	 * @param persistenceXmlLocations an array of Spring resource Strings
	 * identifying the location of the {@code persistence.xml} files to read
	 */
	public void setPersistenceXmlLocations(String... persistenceXmlLocations) {
		this.persistenceXmlLocations = persistenceXmlLocations;
	}

	/**
	 * 设置默认的持久化单元根位置，在无法确定单元特定的持久化单元根时使用。
	 * <p>默认是 "classpath:"，即当前类路径的根（最近的根目录）。
	 * 如果单元特定的解析不起作用，且类路径根也不合适，则可以覆盖此设置。
	 */
	public void setDefaultPersistenceUnitRootLocation(String defaultPersistenceUnitRootLocation) {
		this.defaultPersistenceUnitRootLocation = defaultPersistenceUnitRootLocation;
	}

	/**
	 * 指定默认持久化单元的名称（如果有）。默认是 "default"。
	 * <p>主要应用于通过扫描获得且没有 {@code persistence.xml} 的持久化单元。
	 * 也可用于从多个可用持久化单元中选择一个默认单元。
	 * @see #setPackagesToScan
	 * @see #obtainDefaultPersistenceUnitInfo
	 */
	public void setDefaultPersistenceUnitName(String defaultPersistenceUnitName) {
		this.defaultPersistenceUnitName = defaultPersistenceUnitName;
	}

	/**
	 * 设置是否使用 Spring 的扫描机制在类路径中扫描实体类，
	 * 而不使用 JPA 标准的、基于 {@code persistence.xml} 标记文件对 jar 文件的扫描。
	 * 在使用 Spring 扫描的情况下，不需要 {@code persistence.xml}；
	 * 只需在此处指定要搜索的基础包即可。
	 * <p>默认不扫描。请指定要搜索的包，以在类路径中自动检测实体类。
	 * 这类似于 Spring 的组件扫描特性
	 * （{@link org.springframework.context.annotation.ClassPathBeanDefinitionScanner}）。
	 * <p>此类包扫描会在 Spring 中定义一个"默认持久化单元"，
	 * 它可以与源自 {@code persistence.xml} 的常规定义的单元并存。
	 * 其名称由 {@link #setDefaultPersistenceUnitName} 决定：默认情况下，
	 * 就是 "default"。
	 * <p><b>注意：与常规 JPA 扫描相比，这里可能存在一些限制。</b>
	 * 尤其是，JPA 提供者只有在由 {@code persistence.xml} 驱动时，
	 * 才会拾取针对提供者特定注解的带注解包。自 4.1 起，如果给定的
	 * {@link org.springframework.orm.jpa.JpaVendorAdapter} 支持（例如 Hibernate），
	 * Spring 的扫描也可以检测到带注解的包。
	 * <p>如果在这些包之外没有显式指定
	 * {@link #setMappingResources 映射资源}，此管理器会在类路径中查找默认的
	 * {@code META-INF/orm.xml} 文件；如果该映射文件未与
	 * {@code persistence.xml} 文件放在一起，则将其注册为默认单元的映射资源
	 * （如果放在一起，我们假定它只打算与那里定义的持久化单元一起使用，这与标准 JPA 一致）。
	 * @see #setDefaultPersistenceUnitName
	 * @see #setMappingResources
	 */
	public void setPackagesToScan(String... packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

	/**
	 * 为默认持久化单元指定一个或多个映射资源（相当于 {@code persistence.xml}
	 * 中的 {@code <mapping-file>} 条目）。
	 * 可以单独使用，也可以与类路径中的实体扫描配合使用，
	 * 两种情况下都无需 {@code persistence.xml}。
	 * <p>注意，映射资源必须相对于类路径的根，例如 "META-INF/mappings.xml"
	 * 或 "com/mycompany/repository/mappings.xml"，
	 * 以便能通过 {@code ClassLoader.getResource} 加载它们。
	 * <p>如果在 {@link #setPackagesToScan 要扫描的包} 之外没有显式指定映射资源，
	 * 此管理器会在类路径中查找默认的 {@code META-INF/orm.xml} 文件；
	 * 如果该映射文件未与 {@code persistence.xml} 文件放在一起，
	 * 则将其注册为默认单元的映射资源（如果放在一起，我们假定它只打算与那里定义的
	 * 持久化单元一起使用，这与标准 JPA 一致）。
	 * <p>注意，在此指定空数组/列表会抑制对默认 {@code META-INF/orm.xml} 的检查。
	 * 另一方面，在此显式指定 {@code META-INF/orm.xml}
	 * 即使该文件恰好与 {@code persistence.xml} 文件放在一起，也会注册该文件。
	 * @see #setDefaultPersistenceUnitName
	 * @see #setPackagesToScan
	 */
	public void setMappingResources(String... mappingResources) {
		this.mappingResources = mappingResources;
	}

	/**
	 * 为此管理器所有持久化单元指定 JPA 2.0 共享缓存模式，
	 * 如果设置，则覆盖 {@code persistence.xml} 中的任何值。
	 * @since 4.0
	 * @see javax.persistence.spi.PersistenceUnitInfo#getSharedCacheMode()
	 */
	public void setSharedCacheMode(SharedCacheMode sharedCacheMode) {
		this.sharedCacheMode = sharedCacheMode;
	}

	/**
	 * 为此管理器所有持久化单元指定 JPA 2.0 校验模式，
	 * 如果设置，则覆盖 {@code persistence.xml} 中的任何值。
	 * @since 4.0
	 * @see javax.persistence.spi.PersistenceUnitInfo#getValidationMode()
	 */
	public void setValidationMode(ValidationMode validationMode) {
		this.validationMode = validationMode;
	}

	/**
	 * 指定 JPA 持久化提供者应用于访问数据库的 JDBC DataSource，
	 * 将 {@code persistence.xml} 中的数据源名称与 Spring 管理的 DataSource 进行解析匹配。
	 * <p>指定的 Map 需要为特定的 DataSource 对象定义数据源名称，
	 * 与 {@code persistence.xml} 中使用的数据源名称相匹配。
	 * 如果未指定，数据源名称将改为按 JNDI 名称解析
	 * （如标准 JPA 所定义）。
	 * @see org.springframework.jdbc.datasource.lookup.MapDataSourceLookup
	 */
	public void setDataSources(Map<String, DataSource> dataSources) {
		this.dataSourceLookup = new MapDataSourceLookup(dataSources);
	}

	/**
	 * 指定为持久化提供者提供 DataSource 的 JDBC DataSourceLookup，
	 * 将 {@code persistence.xml} 中的数据源名称与 Spring 管理的 DataSource 实例进行解析匹配。
	 * <p>默认是 JndiDataSourceLookup，它将 DataSource 名称解析为
	 * JNDI 名称（如标准 JPA 所定义）。如果你希望将 DataSource 名称解析为
	 * Spring bean 名称，请指定一个 BeanFactoryDataSourceLookup 实例。
	 * <p>或者，考虑通过 "dataSources" 属性传入从名称到 DataSource 实例的映射。
	 * 如果 {@code persistence.xml} 文件根本没有定义 DataSource 名称，
	 * 请通过 "defaultDataSource" 属性指定一个默认 DataSource。
	 * @see org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup
	 * @see org.springframework.jdbc.datasource.lookup.BeanFactoryDataSourceLookup
	 * @see #setDataSources
	 * @see #setDefaultDataSource
	 */
	public void setDataSourceLookup(@Nullable DataSourceLookup dataSourceLookup) {
		this.dataSourceLookup = (dataSourceLookup != null ? dataSourceLookup : new JndiDataSourceLookup());
	}

	/**
	 * 返回为持久化提供者提供 DataSource 的 JDBC DataSourceLookup，
	 * 它将 {@code persistence.xml} 中的数据源名称与 Spring 管理的 DataSource 实例进行解析匹配。
	 */
	@Nullable
	public DataSourceLookup getDataSourceLookup() {
		return this.dataSourceLookup;
	}

	/**
	 * 指定 JPA 持久化提供者在 {@code persistence.xml} 中未指定时
	 * 应用于访问数据库的 JDBC DataSource。
	 * 此变体表示没有特殊的事务设置，即典型的资源本地（resource-local）模式。
	 * <p>用 JPA 的术语来说，此处传入的 DataSource 将作为传递给 PersistenceProvider
	 * 的 PersistenceUnitInfo 上的 "nonJtaDataSource" 使用，前提是之前未注册过。
	 * @see javax.persistence.spi.PersistenceUnitInfo#getNonJtaDataSource()
	 */
	public void setDefaultDataSource(@Nullable DataSource defaultDataSource) {
		this.defaultDataSource = defaultDataSource;
	}

	/**
	 * 返回 JPA 持久化提供者在 {@code persistence.xml} 中未指定时
	 * 应用于访问数据库的 JDBC DataSource。
	 */
	@Nullable
	public DataSource getDefaultDataSource() {
		return this.defaultDataSource;
	}

	/**
	 * 指定 JPA 持久化提供者在 {@code persistence.xml} 中未指定时
	 * 应用于访问数据库的 JDBC DataSource。
	 * 此变体表示应使用 JTA 作为事务类型。
	 * <p>用 JPA 的术语来说，此处传入的 DataSource 将作为传递给 PersistenceProvider
	 * 的 PersistenceUnitInfo 上的 "jtaDataSource" 使用，前提是之前未注册过。
	 * @see javax.persistence.spi.PersistenceUnitInfo#getJtaDataSource()
	 */
	public void setDefaultJtaDataSource(@Nullable DataSource defaultJtaDataSource) {
		this.defaultJtaDataSource = defaultJtaDataSource;
	}

	/**
	 * 返回 JPA 持久化提供者在 {@code persistence.xml} 中未指定时
	 * 应用于访问数据库的、支持 JTA 的 DataSource。
	 */
	@Nullable
	public DataSource getDefaultJtaDataSource() {
		return this.defaultJtaDataSource;
	}

	/**
	 * 设置要应用于此管理器解析的每个 PersistenceUnitInfo 的 PersistenceUnitPostProcessors。
	 * <p>例如，此类后处理器可以在从 {@code persistence.xml} 读取的元数据之外，
	 * 注册更多的实体类和 jar 文件。
	 */
	public void setPersistenceUnitPostProcessors(@Nullable PersistenceUnitPostProcessor... postProcessors) {
		this.persistenceUnitPostProcessors = postProcessors;
	}

	/**
	 * 返回要应用于此管理器解析的每个 PersistenceUnitInfo 的 PersistenceUnitPostProcessors。
	 */
	@Nullable
	public PersistenceUnitPostProcessor[] getPersistenceUnitPostProcessors() {
		return this.persistenceUnitPostProcessors;
	}

	/**
	 * 指定用于按 JPA 类转换器契约进行类插桩的 Spring LoadTimeWeaver。
	 * <p>并不强制要求指定 LoadTimeWeaver：大多数提供者即使没有类插桩，
	 * 也能提供其功能的一个子集，或者使用在 JVM 启动时指定的自有 VM 代理运行。
	 * 此外，如果运行时 Spring 基于代理的插桩可用，
	 * DefaultPersistenceUnitManager 会回退到 InstrumentationLoadTimeWeaver。
	 * <p>就 Spring 提供的织入选项而言，最重要的是 InstrumentationLoadTimeWeaver，
	 * 它需要在 JVM 启动时指定一个 Spring 专用（但非常通用）的 VM 代理；
	 * 以及 ReflectiveLoadTimeWeaver，它基于底层 ClassLoader 上可用的特定扩展方法与
	 * 其交互（例如与 Spring 的 TomcatInstrumentableClassLoader 交互）。
	 * 可以考虑使用 {@code context:load-time-weaver} XML 标签来创建
	 * 这样一个共享的 LoadTimeWeaver（默认自动检测环境）。
	 * @see org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver
	 * @see org.springframework.instrument.classloading.ReflectiveLoadTimeWeaver
	 */
	@Override
	public void setLoadTimeWeaver(@Nullable LoadTimeWeaver loadTimeWeaver) {
		this.loadTimeWeaver = loadTimeWeaver;
	}

	/**
	 * 返回用于按 JPA 类转换器契约进行类插桩的 Spring LoadTimeWeaver。
	 */
	@Nullable
	public LoadTimeWeaver getLoadTimeWeaver() {
		return this.loadTimeWeaver;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
		this.componentsIndex = CandidateComponentsIndexLoader.loadIndex(resourceLoader.getClassLoader());
	}


	@Override
	public void afterPropertiesSet() {
		if (this.loadTimeWeaver == null && InstrumentationLoadTimeWeaver.isInstrumentationAvailable()) {
			this.loadTimeWeaver = new InstrumentationLoadTimeWeaver(this.resourcePatternResolver.getClassLoader());
		}
		preparePersistenceUnitInfos();
	}

	/**
	 * 根据此管理器的配置准备 PersistenceUnitInfo：扫描 {@code persistence.xml} 文件，
	 * 解析所有匹配的文件，并对它们进行配置和后处理。
	 * <p>在调用此准备方法之前，无法获取 PersistenceUnitInfo。
	 * @see #obtainDefaultPersistenceUnitInfo()
	 * @see #obtainPersistenceUnitInfo(String)
	 */
	public void preparePersistenceUnitInfos() {
		this.persistenceUnitInfoNames.clear();
		this.persistenceUnitInfos.clear();

		List<SpringPersistenceUnitInfo> puis = readPersistenceUnitInfos();
		for (SpringPersistenceUnitInfo pui : puis) {
			if (pui.getPersistenceUnitRootUrl() == null) {
				pui.setPersistenceUnitRootUrl(determineDefaultPersistenceUnitRootUrl());
			}
			if (pui.getJtaDataSource() == null && this.defaultJtaDataSource != null) {
				pui.setJtaDataSource(this.defaultJtaDataSource);
			}
			if (pui.getNonJtaDataSource() == null && this.defaultDataSource != null) {
				pui.setNonJtaDataSource(this.defaultDataSource);
			}
			if (this.sharedCacheMode != null) {
				pui.setSharedCacheMode(this.sharedCacheMode);
			}
			if (this.validationMode != null) {
				pui.setValidationMode(this.validationMode);
			}
			if (this.loadTimeWeaver != null) {
				pui.init(this.loadTimeWeaver);
			}
			else {
				pui.init(this.resourcePatternResolver.getClassLoader());
			}
			postProcessPersistenceUnitInfo(pui);
			String name = pui.getPersistenceUnitName();
			if (!this.persistenceUnitInfoNames.add(name) && !isPersistenceUnitOverrideAllowed()) {
				StringBuilder msg = new StringBuilder();
				msg.append("Conflicting persistence unit definitions for name '").append(name).append("': ");
				msg.append(pui.getPersistenceUnitRootUrl()).append(", ");
				msg.append(this.persistenceUnitInfos.get(name).getPersistenceUnitRootUrl());
				throw new IllegalStateException(msg.toString());
			}
			this.persistenceUnitInfos.put(name, pui);
		}
	}

	/**
	 * 从 {@code persistence.xml} 中读取所有持久化单元信息，
	 * 依据 JPA 规范的定义。
	 */
	private List<SpringPersistenceUnitInfo> readPersistenceUnitInfos() {
		List<SpringPersistenceUnitInfo> infos = new ArrayList<>(1);
		String defaultName = this.defaultPersistenceUnitName;
		boolean buildDefaultUnit = (this.packagesToScan != null || this.mappingResources != null);
		boolean foundDefaultUnit = false;

		PersistenceUnitReader reader = new PersistenceUnitReader(this.resourcePatternResolver, this.dataSourceLookup);
		SpringPersistenceUnitInfo[] readInfos = reader.readPersistenceUnitInfos(this.persistenceXmlLocations);
		for (SpringPersistenceUnitInfo readInfo : readInfos) {
			infos.add(readInfo);
			if (defaultName != null && defaultName.equals(readInfo.getPersistenceUnitName())) {
				foundDefaultUnit = true;
			}
		}

		if (buildDefaultUnit) {
			if (foundDefaultUnit) {
				if (logger.isWarnEnabled()) {
					logger.warn("Found explicit default persistence unit with name '" + defaultName + "' in persistence.xml - " +
							"overriding local default persistence unit settings ('packagesToScan'/'mappingResources')");
				}
			}
			else {
				infos.add(buildDefaultPersistenceUnitInfo());
			}
		}
		return infos;
	}

	/**
	 * 执行基于 Spring 的实体类扫描。
	 * @see #setPackagesToScan
	 */
	private SpringPersistenceUnitInfo buildDefaultPersistenceUnitInfo() {
		SpringPersistenceUnitInfo scannedUnit = new SpringPersistenceUnitInfo();
		if (this.defaultPersistenceUnitName != null) {
			scannedUnit.setPersistenceUnitName(this.defaultPersistenceUnitName);
		}
		scannedUnit.setExcludeUnlistedClasses(true);

		if (this.packagesToScan != null) {
			for (String pkg : this.packagesToScan) {
				scanPackage(scannedUnit, pkg);
			}
		}

		if (this.mappingResources != null) {
			for (String mappingFileName : this.mappingResources) {
				scannedUnit.addMappingFileName(mappingFileName);
			}
		}
		else {
			Resource ormXml = getOrmXmlForDefaultPersistenceUnit();
			if (ormXml != null) {
				scannedUnit.addMappingFileName(DEFAULT_ORM_XML_RESOURCE);
				if (scannedUnit.getPersistenceUnitRootUrl() == null) {
					try {
						scannedUnit.setPersistenceUnitRootUrl(
								PersistenceUnitReader.determinePersistenceUnitRootUrl(ormXml));
					}
					catch (IOException ex) {
						logger.debug("Failed to determine persistence unit root URL from orm.xml location", ex);
					}
				}
			}
		}

		return scannedUnit;
	}

	private void scanPackage(SpringPersistenceUnitInfo scannedUnit, String pkg) {
		if (this.componentsIndex != null) {
			Set<String> candidates = new HashSet<>();
			for (AnnotationTypeFilter filter : entityTypeFilters) {
				candidates.addAll(this.componentsIndex.getCandidateTypes(pkg, filter.getAnnotationType().getName()));
			}
			candidates.forEach(scannedUnit::addManagedClassName);
			Set<String> managedPackages = this.componentsIndex.getCandidateTypes(pkg, "package-info");
			managedPackages.forEach(scannedUnit::addManagedPackage);
			return;
		}

		try {
			String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
					ClassUtils.convertClassNameToResourcePath(pkg) + CLASS_RESOURCE_PATTERN;
			Resource[] resources = this.resourcePatternResolver.getResources(pattern);
			MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(this.resourcePatternResolver);
			for (Resource resource : resources) {
				try {
					MetadataReader reader = readerFactory.getMetadataReader(resource);
					String className = reader.getClassMetadata().getClassName();
					if (matchesFilter(reader, readerFactory)) {
						scannedUnit.addManagedClassName(className);
						if (scannedUnit.getPersistenceUnitRootUrl() == null) {
							URL url = resource.getURL();
							if (ResourceUtils.isJarURL(url)) {
								scannedUnit.setPersistenceUnitRootUrl(ResourceUtils.extractJarFileURL(url));
							}
						}
					}
					else if (className.endsWith(PACKAGE_INFO_SUFFIX)) {
						scannedUnit.addManagedPackage(
								className.substring(0, className.length() - PACKAGE_INFO_SUFFIX.length()));
					}
				}
				catch (FileNotFoundException ex) {
					// Ignore non-readable resource
				}
			}
		}
		catch (IOException ex) {
			throw new PersistenceException("Failed to scan classpath for unlisted entity classes", ex);
		}
	}

	/**
	 * 检查配置的任意实体类型过滤器是否与元数据读取器中包含的
	 * 当前类描述符匹配。
	 */
	private boolean matchesFilter(MetadataReader reader, MetadataReaderFactory readerFactory) throws IOException {
		for (TypeFilter filter : entityTypeFilters) {
			if (filter.match(reader, readerFactory)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 尝试根据给定的 "defaultPersistenceUnitRootLocation"
	 * 确定持久化单元根 URL。
	 * @return the persistence unit root URL to pass to the JPA PersistenceProvider
	 * @see #setDefaultPersistenceUnitRootLocation
	 */
	@Nullable
	private URL determineDefaultPersistenceUnitRootUrl() {
		if (this.defaultPersistenceUnitRootLocation == null) {
			return null;
		}
		try {
			URL url = this.resourcePatternResolver.getResource(this.defaultPersistenceUnitRootLocation).getURL();
			return (ResourceUtils.isJarURL(url) ? ResourceUtils.extractJarFileURL(url) : url);
		}
		catch (IOException ex) {
			throw new PersistenceException("Unable to resolve persistence unit root URL", ex);
		}
	}

	/**
	 * 确定 JPA 默认的 "META-INF/orm.xml" 资源（如果有），以供 Spring 的默认持久化单元使用。
	 * <p>检查类路径中是否存在 "META-INF/orm.xml" 文件，如果它未与
	 * "META-INF/persistence.xml" 文件放在一起，则使用它。
	 */
	@Nullable
	private Resource getOrmXmlForDefaultPersistenceUnit() {
		Resource ormXml = this.resourcePatternResolver.getResource(
				this.defaultPersistenceUnitRootLocation + DEFAULT_ORM_XML_RESOURCE);
		if (ormXml.exists()) {
			try {
				Resource persistenceXml = ormXml.createRelative(PERSISTENCE_XML_FILENAME);
				if (!persistenceXml.exists()) {
					return ormXml;
				}
			}
			catch (IOException ex) {
				// Cannot resolve relative persistence.xml file - let's assume it's not there.
				return ormXml;
			}
		}
		return null;
	}


	/**
	 * 从此管理器已处理的持久化单元缓存中返回指定的 PersistenceUnitInfo，
	 * 并将其保留在缓存中（即不是为使用而"获取"它，而只是为后处理而访问它）。
	 * <p>这可用于 {@link #postProcessPersistenceUnitInfo} 实现中，
	 * 检测已存在的同名持久化单元，并可能将它们合并。
	 * @param persistenceUnitName the name of the desired persistence unit
	 * @return the PersistenceUnitInfo in mutable form, or {@code null} if not available
	 */
	@Nullable
	protected final MutablePersistenceUnitInfo getPersistenceUnitInfo(String persistenceUnitName) {
		PersistenceUnitInfo pui = this.persistenceUnitInfos.get(persistenceUnitName);
		return (MutablePersistenceUnitInfo) pui;
	}

	/**
	 * 钩子方法，允许子类自定义每个 PersistenceUnitInfo。
	 * <p>默认实现委托给所有已注册的 PersistenceUnitPostProcessors。
	 * 通常最好在那里注册更多的实体类、jar 文件等，
	 * 而不是在此管理器的子类中，以便能够复用这些后处理器。
	 * @param pui the chosen PersistenceUnitInfo, as read from {@code persistence.xml}.
	 * Passed in as MutablePersistenceUnitInfo.
	 * @see #setPersistenceUnitPostProcessors
	 */
	protected void postProcessPersistenceUnitInfo(MutablePersistenceUnitInfo pui) {
		PersistenceUnitPostProcessor[] postProcessors = getPersistenceUnitPostProcessors();
		if (postProcessors != null) {
			for (PersistenceUnitPostProcessor postProcessor : postProcessors) {
				postProcessor.postProcessPersistenceUnitInfo(pui);
			}
		}
	}

	/**
	 * 返回是否允许覆盖同名的持久化单元。
	 * <p>默认是 {@code false}。可以重写为返回 {@code true}，
	 * 例如当 {@link #postProcessPersistenceUnitInfo} 能够处理这种情况时。
	 */
	protected boolean isPersistenceUnitOverrideAllowed() {
		return false;
	}


	@Override
	public PersistenceUnitInfo obtainDefaultPersistenceUnitInfo() {
		if (this.persistenceUnitInfoNames.isEmpty()) {
			throw new IllegalStateException("No persistence units parsed from " +
					ObjectUtils.nullSafeToString(this.persistenceXmlLocations));
		}
		if (this.persistenceUnitInfos.isEmpty()) {
			throw new IllegalStateException("All persistence units from " +
					ObjectUtils.nullSafeToString(this.persistenceXmlLocations) + " already obtained");
		}
		if (this.persistenceUnitInfos.size() > 1 && this.defaultPersistenceUnitName != null) {
			return obtainPersistenceUnitInfo(this.defaultPersistenceUnitName);
		}
		PersistenceUnitInfo pui = this.persistenceUnitInfos.values().iterator().next();
		this.persistenceUnitInfos.clear();
		return pui;
	}

	@Override
	public PersistenceUnitInfo obtainPersistenceUnitInfo(String persistenceUnitName) {
		PersistenceUnitInfo pui = this.persistenceUnitInfos.remove(persistenceUnitName);
		if (pui == null) {
			if (!this.persistenceUnitInfoNames.contains(persistenceUnitName)) {
				throw new IllegalArgumentException(
						"No persistence unit with name '" + persistenceUnitName + "' found");
			}
			else {
				throw new IllegalStateException(
						"Persistence unit with name '" + persistenceUnitName + "' already obtained");
			}
		}
		return pui;
	}

}
