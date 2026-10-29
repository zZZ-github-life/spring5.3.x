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

package org.springframework.web.multipart.commons;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.WebUtils;

/**
 * 使用 Apache Commons FileUpload 1.2 或更高版本的 multipart 解析器的基类。
 *
 * <p>为 multipart 请求提供通用的配置属性和解析功能，使用
 * Spring CommonsMultipartFile 实例组成的 Map 表示上传的文件，
 * 并使用基于 String 的参数 Map 表示上传的表单字段。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see CommonsMultipartFile
 * @see CommonsMultipartResolver
 */
public abstract class CommonsFileUploadSupport {

	protected final Log logger = LogFactory.getLog(getClass());

	private final DiskFileItemFactory fileItemFactory;

	private final FileUpload fileUpload;

	private boolean uploadTempDirSpecified = false;

	private boolean preserveFilename = false;


	/**
	 * 使用相应的 FileItemFactory 和 FileUpload 实例
	 * 创建一个新的 CommonsFileUploadSupport。
	 * @see #newFileItemFactory
	 * @see #newFileUpload
	 */
	public CommonsFileUploadSupport() {
		this.fileItemFactory = newFileItemFactory();
		this.fileUpload = newFileUpload(getFileItemFactory());
	}


	/**
	 * 返回底层的 {@code org.apache.commons.fileupload.disk.DiskFileItemFactory}
	 * 实例。几乎不需要访问该方法。
	 * @return the underlying DiskFileItemFactory instance
	 */
	public DiskFileItemFactory getFileItemFactory() {
		return this.fileItemFactory;
	}

	/**
	 * 返回底层的 {@code org.apache.commons.fileupload.FileUpload}
	 * 实例。几乎不需要访问该方法。
	 * @return the underlying FileUpload instance
	 */
	public FileUpload getFileUpload() {
		return this.fileUpload;
	}

	/**
	 * 设置上传被拒绝之前允许的最大大小（以字节为单位）。
	 * -1 表示不限制（默认值）。
	 * @param maxUploadSize the maximum upload size allowed
	 * @see org.apache.commons.fileupload.FileUploadBase#setSizeMax
	 */
	public void setMaxUploadSize(long maxUploadSize) {
		this.fileUpload.setSizeMax(maxUploadSize);
	}

	/**
	 * 设置上传被拒绝之前每个单独文件允许的最大大小（以字节为单位）。
	 * -1 表示不限制（默认值）。
	 * @param maxUploadSizePerFile the maximum upload size per file
	 * @since 4.2
	 * @see org.apache.commons.fileupload.FileUploadBase#setFileSizeMax
	 */
	public void setMaxUploadSizePerFile(long maxUploadSizePerFile) {
		this.fileUpload.setFileSizeMax(maxUploadSizePerFile);
	}

	/**
	 * 设置上传内容写入磁盘之前允许的最大大小（以字节为单位）。
	 * 超过该大小的上传文件仍会被接收，但不会再存储在内存中。
	 * 根据 Commons FileUpload 的约定，默认值为 10240。
	 * @param maxInMemorySize the maximum in memory size allowed
	 * @see org.apache.commons.fileupload.disk.DiskFileItemFactory#setSizeThreshold
	 */
	public void setMaxInMemorySize(int maxInMemorySize) {
		this.fileItemFactory.setSizeThreshold(maxInMemorySize);
	}

	/**
	 * 设置用于解析请求的默认字符编码，该编码将应用于各个部分的头部以及表单字段。
	 * 根据 Servlet 规范，默认值为 ISO-8859-1。
	 * <p>如果请求自身指定了字符编码，则请求的编码将覆盖此设置。
	 * 这也允许在调用 {@code ServletRequest.setCharacterEncoding} 方法的过滤器中
	 * 统一覆盖字符编码。
	 * @param defaultEncoding the character encoding to use
	 * @see javax.servlet.ServletRequest#getCharacterEncoding
	 * @see javax.servlet.ServletRequest#setCharacterEncoding
	 * @see WebUtils#DEFAULT_CHARACTER_ENCODING
	 * @see org.apache.commons.fileupload.FileUploadBase#setHeaderEncoding
	 */
	public void setDefaultEncoding(String defaultEncoding) {
		this.fileUpload.setHeaderEncoding(defaultEncoding);
	}

	/**
	 * 确定用于解析请求的默认编码。
	 * @see #setDefaultEncoding
	 */
	protected String getDefaultEncoding() {
		String encoding = getFileUpload().getHeaderEncoding();
		if (encoding == null) {
			encoding = WebUtils.DEFAULT_CHARACTER_ENCODING;
		}
		return encoding;
	}

