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

package org.springframework.web.servlet.view.tiles3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.ResourceBundleELResolver;
import javax.servlet.ServletContext;
import javax.servlet.jsp.JspFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.TilesContainer;
import org.apache.tiles.TilesException;
import org.apache.tiles.definition.DefinitionsFactory;
import org.apache.tiles.definition.DefinitionsReader;
import org.apache.tiles.definition.dao.BaseLocaleUrlDefinitionDAO;
import org.apache.tiles.definition.dao.CachingLocaleUrlDefinitionDAO;
import org.apache.tiles.definition.digester.DigesterDefinitionsReader;
import org.apache.tiles.el.ELAttributeEvaluator;
import org.apache.tiles.el.ScopeELResolver;
import org.apache.tiles.el.TilesContextBeanELResolver;
import org.apache.tiles.el.TilesContextELResolver;
import org.apache.tiles.evaluator.AttributeEvaluator;
import org.apache.tiles.evaluator.AttributeEvaluatorFactory;
import org.apache.tiles.evaluator.BasicAttributeEvaluatorFactory;
import org.apache.tiles.evaluator.impl.DirectAttributeEvaluator;
import org.apache.tiles.extras.complete.CompleteAutoloadTilesContainerFactory;
import org.apache.tiles.extras.complete.CompleteAutoloadTilesInitializer;
import org.apache.tiles.factory.AbstractTilesContainerFactory;
import org.apache.tiles.factory.BasicTilesContainerFactory;
import org.apache.tiles.impl.mgmt.CachingTilesContainer;
import org.apache.tiles.locale.LocaleResolver;
import org.apache.tiles.preparer.factory.PreparerFactory;
import org.apache.tiles.request.ApplicationContext;
import org.apache.tiles.request.ApplicationContextAware;
import org.apache.tiles.request.ApplicationResource;
import org.apache.tiles.startup.DefaultTilesInitializer;
import org.apache.tiles.startup.TilesInitializer;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.ServletContextAware;

/**
 * 用于为 Spring Framework 配置 Tiles 3.x 的辅助类。有关 Tiles 的更多信息，
 * 请参阅 <a href="https://tiles.apache.org">https://tiles.apache.org</a>。
 * Tiles 基本上是一种用于使用 JSP 和其他模板引擎的 Web 应用的模板机制。
 *
 * <p>TilesConfigurer 简单地使用一组包含定义的配置文件来配置 TilesContainer，
 * 以便 {@link TilesView} 实例访问。这是 Tiles 提供的
 * {@code ServletContextListener} 的基于 Spring 的替代方案（用于 Spring 配置，
 * 例如在 {@code web.xml} 中使用的
 * {@link org.apache.tiles.extras.complete.CompleteAutoloadTilesListener}）。
 *
 * <p>TilesView 可以由任何 {@link org.springframework.web.servlet.ViewResolver} 管理。
 * 对于简单的基于约定的视图解析，可以考虑使用 {@link TilesViewResolver}。
 *
 * <p>典型的 TilesConfigurer bean 定义如下：
 *
 * <pre class="code">
 * &lt;bean id="tilesConfigurer" class="org.springframework.web.servlet.view.tiles3.TilesConfigurer"&gt;
 *   &lt;property name="definitions"&gt;
 *     &lt;list&gt;
 *       &lt;value&gt;/WEB-INF/defs/general.xml&lt;/value&gt;
 *       &lt;value&gt;/WEB-INF/defs/widgets.xml&lt;/value&gt;
 *       &lt;value&gt;/WEB-INF/defs/administrator.xml&lt;/value&gt;
 *       &lt;value&gt;/WEB-INF/defs/customer.xml&lt;/value&gt;
 *       &lt;value&gt;/WEB-INF/defs/templates.xml&lt;/value&gt;
 *     &lt;/list&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 * </pre>
 *
 * 列表中的值是包含定义的实际 Tiles XML 文件。
 * 如果未指定列表，默认值为 {@code "/WEB-INF/tiles.xml"}。
 *
 * <p>请注意，在 Tiles 3 中，包含 Tiles 定义的文件的名称中的下划线
 * 用于表示 Locale 信息，例如：
 *
 * <pre class="code">
 * &lt;bean id="tilesConfigurer" class="org.springframework.web.servlet.view.tiles3.TilesConfigurer"&gt;
 *   &lt;property name="definitions"&gt;
 *     &lt;list&gt;
 *       &lt;value&gt;/WEB-INF/defs/tiles.xml&lt;/value&gt;
 *       &lt;value&gt;/WEB-INF/defs/tiles_fr_FR.xml&lt;/value&gt;
 *     &lt;/list&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 * </pre>
 *
 * @author mick semb wever
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.2
 * @see TilesView
 * @see TilesViewResolver
 */
