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

package org.springframework.web.reactive.result.view.freemarker;

import java.io.IOException;
import java.util.List;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.lang.Nullable;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactory;
import org.springframework.util.Assert;

/**
 * 通过 "configLocation" 和/或 "freemarkerSettings" 和/或 "templateLoaderPath" 属性
 * 将 FreeMarker 配置为用于 Web。
 * 使用此类最简单的方式是仅指定 "templateLoaderPath"（例如 "classpath:templates"），
 * 此时不需要任何进一步配置。
 *
 * <p>此 bean 必须包含在任何使用 {@link FreeMarkerView} 的应用的应用上下文中。
 * 它纯粹是为了配置 FreeMarker 而存在。
 * 它不应由应用组件引用，而仅供 {@code FreeMarkerView} 内部使用。
 * 它实现了 {@link FreeMarkerConfig}，以便 {@code FreeMarkerView} 无需依赖配置器的
 * bean 名称即可找到它。
 *
 * <p>请注意，你也可以通过 "configuration" 属性引用预先配置的 FreeMarker Configuration
 * 实例。例如，这允许在 Web 和电子邮件用途之间共享同一个 FreeMarker Configuration。
 *
 * <p>此配置器为此包注册了一个模板加载器，允许引用此包中包含的 "spring.ftl" 宏库：
 *
 * <pre class="code">
 * &lt;#import "/spring.ftl" as spring/&gt;
 * &lt;@spring.bind "person.age"/&gt;
 * age is ${spring.status.value}</pre>
 *
 * 注意：Spring 的 FreeMarker 支持需要 FreeMarker 2.3 或更高版本。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class FreeMarkerConfigurer extends FreeMarkerConfigurationFactory
		implements FreeMarkerConfig, InitializingBean, ResourceLoaderAware {

	@Nullable
	private Configuration configuration;


	public FreeMarkerConfigurer() {
		setDefaultEncoding("UTF-8");
	}


	/**
	 * 设置用于 FreeMarker Web 配置的预配置 Configuration，例如在 Web 和电子邮件用途之间
	 * 共享的 Configuration。如果未设置此项，则必须指定 FreeMarkerConfigurationFactory
	 * 的属性（由此类继承）。
	 */
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}


	/**
	 * 如果未被预配置的 FreeMarker Configuration 覆盖，则初始化 FreeMarkerConfigurationFactory
	 * 的 Configuration。
	 * <p>设置一个 ClassTemplateLoader 用于加载 Spring 宏。
	 * @see #createConfiguration
	 * @see #setConfiguration
	 */
	@Override
	public void afterPropertiesSet() throws IOException, TemplateException {
		if (this.configuration == null) {
			this.configuration = createConfiguration();
		}
	}

	/**
	 * 此实现为 Spring 提供的宏注册一个额外的 ClassTemplateLoader，添加到列表末尾。
	 */
	@Override
	protected void postProcessTemplateLoaders(List<TemplateLoader> templateLoaders) {
		templateLoaders.add(new ClassTemplateLoader(FreeMarkerConfigurer.class, ""));
	}


	/**
	 * 返回此 bean 包装的 Configuration 对象。
	 */
	@Override
	public Configuration getConfiguration() {
		Assert.state(this.configuration != null, "No Configuration available");
		return this.configuration;
	}

}
