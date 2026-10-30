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

package org.springframework.http.converter.json;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
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
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.Nullable;

/**
 * 用于创建 Jackson 2.x {@link ObjectMapper}（默认）或
 * {@link XmlMapper}（{@code createXmlMapper} 属性设置为 true）的
 * {@link FactoryBean}，带有可在 XML 配置中启用或禁用 Jackson 特性的 setter。
 *
 * <p>它使用以下属性定制 Jackson 的默认属性：
 * <ul>
 * <li>{@link MapperFeature#DEFAULT_VIEW_INCLUSION} 被禁用</li>
 * <li>{@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} 被禁用</li>
 * </ul>
 *
 * <p>与 {@link MappingJackson2HttpMessageConverter} 配合使用的示例：
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.http.converter.json.MappingJackson2HttpMessageConverter"&gt;
 *   &lt;property name="objectMapper"&gt;
 *     &lt;bean class="org.springframework.http.converter.json.Jackson2ObjectMapperFactoryBean"
 *       p:autoDetectFields="false"
 *       p:autoDetectGettersSetters="false"
 *       p:annotationIntrospector-ref="jaxbAnnotationIntrospector" /&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>与 MappingJackson2JsonView 配合使用的示例：
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.web.servlet.view.json.MappingJackson2JsonView"&gt;
 *   &lt;property name="objectMapper"&gt;
 *     &lt;bean class="org.springframework.http.converter.json.Jackson2ObjectMapperFactoryBean"
 *       p:failOnEmptyBeans="false"
 *       p:indentOutput="true"&gt;
 *       &lt;property name="serializers"&gt;
 *         &lt;array&gt;
 *           &lt;bean class="org.mycompany.MyCustomSerializer" /&gt;
 *         &lt;/array&gt;
 *       &lt;/property&gt;
 *     &lt;/bean&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>如果没有为某些较少使用的选项提供特定的 setter，
 * 仍可以使用更通用的方法 {@link #setFeaturesToEnable} 和
 * {@link #setFeaturesToDisable}。
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.http.converter.json.Jackson2ObjectMapperFactoryBean"&gt;
 *   &lt;property name="featuresToEnable"&gt;
 *     &lt;array&gt;
 *       &lt;util:constant static-field="com.fasterxml.jackson.databind.SerializationFeature.WRAP_ROOT_VALUE"/&gt;
 *       &lt;util:constant static-field="com.fasterxml.jackson.databind.SerializationFeature.CLOSE_CLOSEABLE"/&gt;
 *     &lt;/array&gt;
 *   &lt;/property&gt;
 *   &lt;property name="featuresToDisable"&gt;
 *     &lt;array&gt;
 *       &lt;util:constant static-field="com.fasterxml.jackson.databind.MapperFeature.USE_ANNOTATIONS"/&gt;
 *     &lt;/array&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>如果类路径上检测到以下知名模块，它还会自动注册这些模块：
 * <ul>
 * <li><a href="https://github.com/FasterXML/jackson-datatype-jdk7">jackson-datatype-jdk7</a>：
 * 支持 Java 7 类型，如 {@link java.nio.file.Path}</li>
 * <li><a href="https://github.com/FasterXML/jackson-datatype-jdk8">jackson-datatype-jdk8</a>：
 * 支持其他 Java 8 类型，如 {@link java.util.Optional}</li>
 * <li><a href="https://github.com/FasterXML/jackson-datatype-jsr310">jackson-datatype-jsr310</a>：
 * 支持 Java 8 Date &amp; Time API 类型</li>
 * <li><a href="https://github.com/FasterXML/jackson-module-kotlin">jackson-module-kotlin</a>：
 * 支持 Kotlin 类和数据类</li>
 * </ul>
 *
 * <p>如果希望使用自定义的 {@link Module} 配置 Jackson 的 {@link ObjectMapper}，
 * 可以通过 {@link #setModulesToInstall} 按类名注册一个或多个此类 Module：
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.http.converter.json.Jackson2ObjectMapperFactoryBean"&gt;
 *   &lt;property name="modulesToInstall" value="myapp.jackson.MySampleModule,myapp.jackson.MyOtherModule"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>从 Spring 5.3 起，兼容 Jackson 2.9 至 2.12。
 *
 * @author <a href="mailto:dmitry.katsubo@gmail.com">Dmitry Katsubo</a>
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author Tadaya Tsuyukubo
 * @author Sebastien Deleuze
 * @since 3.2
 */
