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

package org.springframework.test.web.servlet.result;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.util.CollectionUtils;

/**
 * 基于 {@link ResultHandler} 的结果操作的静态工厂方法。
 *
 * <h3>Eclipse 用户</h3>
 * <p>建议将此添加为 Java 编辑器收藏项。要导航到此设置，
 * 请打开 Preferences 并输入 “favorites”。
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.2
 */
public abstract class MockMvcResultHandlers {

	private static final Log logger = LogFactory.getLog("org.springframework.test.web.servlet.result");


	/**
	 * 通过 Apache Commons Logging，使用日志类别
	 * {@code org.springframework.test.web.servlet.result}，
	 * 将 {@link MvcResult} 详情记录为 {@code DEBUG} 日志消息。
	 * @since 4.2
	 * @see #print()
	 * @see #print(OutputStream)
	 * @see #print(Writer)
	 */
	public static ResultHandler log() {
		return new LoggingResultHandler();
	}

	/**
	 * 将 {@link MvcResult} 详情打印到“标准”输出流。
	 * @see System#out
	 * @see #print(OutputStream)
	 * @see #print(Writer)
	 * @see #log()
	 */
	public static ResultHandler print() {
		return print(System.out);
	}

	/**
	 * 将 {@link MvcResult} 详情打印到提供的 {@link OutputStream}。
	 * @since 4.2
	 * @see #print()
	 * @see #print(Writer)
	 * @see #log()
	 */
	public static ResultHandler print(OutputStream stream) {
		return new PrintWriterPrintingResultHandler(new PrintWriter(stream, true));
	}

	/**
	 * 将 {@link MvcResult} 详情打印到提供的 {@link Writer}。
	 * @since 4.2
	 * @see #print()
	 * @see #print(OutputStream)
	 * @see #log()
	 */
	public static ResultHandler print(Writer writer) {
		return new PrintWriterPrintingResultHandler(new PrintWriter(writer, true));
	}


	/**
	 * 一个写入 {@link PrintWriter} 的 {@link PrintingResultHandler}。
	 */
	private static class PrintWriterPrintingResultHandler extends PrintingResultHandler {

		public PrintWriterPrintingResultHandler(PrintWriter writer) {
			super(new ResultValuePrinter() {
				@Override
				public void printHeading(String heading) {
					writer.println();
					writer.println(String.format("%s:", heading));
				}
				@Override
				public void printValue(String label, @Nullable Object value) {
					if (value != null && value.getClass().isArray()) {
						value = CollectionUtils.arrayToList(value);
					}
					writer.println(String.format("%17s = %s", label, value));
				}
			});
		}
	}


	/**
	 * 一个通过 Apache Commons Logging 以 {@code DEBUG} 级别
	 * 记录 {@link MvcResult} 详情的 {@link ResultHandler}。
	 *
	 * <p>将构建日志消息的工作委托给 {@link PrintWriterPrintingResultHandler}。
	 * @since 4.2
	 */
	private static class LoggingResultHandler implements ResultHandler {

		@Override
		public void handle(MvcResult result) throws Exception {
			if (logger.isDebugEnabled()) {
				StringWriter stringWriter = new StringWriter();
				ResultHandler printingResultHandler =
						new PrintWriterPrintingResultHandler(new PrintWriter(stringWriter));
				printingResultHandler.handle(result);
				logger.debug("MvcResult details:\n" + stringWriter);
			}
		}
	}

}
