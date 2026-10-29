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

import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * 由简单 String 数组支持的 {@link CommandLinePropertySource} 实现。
 *
 * <h3>用途</h3>
 * <p>此 {@code CommandLinePropertySource} 实现旨在提供尽可能简单的命令行参数解析方式。
 * 与所有 {@code CommandLinePropertySource} 实现一样，命令行参数被分为两个不同的组：
 * <em>选项参数</em>和<em>非选项参数</em>，如下所述
 * <em>（部分内容复制自 {@link SimpleCommandLineArgsParser} 的 Javadoc）</em>：
 *
 * <h3>处理选项参数</h3>
 * <p>选项参数必须遵循以下确切语法：
 *
 * <pre class="code">--optName[=optValue]</pre>
 *
 * <p>也就是说，选项必须以 "{@code --}" 为前缀，并且可以指定值，也可以不指定。
 * 如果指定了值，名称和值必须通过等号（"="）分隔，且<em>不能有空格</em>。
 * 值可选地可以是空字符串。
 *
 * <h4>选项参数的有效示例</h4>
 * <pre class="code">
 * --foo
 * --foo=
 * --foo=""
 * --foo=bar
 * --foo="bar then baz"
 * --foo=bar,baz,biz</pre>
 *
 * <h4>选项参数的无效示例</h4>
 * <pre class="code">
 * -foo
 * --foo bar
 * --foo = bar
 * --foo=bar --foo=baz --foo=biz</pre>
 *
 * <h3>处理非选项参数</h3>
 * <p>在命令行中指定但未带 "{@code --}" 选项前缀的任何及所有参数
 * 都将被视为“非选项参数”，并可通过 {@link CommandLineArgs#getNonOptionArgs()} 方法获取。
 *
 * <h3>典型用法</h3>
 * <pre class="code">
 * public static void main(String[] args) {
 *     PropertySource&lt;?&gt; ps = new SimpleCommandLinePropertySource(args);
 *     // ...
 * }</pre>
 *
 * 有关完整的一般用法示例，请参见 {@link CommandLinePropertySource}。
 *
 * <h3>进阶用法</h3>
 *
 * <p>当需要功能更完整的命令行解析时，可以考虑使用所提供的
 * {@link JOptCommandLinePropertySource}，或者针对你选择的命令行解析库
 * 实现自己的 {@code CommandLinePropertySource}。
 *
 * @author Chris Beams
 * @since 3.1
 * @see CommandLinePropertySource
 * @see JOptCommandLinePropertySource
 */
public class SimpleCommandLinePropertySource extends CommandLinePropertySource<CommandLineArgs> {

	/**
	 * 创建一个具有默认名称、由给定的命令行参数 {@code String[]} 支持的新
	 * {@code SimpleCommandLinePropertySource}。
	 * @see CommandLinePropertySource#COMMAND_LINE_PROPERTY_SOURCE_NAME
	 * @see CommandLinePropertySource#CommandLinePropertySource(Object)
	 */
	public SimpleCommandLinePropertySource(String... args) {
		super(new SimpleCommandLineArgsParser().parse(args));
	}

	/**
	 * 创建一个具有给定名称、由给定的命令行参数 {@code String[]} 支持的新
	 * {@code SimpleCommandLinePropertySource}。
	 */
	public SimpleCommandLinePropertySource(String name, String[] args) {
		super(name, new SimpleCommandLineArgsParser().parse(args));
	}

	/**
	 * 获取选项参数的属性名。
	 */
	@Override
	public String[] getPropertyNames() {
		return StringUtils.toStringArray(this.source.getOptionNames());
	}

	@Override
	protected boolean containsOption(String name) {
		return this.source.containsOption(name);
	}

	@Override
	@Nullable
	protected List<String> getOptionValues(String name) {
		return this.source.getOptionValues(name);
	}

	@Override
	protected List<String> getNonOptionArgs() {
		return this.source.getNonOptionArgs();
	}

}