public class Jackson2ObjectMapperFactoryBean implements FactoryBean<ObjectMapper>, BeanClassLoaderAware,
		ApplicationContextAware, InitializingBean {

	private final Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();

	@Nullable
	private ObjectMapper objectMapper;


	/**
	 * 设置要使用的 {@link ObjectMapper} 实例。如果未设置，
	 * 将使用默认构造函数创建 {@link ObjectMapper}。
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * 如果设置为 true 且未设置自定义的 {@link ObjectMapper}，
	 * 将使用默认构造函数创建 {@link XmlMapper}。
	 * @since 4.1
	 */
	public void setCreateXmlMapper(boolean createXmlMapper) {
		this.builder.createXmlMapper(createXmlMapper);
	}

	/**
	 * 定义用于创建 {@link ObjectMapper} 实例的 {@link JsonFactory}。
	 * @since 5.0
	 */
	public void setFactory(JsonFactory factory) {
		this.builder.factory(factory);
	}

	/**
	 * 使用给定的 {@link DateFormat} 定义日期/时间格式。
	 * <p>注意：根据 Jackson 的线程安全规则，设置此属性会使所公开的
	 * {@link ObjectMapper} 不再是线程安全的。
	 * @see #setSimpleDateFormat(String)
	 */
	public void setDateFormat(DateFormat dateFormat) {
		this.builder.dateFormat(dateFormat);
	}

	/**
	 * 使用 {@link SimpleDateFormat} 定义日期/时间格式。
	 * <p>注意：根据 Jackson 的线程安全规则，设置此属性会使所公开的
	 * {@link ObjectMapper} 不再是线程安全的。
	 * @see #setDateFormat(DateFormat)
	 */
	public void setSimpleDateFormat(String format) {
		this.builder.simpleDateFormat(format);
	}

	/**
	 * 覆盖用于格式化的默认 {@link Locale}。
	 * 默认使用 {@link Locale#getDefault()}。
	 * @since 4.1.5
	 */
	public void setLocale(Locale locale) {
		this.builder.locale(locale);
	}

	/**
	 * 覆盖用于格式化的默认 {@link TimeZone}。
	 * 默认使用 UTC（而非本地时区）。
	 * @since 4.1.5
	 */
	public void setTimeZone(TimeZone timeZone) {
		this.builder.timeZone(timeZone);
	}

	/**
	 * 为序列化和反序列化设置 {@link AnnotationIntrospector}。
	 */
	public void setAnnotationIntrospector(AnnotationIntrospector annotationIntrospector) {
		this.builder.annotationIntrospector(annotationIntrospector);
	}

	/**
	 * 指定用于配置 {@link ObjectMapper} 的
	 * {@link com.fasterxml.jackson.databind.PropertyNamingStrategy}。
	 * @since 4.0.2
	 */
	public void setPropertyNamingStrategy(PropertyNamingStrategy propertyNamingStrategy) {
		this.builder.propertyNamingStrategy(propertyNamingStrategy);
	}

	/**
	 * 指定用于 Jackson 默认类型处理的 {@link TypeResolverBuilder}。
	 * @since 4.2.2
	 */
	public void setDefaultTyping(TypeResolverBuilder<?> typeResolverBuilder) {
		this.builder.defaultTyping(typeResolverBuilder);
	}

	/**
	 * 设置用于序列化的自定义包含策略。
	 * @see com.fasterxml.jackson.annotation.JsonInclude.Include
	 */
	public void setSerializationInclusion(JsonInclude.Include serializationInclusion) {
		this.builder.serializationInclusion(serializationInclusion);
	}

	/**
	 * 设置要使用的全局过滤器，以支持标注 {@link JsonFilter @JsonFilter} 的 POJO。
	 * @since 4.2
	 * @see Jackson2ObjectMapperBuilder#filters(FilterProvider)
	 */
	public void setFilters(FilterProvider filters) {
		this.builder.filters(filters);
	}

	/**
	 * 添加用于增强指定类或接口的混入注解。
	 * @param mixIns 一个 Map，键为其注解将被有效覆盖的目标类（或接口），
	 * 值为其注解将被“添加”到目标类注解上的混入类（或接口）。
	 * @since 4.1.2
	 * @see com.fasterxml.jackson.databind.ObjectMapper#addMixInAnnotations(Class, Class)
	 */
	public void setMixIns(Map<Class<?>, Class<?>> mixIns) {
		this.builder.mixIns(mixIns);
	}

	/**
	 * 配置自定义序列化器。每个序列化器注册到
	 * {@link JsonSerializer#handledType()} 返回的类型，该类型不能为 {@code null}。
	 * @see #setSerializersByType(Map)
	 */
	public void setSerializers(JsonSerializer<?>... serializers) {
		this.builder.serializers(serializers);
	}

	/**
	 * 为给定类型配置自定义序列化器。
	 * @see #setSerializers(JsonSerializer...)
	 */
	public void setSerializersByType(Map<Class<?>, JsonSerializer<?>> serializers) {
		this.builder.serializersByType(serializers);
	}

	/**
	 * 配置自定义反序列化器。每个反序列化器注册到
	 * {@link JsonDeserializer#handledType()} 返回的类型，该类型不能为 {@code null}。
	 * @since 4.3
	 * @see #setDeserializersByType(Map)
	 */
	public void setDeserializers(JsonDeserializer<?>... deserializers) {
		this.builder.deserializers(deserializers);
	}

	/**
	 * 为给定类型配置自定义反序列化器。
	 */
	public void setDeserializersByType(Map<Class<?>, JsonDeserializer<?>> deserializers) {
		this.builder.deserializersByType(deserializers);
	}

	/**
	 * {@link MapperFeature#AUTO_DETECT_FIELDS} 选项的快捷方式。
	 */
	public void setAutoDetectFields(boolean autoDetectFields) {
		this.builder.autoDetectFields(autoDetectFields);
	}

	/**
	 * {@link MapperFeature#AUTO_DETECT_SETTERS}/
	 * {@link MapperFeature#AUTO_DETECT_GETTERS}/{@link MapperFeature#AUTO_DETECT_IS_GETTERS}
	 * 选项的快捷方式。
	 */
	public void setAutoDetectGettersSetters(boolean autoDetectGettersSetters) {
		this.builder.autoDetectGettersSetters(autoDetectGettersSetters);
	}

	/**
	 * {@link MapperFeature#DEFAULT_VIEW_INCLUSION} 选项的快捷方式。
	 * @since 4.1
	 */
	public void setDefaultViewInclusion(boolean defaultViewInclusion) {
		this.builder.defaultViewInclusion(defaultViewInclusion);
	}

	/**
	 * {@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} 选项的快捷方式。
	 * @since 4.1.1
	 */
	public void setFailOnUnknownProperties(boolean failOnUnknownProperties) {
		this.builder.failOnUnknownProperties(failOnUnknownProperties);
	}

	/**
	 * {@link SerializationFeature#FAIL_ON_EMPTY_BEANS} 选项的快捷方式。
	 */
	public void setFailOnEmptyBeans(boolean failOnEmptyBeans) {
		this.builder.failOnEmptyBeans(failOnEmptyBeans);
	}

	/**
	 * {@link SerializationFeature#INDENT_OUTPUT} 选项的快捷方式。
	 */
	public void setIndentOutput(boolean indentOutput) {
		this.builder.indentOutput(indentOutput);
	}

	/**
	 * 定义默认情况下是否为索引属性（List、数组）使用包装器
	 * （仅适用于 {@link XmlMapper}）。
	 * @since 4.3
	 */
	public void setDefaultUseWrapper(boolean defaultUseWrapper) {
		this.builder.defaultUseWrapper(defaultUseWrapper);
	}

	/**
	 * 指定要启用的特性。
	 * @see com.fasterxml.jackson.core.JsonParser.Feature
	 * @see com.fasterxml.jackson.core.JsonGenerator.Feature
	 * @see com.fasterxml.jackson.databind.SerializationFeature
	 * @see com.fasterxml.jackson.databind.DeserializationFeature
	 * @see com.fasterxml.jackson.databind.MapperFeature
	 */
	public void setFeaturesToEnable(Object... featuresToEnable) {
		this.builder.featuresToEnable(featuresToEnable);
	}

	/**
	 * 指定要禁用的特性。
	 * @see com.fasterxml.jackson.core.JsonParser.Feature
	 * @see com.fasterxml.jackson.core.JsonGenerator.Feature
	 * @see com.fasterxml.jackson.databind.SerializationFeature
	 * @see com.fasterxml.jackson.databind.DeserializationFeature
	 * @see com.fasterxml.jackson.databind.MapperFeature
	 */
	public void setFeaturesToDisable(Object... featuresToDisable) {
		this.builder.featuresToDisable(featuresToDisable);
	}

	/**
	 * 设置要向 {@link ObjectMapper} 注册的完整模块列表。
	 * <p>注意：如果设置了此项，将不会进行任何模块查找——
	 * 既不由 Jackson 进行，也不由 Spring 进行
	 * （参见 {@link #setFindModulesViaServiceLoader}）。
	 * 因此，在此处指定空列表将抑制任何形式的模块检测。
	 * <p>请指定此项或 {@link #setModulesToInstall} 之一，不要同时指定两者。
	 * @since 4.0
	 * @see com.fasterxml.jackson.databind.Module
	 */
	public void setModules(List<Module> modules) {
		this.builder.modules(modules);
	}

	/**
	 * 按类（或 XML 中的类名）指定要向 {@link ObjectMapper} 注册的
	 * 一个或多个模块。
	 * <p>此处指定的模块将在 Spring 对 JSR-310 和 Joda-Time 的自动检测之后，
	 * 或在 Jackson 查找模块之后（参见 {@link #setFindModulesViaServiceLoader}）注册，
	 * 从而可以最终覆盖它们的配置。
	 * <p>请指定此项或 {@link #setModules} 之一，不要同时指定两者。
	 * @since 4.0.1
	 * @see com.fasterxml.jackson.databind.Module
	 */
	@SafeVarargs
	public final void setModulesToInstall(Class<? extends Module>... modules) {
		this.builder.modulesToInstall(modules);
	}

	/**
	 * 设置是否让 Jackson 基于类路径中的 META-INF 元数据，
	 * 通过 JDK ServiceLoader 查找可用的模块。要求 Jackson 2.2 或更高版本。
	 * <p>如果未设置此模式，Spring 的 Jackson2ObjectMapperFactoryBean 本身
	 * 将尝试在类路径上查找 JSR-310 和 Joda-Time 支持模块——
	 * 前提是分别提供了 Java 8 和 Joda-Time 本身。
	 * @since 4.0.1
	 * @see com.fasterxml.jackson.databind.ObjectMapper#findModules()
	 */
	public void setFindModulesViaServiceLoader(boolean findModules) {
		this.builder.findModulesViaServiceLoader(findModules);
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.builder.moduleClassLoader(beanClassLoader);
	}

	/**
	 * 自定义 Jackson 处理器
	 * （{@link JsonSerializer}、{@link JsonDeserializer}、{@link KeyDeserializer}、
	 * {@code TypeResolverBuilder} 和 {@code TypeIdResolver}）的构建。
	 * @since 4.1.3
	 * @see Jackson2ObjectMapperFactoryBean#setApplicationContext(ApplicationContext)
	 */
	public void setHandlerInstantiator(HandlerInstantiator handlerInstantiator) {
		this.builder.handlerInstantiator(handlerInstantiator);
	}

	/**
	 * 设置构建器 {@link ApplicationContext}，以便自动装配 Jackson 处理器
	 * （{@link JsonSerializer}、{@link JsonDeserializer}、{@link KeyDeserializer}、
	 * {@code TypeResolverBuilder} 和 {@code TypeIdResolver}）。
	 * @since 4.1.3
	 * @see Jackson2ObjectMapperBuilder#applicationContext(ApplicationContext)
	 * @see SpringHandlerInstantiator
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.builder.applicationContext(applicationContext);
	}


	@Override
	public void afterPropertiesSet() {
		if (this.objectMapper != null) {
			this.builder.configure(this.objectMapper);
		}
		else {
			this.objectMapper = this.builder.build();
		}
	}

	/**
	 * 返回单例 ObjectMapper。
	 */
	@Override
	@Nullable
	public ObjectMapper getObject() {
		return this.objectMapper;
	}

	@Override
	public Class<?> getObjectType() {
		return (this.objectMapper != null ? this.objectMapper.getClass() : null);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
