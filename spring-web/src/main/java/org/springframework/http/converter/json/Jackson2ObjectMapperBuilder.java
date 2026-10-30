/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.http.converter.json;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.KotlinDetector;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.StaxUtils;

/**
 * 用于通过流式 API 创建 {@link ObjectMapper} 实例的构建器。
 *
 * <p>它使用以下属性定制 Jackson 的默认属性：
 * <ul>
 * <li>{@link MapperFeature#DEFAULT_VIEW_INCLUSION} 被禁用</li>
 * <li>{@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} 被禁用</li>
 * </ul>
 *
 * <p>如果类路径上检测到以下知名模块，它还会自动注册这些模块：
 * <ul>
 * <li><a href="https://github.com/FasterXML/jackson-datatype-jdk8">jackson-datatype-jdk8</a>：
 * 支持其他 Java 8 类型，如 {@link java.util.Optional}</li>
 * <li><a href="https://github.com/FasterXML/jackson-datatype-jsr310">jackson-datatype-jsr310</a>：
 * 支持 Java 8 Date &amp; Time API 类型</li>
 * <li><a href="https://github.com/FasterXML/jackson-datatype-joda">jackson-datatype-joda</a>：
 * 支持 Joda-Time 类型</li>
 * <li><a href="https://github.com/FasterXML/jackson-module-kotlin">jackson-module-kotlin</a>：
 * 支持 Kotlin 类和数据类</li>
 * </ul>
 *
 * <p>从 Spring 5.3 起，兼容 Jackson 2.9 至 2.12。
 *
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @author Tadaya Tsuyukubo
 * @author Eddú Meléndez
 * @since 4.1.1
 * @see #build()
 * @see #configure(ObjectMapper)
 * @see Jackson2ObjectMapperFactoryBean
 */
public class Jackson2ObjectMapperBuilder {

	private final Map<Class<?>, Class<?>> mixIns = new LinkedHashMap<>();

	private final Map<Class<?>, JsonSerializer<?>> serializers = new LinkedHashMap<>();

	private final Map<Class<?>, JsonDeserializer<?>> deserializers = new LinkedHashMap<>();

	private final Map<PropertyAccessor, JsonAutoDetect.Visibility> visibilities = new LinkedHashMap<>();

	private final Map<Object, Boolean> features = new LinkedHashMap<>();

	private boolean createXmlMapper = false;

	@Nullable
	private JsonFactory factory;

	@Nullable
	private DateFormat dateFormat;

	@Nullable
	private Locale locale;

	@Nullable
	private TimeZone timeZone;

	@Nullable
	private AnnotationIntrospector annotationIntrospector;

	@Nullable
	private PropertyNamingStrategy propertyNamingStrategy;

	@Nullable
	private TypeResolverBuilder<?> defaultTyping;

	@Nullable
	private JsonInclude.Value serializationInclusion;

	@Nullable
	private FilterProvider filters;

	@Nullable
	private List<Module> modules;

	@Nullable
	private Class<? extends Module>[] moduleClasses;

	private boolean findModulesViaServiceLoader = false;

	private boolean findWellKnownModules = true;

	private ClassLoader moduleClassLoader = getClass().getClassLoader();

	@Nullable
	private HandlerInstantiator handlerInstantiator;

	@Nullable
	private ApplicationContext applicationContext;

	@Nullable
	private Boolean defaultUseWrapper;

	@Nullable
	private Consumer<ObjectMapper> configurer;


	/**
	 * 如果设置为 {@code true}，将使用默认构造函数创建 {@link XmlMapper}。
	 * 这仅适用于 {@link #build()} 调用，
	 * 不适用于 {@link #configure} 调用。
	 */
	public Jackson2ObjectMapperBuilder createXmlMapper(boolean createXmlMapper) {
		this.createXmlMapper = createXmlMapper;
		return this;
	}

	/**
	 * 定义用于创建 {@link ObjectMapper} 实例的 {@link JsonFactory}。
	 * @since 5.0
	 */
	public Jackson2ObjectMapperBuilder factory(JsonFactory factory) {
		this.factory = factory;
		return this;
	}

	/**
	 * 使用给定的 {@link DateFormat} 定义日期/时间格式。
	 * <p>注意：根据 Jackson 的线程安全规则，设置此属性会使所公开的
	 * {@link ObjectMapper} 不再是线程安全的。
	 * @see #simpleDateFormat(String)
	 */
	public Jackson2ObjectMapperBuilder dateFormat(DateFormat dateFormat) {
		this.dateFormat = dateFormat;
		return this;
	}