public class TilesConfigurer implements ServletContextAware, InitializingBean, DisposableBean {

	private static final boolean tilesElPresent =
			ClassUtils.isPresent("org.apache.tiles.el.ELAttributeEvaluator", TilesConfigurer.class.getClassLoader());


	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private TilesInitializer tilesInitializer;

	@Nullable
	private String[] definitions;

	private boolean checkRefresh = false;

	private boolean validateDefinitions = true;

	@Nullable
	private Class<? extends DefinitionsFactory> definitionsFactoryClass;

	@Nullable
	private Class<? extends PreparerFactory> preparerFactoryClass;

	private boolean useMutableTilesContainer = false;

	@Nullable
	private ServletContext servletContext;


	/**
	 * 使用自定义的 TilesInitializer 配置 Tiles，通常指定为内部 bean。
	 * <p>默认是 {@link org.apache.tiles.startup.DefaultTilesInitializer} 的一个变体，
	 * 它遵循此配置器上的 "definitions"、"preparerFactoryClass" 等属性。
	 * <p><b>注意：指定自定义 TilesInitializer 会有效地禁用此配置器上的所有其他 bean
	 * 属性。</b> 整个初始化过程将交由指定的 TilesInitializer 负责。
	 */
	public void setTilesInitializer(TilesInitializer tilesInitializer) {
		this.tilesInitializer = tilesInitializer;
	}

	/**
	 * 指定是否应用 Tiles 3.0 的 "complete-autoload" 配置。
	 * <p>有关 complete-autoload 模式的详细信息，请参阅
	 * {@link org.apache.tiles.extras.complete.CompleteAutoloadTilesContainerFactory}。
	 * <p><b>注意：指定 complete-autoload 模式会有效地禁用此配置器上的所有其他 bean
	 * 属性。</b> 整个初始化过程将交由
	 * {@link org.apache.tiles.extras.complete.CompleteAutoloadTilesInitializer} 负责。
	 * @see org.apache.tiles.extras.complete.CompleteAutoloadTilesContainerFactory
	 * @see org.apache.tiles.extras.complete.CompleteAutoloadTilesInitializer
	 */
	public void setCompleteAutoload(boolean completeAutoload) {
		if (completeAutoload) {
			try {
				this.tilesInitializer = new SpringCompleteAutoloadTilesInitializer();
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Tiles-Extras 3.0 not available", ex);
			}
		}
		else {
			this.tilesInitializer = null;
		}
	}

	/**
	 * 设置 Tiles 定义，即包含定义的文件的列表。
	 * 默认值为 "/WEB-INF/tiles.xml"。
	 */
	public void setDefinitions(String... definitions) {
		this.definitions = definitions;
	}

	/**
	 * 设置是否在运行时检查 Tiles 定义文件是否需要刷新。
	 * 默认值为 "false"。
	 */
	public void setCheckRefresh(boolean checkRefresh) {
		this.checkRefresh = checkRefresh;
	}

	/**
	 * 设置是否校验 Tiles XML 定义。默认值为 "true"。
	 */
	public void setValidateDefinitions(boolean validateDefinitions) {
		this.validateDefinitions = validateDefinitions;
	}

