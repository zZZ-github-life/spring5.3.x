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

package org.springframework.http.codec.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.FormHttpMessageWriter;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerSentEventHttpMessageReader;
import org.springframework.http.codec.multipart.MultipartHttpMessageWriter;
import org.springframework.lang.Nullable;

/**
 * {@link ClientCodecConfigurer.ClientDefaultCodecs} 的默认实现。
 *
 * @author Rossen Stoyanchev
 */
class ClientDefaultCodecsImpl extends BaseDefaultCodecs implements ClientCodecConfigurer.ClientDefaultCodecs {

	@Nullable
	private DefaultMultipartCodecs multipartCodecs;

	@Nullable
	private Decoder<?> sseDecoder;

	@Nullable
	private Supplier<List<HttpMessageWriter<?>>> partWritersSupplier;


	ClientDefaultCodecsImpl() {
	}

	ClientDefaultCodecsImpl(ClientDefaultCodecsImpl other) {
		super(other);
		this.multipartCodecs = (other.multipartCodecs != null ?
				new DefaultMultipartCodecs(other.multipartCodecs) : null);
		this.sseDecoder = other.sseDecoder;
	}


	/**
	 * 设置当未显式配置 {@link #multipartCodecs()} 时供部件写入器使用的提供者。
	 * 除 multipart 写入器本身外，这组写入器与通用写入器相同。
	 */
	void setPartWritersSupplier(Supplier<List<HttpMessageWriter<?>>> supplier) {
		this.partWritersSupplier = supplier;
		initTypedWriters();
	}


	@Override
	public ClientCodecConfigurer.MultipartCodecs multipartCodecs() {
		if (this.multipartCodecs == null) {
			this.multipartCodecs = new DefaultMultipartCodecs();
		}
		return this.multipartCodecs;
	}

	@Override
	public void serverSentEventDecoder(Decoder<?> decoder) {
		this.sseDecoder = decoder;
		initObjectReaders();
	}

	@Override
	protected void extendObjectReaders(List<HttpMessageReader<?>> objectReaders) {

		Decoder<?> decoder = (this.sseDecoder != null ? this.sseDecoder :
				jackson2Present ? getJackson2JsonDecoder() :
				kotlinSerializationJsonPresent ? getKotlinSerializationJsonDecoder() :
				null);

		addCodec(objectReaders, new ServerSentEventHttpMessageReader(decoder));
	}

	@Override
	protected void extendTypedWriters(List<HttpMessageWriter<?>> typedWriters) {
		addCodec(typedWriters, new MultipartHttpMessageWriter(getPartWriters(), new FormHttpMessageWriter()));
	}

	private List<HttpMessageWriter<?>> getPartWriters() {
		if (this.multipartCodecs != null) {
			return this.multipartCodecs.getWriters();
		}
		else if (this.partWritersSupplier != null) {
			return this.partWritersSupplier.get();
		}
		else {
			return Collections.emptyList();
		}
	}


	/**
	 * {@link ClientCodecConfigurer.MultipartCodecs} 的默认实现。
	 */
	private class DefaultMultipartCodecs implements ClientCodecConfigurer.MultipartCodecs {

		private final List<HttpMessageWriter<?>> writers = new ArrayList<>();


		DefaultMultipartCodecs() {
		}

		DefaultMultipartCodecs(DefaultMultipartCodecs other) {
			this.writers.addAll(other.writers);
		}


		@Override
		public ClientCodecConfigurer.MultipartCodecs encoder(Encoder<?> encoder) {
			writer(new EncoderHttpMessageWriter<>(encoder));
			initTypedWriters();
			return this;
		}

		@Override
		public ClientCodecConfigurer.MultipartCodecs writer(HttpMessageWriter<?> writer) {
			this.writers.add(writer);
			initTypedWriters();
			return this;
		}

		List<HttpMessageWriter<?>> getWriters() {
			return this.writers;
		}
	}

}
