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

package org.springframework.web.servlet.view.document;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;

import org.springframework.web.servlet.view.AbstractView;

/**
 * 传统 XLS 格式 Excel 文档视图的便捷超类。
 * 兼容 Apache POI 3.5 及更高版本。
 *
 * <p>关于如何在子类中操作工作簿，请参阅
 * <a href="https://poi.apache.org">Apache 的 POI 网站</a>
 *
 * @author Juergen Hoeller
 * @since 4.2
 */
public abstract class AbstractXlsView extends AbstractView {

	/**
	 * 默认构造器。
	 * 将视图的内容类型设置为 "application/vnd.ms-excel"。
	 */
	public AbstractXlsView() {
		setContentType("application/vnd.ms-excel");
	}


	@Override
	protected boolean generatesDownloadContent() {
		return true;
	}

	/**
	 * 根据指定的 model 渲染 Excel 视图。
	 */
	@Override
	protected final void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		// Create a fresh workbook instance for this render step.
		Workbook workbook = createWorkbook(model, request);

		// Delegate to application-provided document code.
		buildExcelDocument(model, workbook, request, response);

		// Set the content type.
		response.setContentType(getContentType());

		// Flush byte array to servlet output stream.
		renderWorkbook(workbook, response);
	}


	/**
	 * 用于创建 POI {@link Workbook} 实例的模板方法。
	 * <p>默认实现创建一个传统的 {@link HSSFWorkbook}。
	 * Spring 提供的子类针对基于 OOXML 的变体重写了此方法；
	 * 自定义子类可以重写此方法以从文件读取工作簿。
	 * @param model the model Map
	 * @param request current HTTP request (for taking the URL or headers into account)
	 * @return the new {@link Workbook} instance
	 */
	protected Workbook createWorkbook(Map<String, Object> model, HttpServletRequest request) {
		return new HSSFWorkbook();
	}

	/**
	 * 实际的渲染步骤：获取 POI {@link Workbook} 并将其渲染到给定的 response。
	 * @param workbook the POI Workbook to render
	 * @param response current HTTP response
	 * @throws IOException when thrown by I/O methods that we're delegating to
	 */
	protected void renderWorkbook(Workbook workbook, HttpServletResponse response) throws IOException {
		ServletOutputStream out = response.getOutputStream();
		workbook.write(out);
		workbook.close();
	}

	/**
	 * 由应用提供的子类必须实现此方法，以便根据 model
	 * 填充 Excel 工作簿文档。
	 * @param model the model Map
	 * @param workbook the Excel workbook to populate
	 * @param request in case we need locale etc. Shouldn't look at attributes.
	 * @param response in case we need to set cookies. Shouldn't write to it.
	 */
	protected abstract void buildExcelDocument(
			Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response)
			throws Exception;

}
