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

package org.springframework.ui.freemarker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * 配置 FreeMarker Configuration 的工厂。可以独立使用，但通常你会使用
 * FreeMarkerConfigurationFactoryBean 将 Configuration 准备为 bean 引用，
 * 或者使用 FreeMarkerConfigurer 用于 web 视图。
 *
 * <p>可选的 "configLocation" 属性设置 FreeMarker 配置文件的位置，
 * 在当前应用程序内。FreeMarker 属性可以通过 "freemarkerSettings" 覆盖。
 * 所有这些属性将通过调用 FreeMarker 的 {@code Configuration.setSettings()} 方法来设置，
 * 并受 FreeMarker 设定的约束限制。
 *
 * <p>"freemarkerVariables" 属性可用于指定一个共享变量的 Map，
 * 通过 {@code setAllSharedVariables()} 方法应用到 Configuration。
 * 与 {@code setSettings()} 一样，这些条目受 FreeMarker 约束限制。
 *
 * <p>使用此类最简单的方式是指定一个 "templateLoaderPath"；
 * FreeMarker 不需要任何进一步的配置。
 *
 * <p>注意：Spring 的 FreeMarker 支持需要 FreeMarker 2.3 或更高版本。
 *
 * @author Darren Davison
 * @author Juergen Hoeller
 * @since 03.03.2004
 * @see #setConfigLocation
 * @see #setFreemarkerSettings
 * @see #setFreemarkerVariables
 * @see #setTemplateLoaderPath
 * @see #createConfiguration
 * @see FreeMarkerConfigurationFactoryBean
 * @see org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer
 * @see freemarker.template.Configuration
 */
public class FreeMarkerConfigurationFactory {

	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private Resource configLocation;

	@Nullable
	private Properties freemarkerSettings;

	@Nullable
	private Map<String, Object> freemarkerVariables;

	@Nullable
	private String defaultEncoding;

	private final List<TemplateLoader> templateLoaders = new ArrayList<>();

	@Nullable
	private List<TemplateLoader> preTemplateLoaders;

	@Nullable
	private List<TemplateLoader> postTemplateLoaders;

	@Nullable
	private String[] templateLoaderPaths;

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	private boolean preferFileSystemAccess = true;


	/**
	 * 设置 FreeMarker 配置文件的位置。
	 * 或者，你可以在本地指定所有设置。
	 * @see #setFreemarkerSettings
	 * @see #setTemplateLoaderPath
	 */
	public void setConfigLocation(Resource resource) {
		this.configLocation = resource;
	}

	/**
	 * 设置包含已知 FreeMarker 键的属性，这些键将被传递到
	 * FreeMarker 的 {@code Configuration.setSettings} 方法。
	 * @see freemarker.template.Configuration#setSettings
	 */
	public void setFreemarkerSettings(Properties settings) {
		this.freemarkerSettings = settings;
	}

	/**
	 * 设置一个 Map，其中包含已知的 FreeMarker 对象，这些对象将被传递到
	 * FreeMarker 的 {@code Configuration.setAllSharedVariables()} 方法。
	 * @see freemarker.template.Configuration#setAllSharedVariables
	 */
	public void setFreemarkerVariables(Map<String, Object> variables) {
		this.freemarkerVariables = variables;
	}

	/**
	 * 设置 FreeMarker 配置的默认编码。
	 * 如果未指定，FreeMarker 将使用平台文件编码。
	 * <p>用于模板渲染，除非渲染过程指定了显式编码
	 * （例如，在 Spring 的 FreeMarkerView 上）。
	 * @see freemarker.template.Configuration#setDefaultEncoding
	 * @see org.springframework.web.servlet.view.freemarker.FreeMarkerView#setEncoding
	 */
	public void setDefaultEncoding(String defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
	}

	/**
	 * 设置用于搜索模板的 {@code TemplateLoader} 列表。
	 * 例如，可以在此处配置并注入一个或多个自定义加载器，如数据库加载器。
	 * <p>此处指定的 {@link TemplateLoader TemplateLoaders} 将在
	 * 此工厂注册的默认模板加载器<i>之前</i>注册
	 * （例如为指定的 "templateLoaderPaths" 注册的加载器或在
	 * {@link #postProcessTemplateLoaders} 中注册的任何加载器）。
	 * @see #setTemplateLoaderPaths
	 * @see #postProcessTemplateLoaders
	 */
	public void setPreTemplateLoaders(TemplateLoader... preTemplateLoaders) {
		this.preTemplateLoaders = Arrays.asList(preTemplateLoaders);
	}

