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

package org.springframework.http.codec.multipart;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;

/**
 * 表示 "multipart/form-data" 请求中的一个部件。
 *
 * <p>multipart 请求可能源自浏览器表单，此时每个部件要么是
 * {@link FormFieldPart}，要么是 {@link FilePart}。
 *
 * <p>multipart 请求也可以在浏览器之外用于任意内容类型的数据
 * （例如 JSON、PDF 等）。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see <a href="https://tools.ietf.org/html/rfc7578">RFC 7578 (multipart/form-data)</a>
 * @see <a href="https://tools.ietf.org/html/rfc2183">RFC 2183 (Content-Disposition)</a>
 * @see <a href="https://www.w3.org/TR/html5/forms.html#multipart-form-data">HTML5 (multipart forms)</a>
 */
public interface Part {

	/**
	 * 返回该部件在 multipart 表单中的名称。
	 * @return the name of the part, never {@code null} or empty
	 */
	String name();

	/**
	 * 返回与该部件关联的头。
	 */
	HttpHeaders headers();

	/**
	 * 返回此部件的内容。
	 * <p>请注意，对于 {@link FormFieldPart}，可以通过
	 * {@link FormFieldPart#value()} 更方便地访问内容。
	 */
	Flux<DataBuffer> content();

	/**
	 * 返回一个 Mono，订阅时会删除此部件的底层存储。
	 * @since 5.3.13
	 */
	default Mono<Void> delete() {
		return Mono.empty();
	}

}
