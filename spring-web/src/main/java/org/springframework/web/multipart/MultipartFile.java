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

package org.springframework.web.multipart;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.FileCopyUtils;

/**
 * 表示在 multipart 请求中接收到的上传文件。
 *
 * <p>文件内容可以存储在内存中，也可以临时存储在磁盘上。
 * 无论哪种情况，用户都应根据需要负责将文件内容复制到会话级或持久化存储中。
 * 临时存储将在请求处理结束时被清除。
 *
 * @author Juergen Hoeller
 * @author Trevor D. Cook
 * @since 29.09.2003
 * @see org.springframework.web.multipart.MultipartHttpServletRequest
 * @see org.springframework.web.multipart.MultipartResolver
 */
public interface MultipartFile extends InputStreamSource {

	/**
	 * 返回 multipart 表单中该参数的名称。
	 * @return 参数名称（永不为 {@code null} 或空）
	 */
	String getName();

	/**
	 * 返回客户端文件系统中的原始文件名。
	 * <p>根据所使用的浏览器不同，该名称可能包含路径信息，
	 * 但除 Opera 之外，通常不会包含。
	 * <p><strong>注意：</strong>请记住，此文件名由客户端提供，不应盲目使用。
	 * 除了不应使用其中的目录部分之外，文件名还可能包含 ".." 等
	 * 可被恶意利用的字符。建议不要直接使用此文件名。
	 * 最好生成一个唯一的名称，并在必要时将此名称保存到某处以供参考。
	 * @return 原始文件名；如果在 multipart 表单中未选择文件则为空字符串；
	 * 如果未定义或不可用则为 {@code null}
	 * @see org.apache.commons.fileupload.FileItem#getName()
	 * @see org.springframework.web.multipart.commons.CommonsMultipartFile#setPreserveFilename
	 * @see <a href="https://tools.ietf.org/html/rfc7578#section-4.2">RFC 7578, Section 4.2</a>
	 * @see <a href="https://owasp.org/www-community/vulnerabilities/Unrestricted_File_Upload">Unrestricted File Upload</a>
	 */
	@Nullable
	String getOriginalFilename();

	/**
	 * 返回文件的内容类型。
	 * @return 内容类型；如果未定义（或在 multipart 表单中未选择文件）
	 * 则返回 {@code null}
	 */
	@Nullable
	String getContentType();

	/**
	 * 返回上传文件是否为空，即在 multipart 表单中未选择任何文件，
	 * 或者所选文件没有任何内容。
	 */
	boolean isEmpty();

	/**
	 * 返回文件大小（以字节为单位）。
	 * @return 文件大小；如果为空则为 0
	 */
	long getSize();

	/**
	 * 以字节数组形式返回文件内容。
	 * @return 以字节数组表示的文件内容；如果为空则为空字节数组
	 * @throws IOException 访问出错时抛出（如果临时存储失败）
	 */
	byte[] getBytes() throws IOException;

	/**
	 * 返回用于读取文件内容的 InputStream。
	 * <p>用户负责关闭返回的流。
	 * @return 以流形式表示的文件内容；如果为空则为空流
	 * @throws IOException 访问出错时抛出（如果临时存储失败）
	 */
	@Override
	InputStream getInputStream() throws IOException;

	/**
	 * 返回此 MultipartFile 的 Resource 表示形式。可将其用作
	 * {@code RestTemplate} 或 {@code WebClient} 的输入，
	 * 以便连同 InputStream 一起暴露内容长度和文件名。
	 * @return 适配到 Resource 契约的当前 MultipartFile
	 * @since 5.1
	 */
	default Resource getResource() {
		return new MultipartFileResource(this);
	}

	/**
	 * 将接收到的文件传输到给定的目标文件。
	 * <p>这可能是在文件系统中移动文件、在文件系统中复制文件，
	 * 或将内存中保存的内容保存到目标文件。如果目标文件已存在，
	 * 将先被删除。
	 * <p>如果目标文件已在文件系统中被移动，之后将无法再次调用此操作。
	 * 因此，为了兼容各种存储机制，请仅调用此方法一次。
	 * <p><b>注意：</b>取决于底层提供者，临时存储可能依赖于容器，
	 * 包括此处指定的相对目标路径的基础目录（例如在 Servlet 3.0 multipart 处理中）。
	 * 对于绝对路径目标，目标文件可能会从临时位置重命名/移动，
	 * 或者即使临时副本已存在也会重新复制。
	 * @param dest 目标文件（通常为绝对路径）
	 * @throws IOException 读取或写入出错时抛出
	 * @throws IllegalStateException 如果文件已在文件系统中被移动，
	 * 且不再可用于再次传输
	 * @see org.apache.commons.fileupload.FileItem#write(File)
	 * @see javax.servlet.http.Part#write(String)
	 */
	void transferTo(File dest) throws IOException, IllegalStateException;

	/**
	 * 将接收到的文件传输到给定的目标文件。
	 * <p>默认实现只是复制文件输入流。
	 * @since 5.1
	 * @see #getInputStream()
	 * @see #transferTo(File)
 	 */
	default void transferTo(Path dest) throws IOException, IllegalStateException {
		FileCopyUtils.copy(getInputStream(), Files.newOutputStream(dest));
	}

}
