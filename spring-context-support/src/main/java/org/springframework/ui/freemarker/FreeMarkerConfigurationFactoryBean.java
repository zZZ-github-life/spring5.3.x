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

package org.springframework.ui.freemarker;

import java.io.IOException;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.lang.Nullable;

/**
 * 创建 FreeMarker Configuration 并将其作为 bean 引用提供的工厂 bean。
 * 此 bean 适用于应用程序代码中任何类型的 FreeMarker 使用，
 * 例如用于生成电子邮件内容。对于 web 视图，
 * 使用 FreeMarkerConfigurer 来设置 FreeMarkerConfigurationFactory。
 *
 * 使用此类最简单的方式是指定一个 "templateLoaderPath"；
 * 你不需要任何进一步的配置。例如，在 web 应用程序上下文中：
 *
 * <pre class="code"> &lt;bean id="freemarkerConfiguration" class="org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean"&gt;
 *   &lt;property name="templateLoaderPath" value="/WEB-INF/freemarker/"/&gt;
 * &lt;/bean&gt;</pre>

 * 有关配置详细信息，请参见基类 FreeMarkerConfigurationFactory。
 *
 * <p>注意：Spring 的 FreeMarker 支持需要 FreeMarker 2.3 或更高版本。
 *
 * @author Darren Davison
 * @since 03.03.2004
 * @see #setConfigLocation
 * @see #setFreemarkerSettings
 * @see #setTemplateLoaderPath
 * @see org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer
 */
public class FreeMarkerConfigurationFactoryBean extends FreeMarkerConfigurationFactory
		implements FactoryBean<Configuration>, InitializingBean, ResourceLoaderAware {

	@Nullable
	private Configuration configuration;


	@Override
	public void afterPropertiesSet() throws IOException, TemplateException {
		this.configuration = createConfiguration();
	}


	@Override
	@Nullable
	public Configuration getObject() {
		return this.configuration;
	}

	@Override
	public Class<? extends Configuration> getObjectType() {
		return Configuration.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