	/**
	 * 使用 {@link SimpleDateFormat} 定义日期/时间格式。
	 * <p>注意：根据 Jackson 的线程安全规则，设置此属性会使所公开的
	 * {@link ObjectMapper} 不再是线程安全的。
	 * @see #dateFormat(DateFormat)
	 */
	public Jackson2ObjectMapperBuilder simpleDateFormat(String format) {
		this.dateFormat = new SimpleDateFormat(format);
		return this;
	}

	/**
	 * 覆盖用于格式化的默认 {@link Locale}。
	 * 默认使用 {@link Locale#getDefault()}。
	 * @since 4.1.5
	 */
	public Jackson2ObjectMapperBuilder locale(Locale locale) {
		this.locale = locale;
		return this;
	}

	/**
	 * 覆盖用于格式化的默认 {@link Locale}。
	 * 默认使用 {@link Locale#getDefault()}。
	 * @param localeString 以 String 表示的 Locale ID
	 * @since 4.1.5
	 */
	public Jackson2ObjectMapperBuilder locale(String localeString) {
		this.locale = StringUtils.parseLocale(localeString);
		return this;
	}

	/**
	 * 覆盖用于格式化的默认 {@link TimeZone}。
	 * 默认使用 UTC（而非本地时区）。
	 * @since 4.1.5
	 */
	public Jackson2ObjectMapperBuilder timeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
		return this;
	}

	/**
	 * 覆盖用于格式化的默认 {@link TimeZone}。
	 * 默认使用 UTC（而非本地时区）。
	 * @param timeZoneString 以 String 表示的时区 ID
	 * @since 4.1.5
	 */
	public Jackson2ObjectMapperBuilder timeZone(String timeZoneString) {
		this.timeZone = StringUtils.parseTimeZoneString(timeZoneString);
		return this;
	}

	/**
	 * 为序列化和反序列化设置 {@link AnnotationIntrospector}。
	 */
	public Jackson2ObjectMapperBuilder annotationIntrospector(AnnotationIntrospector annotationIntrospector) {
		this.annotationIntrospector = annotationIntrospector;
		return this;
	}

	/**
	 * {@link #annotationIntrospector(AnnotationIntrospector)} 的替代方法，
	 * 允许与当前设置的注解内省器合并而非替换，例如通过
	 * {@link AnnotationIntrospectorPair#pair(AnnotationIntrospector, AnnotationIntrospector)}。
	 * @param pairingFunction 要应用于当前所设置的内省器的函数（可能为 {@code null}）；
	 * 该函数的结果将成为新的内省器。
	 * @since 5.2.4
	 */
	public Jackson2ObjectMapperBuilder annotationIntrospector(
			Function<AnnotationIntrospector, AnnotationIntrospector> pairingFunction) {

		this.annotationIntrospector = pairingFunction.apply(this.annotationIntrospector);
		return this;
	}

	/**
	 * 指定用于配置 {@link ObjectMapper} 的
	 * {@link com.fasterxml.jackson.databind.PropertyNamingStrategy}。
	 */
	public Jackson2ObjectMapperBuilder propertyNamingStrategy(PropertyNamingStrategy propertyNamingStrategy) {
		this.propertyNamingStrategy = propertyNamingStrategy;
		return this;
	}

	/**
	 * 指定用于 Jackson 默认类型处理的 {@link TypeResolverBuilder}。
	 * @since 4.2.2
	 */
	public Jackson2ObjectMapperBuilder defaultTyping(TypeResolverBuilder<?> typeResolverBuilder) {
		this.defaultTyping = typeResolverBuilder;
		return this;
	}

	/**
	 * 设置用于序列化的自定义包含策略。
	 * @see com.fasterxml.jackson.annotation.JsonInclude.Include
	 */
	public Jackson2ObjectMapperBuilder serializationInclusion(JsonInclude.Include inclusion) {
		return serializationInclusion(JsonInclude.Value.construct(inclusion, inclusion));
	}

	/**
	 * 设置用于序列化的自定义包含策略。
	 * @since 5.3
	 * @see com.fasterxml.jackson.annotation.JsonInclude.Value
	 */
	public Jackson2ObjectMapperBuilder serializationInclusion(JsonInclude.Value serializationInclusion) {
		this.serializationInclusion = serializationInclusion;
		return this;
	}

	/**
	 * 设置要使用的全局过滤器，以支持标注 {@link JsonFilter @JsonFilter} 的 POJO。
	 * @since 4.2
	 * @see MappingJacksonValue#setFilters(FilterProvider)
	 */
	public Jackson2ObjectMapperBuilder filters(FilterProvider filters) {
		this.filters = filters;
		return this;
	}

	/**
	 * 添加用于增强指定类或接口的混入注解。
	 * @param target 其注解将被有效覆盖的类（或接口）
	 * @param mixinSource 其注解将作为值“添加”到目标类注解上的类（或接口）
	 * @since 4.1.2
	 * @see com.fasterxml.jackson.databind.ObjectMapper#addMixIn(Class, Class)
	 */
	public Jackson2ObjectMapperBuilder mixIn(Class<?> target, Class<?> mixinSource) {
		this.mixIns.put(target, mixinSource);
		return this;
	}

	/**
	 * 添加用于增强指定类或接口的混入注解。
	 * @param mixIns 一个 Map，键为其注解将被有效覆盖的目标类（或接口），
	 * 值为其注解将被“添加”到目标类注解上的混入类（或接口）。
	 * @since 4.1.2
	 * @see com.fasterxml.jackson.databind.ObjectMapper#addMixIn(Class, Class)
	 */
	public Jackson2ObjectMapperBuilder mixIns(Map<Class<?>, Class<?>> mixIns) {
		this.mixIns.putAll(mixIns);
		return this;
	}

	/**
	 * 配置自定义序列化器。每个序列化器注册到
	 * {@link JsonSerializer#handledType()} 返回的类型，该类型不能为 {@code null}。
	 * @see #serializersByType(Map)
	 */
	public Jackson2ObjectMapperBuilder serializers(JsonSerializer<?>... serializers) {
		for (JsonSerializer<?> serializer : serializers) {
			Class<?> handledType = serializer.handledType();
			if (handledType == null || handledType == Object.class) {
				throw new IllegalArgumentException("Unknown handled type in " + serializer.getClass().getName());
			}
			this.serializers.put(serializer.handledType(), serializer);
		}
		return this;
	}

	/**
	 * 为给定类型配置自定义序列化器。
	 * @since 4.1.2
	 * @see #serializers(JsonSerializer...)
	 */
	public Jackson2ObjectMapperBuilder serializerByType(Class<?> type, JsonSerializer<?> serializer) {
		this.serializers.put(type, serializer);
		return this;
	}

	/**
	 * 为给定类型配置自定义序列化器。
	 * @see #serializers(JsonSerializer...)
	 */
	public Jackson2ObjectMapperBuilder serializersByType(Map<Class<?>, JsonSerializer<?>> serializers) {
		this.serializers.putAll(serializers);
		return this;
	}

	/**
	 * 配置自定义反序列化器。每个反序列化器注册到
	 * {@link JsonDeserializer#handledType()} 返回的类型，该类型不能为 {@code null}。
	 * @since 4.3
	 * @see #deserializersByType(Map)
	 */
	public Jackson2ObjectMapperBuilder deserializers(JsonDeserializer<?>... deserializers) {
		for (JsonDeserializer<?> deserializer : deserializers) {
			Class<?> handledType = deserializer.handledType();
			if (handledType == null || handledType == Object.class) {
				throw new IllegalArgumentException("Unknown handled type in " + deserializer.getClass().getName());
			}
			this.deserializers.put(deserializer.handledType(), deserializer);
		}
		return this;
	}

	/**
	 * 为给定类型配置自定义反序列化器。
	 * @since 4.1.2
	 */
	public Jackson2ObjectMapperBuilder deserializerByType(Class<?> type, JsonDeserializer<?> deserializer) {
		this.deserializers.put(type, deserializer);
		return this;
	}

	/**
	 * 为给定类型配置自定义反序列化器。
	 */
	public Jackson2ObjectMapperBuilder deserializersByType(Map<Class<?>, JsonDeserializer<?>> deserializers) {
		this.deserializers.putAll(deserializers);
		return this;
	}

	/**
	 * {@link MapperFeature#AUTO_DETECT_FIELDS} 选项的快捷方式。
	 */
	public Jackson2ObjectMapperBuilder autoDetectFields(boolean autoDetectFields) {
		this.features.put(MapperFeature.AUTO_DETECT_FIELDS, autoDetectFields);
		return this;
	}

	/**
	 * {@link MapperFeature#AUTO_DETECT_SETTERS}/
	 * {@link MapperFeature#AUTO_DETECT_GETTERS}/{@link MapperFeature#AUTO_DETECT_IS_GETTERS}
	 * 选项的快捷方式。
	 */
	public Jackson2ObjectMapperBuilder autoDetectGettersSetters(boolean autoDetectGettersSetters) {
		this.features.put(MapperFeature.AUTO_DETECT_GETTERS, autoDetectGettersSetters);
		this.features.put(MapperFeature.AUTO_DETECT_SETTERS, autoDetectGettersSetters);
		this.features.put(MapperFeature.AUTO_DETECT_IS_GETTERS, autoDetectGettersSetters);
		return this;
	}

	/**
	 * {@link MapperFeature#DEFAULT_VIEW_INCLUSION} 选项的快捷方式。
	 */
	public Jackson2ObjectMapperBuilder defaultViewInclusion(boolean defaultViewInclusion) {
		this.features.put(MapperFeature.DEFAULT_VIEW_INCLUSION, defaultViewInclusion);
		return this;
	}

	/**
	 * {@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} 选项的快捷方式。
	 */
	public Jackson2ObjectMapperBuilder failOnUnknownProperties(boolean failOnUnknownProperties) {
		this.features.put(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, failOnUnknownProperties);
		return this;
	}

	/**
	 * {@link SerializationFeature#FAIL_ON_EMPTY_BEANS} 选项的快捷方式。
	 */
	public Jackson2ObjectMapperBuilder failOnEmptyBeans(boolean failOnEmptyBeans) {
		this.features.put(SerializationFeature.FAIL_ON_EMPTY_BEANS, failOnEmptyBeans);
		return this;
	}

	/**
	 * {@link SerializationFeature#INDENT_OUTPUT} 选项的快捷方式。
	 */
	public Jackson2ObjectMapperBuilder indentOutput(boolean indentOutput) {
		this.features.put(SerializationFeature.INDENT_OUTPUT, indentOutput);
		return this;
	}

	/**
	 * 定义默认情况下是否为索引属性（List、数组）使用包装器
	 * （仅适用于 {@link XmlMapper}）。
	 * @since 4.3
	 */
	public Jackson2ObjectMapperBuilder defaultUseWrapper(boolean defaultUseWrapper) {
		this.defaultUseWrapper = defaultUseWrapper;
		return this;
	}

	/**
	 * 指定可见性以限制自动检测的属性种类。
	 * @since 5.1
	 * @see com.fasterxml.jackson.annotation.PropertyAccessor
	 * @see com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
	 */
	public Jackson2ObjectMapperBuilder visibility(PropertyAccessor accessor, JsonAutoDetect.Visibility visibility) {
		this.visibilities.put(accessor, visibility);
		return this;
	}

	/**
	 * 指定要启用的特性。
	 * @see com.fasterxml.jackson.core.JsonParser.Feature
	 * @see com.fasterxml.jackson.core.JsonGenerator.Feature
	 * @see com.fasterxml.jackson.databind.SerializationFeature
	 * @see com.fasterxml.jackson.databind.DeserializationFeature
	 * @see com.fasterxml.jackson.databind.MapperFeature
	 */
	public Jackson2ObjectMapperBuilder featuresToEnable(Object... featuresToEnable) {
		for (Object feature : featuresToEnable) {
			this.features.put(feature, Boolean.TRUE);
		}
		return this;
	}

	/**
	 * 指定要禁用的特性。
	 * @see com.fasterxml.jackson.core.JsonParser.Feature
	 * @see com.fasterxml.jackson.core.JsonGenerator.Feature
	 * @see com.fasterxml.jackson.databind.SerializationFeature
	 * @see com.fasterxml.jackson.databind.DeserializationFeature
	 * @see com.fasterxml.jackson.databind.MapperFeature
	 */
	public Jackson2ObjectMapperBuilder featuresToDisable(Object... featuresToDisable) {
		for (Object feature : featuresToDisable) {
			this.features.put(feature, Boolean.FALSE);
		}
		return this;
	}

	/**
	 * 指定要向 {@link ObjectMapper} 注册的模块。
	 * <p>多次调用不会累加，最后一次调用定义要注册的模块。
	 * <p>注意：如果设置了此项，将不会进行任何模块查找——既不由
	 * Jackson 进行，也不由 Spring 进行（参见 {@link #findModulesViaServiceLoader}）。
	 * 因此，在此处指定空列表将抑制任何形式的模块检测。
	 * <p>请指定此项或 {@link #modulesToInstall} 之一，不要同时指定两者。
	 * @since 4.1.5
	 * @see #modules(List)
	 * @see com.fasterxml.jackson.databind.Module
	 */
	public Jackson2ObjectMapperBuilder modules(Module... modules) {
		return modules(Arrays.asList(modules));
	}

	/**
	 * {@link #modules(Module...)} 的变体，使用 {@link List}。
	 * @see #modules(Module...)
	 * @see #modules(Consumer)
	 * @see com.fasterxml.jackson.databind.Module
	 */
	public Jackson2ObjectMapperBuilder modules(List<Module> modules) {
		this.modules = new ArrayList<>(modules);
		this.findModulesViaServiceLoader = false;
		this.findWellKnownModules = false;
		return this;
	}

	/**
	 * {@link #modules(Module...)} 的变体，使用 {@link Consumer}
	 * 以完全控制底层的模块列表。
	 * @since 5.3.22
	 * @see #modules(Module...)
	 * @see #modules(List)
	 * @see com.fasterxml.jackson.databind.Module
	 */
	public Jackson2ObjectMapperBuilder modules(Consumer<List<Module>> consumer) {
		this.modules = (this.modules != null ? this.modules : new ArrayList<>());
		this.findModulesViaServiceLoader = false;
		this.findWellKnownModules = false;
		consumer.accept(this.modules);
		return this;
	}

	/**
	 * 指定要向 {@link ObjectMapper} 注册的一个或多个模块。
	 * <p>多次调用不会累加，最后一次调用定义要注册的模块。
	 * <p>此处指定的模块将在 Spring 对 JSR-310 和 Joda-Time 的自动检测之后，
	 * 或在 Jackson 查找模块之后（参见 {@link #findModulesViaServiceLoader}）注册，
	 * 从而可以最终覆盖它们的配置。
	 * <p>请指定此项或 {@link #modules(Module...)} 之一，不要同时指定两者。
	 * @since 4.1.5
	 * @see #modulesToInstall(Consumer)
	 * @see #modulesToInstall(Class...)
	 * @see com.fasterxml.jackson.databind.Module
	 */
	public Jackson2ObjectMapperBuilder modulesToInstall(Module... modules) {
		this.modules = Arrays.asList(modules);
		this.findWellKnownModules = true;
		return this;
	}

	/**
	 * {@link #modulesToInstall(Module...)} 的变体，使用 {@link Consumer}
	 * 以完全控制底层的模块列表。
	 * @since 5.3.22
	 * @see #modulesToInstall(Module...)
	 * @see #modulesToInstall(Class...)
	 * @see com.fasterxml.jackson.databind.Module
	 */
	public Jackson2ObjectMapperBuilder modulesToInstall(Consumer<List<Module>> consumer) {
		this.modules = (this.modules != null ? this.modules : new ArrayList<>());
		this.findWellKnownModules = true;
		consumer.accept(this.modules);
		return this;
	}

	/**
	 * 按类指定要向 {@link ObjectMapper} 注册的一个或多个模块。
	 * <p>多次调用不会累加，最后一次调用定义要注册的模块。
	 * <p>此处指定的模块将在 Spring 对 JSR-310 和 Joda-Time 的自动检测之后，
	 * 或在 Jackson 查找模块之后（参见 {@link #findModulesViaServiceLoader}）注册，
	 * 从而可以最终覆盖它们的配置。
	 * <p>请指定此项或 {@link #modules(Module...)} 之一，不要同时指定两者。
	 * @see #modulesToInstall(Module...)
	 * @see #modulesToInstall(Consumer)
	 * @see com.fasterxml.jackson.databind.Module
	 */
	@SafeVarargs
	@SuppressWarnings("varargs")
	public final Jackson2ObjectMapperBuilder modulesToInstall(Class<? extends Module>... modules) {
		this.moduleClasses = modules;
		this.findWellKnownModules = true;
		return this;
	}

	/**
	 * 设置是否让 Jackson 基于类路径中的 META-INF 元数据，
	 * 通过 JDK ServiceLoader 查找可用的模块。
	 * <p>如果未设置此模式，Spring 的 Jackson2ObjectMapperBuilder 本身
	 * 将尝试在类路径上查找 JSR-310 和 Joda-Time 支持模块——
	 * 前提是分别提供了 Java 8 和 Joda-Time 本身。
	 * @see com.fasterxml.jackson.databind.ObjectMapper#findModules()
	 */
	public Jackson2ObjectMapperBuilder findModulesViaServiceLoader(boolean findModules) {
		this.findModulesViaServiceLoader = findModules;
		return this;
	}

	/**
	 * 设置用于加载 Jackson 扩展模块的 ClassLoader。
	 */
	public Jackson2ObjectMapperBuilder moduleClassLoader(ClassLoader moduleClassLoader) {
		this.moduleClassLoader = moduleClassLoader;
		return this;
	}

	/**
	 * 自定义 Jackson 处理器（{@link JsonSerializer}、{@link JsonDeserializer}、
	 * {@link KeyDeserializer}、{@code TypeResolverBuilder} 和 {@code TypeIdResolver}）的构建。
	 * @since 4.1.3
	 * @see Jackson2ObjectMapperBuilder#applicationContext(ApplicationContext)
	 */
	public Jackson2ObjectMapperBuilder handlerInstantiator(HandlerInstantiator handlerInstantiator) {
		this.handlerInstantiator = handlerInstantiator;
		return this;
	}

	/**
	 * 设置 Spring {@link ApplicationContext}，以便自动装配 Jackson 处理器
	 * （{@link JsonSerializer}、{@link JsonDeserializer}、{@link KeyDeserializer}、
	 * {@code TypeResolverBuilder} 和 {@code TypeIdResolver}）。
	 * @since 4.1.3
	 * @see SpringHandlerInstantiator
	 */
	public Jackson2ObjectMapperBuilder applicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
		return this;
	}

	/**
	 * 一个选项，用于在应用构建器的所有其他配置属性之后，
	 * 直接对 {@code ObjectMapper} 实例应用额外的定制。
	 * @param configurer 要应用的配置器。如果注册了多个配置器，
	 * 它们将按注册顺序依次应用。
	 * @since 5.3
	 */
	public Jackson2ObjectMapperBuilder postConfigurer(Consumer<ObjectMapper> configurer) {
		this.configurer = (this.configurer != null ? this.configurer.andThen(configurer) : configurer);
		return this;
	}


	/**
	 * 构建一个新的 {@link ObjectMapper} 实例。
	 * <p>每次构建操作都会生成一个独立的 {@link ObjectMapper} 实例。
	 * 构建器的设置可能会被修改，随后的构建操作将基于最新的设置
	 * 生成新的 {@link ObjectMapper}。
	 * @return 新构建的 ObjectMapper
	 */
	@SuppressWarnings("unchecked")
	public <T extends ObjectMapper> T build() {
		ObjectMapper mapper;
		if (this.createXmlMapper) {
			mapper = (this.defaultUseWrapper != null ?
					new XmlObjectMapperInitializer().create(this.defaultUseWrapper, this.factory) :
					new XmlObjectMapperInitializer().create(this.factory));
		}
		else {
			mapper = (this.factory != null ? new ObjectMapper(this.factory) : new ObjectMapper());
		}
		configure(mapper);
		return (T) mapper;
	}

	/**
	 * 使用此构建器的设置配置现有的 {@link ObjectMapper} 实例。
	 * 这可以应用于任意数量的 {@code ObjectMappers}。
	 * @param objectMapper 要配置的 ObjectMapper
	 */
	public void configure(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "ObjectMapper must not be null");

		MultiValueMap<Object, Module> modulesToRegister = new LinkedMultiValueMap<>();
		if (this.findModulesViaServiceLoader) {
			ObjectMapper.findModules(this.moduleClassLoader).forEach(module -> registerModule(module, modulesToRegister));
		}
		else if (this.findWellKnownModules) {
			registerWellKnownModulesIfAvailable(modulesToRegister);
		}

		if (this.modules != null) {
			this.modules.forEach(module -> registerModule(module, modulesToRegister));
		}
		if (this.moduleClasses != null) {
			for (Class<? extends Module> moduleClass : this.moduleClasses) {
				registerModule(BeanUtils.instantiateClass(moduleClass), modulesToRegister);
			}
		}
		List<Module> modules = new ArrayList<>();
		for (List<Module> nestedModules : modulesToRegister.values()) {
			modules.addAll(nestedModules);
		}
		objectMapper.registerModules(modules);

		if (this.dateFormat != null) {
			objectMapper.setDateFormat(this.dateFormat);
		}
		if (this.locale != null) {
			objectMapper.setLocale(this.locale);
		}
		if (this.timeZone != null) {
			objectMapper.setTimeZone(this.timeZone);
		}

		if (this.annotationIntrospector != null) {
			objectMapper.setAnnotationIntrospector(this.annotationIntrospector);
		}
		if (this.propertyNamingStrategy != null) {
			objectMapper.setPropertyNamingStrategy(this.propertyNamingStrategy);
		}
		if (this.defaultTyping != null) {
			objectMapper.setDefaultTyping(this.defaultTyping);
		}
		if (this.serializationInclusion != null) {
			objectMapper.setDefaultPropertyInclusion(this.serializationInclusion);
		}

		if (this.filters != null) {
			objectMapper.setFilterProvider(this.filters);
		}

		this.mixIns.forEach(objectMapper::addMixIn);

		if (!this.serializers.isEmpty() || !this.deserializers.isEmpty()) {
			SimpleModule module = new SimpleModule();
			addSerializers(module);
			addDeserializers(module);
			objectMapper.registerModule(module);
		}

		this.visibilities.forEach(objectMapper::setVisibility);

		customizeDefaultFeatures(objectMapper);
		this.features.forEach((feature, enabled) -> configureFeature(objectMapper, feature, enabled));

		if (this.handlerInstantiator != null) {
			objectMapper.setHandlerInstantiator(this.handlerInstantiator);
		}
		else if (this.applicationContext != null) {
			objectMapper.setHandlerInstantiator(
					new SpringHandlerInstantiator(this.applicationContext.getAutowireCapableBeanFactory()));
		}

		if (this.configurer != null) {
			this.configurer.accept(objectMapper);
		}
	}

	private void registerModule(Module module, MultiValueMap<Object, Module> modulesToRegister) {
		if (module.getTypeId() == null) {
			modulesToRegister.add(SimpleModule.class.getName(), module);
		}
		else {
			modulesToRegister.set(module.getTypeId(), module);
		}
	}


	// Any change to this method should be also applied to spring-jms and spring-messaging
	// MappingJackson2MessageConverter default constructors
	private void customizeDefaultFeatures(ObjectMapper objectMapper) {
		if (!this.features.containsKey(MapperFeature.DEFAULT_VIEW_INCLUSION)) {
			configureFeature(objectMapper, MapperFeature.DEFAULT_VIEW_INCLUSION, false);
		}
		if (!this.features.containsKey(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)) {
			configureFeature(objectMapper, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> void addSerializers(SimpleModule module) {
		this.serializers.forEach((type, serializer) ->
				module.addSerializer((Class<? extends T>) type, (JsonSerializer<T>) serializer));
	}

	@SuppressWarnings("unchecked")
	private <T> void addDeserializers(SimpleModule module) {
		this.deserializers.forEach((type, deserializer) ->
				module.addDeserializer((Class<T>) type, (JsonDeserializer<? extends T>) deserializer));
	}

	@SuppressWarnings("deprecation")  // on Jackson 2.13: configure(MapperFeature, boolean)
	private void configureFeature(ObjectMapper objectMapper, Object feature, boolean enabled) {
		if (feature instanceof JsonParser.Feature) {
			objectMapper.configure((JsonParser.Feature) feature, enabled);
		}
		else if (feature instanceof JsonGenerator.Feature) {
			objectMapper.configure((JsonGenerator.Feature) feature, enabled);
		}
		else if (feature instanceof SerializationFeature) {
			objectMapper.configure((SerializationFeature) feature, enabled);
		}
		else if (feature instanceof DeserializationFeature) {
			objectMapper.configure((DeserializationFeature) feature, enabled);
		}
		else if (feature instanceof MapperFeature) {
			objectMapper.configure((MapperFeature) feature, enabled);
		}
		else {
			throw new IllegalArgumentException("Unknown feature class: " + feature.getClass().getName());
		}
	}

	@SuppressWarnings("unchecked")
	private void registerWellKnownModulesIfAvailable(MultiValueMap<Object, Module> modulesToRegister) {
		try {
			Class<? extends Module> jdk8ModuleClass = (Class<? extends Module>)
					ClassUtils.forName("com.fasterxml.jackson.datatype.jdk8.Jdk8Module", this.moduleClassLoader);
			Module jdk8Module = BeanUtils.instantiateClass(jdk8ModuleClass);
			modulesToRegister.set(jdk8Module.getTypeId(), jdk8Module);
		}
		catch (ClassNotFoundException ex) {
			// jackson-datatype-jdk8 not available
		}

		try {
			Class<? extends Module> javaTimeModuleClass = (Class<? extends Module>)
					ClassUtils.forName("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule", this.moduleClassLoader);
			Module javaTimeModule = BeanUtils.instantiateClass(javaTimeModuleClass);
			modulesToRegister.set(javaTimeModule.getTypeId(), javaTimeModule);
		}
		catch (ClassNotFoundException ex) {
			// jackson-datatype-jsr310 not available
		}

		// Joda-Time 2.x present?
		if (ClassUtils.isPresent("org.joda.time.YearMonth", this.moduleClassLoader)) {
			try {
				Class<? extends Module> jodaModuleClass = (Class<? extends Module>)
						ClassUtils.forName("com.fasterxml.jackson.datatype.joda.JodaModule", this.moduleClassLoader);
				Module jodaModule = BeanUtils.instantiateClass(jodaModuleClass);
				modulesToRegister.set(jodaModule.getTypeId(), jodaModule);
			}
			catch (ClassNotFoundException ex) {
				// jackson-datatype-joda not available
			}
		}

		// Kotlin present?
		if (KotlinDetector.isKotlinPresent()) {
			try {
				Class<? extends Module> kotlinModuleClass = (Class<? extends Module>)
						ClassUtils.forName("com.fasterxml.jackson.module.kotlin.KotlinModule", this.moduleClassLoader);
				Module kotlinModule = BeanUtils.instantiateClass(kotlinModuleClass);
				modulesToRegister.set(kotlinModule.getTypeId(), kotlinModule);
			}
			catch (ClassNotFoundException ex) {
				// jackson-module-kotlin not available
			}
		}
	}


	// Convenience factory methods

	/**
	 * 获取 {@link Jackson2ObjectMapperBuilder} 实例，以便
	 * 构建常规的 JSON {@link ObjectMapper} 实例。
	 */
	public static Jackson2ObjectMapperBuilder json() {
		return new Jackson2ObjectMapperBuilder();
	}

	/**
	 * 获取 {@link Jackson2ObjectMapperBuilder} 实例，以便
	 * 构建 {@link XmlMapper} 实例。
	 */
	public static Jackson2ObjectMapperBuilder xml() {
		return new Jackson2ObjectMapperBuilder().createXmlMapper(true);
	}

	/**
	 * 获取 {@link Jackson2ObjectMapperBuilder} 实例，以便
	 * 构建 Smile 数据格式的 {@link ObjectMapper} 实例。
	 * @since 5.0
	 */
	public static Jackson2ObjectMapperBuilder smile() {
		return new Jackson2ObjectMapperBuilder().factory(new SmileFactoryInitializer().create());
	}

	/**
	 * 获取 {@link Jackson2ObjectMapperBuilder} 实例，以便
	 * 构建 CBOR 数据格式的 {@link ObjectMapper} 实例。
	 * @since 5.0
	 */
	public static Jackson2ObjectMapperBuilder cbor() {
		return new Jackson2ObjectMapperBuilder().factory(new CborFactoryInitializer().create());
	}


	private static class XmlObjectMapperInitializer {

		public ObjectMapper create(@Nullable JsonFactory factory) {
			if (factory != null) {
				return new XmlMapper((XmlFactory) factory);
			}
			else {
				return new XmlMapper(StaxUtils.createDefensiveInputFactory());
			}
		}

		public ObjectMapper create(boolean defaultUseWrapper, @Nullable JsonFactory factory) {
			JacksonXmlModule module = new JacksonXmlModule();
			module.setDefaultUseWrapper(defaultUseWrapper);
			if (factory != null) {
				return new XmlMapper((XmlFactory) factory, module);
			}
			else {
				return new XmlMapper(new XmlFactory(StaxUtils.createDefensiveInputFactory()), module);
			}
		}
	}


	private static class SmileFactoryInitializer {

		public JsonFactory create() {
			return new SmileFactory();
		}
	}


	private static class CborFactoryInitializer {

		public JsonFactory create() {
			return new CBORFactory();
		}
	}

}
