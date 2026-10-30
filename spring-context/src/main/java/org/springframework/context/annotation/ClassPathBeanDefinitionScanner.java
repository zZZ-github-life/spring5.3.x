/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.context.annotation;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionDefaults;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;

/**
 * 一种 Bean 定义扫描器，用于检测类路径上的 Bean 候选者，并将相应的 Bean 定义注册到给定的注册表
 * （{@code BeanFactory} 或 {@code ApplicationContext}）。
 *
 * <p>候选类通过可配置的类型过滤器进行检测。默认过滤器包含标注了 Spring 的
 * {@link org.springframework.stereotype.Component @Component}、
 * {@link org.springframework.stereotype.Repository @Repository}、
 * {@link org.springframework.stereotype.Service @Service} 或
 * {@link org.springframework.stereotype.Controller @Controller} 构造型注解的类。
 *
 * <p>如果可用，还支持 Java EE 6 的 {@link javax.annotation.ManagedBean} 和 JSR-330 的
 * {@link javax.inject.Named} 注解。
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 2.5
 * @see AnnotationConfigApplicationContext#scan
 * @see org.springframework.stereotype.Component
 * @see org.springframework.stereotype.Repository
 * @see org.springframework.stereotype.Service
 * @see org.springframework.stereotype.Controller
 */
public class ClassPathBeanDefinitionScanner extends ClassPathScanningCandidateComponentProvider {

	private final BeanDefinitionRegistry registry;

	private BeanDefinitionDefaults beanDefinitionDefaults = new BeanDefinitionDefaults();

	@Nullable
	private String[] autowireCandidatePatterns;

	private BeanNameGenerator beanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;

	private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();

	private boolean includeAnnotationConfig = true;