	/**
	 * 设置要使用的 {@link org.apache.tiles.definition.DefinitionsFactory} 实现。
	 * 默认是 {@link org.apache.tiles.definition.UnresolvingLocaleDefinitionsFactory}，
	 * 它基于定义资源 URL 进行操作。
	 * <p>指定自定义的 DefinitionsFactory（例如 UrlDefinitionsFactory 的子类），
	 * 以自定义 Tiles Definition 对象的创建。请注意，此类
	 * DefinitionsFactory 必须能够处理 {@link java.net.URL} 源对象，
	 * 除非你配置了不同的 TilesContainerFactory。
	 */
	public void setDefinitionsFactoryClass(Class<? extends DefinitionsFactory> definitionsFactoryClass) {
		this.definitionsFactoryClass = definitionsFactoryClass;
	}

	/**
	 * 设置要使用的 {@link org.apache.tiles.preparer.factory.PreparerFactory} 实现。
	 * 默认是 {@link org.apache.tiles.preparer.factory.BasicPreparerFactory}，它为指定的
	 * preparer 类创建共享实例。
	 * <p>指定 {@link SimpleSpringPreparerFactory}，以基于指定的
	 * preparer 类自动装配 {@link org.apache.tiles.preparer.ViewPreparer} 实例，
	 * 同时应用 Spring 的容器回调以及已配置的 Spring BeanPostProcessor。
	 * 如果已激活 Spring 上下文范围的 annotation-config，
	 * 则将自动检测并应用 ViewPreparer 类中的注解。
	 * <p>指定 {@link SpringBeanPreparerFactory}，以针对指定的 preparer
	 * <i>名称</i>而不是类进行操作，从 DispatcherServlet 的应用上下文中
	 * 获取相应的 Spring bean。在这种情况下，完整的 bean 创建过程
	 * 将由 Spring 应用上下文控制，从而允许使用作用域 bean 等。
	 * 请注意，需要为每个 preparer 名称（即在 Tiles 定义中使用的名称）
	 * 定义一个 Spring bean 定义。
	 * @see SimpleSpringPreparerFactory
	 * @see SpringBeanPreparerFactory
	 */
	public void setPreparerFactoryClass(Class<? extends PreparerFactory> preparerFactoryClass) {
		this.preparerFactoryClass = preparerFactoryClass;
	}

