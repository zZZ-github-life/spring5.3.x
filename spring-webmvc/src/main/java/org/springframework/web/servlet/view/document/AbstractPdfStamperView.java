/*
 * Copyright 2002-2018 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;

import org.springframework.util.Assert;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

/**
 * 用于在带有 AcroForm 的现有文档上进行操作的 PDF 视图的抽象超类。
 * 应用特有的视图类将扩展此类，以便将 PDF 表单与 model 数据合并。
 *
 * <p>此视图实现使用 Bruno Lowagie 的
 * <a href="https://www.lowagie.com/iText">iText</a> API。
 * 已知可与原始的 iText 2.1.7 及其分支
 * <a href="https://github.com/LibrePDF/OpenPDF">OpenPDF</a> 配合工作。
 * <b>我们强烈推荐 OpenPDF，因为它得到积极维护，
 * 并修复了针对不可信 PDF 内容的一个重要漏洞。</b>
 *
 * <p>感谢 Bryant Larsen 提出建议并提供原始原型！
 *
 * @author Juergen Hoeller
 * @since 2.5.4
 * @see AbstractPdfView
 */
public abstract class AbstractPdfStamperView extends AbstractUrlBasedView {

	public AbstractPdfStamperView(){
		setContentType("application/pdf");
	}


	@Override
	protected boolean generatesDownloadContent() {
		return true;
	}

	@Override
	protected final void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		// IE workaround: write into byte array first.
		ByteArrayOutputStream baos = createTemporaryOutputStream();

		PdfReader reader = readPdfResource();
		PdfStamper stamper = new PdfStamper(reader, baos);
		mergePdfDocument(model, stamper, request, response);
		stamper.close();

		// Flush to HTTP response.
		writeToResponse(response, baos);
	}

	/**
	 * 将原始 PDF 资源读取为一个 iText PdfReader。
	 * <p>默认实现会将指定的 "url" 属性解析为 ApplicationContext 资源。
	 * @return the PdfReader instance
	 * @throws IOException if resource access failed
	 * @see #setUrl
	 */
	protected PdfReader readPdfResource() throws IOException {
		String url = getUrl();
		Assert.state(url != null, "'url' not set");
		return new PdfReader(obtainApplicationContext().getResource(url).getInputStream());
	}

	/**
	 * 子类必须实现此方法，以便将 PDF 表单与给定的 model 数据合并。
	 * <p>你可以在此处为 AcroForm 设置值。以下示例展示了该层面可以完成的操作：
	 * <pre class="code">
	 * // get the form from the document
	 * AcroFields form = stamper.getAcroFields();
	 *
	 * // set some values on the form
	 * form.setField("field1", "value1");
	 * form.setField("field2", "value2");
	 *
	 * // set the disposition and filename
	 * response.setHeader("Content-disposition", "attachment; FILENAME=someName.pdf");</pre>
	 * <p>注意，传入的 HTTP response 仅应用于设置 cookie 或其他 HTTP 头。
	 * 构建好的 PDF 文档本身将在该方法返回后自动写入 response。
	 * @param model the model Map
	 * @param stamper the PdfStamper instance that will contain the AcroFields.
	 * You may also customize this PdfStamper instance according to your needs,
	 * e.g. setting the "formFlattening" property.
	 * @param request in case we need locale etc. Shouldn't look at attributes.
	 * @param response in case we need to set cookies. Shouldn't write to it.
	 * @throws Exception any exception that occurred during document building
     */
	protected abstract void mergePdfDocument(Map<String, Object> model, PdfStamper stamper,
			HttpServletRequest request, HttpServletResponse response) throws Exception;

}