	/**
	 * 为给定的 Bean 工厂创建一个新的 {@code ClassPathBeanDefinitionScanner}。
	 * @param registry 要加载 Bean 定义的目标 {@code BeanFactory}，
	 * 以 {@code BeanDefinitionRegistry} 形式提供
	 */
	public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry) {
		this(registry, true);
	}

	/**
	 * 为给定的 Bean 工厂创建一个新的 {@code ClassPathBeanDefinitionScanner}。<p>如果传入的 Bean 工厂不
	 * 仅实现了 {@code BeanDefinitionRegistry} 接口，还实现了 {@code ResourceLoader} 接口，那么它也会被
	 * 用作默认的 {@code ResourceLoader}。对于 {@link org.springframework.context.ApplicationContext}
	 * 实现来说，通常就是这种情况。<p>如果传入的是普通的 {@code BeanDefinitionRegistry}，默认的
	 * {@code ResourceLoader} 将是
	 * {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver}。<p>如果传入的
	 * Bean 工厂还实现了 {@link EnvironmentCapable}，则此读取器将使用其环境。否则，读取器将初始化并使用
	 * {@link org.springframework.core.env.StandardEnvironment}。所有 {@code ApplicationContext} 实现都
	 * 是 {@code EnvironmentCapable}，而普通的 {@code BeanFactory} 实现则不是。
	 * @param registry 要加载 Bean 定义的目标 {@code BeanFactory}，
	 * 以 {@code BeanDefinitionRegistry} 形式提供
	 * @param useDefaultFilters 是否包含以下构造型注解的默认过滤器：
	 * {@link org.springframework.stereotype.Component @Component}、
	 * {@link org.springframework.stereotype.Repository @Repository}、
	 * {@link org.springframework.stereotype.Service @Service} 和
	 * {@link org.springframework.stereotype.Controller @Controller}
	 * @see #setResourceLoader
	 * @see #setEnvironment
	 */
	public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters) {
		this(registry, useDefaultFilters, getOrCreateEnvironment(registry));
	}

	/**
	 * 为给定的 Bean 工厂创建一个新的 {@code ClassPathBeanDefinitionScanner}，并在评估 Bean 定义
	 * profile 元数据时使用给定的 {@link Environment}。<p>如果传入的 Bean 工厂不仅实现了
	 * {@code BeanDefinitionRegistry} 接口，还实现了 {@link ResourceLoader} 接口，那么它也会被用作默认
	 * 的 {@code ResourceLoader}。对于 {@link org.springframework.context.ApplicationContext} 实现来说，
	 * 通常就是这种情况。<p>如果传入的是普通的 {@code BeanDefinitionRegistry}，默认的
	 * {@code ResourceLoader} 将是
	 * {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver}。
	 * @param registry 要加载 Bean 定义的目标 {@code BeanFactory}，
	 * 以 {@code BeanDefinitionRegistry} 形式提供
	 * @param useDefaultFilters 是否包含以下构造型注解的默认过滤器：
	 * {@link org.springframework.stereotype.Component @Component}、
	 * {@link org.springframework.stereotype.Repository @Repository}、
	 * {@link org.springframework.stereotype.Service @Service} 和
	 * {@link org.springframework.stereotype.Controller @Controller}
	 * @param environment 在评估 Bean 定义 profile 元数据时使用的 Spring {@link Environment}
	 * @since 3.1
	 * @see #setResourceLoader
	 */
	public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters,
			Environment environment) {

		this(registry, useDefaultFilters, environment,
				(registry instanceof ResourceLoader ? (ResourceLoader) registry : null));
	}


	/**
	 * 创建ClassPathBeanDefinitionScanner实例
	 * 1. 给BeanDefinitionRegistry赋值
	 * 2. 给父类的includeFilters 添加默认的注解过滤器 @Component
	 * 3. 给父类的environment 赋值
	 * 4. 为父类设置资源加载器，三个属性赋值
	 * 		ResourcePatternResolver的实例为AnnotationConfigApplicationContext 、
	 * 		创建metadataReaderFactory实例为缓存工厂，内部维护了一个map作为缓存（实例为concurrentMap）
	 * 		赋值componentsIndex 第一次暂时为null
	 * @param registry 实际为 AnnotationConfigApplicationContext
	 * @param useDefaultFilters 实际为 true
	 * @param environment 实际为 StandardEnvironment
	 * @param resourceLoader 实际为 AnnotationConfigApplicationContext
	 */
	public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters,
			Environment environment, @Nullable ResourceLoader resourceLoader) {

		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		/*记录bean定义的注册器*/
		this.registry = registry;

		/*注册三个过滤器，哪些注解要被扫描到：
		* 	1.@Component注解，以及被@Component元注解标记的注解
		* 	2.JavaEE6的@ManagedBean注解，如果有的话
		* 	3.JSR-330的@Named注解，如果有的话
		* */
		if (useDefaultFilters) {
			registerDefaultFilters();
		}
		/*设置扫描环境*/
		setEnvironment(environment);
		/*
		* 设置解析相关资源的处理器
		* 	这里最主要的就是创建了一个元数据的读取工厂。为后面初始化bean缓存了bean的类以及真是文件信息
		* */
		setResourceLoader(resourceLoader);
	}


	/**
	 * 返回此扫描器所操作的 BeanDefinitionRegistry。
	 */
	@Override
	public final BeanDefinitionRegistry getRegistry() {
		return this.registry;
	}

	/**
	 * 设置用于检测到的 Bean 的默认值。
	 * @see BeanDefinitionDefaults
	 */
	public void setBeanDefinitionDefaults(@Nullable BeanDefinitionDefaults beanDefinitionDefaults) {
		this.beanDefinitionDefaults =
				(beanDefinitionDefaults != null ? beanDefinitionDefaults : new BeanDefinitionDefaults());
	}

	/**
	 * 返回用于检测到的 Bean 的默认值（永不为 {@code null}）。
	 * @since 4.1
	 */
	public BeanDefinitionDefaults getBeanDefinitionDefaults() {
		return this.beanDefinitionDefaults;
	}

	/**
	 * 设置用于确定自动装配候选者的名称匹配模式。
	 * @param autowireCandidatePatterns 要匹配的模式
	 */
	public void setAutowireCandidatePatterns(@Nullable String... autowireCandidatePatterns) {
		this.autowireCandidatePatterns = autowireCandidatePatterns;
	}

	/**
	 * 设置用于检测到的 Bean 类的 BeanNameGenerator。<p>默认是 {@link AnnotationBeanNameGenerator}。
	 */
	public void setBeanNameGenerator(@Nullable BeanNameGenerator beanNameGenerator) {
		this.beanNameGenerator =
				(beanNameGenerator != null ? beanNameGenerator : AnnotationBeanNameGenerator.INSTANCE);
	}

	/**
	 * 设置用于检测到的 Bean 类的 ScopeMetadataResolver。请注意，这会覆盖任何自定义的 “scopedProxyMode”
	 * 设置。<p>默认是 {@link AnnotationScopeMetadataResolver}。
	 * @see #setScopedProxyMode
	 */
	public void setScopeMetadataResolver(@Nullable ScopeMetadataResolver scopeMetadataResolver) {
		this.scopeMetadataResolver =
				(scopeMetadataResolver != null ? scopeMetadataResolver : new AnnotationScopeMetadataResolver());
	}

	/**
	 * 指定非 singleton 作用域 Bean 的代理行为。请注意，这会覆盖任何自定义的 “scopeMetadataResolver” 设
	 * 置。<p>默认是 {@link ScopedProxyMode#NO}。
	 * @see #setScopeMetadataResolver
	 */
	public void setScopedProxyMode(ScopedProxyMode scopedProxyMode) {
		this.scopeMetadataResolver = new AnnotationScopeMetadataResolver(scopedProxyMode);
	}

	/**
	 * 指定是否注册注解配置后处理器。<p>默认会注册这些后处理器。将其关闭即可忽略这些注解或以不同方式处
	 * 理它们。
	 */
	public void setIncludeAnnotationConfig(boolean includeAnnotationConfig) {
		this.includeAnnotationConfig = includeAnnotationConfig;
	}


	/**
	 * 在指定的基础包内执行扫描。
	 * @param basePackages 要检查注解类的包
	 * @return 注册的 Bean 数量
	 */
	public int scan(String... basePackages) {
		int beanCountAtScanStart = this.registry.getBeanDefinitionCount();

		doScan(basePackages);

		// Register annotation config processors, if necessary.
		if (this.includeAnnotationConfig) {
			AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
		}

		return (this.registry.getBeanDefinitionCount() - beanCountAtScanStart);
	}

	/**
	 * 在指定的基础包内执行扫描，并返回已注册的 Bean 定义。<p>此方法<i>不会</i>注册注解配置处理器，而是
	 * 将其留给调用方。
	 * @param basePackages 要检查注解类的包
	 * @return 注册的 Bean 集合（如果有，用于工具注册目的，永不为 {@code null}）
	 */

	/**
	 * 扫描指定的包，将包下符合条件的bean注册到bean工厂，并且返回已经注册的bean集合
	 * @param basePackages 指定的包
	 * @return 已经注册的bean集合
	 */
	protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
		Assert.notEmpty(basePackages, "At least one base package must be specified");
		Set<BeanDefinitionHolder> beanDefinitions = new LinkedHashSet<>();
		for (String basePackage : basePackages) {
			//当前包下所有符合spring条件的bean
			Set<BeanDefinition> candidates = findCandidateComponents(basePackage);
			for (BeanDefinition candidate : candidates) {
				//获取bean的作用域信息（如设置为单例）
				ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(candidate);
				candidate.setScope(scopeMetadata.getScopeName());
				//生成bean名称
				String beanName = this.beanNameGenerator.generateBeanName(candidate, this.registry);
				if (candidate instanceof AbstractBeanDefinition) {
					//给candidate设置一些默认配置，如懒加载，注入模型，初始化方法，销毁方法
					postProcessBeanDefinition((AbstractBeanDefinition) candidate, beanName);
				}
				if (candidate instanceof AnnotatedBeanDefinition) {
					//处理candidate的一些通用的注解
					AnnotationConfigUtils.processCommonDefinitionAnnotations((AnnotatedBeanDefinition) candidate);
				}
					//检查该bean是否需要注册
					//如果容器中没有则需要注册，如果有则检查是否冲突（兼容的作用？）
				if (checkCandidate(beanName, candidate)) {
					BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(candidate, beanName);
					definitionHolder =
							AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
					beanDefinitions.add(definitionHolder);
					/*
					* 将bean注册到bean工厂
					* */
					registerBeanDefinition(definitionHolder, this.registry);
				}
			}
		}
		return beanDefinitions;
	}

	/**
	 * 对给定的 Bean 定义应用进一步设置，这些设置超出扫描组件类所获得的内容。
	 * @param beanDefinition 扫描得到的 Bean 定义
	 * @param beanName 为给定 Bean 生成的 Bean 名称
	 */
	protected void postProcessBeanDefinition(AbstractBeanDefinition beanDefinition, String beanName) {
		beanDefinition.applyDefaults(this.beanDefinitionDefaults);
		if (this.autowireCandidatePatterns != null) {
			beanDefinition.setAutowireCandidate(PatternMatchUtils.simpleMatch(this.autowireCandidatePatterns, beanName));
		}
	}

	/**
	 * 用给定的注册表注册指定的 Bean。<p>可以在子类中覆盖，例如调整注册过程或为每个扫描到的 Bean 注册更
	 * 多 Bean 定义。
	 * @param definitionHolder 该 Bean 的 Bean 定义及 Bean 名称
	 * @param registry 要将 Bean 注册到的 BeanDefinitionRegistry
	 */
	protected void registerBeanDefinition(BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry) {
		BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);
	}


	/**
	 * 检查给定候选者的 Bean 名称，确定相应的 Bean 定义是否需要注册，或者是否与现有定义冲突。
	 * @param beanName 为该 Bean 建议的名称
	 * @param beanDefinition 相应的 Bean 定义
	 * @return 如果该 Bean 可以原样注册，则返回 {@code true}；
	 * 如果因为指定名称已存在兼容的 Bean 定义而应跳过它，则返回 {@code false}
	 * @throws ConflictingBeanDefinitionException 如果为指定名称找到了现有但不兼容的 Bean 定义
	 */
	protected boolean checkCandidate(String beanName, BeanDefinition beanDefinition) throws IllegalStateException {
		if (!this.registry.containsBeanDefinition(beanName)) {
			return true;
		}
		BeanDefinition existingDef = this.registry.getBeanDefinition(beanName);
		BeanDefinition originatingDef = existingDef.getOriginatingBeanDefinition();
		if (originatingDef != null) {
			existingDef = originatingDef;
		}
		if (isCompatible(beanDefinition, existingDef)) {
			return false;
		}
		throw new ConflictingBeanDefinitionException("Annotation-specified bean name '" + beanName +
				"' for bean class [" + beanDefinition.getBeanClassName() + "] conflicts with existing, " +
				"non-compatible bean definition of same name and class [" + existingDef.getBeanClassName() + "]");
	}

	/**
	 * 确定给定的新 Bean 定义是否与给定的现有 Bean 定义兼容。<p>当现有 Bean 定义来自相同来源或来自非扫
	 * 描来源时，默认实现认为它们是兼容的。
	 * @param newDefinition 新的 Bean 定义，来源于扫描
	 * @param existingDefinition 现有的 Bean 定义，可能是显式定义的，
	 * 也可能是之前由扫描生成的
	 * @return 这些定义是否被视为兼容，
	 * 若兼容则跳过新定义而采用现有定义
	 */
	protected boolean isCompatible(BeanDefinition newDefinition, BeanDefinition existingDefinition) {
		return (!(existingDefinition instanceof ScannedGenericBeanDefinition) ||  // explicitly registered overriding bean
				(newDefinition.getSource() != null && newDefinition.getSource().equals(existingDefinition.getSource())) ||  // scanned same file twice
				newDefinition.equals(existingDefinition));  // scanned equivalent class twice
	}


	/**
	 * 如果可能，从给定的注册表获取 Environment，否则返回一个新的 StandardEnvironment。
	 */
	private static Environment getOrCreateEnvironment(BeanDefinitionRegistry registry) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		if (registry instanceof EnvironmentCapable) {
			return ((EnvironmentCapable) registry).getEnvironment();
		}
		return new StandardEnvironment();
	}

}