	/**
	 * 设置用于搜索模板的 {@code TemplateLoader} 列表。
	 * 例如，可以配置一个或多个自定义加载器，如数据库加载器。
	 * <p>此处指定的 {@link TemplateLoader TemplateLoaders} 将在
	 * 此工厂注册的默认模板加载器<i>之后</i>注册
	 * （例如为指定的 "templateLoaderPaths" 注册的加载器或在
	 * {@link #postProcessTemplateLoaders} 中注册的任何加载器）。
	 * @see #setTemplateLoaderPaths
	 * @see #postProcessTemplateLoaders
	 */
	public void setPostTemplateLoaders(TemplateLoader... postTemplateLoaders) {
		this.postTemplateLoaders = Arrays.asList(postTemplateLoaders);
	}

	/**
	 * 通过 Spring 资源位置设置 Freemarker 模板加载器路径。
	 * 有关路径处理的详细信息，请参见 "templateLoaderPaths" 属性。
	 * @see #setTemplateLoaderPaths
	 */
	public void setTemplateLoaderPath(String templateLoaderPath) {
		this.templateLoaderPaths = new String[] {templateLoaderPath};
	}

	/**
	 * 通过 Spring 资源位置设置多个 Freemarker 模板加载器路径。
	 * <p>当通过 String 填充时，支持标准 URL，如 "file:" 和 "classpath:"
	 * 伪 URL，如 ResourceEditor 所理解。允许在 ApplicationContext 中运行时使用相对路径。
	 * <p>将为默认的 FreeMarker 模板加载器定义路径。
	 * 如果指定的资源无法解析为 {@code java.io.File}，
	 * 将使用通用的 SpringTemplateLoader，不进行修改检测。
	 * <p>要强制使用 SpringTemplateLoader，即在任何情况下都不将路径解析为文件系统资源，
	 * 请关闭 "preferFileSystemAccess" 标志。详细信息请参见后者的 javadoc。
	 * <p>如果你希望指定自己的 TemplateLoader 列表，请不要设置此属性，
	 * 而是使用 {@code setTemplateLoaders(List templateLoaders)}
	 * @see org.springframework.core.io.ResourceEditor
	 * @see org.springframework.context.ApplicationContext#getResource
	 * @see freemarker.template.Configuration#setDirectoryForTemplateLoading
	 * @see SpringTemplateLoader
	 */
	public void setTemplateLoaderPaths(String... templateLoaderPaths) {
		this.templateLoaderPaths = templateLoaderPaths;
	}

