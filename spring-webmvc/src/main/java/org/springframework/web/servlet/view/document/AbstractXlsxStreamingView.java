/*
 * Copyright 2002-2015 the original author or authors.
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

/**
 * Office 2007 XLSX 格式 Excel 文档视图的便捷超类，
 * 使用 POI 的流式变体。兼容 Apache POI 3.9 及更高版本。
 *
 * <p>关于如何在子类中操作工作簿，请参阅
 * <a href="https://poi.apache.org">Apache 的 POI 网站</a>。
 *
 * @author Juergen Hoeller
 * @since 4.2
 */
public abstract class AbstractXlsxStreamingView extends AbstractXlsxView {

	/**
	 * 此实现创建一个 {@link SXSSFWorkbook} 用于流式输出 XLSX 格式。
	 */
	@Override
	protected SXSSFWorkbook createWorkbook(Map<String, Object> model, HttpServletRequest request) {
		return new SXSSFWorkbook();
	}

	/**
	 * 此实现在渲染完成后释放 {@link SXSSFWorkbook}。
	 * @see org.apache.poi.xssf.streaming.SXSSFWorkbook#dispose()
	 */
	@Override
	protected void renderWorkbook(Workbook workbook, HttpServletResponse response) throws IOException {
		super.renderWorkbook(workbook, response);

		// Dispose of temporary files in case of streaming variant...
		((SXSSFWorkbook) workbook).dispose();
	}

}
