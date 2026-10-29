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

package org.springframework.messaging.converter;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.util.JsonFormat;

import org.springframework.lang.Nullable;

/**
 * {@link ProtobufMessageConverter} 的子类，用于配合官方
 * {@code "com.google.protobuf:protobuf-java-util"} 库提供 JSON 支持。
 *
 * <p>最重要的是，此类允许通过 {@link JsonFormat} 工具自定义 JSON 解析器和打印器配置。
 * 如果未提供特殊的解析器或打印器配置，则使用默认变体。
 *
 * <p>需要 Protobuf 3.x 和 {@code "com.google.protobuf:protobuf-java-util"} 3.x，
 * 建议使用 3.3 或更高版本。
 *
 * @author Rossen Stoyanchev
 * @since 5.2.2
 */
public class ProtobufJsonFormatMessageConverter extends ProtobufMessageConverter {

	/**
	 * 使用默认的 {@link com.google.protobuf.util.JsonFormat.Parser
	 * JsonFormat.Parser}、{@link com.google.protobuf.util.JsonFormat.Printer
	 * JsonFormat.Printer} 和 {@link ExtensionRegistry} 实例的构造函数。
	 */
	public ProtobufJsonFormatMessageConverter(@Nullable ExtensionRegistry extensionRegistry) {
		this(null, null);
	}

	/**
	 * 使用给定的 {@link com.google.protobuf.util.JsonFormat.Parser
	 * JsonFormat.Parser}、{@link com.google.protobuf.util.JsonFormat.Printer
	 * JsonFormat.Printer} 实例以及默认的 {@link ExtensionRegistry} 实例的构造函数。
	 */
	public ProtobufJsonFormatMessageConverter(
			@Nullable JsonFormat.Parser parser, @Nullable JsonFormat.Printer printer) {

		this(parser, printer, null);
	}

	/**
	 * 使用给定的 {@link com.google.protobuf.util.JsonFormat.Parser
	 * JsonFormat.Parser}、{@link com.google.protobuf.util.JsonFormat.Printer
	 * JsonFormat.Printer} 和 {@link ExtensionRegistry} 实例的构造函数。
	 */
	public ProtobufJsonFormatMessageConverter(@Nullable JsonFormat.Parser parser,
			@Nullable JsonFormat.Printer printer, @Nullable ExtensionRegistry extensionRegistry) {

		super(new ProtobufJavaUtilSupport(parser, printer), extensionRegistry);
	}

}