	/**
	 * 设置此应用是否使用 MutableTilesContainer（通常是 CachingTilesContainer
	 * 实现）。默认值为 "false"。
	 * @see org.apache.tiles.mgmt.MutableTilesContainer
	 * @see org.apache.tiles.impl.mgmt.CachingTilesContainer
	 */
	public void setUseMutableTilesContainer(boolean useMutableTilesContainer) {
		this.useMutableTilesContainer = useMutableTilesContainer;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	/**
	 * 为此 Web 应用创建并暴露一个 TilesContainer，
	 * 并委托给 TilesInitializer。
	 * @throws TilesException 如果设置失败
	 */
	@Override
	public void afterPropertiesSet() throws TilesException {
		Assert.state(this.servletContext != null, "No ServletContext available");
		ApplicationContext preliminaryContext = new SpringWildcardServletTilesApplicationContext(this.servletContext);
		if (this.tilesInitializer == null) {
			this.tilesInitializer = new SpringTilesInitializer();
		}
		this.tilesInitializer.initialize(preliminaryContext);
	}

	/**
	 * 从此 Web 应用中移除 TilesContainer。
	 * @throws TilesException 如果清理失败
	 */
	@Override
	public void destroy() throws TilesException {
		if (this.tilesInitializer != null) {
			this.tilesInitializer.destroy();
		}
	}


	private class SpringTilesInitializer extends DefaultTilesInitializer {

		@Override
		protected AbstractTilesContainerFactory createContainerFactory(ApplicationContext context) {
			return new SpringTilesContainerFactory();
		}
	}


	private class SpringTilesContainerFactory extends BasicTilesContainerFactory {

		@Override
		protected TilesContainer createDecoratedContainer(TilesContainer originalContainer, ApplicationContext context) {
			return (useMutableTilesContainer ? new CachingTilesContainer(originalContainer) : originalContainer);
		}

		@Override
		protected List<ApplicationResource> getSources(ApplicationContext applicationContext) {
			if (definitions != null) {
				List<ApplicationResource> result = new ArrayList<>();
				for (String definition : definitions) {
					Collection<ApplicationResource> resources = applicationContext.getResources(definition);
					if (resources != null) {
						result.addAll(resources);
					}
				}
				return result;
			}
			else {
				return super.getSources(applicationContext);
			}
		}

		@Override
		protected BaseLocaleUrlDefinitionDAO instantiateLocaleDefinitionDao(ApplicationContext applicationContext,
				LocaleResolver resolver) {
			BaseLocaleUrlDefinitionDAO dao = super.instantiateLocaleDefinitionDao(applicationContext, resolver);
			if (checkRefresh && dao instanceof CachingLocaleUrlDefinitionDAO) {
				((CachingLocaleUrlDefinitionDAO) dao).setCheckRefresh(true);
			}
			return dao;
		}

		@Override
		protected DefinitionsReader createDefinitionsReader(ApplicationContext context) {
			DigesterDefinitionsReader reader = (DigesterDefinitionsReader) super.createDefinitionsReader(context);
			reader.setValidating(validateDefinitions);
			return reader;
		}

		@Override
		protected DefinitionsFactory createDefinitionsFactory(ApplicationContext applicationContext,
				LocaleResolver resolver) {

			if (definitionsFactoryClass != null) {
				DefinitionsFactory factory = BeanUtils.instantiateClass(definitionsFactoryClass);
				if (factory instanceof ApplicationContextAware) {
					((ApplicationContextAware) factory).setApplicationContext(applicationContext);
				}
				BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(factory);
				if (bw.isWritableProperty("localeResolver")) {
					bw.setPropertyValue("localeResolver", resolver);
				}
				if (bw.isWritableProperty("definitionDAO")) {
					bw.setPropertyValue("definitionDAO", createLocaleDefinitionDao(applicationContext, resolver));
				}
				return factory;
			}
			else {
				return super.createDefinitionsFactory(applicationContext, resolver);
			}
		}

		@Override
		protected PreparerFactory createPreparerFactory(ApplicationContext context) {
			if (preparerFactoryClass != null) {
				return BeanUtils.instantiateClass(preparerFactoryClass);
			}
			else {
				return super.createPreparerFactory(context);
			}
		}

		@Override
		protected LocaleResolver createLocaleResolver(ApplicationContext context) {
			return new SpringLocaleResolver();
		}

		@Override
		protected AttributeEvaluatorFactory createAttributeEvaluatorFactory(ApplicationContext context,
				LocaleResolver resolver) {
			AttributeEvaluator evaluator;
			if (tilesElPresent && JspFactory.getDefaultFactory() != null) {
				evaluator = new TilesElActivator().createEvaluator();
			}
			else {
				evaluator = new DirectAttributeEvaluator();
			}
			return new BasicAttributeEvaluatorFactory(evaluator);
		}
	}


	private static class SpringCompleteAutoloadTilesInitializer extends CompleteAutoloadTilesInitializer {

		@Override
		protected AbstractTilesContainerFactory createContainerFactory(ApplicationContext context) {
			return new SpringCompleteAutoloadTilesContainerFactory();
		}
	}


	private static class SpringCompleteAutoloadTilesContainerFactory extends CompleteAutoloadTilesContainerFactory {

		@Override
		protected LocaleResolver createLocaleResolver(ApplicationContext applicationContext) {
			return new SpringLocaleResolver();
		}
	}


	private class TilesElActivator {

		public AttributeEvaluator createEvaluator() {
			ELAttributeEvaluator evaluator = new ELAttributeEvaluator();
			evaluator.setExpressionFactory(
					JspFactory.getDefaultFactory().getJspApplicationContext(servletContext).getExpressionFactory());
			evaluator.setResolver(new CompositeELResolverImpl());
			return evaluator;
		}
	}


	private static class CompositeELResolverImpl extends CompositeELResolver {

		public CompositeELResolverImpl() {
			add(new ScopeELResolver());
			add(new TilesContextELResolver(new TilesContextBeanELResolver()));
			add(new TilesContextBeanELResolver());
			add(new ArrayELResolver(false));
			add(new ListELResolver(false));
			add(new MapELResolver(false));
			add(new ResourceBundleELResolver());
			add(new BeanELResolver(false));
		}
	}

}
