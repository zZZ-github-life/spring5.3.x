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

package org.springframework.orm.jpa;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.sql.DataSource;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.weaving.LoadTimeWeaverAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.jdbc.datasource.lookup.SingleDataSourceLookup;
import org.springframework.lang.Nullable;
import org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;
import org.springframework.orm.jpa.persistenceunit.SmartPersistenceUnitInfo;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 按照 JPA 标准<i>容器</i>引导契约创建 JPA
 * {@link javax.persistence.EntityManagerFactory} 的
 * {@link org.springframework.beans.factory.FactoryBean}。
 * 这是在 Spring 应用上下文中设置共享 JPA EntityManagerFactory 最强大的方式；
 * 然后可以通过依赖注入将 EntityManagerFactory 传递给基于 JPA 的 DAO。
 * 请注意，切换到 JNDI 查找或 {@link LocalEntityManagerFactoryBean} 定义
 * 只是配置上的变化！
 *
 * <p>与 {@link LocalEntityManagerFactoryBean} 一样，配置设置通常
 * 按照通用 JPA 配置契约从位于类路径中的 {@code META-INF/persistence.xml}
 * 配置文件读取。不过，此 FactoryBean 更加灵活：
 * 你可以覆盖 {@code persistence.xml} 文件的位置、指定要链接的 JDBC DataSource 等。
 * 此外，它还允许通过 Spring 的
 * {@link org.springframework.instrument.classloading.LoadTimeWeaver} 抽象
 * 插入可插拔的类插桩，而不必绑定到 JVM 启动时指定的特殊 VM agent。
 *
 * <p>在内部，此 FactoryBean 会自行解析 {@code persistence.xml} 文件，
 * 并创建相应的 {@link javax.persistence.spi.PersistenceUnitInfo} 对象
 * （并合并进一步配置，例如 JDBC DataSource 和 Spring LoadTimeWeaver），
 * 然后将该对象传递给所选的 JPA
 * {@link javax.persistence.spi.PersistenceProvider}。
 * 这相当于一个完整支持标准 JPA 容器契约的本地 JPA 容器。
 *
 * <p>所暴露的 EntityManagerFactory 对象将实现 PersistenceProvider 返回的
 * 底层原生 EntityManagerFactory 的所有接口，以及
 * {@link EntityManagerFactoryInfo} 接口，后者暴露此 FactoryBean
 * 所汇总的额外元数据。
 *
 * <p><b>注意：自 Spring 5.0 起，Spring 的 JPA 支持要求 JPA 2.1 或更高版本。</b>
 * 基于 JPA 1.0/2.0 的应用程序仍然受支持；不过，运行时需要符合 JPA 2.1 的
 * 持久化提供者。
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 2.0
 * @see #setPersistenceXmlLocation
 * @see #setJpaProperties
 * @see #setJpaVendorAdapter
 * @see #setLoadTimeWeaver
 * @see #setDataSource
 * @see EntityManagerFactoryInfo
 * @see LocalEntityManagerFactoryBean
 * @see org.springframework.orm.jpa.support.SharedEntityManagerBean
 * @see javax.persistence.spi.PersistenceProvider#createContainerEntityManagerFactory
 */
