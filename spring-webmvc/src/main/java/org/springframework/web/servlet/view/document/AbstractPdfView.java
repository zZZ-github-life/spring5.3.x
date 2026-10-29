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
import java.io.OutputStream;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfWriter;

import org.springframework.web.servlet.view.AbstractView;

/**
 * PDF 视图的抽象超类。应用特有的视图类将扩展此类。
 * 视图内容本身保存在子类中，而不是保存在模板中。
 *
 * <p>此视图实现使用 Bruno Lowagie 的
 * <a href="https://www.lowagie.com/iText">iText</a> API。
 * 已知可与原始的 iText 2.1.7 及其分支
 * <a href="https://github.com/LibrePDF/OpenPDF">OpenPDF</a> 配合工作。
 * <b>我们强烈推荐 OpenPDF，因为它得到积极维护，
 * 并修复了针对不可信 PDF 内容的一个重要漏洞。</b>
 *
 * <p>注意：Internet Explorer 要求使用 ".pdf" 扩展名，
 * 因为它并不总是遵循声明的 content type。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Jean-Pierre Pawlak
 * @see AbstractPdfStamperView
 */
public abstract class AbstractPdfView extends AbstractView {

	/**
	 * 此构造器设置适当的内容类型 "application/pdf"。
	 * 请注意 IE 不会太在意这一点，但我们也无能为力。
	 * 生成的文档应带有 ".pdf" 扩展名。
	 */
	public AbstractPdfView() {
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

		// Apply preferences and build metadata.
		Document document = newDocument();
		PdfWriter writer = newWriter(document, baos);
		prepareWriter(model, writer, request);
		buildPdfMetadata(model, document, request);

		// Build PDF document.
		document.open();
		buildPdfDocument(model, document, writer, request, response);
		document.close();

		// Flush to HTTP response.
		writeToResponse(response, baos);
	}

	/**
	 * 创建一个新文档以承载 PDF 内容。
	 * <p>默认返回 A4 文档，但子类可以指定任意 Document，
	 * 并可通过在 View 上定义的 bean 属性进行参数化。
	 * @return the newly created iText Document instance
	 * @see com.lowagie.text.Document#Document(com.lowagie.text.Rectangle)
	 */
	protected Document newDocument() {
		return new Document(PageSize.A4);
	}

	/**
	 * 为给定的 iText Document 创建一个新的 PdfWriter。
	 * @param document the iText Document to create a writer for
	 * @param os the OutputStream to write to
	 * @return the PdfWriter instance to use
	 * @throws DocumentException if thrown during writer creation
	 */
	protected PdfWriter newWriter(Document document, OutputStream os) throws DocumentException {
		return PdfWriter.getInstance(document, os);
	}

	/**
	 * 准备给定的 PdfWriter。在构建 PDF 文档之前调用，
	 * 即在调用 {@code Document.open()} 之前调用。
	 * <p>例如，这适合用于注册页面事件监听器。
	 * 默认实现会设置此类 {@code getViewerPreferences()} 方法返回的
	 * 阅读器偏好设置。
	 * @param model the model, in case meta information must be populated from it
	 * @param writer the PdfWriter to prepare
	 * @param request in case we need locale etc. Shouldn't look at attributes.
	 * @throws DocumentException if thrown during writer preparation
	 * @see com.lowagie.text.Document#open()
	 * @see com.lowagie.text.pdf.PdfWriter#setPageEvent
	 * @see com.lowagie.text.pdf.PdfWriter#setViewerPreferences
	 * @see #getViewerPreferences()
	 */
	protected void prepareWriter(Map<String, Object> model, PdfWriter writer, HttpServletRequest request)
			throws DocumentException {

		writer.setViewerPreferences(getViewerPreferences());
	}

	/**
	 * 返回 PDF 文件的阅读器偏好设置。
	 * <p>默认返回 {@code AllowPrinting} 和
	 * {@code PageLayoutSinglePage}，但子类可以覆盖。
	 * 子类既可以使用固定的偏好设置，
	 * 也可以从 View 上定义的 bean 属性中获取。
	 * @return an int containing the bits information against PdfWriter definitions
	 * @see com.lowagie.text.pdf.PdfWriter#AllowPrinting
	 * @see com.lowagie.text.pdf.PdfWriter#PageLayoutSinglePage
	 */
	protected int getViewerPreferences() {
		return PdfWriter.ALLOW_PRINTING | PdfWriter.PageLayoutSinglePage;
	}

	/**
	 * 填充 iText Document 的元数据字段（作者、标题等）。
	 * <br>默认实现为空。子类可以重写此方法，
	 * 添加标题、主题、作者、创建者、关键字等元数据字段。
	 * 此方法在将 PdfWriter 关联到 Document 之后、
	 * 调用 {@code document.open()} 之前调用。
	 * @param model the model, in case meta information must be populated from it
	 * @param document the iText document being populated
	 * @param request in case we need locale etc. Shouldn't look at attributes.
	 * @see com.lowagie.text.Document#addTitle
	 * @see com.lowagie.text.Document#addSubject
	 * @see com.lowagie.text.Document#addKeywords
	 * @see com.lowagie.text.Document#addAuthor
	 * @see com.lowagie.text.Document#addCreator
	 * @see com.lowagie.text.Document#addProducer
	 * @see com.lowagie.text.Document#addCreationDate
	 * @see com.lowagie.text.Document#addHeader
	 */
	protected void buildPdfMetadata(Map<String, Object> model, Document document, HttpServletRequest request) {
	}

	/**
	 * 子类必须实现此方法，以便根据 model 构建 iText PDF 文档。
	 * 该方法在 {@code Document.open()} 与
	 * {@code Document.close()} 调用之间被调用。
	 * <p>注意，传入的 HTTP response 仅应用于设置 cookie 或其他 HTTP 头。
	 * 构建好的 PDF 文档本身将在该方法返回后自动写入 response。
	 * @param model the model Map
	 * @param document the iText Document to add elements to
	 * @param writer the PdfWriter to use
	 * @param request in case we need locale etc. Shouldn't look at attributes.
	 * @param response in case we need to set cookies. Shouldn't write to it.
	 * @throws Exception any exception that occurred during document building
	 * @see com.lowagie.text.Document#open()
	 * @see com.lowagie.text.Document#close()
	 */
	protected abstract void buildPdfDocument(Map<String, Object> model, Document document, PdfWriter writer,
			HttpServletRequest request, HttpServletResponse response) throws Exception;

}
