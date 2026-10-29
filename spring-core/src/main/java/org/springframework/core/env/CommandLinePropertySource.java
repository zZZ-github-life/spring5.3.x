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

package org.springframework.core.env;

import java.util.Collection;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * 由命令行参数支持的 {@link PropertySource} 实现的抽象基类。参数化类型 {@code T} 表示命令行选项的
 * 底层源。在 {@link SimpleCommandLinePropertySource} 的情况下，它可以简单到是一个 String 数组；而
 * 在 {@link JOptCommandLinePropertySource} 的情况下，则可能特定于某个 API，例如 JOpt 的
 * {@code OptionSet}。
 *
 * <h3>用途与一般用法</h3>
 *
 * 用于独立的基于 Spring 的应用程序，即通过传统的 {@code main} 方法（接收来自命令行的
 * {@code String[]} 参数）引导的应用程序。在许多情况下，直接在 {@code main} 方法中处理命令行参数可
 * 能就足够了，但在其他情况下，可能希望将参数作为值注入 Spring Bean。
 * {@code CommandLinePropertySource} 在后一种情况下变得有用。通常会将
 * {@code CommandLinePropertySource} 添加到 Spring {@code ApplicationContext} 的{@link Environment}
 * 中，此时所有命令行参数都可通过{@link Environment#getProperty(String)} 一族方法获得。例如：
 *
 * <pre class="code">
 * public static void main(String[] args) {
 *     CommandLinePropertySource clps = ...;
 *     AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
 *     ctx.getEnvironment().getPropertySources().addFirst(clps);
 *     ctx.register(AppConfig.class);
 *     ctx.refresh();
 * }</pre>
 *
 * 借助上面的引导逻辑，{@code AppConfig} 类可以 {@code @Inject} Spring {@code Environment}并直接从
 * 中查询属性：
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     &#064;Inject Environment env;
 *
 *     &#064;Bean
 *     public void DataSource dataSource() {
 *         MyVendorDataSource dataSource = new MyVendorDataSource();
 *         dataSource.setHostname(env.getProperty("db.hostname", "localhost"));
 *         dataSource.setUsername(env.getRequiredProperty("db.username"));
 *         dataSource.setPassword(env.getRequiredProperty("db.password"));
 *         // ...
 *         return dataSource;
 *     }
 * }</pre>
 *
 * 由于 {@code CommandLinePropertySource} 通过 {@code #addFirst} 方法添加到了{@code Environment} 的
 * {@link MutablePropertySources} 集合中，因此它具有最高的搜索优先级，这意味着即使“db.hostname”和其
 * 他属性可能存在于其他属性源（如系统环境变量）中，也会首先从命令行属性源中选择它。考虑到命令行上指
 * 定的参数天然比作为环境变量指定的参数更具体，这是一种合理的方式。
 *
 * <p>作为注入 {@code Environment} 的替代方案，可以使用 Spring 的 {@code @Value} 注解来注入这些属性，
 * 前提是已经注册了一个 {@link PropertySourcesPropertyResolver} Bean，无论是直接注册还是通过使用
 * {@code <context:property-placeholder>} 元素注册。例如：
 *
 * <pre class="code">
 * &#064;Component
 * public class MyComponent {
 *
 *     &#064;Value("my.property:defaultVal")
 *     private String myProperty;
 *
 *     public void getMyProperty() {
 *         return this.myProperty;
 *     }
 *
 *     // ...
 * }</pre>
 *
 * <h3>使用选项参数</h3>
 *
 * <p>各个命令行参数通过通常的 {@link PropertySource#getProperty(String)} 和
 * {@link PropertySource#containsProperty(String)} 方法表示为属性。例如，给定以下命令行：
 *
 * <pre class="code">--o1=v1 --o2</pre>
 *
 * “o1”和“o2”被视为“选项参数”，以下断言的结果将为 true：
 *
 * <pre class="code">
 * CommandLinePropertySource&lt;?&gt; ps = ...
 * assert ps.containsProperty("o1") == true;
 * assert ps.containsProperty("o2") == true;
 * assert ps.containsProperty("o3") == false;
 * assert ps.getProperty("o1").equals("v1");
 * assert ps.getProperty("o2").equals("");
 * assert ps.getProperty("o3") == null;
 * </pre>
 *
 * 请注意，“o2”选项没有参数，但 {@code getProperty("o2")} 解析为空字符串（{@code ""}）而不是
 * {@code null}，而 {@code getProperty("o3")} 由于未被指定而解析为 {@code null}。此行为与所有
 * {@code PropertySource} 实现应遵循的一般约定一致。
 *
 * <p>另请注意，虽然上面的示例使用“--”表示选项参数，但这种语法可能因各个命令行参数库而异。例如，基
 * 于 JOpt 或 Commons CLI 的实现可能允许使用单破折号（“-”）的“短”选项参数等。
 *
 * <h3>使用非选项参数</h3>
 *
 * <p>此抽象也支持非选项参数。任何未带选项风格前缀（如“-”或“--”）提供的参数都被视为“非选项参数”，可
 * 通过特殊的{@linkplain #DEFAULT_NON_OPTION_ARGS_PROPERTY_NAME "nonOptionArgs"} 属性获取。如果指定
 * 了多个非选项参数，该属性的值将是一个包含所有参数的逗号分隔字符串。这种方式确保了来自
 * {@code CommandLinePropertySource} 的所有属性都具有简单一致的返回类型（String），同时在与 Spring
 * {@link Environment} 及其内置的 {@code ConversionService} 配合使用时也便于转换。考虑以下示例：
 *
 * <pre class="code">--o1=v1 --o2=v2 /path/to/file1 /path/to/file2</pre>
 *
 * 在此示例中，“o1”和“o2”将被视为“选项参数”，而这两个文件系统路径则属于“非选项参数”。因此，以下断言
 * 的结果将为 true：
 *
 * <pre class="code">
 * CommandLinePropertySource&lt;?&gt; ps = ...
 * assert ps.containsProperty("o1") == true;
 * assert ps.containsProperty("o2") == true;
 * assert ps.containsProperty("nonOptionArgs") == true;
 * assert ps.getProperty("o1").equals("v1");
 * assert ps.getProperty("o2").equals("v2");
 * assert ps.getProperty("nonOptionArgs").equals("/path/to/file1,/path/to/file2");
 * </pre>
 *
 * <p>如上所述，当与 Spring {@code Environment} 抽象配合使用时，这个逗号分隔的字符串可以轻松转换为
 * String 数组或列表：
 *
 * <pre class="code">
 * Environment env = applicationContext.getEnvironment();
 * String[] nonOptionArgs = env.getProperty("nonOptionArgs", String[].class);
 * assert nonOptionArgs[0].equals("/path/to/file1");
 * assert nonOptionArgs[1].equals("/path/to/file2");
 * </pre>
 *
 * <p>可以通过 {@link #setNonOptionArgsPropertyName(String)} 方法自定义特殊“非选项参数”属性的名称。
 * 建议这样做，因为它能为非选项参数赋予恰当的语义。例如，如果将文件系统路径指定为非选项参数，那么将
 * 其称为“file.locations”之类的名称可能比默认的“nonOptionArgs”更合适：
 *
 * <pre class="code">
 * public static void main(String[] args) {
 *     CommandLinePropertySource clps = ...;
 *     clps.setNonOptionArgsPropertyName("file.locations");
 *
 *     AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
 *     ctx.getEnvironment().getPropertySources().addFirst(clps);
 *     ctx.register(AppConfig.class);
 *     ctx.refresh();
 * }</pre>
 *
 * <h3>限制</h3>
 *
 * 此抽象并不打算暴露 JOpt 或 Commons CLI 等底层命令行解析 API 的全部能力。它的意图恰恰相反：提供最
 * 简单的抽象，用于在命令行参数<em>被解析之后</em>访问它们。因此，典型情况将包括：完整配置底层命令
 * 行解析 API，解析进入 main 方法的 {@code String[]} 参数，然后简单地将解析结果提供给
 * {@code CommandLinePropertySource} 的实现。此时，所有参数都可以视为“选项”或“非选项”参数，并且如上
 * 所述可以通过常规的 {@code PropertySource} 和 {@code Environment} API 访问。
 *
 * @author Chris Beams
 * @since 3.1
 * @param <T> the source type
 * @see PropertySource
 * @see SimpleCommandLinePropertySource
 * @see JOptCommandLinePropertySource
 */
public abstract class CommandLinePropertySource<T> extends EnumerablePropertySource<T> {

	/** 赋予 {@link CommandLinePropertySource} 实例的默认名称：{@value}。 */
	public static final String COMMAND_LINE_PROPERTY_SOURCE_NAME = "commandLineArgs";

	/** 表示非选项参数的属性的默认名称：{@value}。 */
	public static final String DEFAULT_NON_OPTION_ARGS_PROPERTY_NAME = "nonOptionArgs";


	private String nonOptionArgsPropertyName = DEFAULT_NON_OPTION_ARGS_PROPERTY_NAME;


	/**
	 * 创建一个具有默认名称 {@value #COMMAND_LINE_PROPERTY_SOURCE_NAME}并由给定源对象支持的新
	 * {@code CommandLinePropertySource}。
	 */
	public CommandLinePropertySource(T source) {
		super(COMMAND_LINE_PROPERTY_SOURCE_NAME, source);
	}

	/**
	 * 创建一个具有给定名称并由给定源对象支持的新 {@link CommandLinePropertySource}。
	 */
	public CommandLinePropertySource(String name, T source) {
		super(name, source);
	}


	/**
	 * 指定特殊“非选项参数”属性的名称。默认值为 {@value #DEFAULT_NON_OPTION_ARGS_PROPERTY_NAME}。
	 */
	public void setNonOptionArgsPropertyName(String nonOptionArgsPropertyName) {
		this.nonOptionArgsPropertyName = nonOptionArgsPropertyName;
	}

	/**
	 * 此实现首先检查指定的名称是否为特殊的
	 * {@linkplain #setNonOptionArgsPropertyName(String) “非选项参数”属性}，如果是，则委托给抽象方法
	 * {@link #getNonOptionArgs()}，检查其是否返回空集合。否则，委托给并返回抽象方法
	 * {@link #containsOption(String)} 的值。
	 */
	@Override
	public final boolean containsProperty(String name) {
		if (this.nonOptionArgsPropertyName.equals(name)) {
			return !this.getNonOptionArgs().isEmpty();
		}
		return this.containsOption(name);
	}

	/**
	 * 此实现首先检查指定的名称是否为特殊的
	 * {@linkplain #setNonOptionArgsPropertyName(String) “非选项参数”属性}，如果是，则委托给抽象方法
	 * {@link #getNonOptionArgs()}。若如此且非选项参数集合为空，此方法返回 {@code null}。如果不为空，则
	 * 返回所有非选项参数的逗号分隔 String。否则，委托给并返回抽象方法 {@link #getOptionValues(String)}
	 * 的结果。
	 */
	@Override
	@Nullable
	public final String getProperty(String name) {
		if (this.nonOptionArgsPropertyName.equals(name)) {
			Collection<String> nonOptionArguments = this.getNonOptionArgs();
			if (nonOptionArguments.isEmpty()) {
				return null;
			}
			else {
				return StringUtils.collectionToCommaDelimitedString(nonOptionArguments);
			}
		}
		Collection<String> optionValues = this.getOptionValues(name);
		if (optionValues == null) {
			return null;
		}
		else {
			return StringUtils.collectionToCommaDelimitedString(optionValues);
		}
	}


	/**
	 * 返回从命令行解析出的选项参数集合中是否包含具有给定名称的选项。
	 */
	protected abstract boolean containsOption(String name);

	/**
	 * 返回与具有给定名称的命令行选项关联的值集合。
	 * <ul>
	 * <li>如果该选项存在且没有参数（例如 "--foo"），则返回空集合（{@code []}）</li>
	 * <li>如果该选项存在且只有一个值（例如 "--foo=bar"），则返回包含一个元素的集合（{@code ["bar"]}）</li>
	 * <li>如果该选项存在且底层命令行解析库支持多个参数（例如 "--foo=bar --foo=baz"），则返回为每个值都包含元素的集合（{@code ["bar", "baz"]}）</li>
	 * <li>如果该选项不存在，则返回 {@code null}</li>
	 * </ul>
	 */
	@Nullable
	protected abstract List<String> getOptionValues(String name);

	/**
	 * 返回从命令行解析出的非选项参数集合。永远不会是 {@code null}。
	 */
	protected abstract List<String> getNonOptionArgs();

}