	/**
	 * 设置存储上传文件的临时目录。
	 * 默认为 Servlet 容器为该 Web 应用提供的临时目录。
	 * @see org.springframework.web.util.WebUtils#TEMP_DIR_CONTEXT_ATTRIBUTE
	 */
	public void setUploadTempDir(Resource uploadTempDir) throws IOException {
		if (!uploadTempDir.exists() && !uploadTempDir.getFile().mkdirs()) {
			throw new IllegalArgumentException("Given uploadTempDir [" + uploadTempDir + "] could not be created");
		}
		this.fileItemFactory.setRepository(uploadTempDir.getFile());
		this.uploadTempDirSpecified = true;
	}

	/**
	 * 返回存储上传文件的临时目录。
	 * @see #setUploadTempDir
	 */
	protected boolean isUploadTempDirSpecified() {
		return this.uploadTempDirSpecified;
	}

	/**
	 * 设置是否保留客户端发送的文件名，而不在
	 * {@link CommonsMultipartFile#getOriginalFilename()} 中去掉路径信息。
	 * <p>默认值为 "false"，即去掉可能作为实际文件名前缀的路径信息（例如来自 Opera）。
	 * 将其设置为 "true" 可原样保留客户端指定的文件名，包括可能的路径分隔符。
	 * @since 4.3.5
	 * @see MultipartFile#getOriginalFilename()
	 * @see CommonsMultipartFile#setPreserveFilename(boolean)
	 */
	public void setPreserveFilename(boolean preserveFilename) {
		this.preserveFilename = preserveFilename;
	}


	/**
	 * Commons DiskFileItemFactory 实例的工厂方法。
	 * <p>默认实现返回标准的 DiskFileItemFactory。
	 * 可以重写以使用自定义子类，例如用于测试目的。
	 * @return the new DiskFileItemFactory instance
	 */
	protected DiskFileItemFactory newFileItemFactory() {
		return new DiskFileItemFactory();
	}

	/**
	 * Commons FileUpload 实例的工厂方法。
	 * <p><b>需由子类实现。</b>
	 * @param fileItemFactory the Commons FileItemFactory to build upon
	 * @return the Commons FileUpload instance
	 */
	protected abstract FileUpload newFileUpload(FileItemFactory fileItemFactory);


	/**
	 * 为给定编码确定合适的 FileUpload 实例。
	 * <p>默认实现会在编码匹配时返回共享的 FileUpload 实例，
	 * 否则创建一个除所需编码外其余配置相同的新 FileUpload 实例。
	 * @param encoding the character encoding to use
	 * @return an appropriate FileUpload instance.
	 */
	protected FileUpload prepareFileUpload(@Nullable String encoding) {
		FileUpload fileUpload = getFileUpload();
		FileUpload actualFileUpload = fileUpload;

		// Use new temporary FileUpload instance if the request specifies
		// its own encoding that does not match the default encoding.
		if (encoding != null && !encoding.equals(fileUpload.getHeaderEncoding())) {
			actualFileUpload = newFileUpload(getFileItemFactory());
			actualFileUpload.setSizeMax(fileUpload.getSizeMax());
			actualFileUpload.setFileSizeMax(fileUpload.getFileSizeMax());
			actualFileUpload.setHeaderEncoding(encoding);
		}

		return actualFileUpload;
	}

