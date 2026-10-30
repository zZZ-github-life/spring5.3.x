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

package org.springframework.http.codec.multipart;

import java.io.File;
import java.nio.file.Path;

import reactor.core.publisher.Mono;

/**
 * {@link Part} 的特化，表示在 multipart 请求中接收到的上传文件。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 5.0
 */
public interface FilePart extends Part {

	/**
	 * 返回客户端文件系统中的原始文件名。
	 * <p><strong>注意：</strong>请记住，此文件名由客户端提供，
	 * 不应盲目使用。除了不应使用其中的目录部分之外，
	 * 文件名还可能包含 ".." 等可被恶意利用的字符。
	 * 建议不要直接使用此文件名。最好生成一个唯一的名称，
	 * 并在必要时将此名称保存到某处以供参考。
	 * @return 原始文件名；如果在 multipart 表单中未选择文件则为空字符串，
	 * 如果未定义或不可用则为 {@code null}
	 * @see <a href="https://tools.ietf.org/html/rfc7578#section-4.2">RFC 7578, Section 4.2</a>
	 * @see <a href="https://owasp.org/www-community/vulnerabilities/Unrestricted_File_Upload">Unrestricted File Upload</a>
	 */
	String filename();

	/**
	 * 便捷方法，用于将此部件中文件的内容复制到给定的目标文件。
	 * 如果目标文件已存在，将先被截断。
	 * <p>默认实现委托给 {@link #transferTo(Path)}。
	 * @param dest 目标文件
	 * @return 包含文件传输结果的完成 {@code Mono}，
	 * 如果该部分不是文件则可能为 {@link IllegalStateException}
	 * @see #transferTo(Path)
	 */
	default Mono<Void> transferTo(File dest) {
		return transferTo(dest.toPath());
	}

	/**
	 * 便捷方法，用于将此部件中文件的内容复制到给定的目标文件。
	 * 如果目标文件已存在，将先被截断。
	 * @param dest 目标文件
	 * @return 包含文件传输结果的完成 {@code Mono}，
	 * 如果该部分不是文件则可能为 {@link IllegalStateException}
	 * @since 5.1
	 * @see #transferTo(File)
	 */
	Mono<Void> transferTo(Path dest);

}
