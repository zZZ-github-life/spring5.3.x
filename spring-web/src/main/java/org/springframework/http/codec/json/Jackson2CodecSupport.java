/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.http.codec.json;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Hints;
import org.springframework.http.HttpLogging;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;
import org.springframework.util.ObjectUtils;

/**
 * 为 Jackson 2.9 编码和解码提供支持方法的基类。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class Jackson2CodecSupport {

	/**
	 * 用于指定编码或解码时所用"JSON 视图"的提示键，
	 * 其值应为 {@link Class}。
	 * @see <a href="https://www.baeldung.com/jackson-json-view-annotation">Jackson JSON Views</a>
	 */
	public static final String JSON_VIEW_HINT = Jackson2CodecSupport.class.getName() + ".jsonView";

	/**
	 * 用于访问传入
	 * {@link org.springframework.http.codec.HttpMessageReader#read(ResolvableType, ResolvableType, ServerHttpRequest, ServerHttpResponse, Map)}
	 * 的实际 ResolvableType 的提示键（仅限服务器端）。当前在方法参数带有泛型时设置，
	 * 因为在响应式类型的情况下，使用 {@code ResolvableType.getGeneric()} 意味着
	 * 没有 MethodParameter 来源，也无法获知所属类。
	 */
	static final String ACTUAL_TYPE_HINT = Jackson2CodecSupport.class.getName() + ".actualType";

	private static final String JSON_VIEW_HINT_ERROR =
			"@JsonView only supported for write hints with exactly 1 class argument: ";

	private static final List<MimeType> DEFAULT_MIME_TYPES = Collections.unmodifiableList(
			Arrays.asList(
					MediaType.APPLICATION_JSON,
					new MediaType("application", "*+json"),
					MediaType.APPLICATION_NDJSON));


	protected final Log logger = HttpLogging.forLogName(getClass());

	private ObjectMapper defaultObjectMapper;

	@Nullable
	private Map<Class<?>, Map<MimeType, ObjectMapper>> objectMapperRegistrations;

	private final List<MimeType> mimeTypes;


	/**
	 * 使用要采用的 Jackson {@link ObjectMapper} 的构造函数。
	 */
	protected Jackson2CodecSupport(ObjectMapper objectMapper, MimeType... mimeTypes) {
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
		this.defaultObjectMapper = objectMapper;
		this.mimeTypes = !ObjectUtils.isEmpty(mimeTypes) ?
				Collections.unmodifiableList(Arrays.asList(mimeTypes)) : DEFAULT_MIME_TYPES;
	}


	/**
	 * 配置要使用的默认 ObjectMapper 实例。
	 * @param objectMapper ObjectMapper 实例
	 * @since 5.3.4
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
		this.defaultObjectMapper = objectMapper;
	}

	/**
	 * 返回{@link #setObjectMapper 已配置}的默认 ObjectMapper。
	 */
	public ObjectMapper getObjectMapper() {
		return this.defaultObjectMapper;
	}

	/**
	 * 为给定的 {@link Class} 配置要使用的 {@link ObjectMapper} 实例。
	 * 当希望偏离{@link #getObjectMapper() 默认}的 ObjectMapper，
	 * 或希望 {@code ObjectMapper} 随 {@code MediaType} 变化时，这很有用。
	 * <p><strong>注意：</strong>使用此方法会实际关闭对给定类的默认
	 * {@link #getObjectMapper() ObjectMapper} 和支持的
	 * {@link #getMimeTypes() MimeTypes} 的使用。因此，此处配置的映射
	 * 必须{@link MediaType#includes(MediaType) 包含}给定类必须支持的
	 * 每一种 MediaType，这一点很重要。
	 * @param clazz 要为其注册 ObjectMapper 实例的对象类型
	 * @param registrar 用于填充或以其他方式更新给定类的
	 * MediaType 到 ObjectMapper 关联的 consumer
	 * @since 5.3.4
	 */
	public void registerObjectMappersForType(Class<?> clazz, Consumer<Map<MimeType, ObjectMapper>> registrar) {
		if (this.objectMapperRegistrations == null) {
			this.objectMapperRegistrations = new LinkedHashMap<>();
		}
		Map<MimeType, ObjectMapper> registrations =
				this.objectMapperRegistrations.computeIfAbsent(clazz, c -> new LinkedHashMap<>());
		registrar.accept(registrations);
	}

	/**
	 * 返回给定类的 ObjectMapper 注册信息（如果有）。
	 * @param clazz 要查找其注册信息的类
	 * @return 已注册的 MediaType 到 ObjectMapper 注册映射，
	 * 如果给定类没有任何注册则为空。
	 * @since 5.3.4
	 */
	@Nullable
	public Map<MimeType, ObjectMapper> getObjectMappersForType(Class<?> clazz) {
		for (Map.Entry<Class<?>, Map<MimeType, ObjectMapper>> entry : getObjectMapperRegistrations().entrySet()) {
			if (entry.getKey().isAssignableFrom(clazz)) {
				return entry.getValue();
			}
		}
		return Collections.emptyMap();
	}

	protected Map<Class<?>, Map<MimeType, ObjectMapper>> getObjectMapperRegistrations() {
		return (this.objectMapperRegistrations != null ? this.objectMapperRegistrations : Collections.emptyMap());
	}

	/**
	 * 子类应将其公开为"可解码"或"可编码"的 MIME 类型。
	 */
	protected List<MimeType> getMimeTypes() {
		return this.mimeTypes;
	}

	protected List<MimeType> getMimeTypes(ResolvableType elementType) {
		Class<?> elementClass = elementType.toClass();
		List<MimeType> result = null;
		for (Map.Entry<Class<?>, Map<MimeType, ObjectMapper>> entry : getObjectMapperRegistrations().entrySet()) {
			if (entry.getKey().isAssignableFrom(elementClass)) {
				result = (result != null ? result : new ArrayList<>(entry.getValue().size()));
				result.addAll(entry.getValue().keySet());
			}
		}
		return (CollectionUtils.isEmpty(result) ? getMimeTypes() : result);
	}

	protected boolean supportsMimeType(@Nullable MimeType mimeType) {
		if (mimeType == null) {
			return true;
		}
		for (MimeType supportedMimeType : this.mimeTypes) {
			if (supportedMimeType.isCompatibleWith(mimeType)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 确定是否记录来自
	 * {@link ObjectMapper#canDeserialize} / {@link ObjectMapper#canSerialize} 检查的给定异常。
	 * @param type Jackson 检查其（反）序列化能力的类
	 * @param cause 要评估的 Jackson 抛出的异常（通常是 {@link JsonMappingException}）
	 * @since 5.3.1
	 */
	protected void logWarningIfNecessary(Type type, @Nullable Throwable cause) {
		if (cause == null) {
			return;
		}
		if (logger.isDebugEnabled()) {
			String msg = "Failed to evaluate Jackson " + (type instanceof JavaType ? "de" : "") +
					"serialization for type [" + type + "]";
			logger.debug(msg, cause);
		}
	}

	protected JavaType getJavaType(Type type, @Nullable Class<?> contextClass) {
		return this.defaultObjectMapper.constructType(GenericTypeResolver.resolveType(type, contextClass));
	}

	protected Map<String, Object> getHints(ResolvableType resolvableType) {
		MethodParameter param = getParameter(resolvableType);
		if (param != null) {
			Map<String, Object> hints = null;
			if (resolvableType.hasGenerics()) {
				hints = new HashMap<>(2);
				hints.put(ACTUAL_TYPE_HINT, resolvableType);
			}
			JsonView annotation = getAnnotation(param, JsonView.class);
			if (annotation != null) {
				Class<?>[] classes = annotation.value();
				Assert.isTrue(classes.length == 1, () -> JSON_VIEW_HINT_ERROR + param);
				hints = (hints != null ? hints : new HashMap<>(1));
				hints.put(JSON_VIEW_HINT, classes[0]);
			}
			if (hints != null) {
				return hints;
			}
		}
		return Hints.none();
	}

	@Nullable
	protected MethodParameter getParameter(ResolvableType type) {
		return (type.getSource() instanceof MethodParameter ? (MethodParameter) type.getSource() : null);
	}

	@Nullable
	protected abstract <A extends Annotation> A getAnnotation(MethodParameter parameter, Class<A> annotType);

	/**
	 * 选择要使用的 ObjectMapper，可以是主 ObjectMapper，或者在通过
	 * {@link #registerObjectMappersForType(Class, Consumer)} 自定义了给定类的
	 * 处理方式时使用另一个 ObjectMapper。
	 * @since 5.3.4
	 */
	@Nullable
	protected ObjectMapper selectObjectMapper(ResolvableType targetType, @Nullable MimeType targetMimeType) {
		if (targetMimeType == null || CollectionUtils.isEmpty(this.objectMapperRegistrations)) {
			return this.defaultObjectMapper;
		}
		Class<?> targetClass = targetType.toClass();
		for (Map.Entry<Class<?>, Map<MimeType, ObjectMapper>> typeEntry : getObjectMapperRegistrations().entrySet()) {
			if (typeEntry.getKey().isAssignableFrom(targetClass)) {
				for (Map.Entry<MimeType, ObjectMapper> objectMapperEntry : typeEntry.getValue().entrySet()) {
					if (objectMapperEntry.getKey().includes(targetMimeType)) {
						return objectMapperEntry.getValue();
					}
				}
				// No matching registrations
				return null;
			}
		}
		// No registrations
		return this.defaultObjectMapper;
	}

}