@SuppressWarnings("serial")
public class LocalContainerEntityManagerFactoryBean extends AbstractEntityManagerFactoryBean
		implements ResourceLoaderAware, LoadTimeWeaverAware {

	@Nullable
	private PersistenceUnitManager persistenceUnitManager;

	private final DefaultPersistenceUnitManager internalPersistenceUnitManager = new DefaultPersistenceUnitManager();

	@Nullable
	private PersistenceUnitInfo persistenceUnitInfo;


	/**
	 * 设置用于获取 JPA 持久化单元的 PersistenceUnitManager，
	 * 此 FactoryBean 将为其构建 EntityManagerFactory。
	 * <p>默认情况下，依赖此 FactoryBean 上指定的本地设置，
	 * 例如 "persistenceXmlLocation"、"dataSource" 和 "loadTimeWeaver"。
	 * <p>如需复用现有的持久化单元配置或采用更高级的自定义持久化单元处理方式，
	 * 可考虑定义一个单独的 PersistenceUnitManager bean
	 * （通常是 DefaultPersistenceUnitManager 实例）并在此处链接它。
	 * 在这种场景下，{@code persistence.xml} 位置、DataSource 配置和
	 * LoadTimeWeaver 将在该单独的 DefaultPersistenceUnitManager bean 上定义。
	 * @see #setPersistenceXmlLocation
	 * @see #setDataSource
	 * @see #setLoadTimeWeaver
	 * @see org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager
	 */
	public void setPersistenceUnitManager(PersistenceUnitManager persistenceUnitManager) {
		this.persistenceUnitManager = persistenceUnitManager;
	}

	/**
	 * 设置我们要使用的 {@code persistence.xml} 文件的位置。
	 * 这是一个 Spring 资源位置。
	 * <p>默认值为 "classpath:META-INF/persistence.xml"。
	 * <p><b>注意：仅在未指定外部 PersistenceUnitManager 时生效。</b>
	 * @param persistenceXmlLocation a Spring resource String
	 * identifying the location of the {@code persistence.xml} file
	 * that this LocalContainerEntityManagerFactoryBean should parse
	 * @see #setPersistenceUnitManager
	 */
	public void setPersistenceXmlLocation(String persistenceXmlLocation) {
		this.internalPersistenceUnitManager.setPersistenceXmlLocation(persistenceXmlLocation);
	}

	/**
	 * 如果适用，将指定的持久化单元名称用作默认持久化单元的名称。
	 * <p><b>注意：仅在未指定外部 PersistenceUnitManager 时生效。</b>
	 * @see DefaultPersistenceUnitManager#setDefaultPersistenceUnitName
	 */
	@Override
	public void setPersistenceUnitName(@Nullable String persistenceUnitName) {
		super.setPersistenceUnitName(persistenceUnitName);
		if (persistenceUnitName != null) {
			this.internalPersistenceUnitManager.setDefaultPersistenceUnitName(persistenceUnitName);
		}
	}

	/**
	 * 为默认持久化单元设置持久化单元根位置。
	 * <p>默认值为 "classpath:"，即当前类路径的根（最近的根目录）。
	 * 如果单元特定的解析不起作用且类路径根也不合适，可以覆盖此设置。
	 * <p><b>注意：仅在未指定外部 PersistenceUnitManager 时生效。</b>
	 * @since 4.3.3
	 * @see DefaultPersistenceUnitManager#setDefaultPersistenceUnitRootLocation
	 */
	public void setPersistenceUnitRootLocation(String defaultPersistenceUnitRootLocation) {
		this.internalPersistenceUnitManager.setDefaultPersistenceUnitRootLocation(defaultPersistenceUnitRootLocation);
	}

	/**
	 * 设置是否使用基于 Spring 的类路径实体类扫描，
	 * 而不使用 JPA 标准的基于 jar 文件中 {@code persistence.xml} 标记的扫描。
	 * 在基于 Spring 的扫描情况下，无需 {@code persistence.xml}；
	 * 你只需在此处指定要搜索的基础包。
	 * <p>默认不指定。指定包以自动检测类路径中的实体类。
	 * 这类似于 Spring 的组件扫描特性
	 * （{@link org.springframework.context.annotation.ClassPathBeanDefinitionScanner}）。
	 * <p><b>注意：与常规 JPA 扫描相比可能存在限制。</b>
	 * 特别是，JPA 提供者可能仅在由 {@code persistence.xml} 驱动时
	 * 才会识别带有提供者特定注解的包。自 4.1 起，
	 * 如果给定的 {@link JpaVendorAdapter} 支持（例如对于 Hibernate），
	 * Spring 的扫描也可以检测带注解的包。
	 * <p>如果除了这些包之外没有指定显式的
	 * {@link #setMappingResources 映射资源}，Spring 的设置会在类路径中
	 * 查找默认的 {@code META-INF/orm.xml} 文件；如果该映射文件
	 * 没有与 {@code persistence.xml} 文件位于同一位置（在这种情况下，
	 * 我们假定它仅用于在那里定义的持久化单元，如同标准 JPA 那样），
	 * 则将其注册为默认单元的映射资源。
	 * <p><b>注意：仅在未指定外部 PersistenceUnitManager 时生效。</b>
	 * @param packagesToScan one or more base packages to search, analogous to
	 * Spring's component-scan configuration for regular Spring components
	 * @see #setPersistenceUnitManager
	 * @see DefaultPersistenceUnitManager#setPackagesToScan
	 */
	public void setPackagesToScan(String... packagesToScan) {
		this.internalPersistenceUnitManager.setPackagesToScan(packagesToScan);
	}

	/**
	 * 为默认持久化单元指定一个或多个映射资源
	 * （相当于 {@code persistence.xml} 中的 {@code <mapping-file>} 条目）。
	 * 可以单独使用，也可以与类路径中的实体扫描结合使用，
	 * 两者都可以避免使用 {@code persistence.xml}。
	 * <p>请注意，映射资源必须相对于类路径根，
	 * 例如 "META-INF/mappings.xml" 或 "com/mycompany/repository/mappings.xml"，
	 * 这样才能通过 {@code ClassLoader.getResource} 加载。
	 * <p>如果在 {@link #setPackagesToScan 要扫描的包} 之外
	 * 没有指定显式的映射资源，Spring 的设置会在类路径中查找默认的
	 * {@code META-INF/orm.xml} 文件；如果该映射文件没有与
	 * {@code persistence.xml} 文件位于同一位置（在这种情况下，
	 * 我们假定它仅用于在那里定义的持久化单元，如同标准 JPA 那样），
	 * 则将其注册为默认单元的映射资源。
	 * <p>请注意，在此处指定空数组/空列表会抑制对默认
	 * {@code META-INF/orm.xml} 的检查。相反，在此处显式指定
	 * {@code META-INF/orm.xml} 将注册该文件，即使它恰好与
	 * {@code persistence.xml} 文件位于同一位置。
	 * <p><b>注意：仅在未指定外部 PersistenceUnitManager 时生效。</b>
	 * @see #setPersistenceUnitManager
	 * @see DefaultPersistenceUnitManager#setMappingResources
	 */
	public void setMappingResources(String... mappingResources) {
		this.internalPersistenceUnitManager.setMappingResources(mappingResources);
	}

	/**
	 * 为此持久化单元指定 JPA 2.0 共享缓存模式，
	 * 如果已设置则覆盖 {@code persistence.xml} 中的值。
	 * <p><b>注意：仅在未指定外部 PersistenceUnitManager 时生效。</b>
	 * @since 4.0
	 * @see javax.persistence.spi.PersistenceUnitInfo#getSharedCacheMode()
	 * @see #setPersistenceUnitManager
	 */
	public void setSharedCacheMode(SharedCacheMode sharedCacheMode) {
		this.internalPersistenceUnitManager.setSharedCacheMode(sharedCacheMode);
	}

	/**
	 * 为此持久化单元指定 JPA 2.0 验证模式，
	 * 如果已设置则覆盖 {@code persistence.xml} 中的值。
	 * <p><b>注意：仅在未指定外部 PersistenceUnitManager 时生效。</b>
	 * @since 4.0
	 * @see javax.persistence.spi.PersistenceUnitInfo#getValidationMode()
	 * @see #setPersistenceUnitManager
	 */
	public void setValidationMode(ValidationMode validationMode) {
		this.internalPersistenceUnitManager.setValidationMode(validationMode);
	}

	/**
	 * 指定 JPA 持久化提供者应用于访问数据库的 JDBC DataSource。
	 * 这是将 JDBC 配置保留在 {@code persistence.xml} 中并改为传入
	 * Spring 管理的 DataSource 的替代方式。
	 * <p>用 JPA 的话来说，此处传入的 DataSource 将作为传递给
	 * PersistenceProvider 的 PersistenceUnitInfo 上的 "nonJtaDataSource"，
	 * 并会覆盖 {@code persistence.xml} 中的数据源配置（如果有）。
	 * 请注意，此变体通常也适用于 JTA 事务管理；
	 * 如果不适用，请考虑改用显式的 {@link #setJtaDataSource}。
	 * <p><b>注意：仅在未指定外部 PersistenceUnitManager 时生效。</b>
	 * @see javax.persistence.spi.PersistenceUnitInfo#getNonJtaDataSource()
	 * @see #setPersistenceUnitManager
	 */
	public void setDataSource(DataSource dataSource) {
		this.internalPersistenceUnitManager.setDataSourceLookup(new SingleDataSourceLookup(dataSource));
		this.internalPersistenceUnitManager.setDefaultDataSource(dataSource);
	}

	/**
	 * 指定 JPA 持久化提供者应用于访问数据库的 JDBC DataSource。
	 * 这是将 JDBC 配置保留在 {@code persistence.xml} 中并改为传入
	 * Spring 管理的 DataSource 的替代方式。
	 * <p>用 JPA 的话来说，此处传入的 DataSource 将作为传递给
	 * PersistenceProvider 的 PersistenceUnitInfo 上的 "jtaDataSource"，
	 * 并会覆盖 {@code persistence.xml} 中的数据源配置（如果有）。
	 * <p><b>注意：仅在未指定外部 PersistenceUnitManager 时生效。</b>
	 * @see javax.persistence.spi.PersistenceUnitInfo#getJtaDataSource()
	 * @see #setPersistenceUnitManager
	 */
	public void setJtaDataSource(DataSource jtaDataSource) {
		this.internalPersistenceUnitManager.setDataSourceLookup(new SingleDataSourceLookup(jtaDataSource));
		this.internalPersistenceUnitManager.setDefaultJtaDataSource(jtaDataSource);
	}

	/**
	 * 设置要应用于用于创建此 EntityManagerFactory 的 PersistenceUnitInfo
	 * 的 PersistenceUnitPostProcessor。
	 * <p>例如，此类后处理器可以在从 {@code persistence.xml}
	 * 读取的元数据之外，注册更多的实体类和 jar 文件。
	 * <p><b>注意：仅在未指定外部 PersistenceUnitManager 时生效。</b>
	 * @see #setPersistenceUnitManager
	 */
	public void setPersistenceUnitPostProcessors(PersistenceUnitPostProcessor... postProcessors) {
		this.internalPersistenceUnitManager.setPersistenceUnitPostProcessors(postProcessors);
	}

	/**
	 * 指定用于按照 JPA 类转换器契约进行类插桩的 Spring LoadTimeWeaver。
	 * <p>并非必须指定 LoadTimeWeaver：大多数提供者即使没有类插桩
	 * 也能提供其部分功能，或者可以使用在 JVM 启动时指定的 VM agent 运行。
	 * <p>就 Spring 提供的织入选项而言，最重要的两个是
	 * InstrumentationLoadTimeWeaver（它需要在 JVM 启动时指定一个 Spring 特定的、
	 * 但非常通用的 VM agent）和 ReflectiveLoadTimeWeaver（它基于底层
	 * ClassLoader 上可用的特定扩展方法与 ClassLoader 交互）。
	 * <p><b>注意：</b>自 Spring 2.5 起，如果可用，
	 * 上下文的默认 LoadTimeWeaver（定义为名为 "loadTimeWeaver" 的 bean）
	 * 将被自动获取，从而无需在每个受影响的 target bean 上配置 LoadTimeWeaver。
	 * 考虑使用 {@code context:load-time-weaver} XML 标签来创建这样一个共享的
	 * LoadTimeWeaver（默认自动检测环境）。
	 * <p><b>注意：</b>仅在未指定外部 PersistenceUnitManager 时生效。
	 * 否则，由外部 {@link #setPersistenceUnitManager PersistenceUnitManager}
	 * 负责织入配置。
	 * @see org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver
	 * @see org.springframework.instrument.classloading.ReflectiveLoadTimeWeaver
	 */
	@Override
	public void setLoadTimeWeaver(LoadTimeWeaver loadTimeWeaver) {
		this.internalPersistenceUnitManager.setLoadTimeWeaver(loadTimeWeaver);
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.internalPersistenceUnitManager.setResourceLoader(resourceLoader);
	}


	@Override
	public void afterPropertiesSet() throws PersistenceException {
		PersistenceUnitManager managerToUse = this.persistenceUnitManager;
		if (this.persistenceUnitManager == null) {
			this.internalPersistenceUnitManager.afterPropertiesSet();
			managerToUse = this.internalPersistenceUnitManager;
		}

		this.persistenceUnitInfo = determinePersistenceUnitInfo(managerToUse);
		JpaVendorAdapter jpaVendorAdapter = getJpaVendorAdapter();
		if (jpaVendorAdapter != null && this.persistenceUnitInfo instanceof SmartPersistenceUnitInfo) {
			String rootPackage = jpaVendorAdapter.getPersistenceProviderRootPackage();
			if (rootPackage != null) {
				((SmartPersistenceUnitInfo) this.persistenceUnitInfo).setPersistenceProviderPackageName(rootPackage);
			}
		}

		super.afterPropertiesSet();
	}

	@Override
	protected EntityManagerFactory createNativeEntityManagerFactory() throws PersistenceException {
		Assert.state(this.persistenceUnitInfo != null, "PersistenceUnitInfo not initialized");

		PersistenceProvider provider = getPersistenceProvider();
		if (provider == null) {
			String providerClassName = this.persistenceUnitInfo.getPersistenceProviderClassName();
			if (providerClassName == null) {
				throw new IllegalArgumentException(
						"No PersistenceProvider specified in EntityManagerFactory configuration, " +
						"and chosen PersistenceUnitInfo does not specify a provider class name either");
			}
			Class<?> providerClass = ClassUtils.resolveClassName(providerClassName, getBeanClassLoader());
			provider = (PersistenceProvider) BeanUtils.instantiateClass(providerClass);
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Building JPA container EntityManagerFactory for persistence unit '" +
					this.persistenceUnitInfo.getPersistenceUnitName() + "'");
		}
		EntityManagerFactory emf =
				provider.createContainerEntityManagerFactory(this.persistenceUnitInfo, getJpaPropertyMap());
		postProcessEntityManagerFactory(emf, this.persistenceUnitInfo);

		return emf;
	}


	/**
	 * 确定要用于此 bean 创建的 EntityManagerFactory 的 PersistenceUnitInfo。
	 * <p>默认实现从 {@code persistence.xml} 中读取所有持久化单元信息，
	 * 如 JPA 规范所定义。如果未指定实体管理器名称，
	 * 则取读取器返回的数组中的第一个信息。否则，检查是否有匹配的名称。
	 * @param persistenceUnitManager the PersistenceUnitManager to obtain from
	 * @return the chosen PersistenceUnitInfo
	 */
	protected PersistenceUnitInfo determinePersistenceUnitInfo(PersistenceUnitManager persistenceUnitManager) {
		if (getPersistenceUnitName() != null) {
			return persistenceUnitManager.obtainPersistenceUnitInfo(getPersistenceUnitName());
		}
		else {
			return persistenceUnitManager.obtainDefaultPersistenceUnitInfo();
		}
	}

	/**
	 * 钩子方法，允许子类在通过 PersistenceProvider 创建 EntityManagerFactory
	 * 之后对其进行定制。
	 * <p>默认实现为空。
	 * @param emf the newly created EntityManagerFactory we are working with
	 * @param pui the PersistenceUnitInfo used to configure the EntityManagerFactory
	 * @see javax.persistence.spi.PersistenceProvider#createContainerEntityManagerFactory
	 */
	protected void postProcessEntityManagerFactory(EntityManagerFactory emf, PersistenceUnitInfo pui) {
	}


	@Override
	@Nullable
	public PersistenceUnitInfo getPersistenceUnitInfo() {
		return this.persistenceUnitInfo;
	}

	@Override
	@Nullable
	public String getPersistenceUnitName() {
		if (this.persistenceUnitInfo != null) {
			return this.persistenceUnitInfo.getPersistenceUnitName();
		}
		return super.getPersistenceUnitName();
	}

	@Override
	public DataSource getDataSource() {
		if (this.persistenceUnitInfo != null) {
			return (this.persistenceUnitInfo.getJtaDataSource() != null ?
					this.persistenceUnitInfo.getJtaDataSource() :
					this.persistenceUnitInfo.getNonJtaDataSource());
		}
		return (this.internalPersistenceUnitManager.getDefaultJtaDataSource() != null ?
				this.internalPersistenceUnitManager.getDefaultJtaDataSource() :
				this.internalPersistenceUnitManager.getDefaultDataSource());
	}

}
