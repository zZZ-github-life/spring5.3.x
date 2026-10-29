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

package org.springframework.build.optional;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Usage;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;

/**
 * 一个 {@code Plugin}，添加对 Maven 风格可选依赖的支持。
 * 创建一个新的 {@code optional} 配置。该 {@code optional} 配置是项目
 * 编译和运行时类路径的一部分，但不会影响依赖项目的类路径。
 *
 * @author Andy Wilkinson
 */
public class OptionalDependenciesPlugin implements Plugin<Project> {

	/**
	 * {@code optional} 配置的名称。
	 */
	public static final String OPTIONAL_CONFIGURATION_NAME = "optional";

	@Override
	public void apply(Project project) {
		Configuration optional = project.getConfigurations().create("optional");
		optional.setCanBeConsumed(false);
		optional.setCanBeResolved(false);
		project.getPlugins().withType(JavaPlugin.class, (javaPlugin) -> {
			SourceSetContainer sourceSets = project.getConvention().getPlugin(JavaPluginConvention.class)
					.getSourceSets();
			sourceSets.all((sourceSet) -> {
				project.getConfigurations().getByName(sourceSet.getCompileClasspathConfigurationName()).extendsFrom(optional);
				project.getConfigurations().getByName(sourceSet.getRuntimeClasspathConfigurationName()).extendsFrom(optional);
			});
		});
	}

}