	/**
	 * 设置用于加载 FreeMarker 模板文件的 Spring ResourceLoader。
	 * 默认为 DefaultResourceLoader。在上下文中运行时将被 ApplicationContext 覆盖。
	 * @see org.springframework.core.io.DefaultResourceLoader
	 */
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
	 * 返回用于加载 FreeMarker 模板文件的 Spring ResourceLoader。
	 */
	protected ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}

	/**
	 * 设置是否优先使用文件系统访问进行模板加载。
	 * 文件系统访问可以实时检测模板更改。
	 * <p>如果启用此选项，FreeMarkerConfigurationFactory 将尝试将指定的
	 * "templateLoaderPath" 解析为文件系统资源（对于展开的类路径资源和
	 * ServletContext 资源也可以工作）。
	 * <p>默认为 "true"。关闭此选项可始终通过 SpringTemplateLoader 加载
	 * （即作为流，不进行模板更改的实时检测），如果你的某些模板位于
	 * 展开的 classes 目录中而其他模板位于 jar 文件中，则可能需要这样做。
	 * @see #setTemplateLoaderPath
	 */
	public void setPreferFileSystemAccess(boolean preferFileSystemAccess) {
		this.preferFileSystemAccess = preferFileSystemAccess;
	}

	/**
	 * 返回是否优先使用文件系统访问进行模板加载。
	 */
	protected boolean isPreferFileSystemAccess() {
		return this.preferFileSystemAccess;
	}


	/**
	 * 准备 FreeMarker Configuration 并返回。
	 * @return FreeMarker Configuration 对象
	 * @throws IOException 如果未找到配置文件
	 * @throws TemplateException FreeMarker 初始化失败时
	 */
	public Configuration createConfiguration() throws IOException, TemplateException {
		Configuration config = newConfiguration();
		Properties props = new Properties();

		// Load config file if specified.
		if (this.configLocation != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Loading FreeMarker configuration from " + this.configLocation);
			}
			PropertiesLoaderUtils.fillProperties(props, this.configLocation);
		}

		// Merge local properties if specified.
		if (this.freemarkerSettings != null) {
			props.putAll(this.freemarkerSettings);
		}

		// FreeMarker will only accept known keys in its setSettings and
		// setAllSharedVariables methods.
		if (!props.isEmpty()) {
			config.setSettings(props);
		}

		if (!CollectionUtils.isEmpty(this.freemarkerVariables)) {
			config.setAllSharedVariables(new SimpleHash(this.freemarkerVariables, config.getObjectWrapper()));
		}

		if (this.defaultEncoding != null) {
			config.setDefaultEncoding(this.defaultEncoding);
		}

		List<TemplateLoader> templateLoaders = new ArrayList<>(this.templateLoaders);

		// Register template loaders that are supposed to kick in early.
		if (this.preTemplateLoaders != null) {
			templateLoaders.addAll(this.preTemplateLoaders);
		}

		// Register default template loaders.
		if (this.templateLoaderPaths != null) {
			for (String path : this.templateLoaderPaths) {
				templateLoaders.add(getTemplateLoaderForPath(path));
			}
		}
		postProcessTemplateLoaders(templateLoaders);

		// Register template loaders that are supposed to kick in late.
		if (this.postTemplateLoaders != null) {
			templateLoaders.addAll(this.postTemplateLoaders);
		}

		TemplateLoader loader = getAggregateTemplateLoader(templateLoaders);
		if (loader != null) {
			config.setTemplateLoader(loader);
		}

		postProcessConfiguration(config);
		return config;
	}

	/**
	 * 返回一个新的 Configuration 对象。子类可以覆盖此方法以进行自定义初始化
	 * （例如，指定 FreeMarker 兼容性级别，这是 FreeMarker 2.3.21 中的新特性），
	 * 或用于测试的模拟对象。
	 * <p>由 {@code createConfiguration()} 调用。
	 * @return Configuration 对象
	 * @throws IOException 如果未找到配置文件
	 * @throws TemplateException FreeMarker 初始化失败时
	 * @see #createConfiguration()
	 */
	protected Configuration newConfiguration() throws IOException, TemplateException {
		return new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
	}

	/**
	 * 确定给定路径的 FreeMarker TemplateLoader。
	 * <p>默认实现创建 FileTemplateLoader 或 SpringTemplateLoader。
	 * @param templateLoaderPath 从中加载模板的路径
	 * @return 合适的 TemplateLoader
	 * @see freemarker.cache.FileTemplateLoader
	 * @see SpringTemplateLoader
	 */
	protected TemplateLoader getTemplateLoaderForPath(String templateLoaderPath) {
		if (isPreferFileSystemAccess()) {
			// Try to load via the file system, fall back to SpringTemplateLoader
			// (for hot detection of template changes, if possible).
			try {
				Resource path = getResourceLoader().getResource(templateLoaderPath);
				File file = path.getFile();  // will fail if not resolvable in the file system
				if (logger.isDebugEnabled()) {
					logger.debug(
							"Template loader path [" + path + "] resolved to file path [" + file.getAbsolutePath() + "]");
				}
				return new FileTemplateLoader(file);
			}
			catch (Exception ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Cannot resolve template loader path [" + templateLoaderPath +
							"] to [java.io.File]: using SpringTemplateLoader as fallback", ex);
				}
				return new SpringTemplateLoader(getResourceLoader(), templateLoaderPath);
			}
		}
		else {
			// Always load via SpringTemplateLoader (without hot detection of template changes).
			logger.debug("File system access not preferred: using SpringTemplateLoader");
			return new SpringTemplateLoader(getResourceLoader(), templateLoaderPath);
		}
	}

	/**
	 * 由子类覆盖，用于在此工厂创建默认模板加载器之后
	 * 注册自定义 TemplateLoader 实例。
	 * <p>由 {@code createConfiguration()} 调用。注意指定的
	 * "postTemplateLoaders" 将在由此回调注册的任何加载器<i>之后</i>注册；
	 * 因此，它们<i>不</i>包含在给定的 List 中。
	 * @param templateLoaders 当前的 TemplateLoader 实例列表，可由子类修改
	 * @see #createConfiguration()
	 * @see #setPostTemplateLoaders
	 */
	protected void postProcessTemplateLoaders(List<TemplateLoader> templateLoaders) {
	}

	/**
	 * 基于给定的 TemplateLoader 列表返回一个 TemplateLoader。
	 * 如果注册了多个 TemplateLoader，则需要创建 FreeMarker MultiTemplateLoader。
	 * @param templateLoaders TemplateLoader 实例的最终列表
	 * @return 聚合的 TemplateLoader
	 */
	@Nullable
	protected TemplateLoader getAggregateTemplateLoader(List<TemplateLoader> templateLoaders) {
		switch (templateLoaders.size()) {
			case 0:
				logger.debug("No FreeMarker TemplateLoaders specified");
				return null;
			case 1:
				return templateLoaders.get(0);
			default:
				TemplateLoader[] loaders = templateLoaders.toArray(new TemplateLoader[0]);
				return new MultiTemplateLoader(loaders);
		}
	}

	/**
	 * 由子类覆盖，用于在此工厂执行默认初始化之后
	 * 对 Configuration 对象进行自定义后处理。
	 * <p>由 {@code createConfiguration()} 调用。
	 * @param config 当前的 Configuration 对象
	 * @throws IOException 如果未找到配置文件
	 * @throws TemplateException FreeMarker 初始化失败时
	 * @see #createConfiguration()
	 */
	protected void postProcessConfiguration(Configuration config) throws IOException, TemplateException {
	}

}
