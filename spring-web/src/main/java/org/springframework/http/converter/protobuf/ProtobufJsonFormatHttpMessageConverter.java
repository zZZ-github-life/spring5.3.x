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

package org.springframework.http.converter.protobuf;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.util.JsonFormat;

import org.springframework.lang.Nullable;

/**
 * {@link ProtobufHttpMessageConverter} 的子类，强制使用 Protobuf 3 及其官方库
 * {@code "com.google.protobuf:protobuf-java-util"} 进行 JSON 处理。
 *
 * <p>最重要的是，此类允许通过 {@link JsonFormat} 工具自定义 JSON 解析器和
 * 打印机配置。如果未提供特殊的解析器或打印机配置，将使用默认变体。
 *
 * <p>要求 Protobuf 3.x 和 {@code "com.google.protobuf:protobuf-java-util"} 3.x，
 * 建议使用 3.3 或更高版本。
 *
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 5.0
 * @see JsonFormat#parser()
 * @see JsonFormat#printer()
 * @see #ProtobufJsonFormatHttpMessageConverter(com.google.protobuf.util.JsonFormat.Parser, com.google.protobuf.util.JsonFormat.Printer)
 */
public class ProtobufJsonFormatHttpMessageConverter extends ProtobufHttpMessageConverter {

	/**
	 * 使用默认的
	 * {@link com.google.protobuf.util.JsonFormat.Parser JsonFormat.Parser}、
	 * {@link com.google.protobuf.util.JsonFormat.Printer JsonFormat.Printer}
	 * 和 {@link ExtensionRegistry} 实例的构造函数。
	 */
	public ProtobufJsonFormatHttpMessageConverter() {
		this(null, null);
	}

	/**
	 * 使用给定的
	 * {@link com.google.protobuf.util.JsonFormat.Parser JsonFormat.Parser}、
	 * {@link com.google.protobuf.util.JsonFormat.Printer JsonFormat.Printer}
	 * 实例以及默认的 {@link ExtensionRegistry} 实例的构造函数。
	 */
	public ProtobufJsonFormatHttpMessageConverter(
			@Nullable JsonFormat.Parser parser, @Nullable JsonFormat.Printer printer) {

		this(parser, printer, (ExtensionRegistry) null);
	}

	/**
	 * 使用给定的
	 * {@link com.google.protobuf.util.JsonFormat.Parser JsonFormat.Parser}、
	 * {@link com.google.protobuf.util.JsonFormat.Printer JsonFormat.Printer}
	 * 和 {@link ExtensionRegistry} 实例的构造函数。
	 */
	public ProtobufJsonFormatHttpMessageConverter(@Nullable JsonFormat.Parser parser,
			@Nullable JsonFormat.Printer printer, @Nullable ExtensionRegistry extensionRegistry) {

		super(new ProtobufJavaUtilSupport(parser, printer), extensionRegistry);
	}

	/**
	 * 使用给定的 {@code JsonFormat.Parser} 和 {@code JsonFormat.Printer} 配置
	 * 构造一个新的 {@code ProtobufJsonFormatHttpMessageConverter}，
	 * 同时接受一个允许注册消息扩展的初始化器。
	 * @param parser JSON 解析器配置
	 * @param printer JSON 打印器配置
	 * @param registryInitializer 消息扩展的初始化器
	 * @deprecated as of 5.1, in favor of
	 * {@link #ProtobufJsonFormatHttpMessageConverter(com.google.protobuf.util.JsonFormat.Parser, com.google.protobuf.util.JsonFormat.Printer, ExtensionRegistry)}
	 */
	@Deprecated
	public ProtobufJsonFormatHttpMessageConverter(@Nullable JsonFormat.Parser parser,
			@Nullable JsonFormat.Printer printer, @Nullable ExtensionRegistryInitializer registryInitializer) {

		super(new ProtobufJavaUtilSupport(parser, printer), null);
		if (registryInitializer != null) {
			registryInitializer.initializeExtensionRegistry(this.extensionRegistry);
		}
	}

}
