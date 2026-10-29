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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Office 2007 XLSX 格式（由 POI-OOXML 支持）Excel 文档视图的便捷超类。
 * 兼容 Apache POI 3.5 及更高版本。
 *
 * <p>关于如何在子类中操作工作簿，请参阅
 * <a href="https://poi.apache.org">Apache 的 POI 网站</a>。
 *
 * @author Juergen Hoeller
 * @since 4.2
 */
public abstract class AbstractXlsxView extends AbstractXlsView {

	/**
	 * 默认构造器。
	 * <p>将视图的内容类型设置为
	 * {@code "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"}。
	 */
	public AbstractXlsxView() {
		setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	}

	/**
	 * 此实现为 XLSX 格式创建一个 {@link XSSFWorkbook}。
	 */
	@Override
	protected Workbook createWorkbook(Map<String, Object> model, HttpServletRequest request) {
		return new XSSFWorkbook();
	}

}