	/**
	 * 将给定的 Commons FileItem 列表解析为 Spring 的 MultipartParsingResult，
	 * 其中包含 Spring MultipartFile 实例以及 multipart 参数的 Map。
	 * @param fileItems the Commons FileItems to parse
	 * @param encoding the encoding to use for form fields
	 * @return the Spring MultipartParsingResult
	 * @see CommonsMultipartFile#CommonsMultipartFile(org.apache.commons.fileupload.FileItem)
	 */
	protected MultipartParsingResult parseFileItems(List<FileItem> fileItems, String encoding) {
		MultiValueMap<String, MultipartFile> multipartFiles = new LinkedMultiValueMap<>();
		Map<String, String[]> multipartParameters = new HashMap<>();
		Map<String, String> multipartParameterContentTypes = new HashMap<>();

		// Extract multipart files and multipart parameters.
		for (FileItem fileItem : fileItems) {
			if (fileItem.isFormField()) {
				String value;
				String partEncoding = determineEncoding(fileItem.getContentType(), encoding);
				try {
					value = fileItem.getString(partEncoding);
				}
				catch (UnsupportedEncodingException ex) {
					if (logger.isWarnEnabled()) {
						logger.warn("Could not decode multipart item '" + fileItem.getFieldName() +
								"' with encoding '" + partEncoding + "': using platform default");
					}
					value = fileItem.getString();
				}
				String[] curParam = multipartParameters.get(fileItem.getFieldName());
				if (curParam == null) {
					// simple form field
					multipartParameters.put(fileItem.getFieldName(), new String[] {value});
				}
				else {
					// array of simple form fields
					String[] newParam = StringUtils.addStringToArray(curParam, value);
					multipartParameters.put(fileItem.getFieldName(), newParam);
				}
				multipartParameterContentTypes.put(fileItem.getFieldName(), fileItem.getContentType());
			}
			else {
				// multipart file field
				CommonsMultipartFile file = createMultipartFile(fileItem);
				multipartFiles.add(file.getName(), file);
				LogFormatUtils.traceDebug(logger, traceOn ->
						"Part '" + file.getName() + "', size " + file.getSize() +
								" bytes, filename='" + file.getOriginalFilename() + "'" +
								(traceOn ? ", storage=" + file.getStorageDescription() : "")
				);
			}
		}
		return new MultipartParsingResult(multipartFiles, multipartParameters, multipartParameterContentTypes);
	}

	/**
	 * 为给定的 Commons {@link FileItem} 创建一个 {@link CommonsMultipartFile} 包装器。
	 * @param fileItem the Commons FileItem to wrap
	 * @return the corresponding CommonsMultipartFile (potentially a custom subclass)
	 * @since 4.3.5
	 * @see #setPreserveFilename(boolean)
	 * @see CommonsMultipartFile#setPreserveFilename(boolean)
	 */
	protected CommonsMultipartFile createMultipartFile(FileItem fileItem) {
		CommonsMultipartFile multipartFile = new CommonsMultipartFile(fileItem);
		multipartFile.setPreserveFilename(this.preserveFilename);
		return multipartFile;
	}

	/**
	 * 清理 multipart 解析期间创建的 Spring MultipartFile，
	 * 这些文件可能持有磁盘上的临时数据。
	 * <p>删除底层的 Commons FileItem 实例。
	 * @param multipartFiles a Collection of MultipartFile instances
	 * @see org.apache.commons.fileupload.FileItem#delete()
	 */
	protected void cleanupFileItems(MultiValueMap<String, MultipartFile> multipartFiles) {
		for (List<MultipartFile> files : multipartFiles.values()) {
			for (MultipartFile file : files) {
				if (file instanceof CommonsMultipartFile) {
					CommonsMultipartFile cmf = (CommonsMultipartFile) file;
					cmf.getFileItem().delete();
					LogFormatUtils.traceDebug(logger, traceOn ->
							"Cleaning up part '" + cmf.getName() +
									"', filename '" + cmf.getOriginalFilename() + "'" +
									(traceOn ? ", stored " + cmf.getStorageDescription() : ""));
				}
			}
		}
	}

	private String determineEncoding(String contentTypeHeader, String defaultEncoding) {
		if (!StringUtils.hasText(contentTypeHeader)) {
			return defaultEncoding;
		}
		MediaType contentType = MediaType.parseMediaType(contentTypeHeader);
		Charset charset = contentType.getCharset();
		return (charset != null ? charset.name() : defaultEncoding);
	}


	/**
	 * 持有 Spring MultipartFile 的 Map 以及
	 * multipart 参数 Map 的容器。
	 */
	protected static class MultipartParsingResult {

		private final MultiValueMap<String, MultipartFile> multipartFiles;

		private final Map<String, String[]> multipartParameters;

		private final Map<String, String> multipartParameterContentTypes;

		public MultipartParsingResult(MultiValueMap<String, MultipartFile> mpFiles,
				Map<String, String[]> mpParams, Map<String, String> mpParamContentTypes) {

			this.multipartFiles = mpFiles;
			this.multipartParameters = mpParams;
			this.multipartParameterContentTypes = mpParamContentTypes;
		}

		public MultiValueMap<String, MultipartFile> getMultipartFiles() {
			return this.multipartFiles;
		}

		public Map<String, String[]> getMultipartParameters() {
			return this.multipartParameters;
		}

		public Map<String, String> getMultipartParameterContentTypes() {
			return this.multipartParameterContentTypes;
		}
	}

}
