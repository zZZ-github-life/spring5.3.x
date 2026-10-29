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

package org.springframework.validation;

import java.beans.PropertyEditor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessException;
import org.springframework.beans.PropertyAccessorUtils;
import org.springframework.beans.PropertyBatchUpdateException;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.format.Formatter;
import org.springframework.format.support.FormatterPropertyEditorAdapter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * 允许在目标对象上设置属性值的绑定器，包括对验证和绑定结果分析的支持。
 *
 * <p>可以通过指定允许的字段模式、必填字段、自定义编辑器等来自定义绑定过程。
 *
 * <p><strong>WARNING</strong>：数据绑定可能因暴露对象图中不打算由外部客户端访问或修改的部分而导致
 * 安全问题。因此，在安全方面应仔细考虑数据绑定的设计和使用。有关更多详细信息，请参阅参考手册中关于
 * 数据绑定的专门章节：
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-initbinder-model-design">
 * Spring Web MVC</a> 和
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-ann-initbinder-model-design">
 * Spring WebFlux</a>。
 *
 * <p>可以通过 {@link BindingResult} 接口检查绑定结果，该接口扩展了 {@link Errors} 接口：请参见
 * {@link #getBindingResult()} 方法。缺失字段和属性访问异常将被转换为
 * {@link FieldError FieldErrors}，并收集到 Errors 实例中，使用以下错误代码：
 *
 * <ul>
 * <li>Missing field error: "required"
 * <li>Type mismatch error: "typeMismatch"
 * <li>Method invocation error: "methodInvocation"
 * </ul>
 *
 * <p>默认情况下，绑定错误通过 {@link BindingErrorProcessor} 策略解析，处理缺失字段和属性访问异常：
 * 请参见 {@link #setBindingErrorProcessor} 方法。如有需要，可以覆盖默认策略，例如生成不同的错误代
 * 码。
 *
 * <p>自定义验证错误可以在之后添加。通常您会希望将这些错误代码解析为适当的用户可见错误消息；这可以
 * 通过使用 {@link org.springframework.context.MessageSource} 解析每个错误来实现，该接口能够通过其
 * {@link org.springframework.context.MessageSource#getMessage(org.springframework.context.MessageSourceResolvable, java.util.Locale)}
 * 方法解析 {@link ObjectError}/{@link FieldError}。消息代码列表可以通过
 * {@link MessageCodesResolver} 策略进行自定义：请参见 {@link #setMessageCodesResolver} 方法。
 * {@link DefaultMessageCodesResolver} 的 javadoc 说明了默认解析规则的详细信息。
 *
 * <p>此通用数据绑定器可用于任何类型的环境。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Stephane Nicoll
 * @author Kazuki Shimizu
 * @author Sam Brannen
 * @see #setAllowedFields
 * @see #setRequiredFields
 * @see #registerCustomEditor
 * @see #setMessageCodesResolver
 * @see #setBindingErrorProcessor
 * @see #bind
 * @see #getBindingResult
 * @see DefaultMessageCodesResolver
 * @see DefaultBindingErrorProcessor
 * @see org.springframework.context.MessageSource
 */
public class DataBinder implements PropertyEditorRegistry, TypeConverter {

	/** 用于绑定的默认对象名称："target"。 */
	public static final String DEFAULT_OBJECT_NAME = "target";

	/** 数组和集合增长的默认限制：256。 */
	public static final int DEFAULT_AUTO_GROW_COLLECTION_LIMIT = 256;


	/**
	 * 我们将创建大量 DataBinder 实例：让我们使用静态日志记录器。
	 */
	protected static final Log logger = LogFactory.getLog(DataBinder.class);

	@Nullable
	private final Object target;

	private final String objectName;

	@Nullable
	private AbstractPropertyBindingResult bindingResult;

	private boolean directFieldAccess = false;

	@Nullable
	private SimpleTypeConverter typeConverter;

	private boolean ignoreUnknownFields = true;

	private boolean ignoreInvalidFields = false;

	private boolean autoGrowNestedPaths = true;

	private int autoGrowCollectionLimit = DEFAULT_AUTO_GROW_COLLECTION_LIMIT;

	@Nullable
	private String[] allowedFields;

	@Nullable
	private String[] disallowedFields;

	@Nullable
	private String[] requiredFields;

	@Nullable
	private ConversionService conversionService;

	@Nullable
	private MessageCodesResolver messageCodesResolver;

	private BindingErrorProcessor bindingErrorProcessor = new DefaultBindingErrorProcessor();

	private final List<Validator> validators = new ArrayList<>();


	/**
	 * 使用默认对象名称创建新的 DataBinder 实例。
	 * @param target the target object to bind onto (or {@code null}
	 * if the binder is just used to convert a plain parameter value)
	 * @see #DEFAULT_OBJECT_NAME
	 */
	public DataBinder(@Nullable Object target) {
		this(target, DEFAULT_OBJECT_NAME);
	}

	/**
	 * 创建新的 DataBinder 实例。
	 * @param target the target object to bind onto (or {@code null}
	 * if the binder is just used to convert a plain parameter value)
	 * @param objectName the name of the target object
	 */
	public DataBinder(@Nullable Object target, String objectName) {
		this.target = ObjectUtils.unwrapOptional(target);
		this.objectName = objectName;
	}


	/**
	 * 返回被包装的目标对象。
	 */
	@Nullable
	public Object getTarget() {
		return this.target;
	}

	/**
	 * 返回绑定对象的名称。
	 */
	public String getObjectName() {
		return this.objectName;
	}

	/**
	 * 设置此绑定器是否应尝试对包含 null 值的嵌套路径进行“自动增长”。<p>如果为 "true"，则 null 路径位置
	 * 将用默认对象值填充并进行遍历，而不是导致异常。此标志还会在访问越界索引时启用集合元素的自动增长。
	 * <p>在标准 DataBinder 上默认为 "true"。请注意，从 Spring 4.1 起，此功能支持 Bean 属性访问
	 * （DataBinder 的默认模式）和字段访问。
	 * @see #initBeanPropertyAccess()
	 * @see org.springframework.beans.BeanWrapper#setAutoGrowNestedPaths
	 */
	public void setAutoGrowNestedPaths(boolean autoGrowNestedPaths) {
		Assert.state(this.bindingResult == null,
				"DataBinder is already initialized - call setAutoGrowNestedPaths before other configuration methods");
		this.autoGrowNestedPaths = autoGrowNestedPaths;
	}

	/**
	 * 返回是否已激活嵌套路径的“自动增长”。
	 */
	public boolean isAutoGrowNestedPaths() {
		return this.autoGrowNestedPaths;
	}

	/**
	 * 指定数组和集合自动增长的上限。<p>默认值为 256，可在索引较大时防止 OutOfMemoryErrors。如果您的自
	 * 动增长需求异常高，可以提高此上限。
	 * @see #initBeanPropertyAccess()
	 * @see org.springframework.beans.BeanWrapper#setAutoGrowCollectionLimit
	 */
	public void setAutoGrowCollectionLimit(int autoGrowCollectionLimit) {
		Assert.state(this.bindingResult == null,
				"DataBinder is already initialized - call setAutoGrowCollectionLimit before other configuration methods");
		this.autoGrowCollectionLimit = autoGrowCollectionLimit;
	}

	/**
	 * 返回数组和集合自动增长的当前上限。
	 */
	public int getAutoGrowCollectionLimit() {
		return this.autoGrowCollectionLimit;
	}

	/**
	 * 为此 DataBinder 初始化标准 JavaBean 属性访问。<p>这是默认行为；显式调用只会导致提前初始化。
	 * @see #initDirectFieldAccess()
	 * @see #createBeanPropertyBindingResult()
	 */
	public void initBeanPropertyAccess() {
		Assert.state(this.bindingResult == null,
				"DataBinder is already initialized - call initBeanPropertyAccess before other configuration methods");
		this.directFieldAccess = false;
	}

	/**
	 * 使用标准 JavaBean 属性访问创建 {@link AbstractPropertyBindingResult} 实例。
	 * @since 4.2.1
	 */
	protected AbstractPropertyBindingResult createBeanPropertyBindingResult() {
		BeanPropertyBindingResult result = new BeanPropertyBindingResult(getTarget(),
				getObjectName(), isAutoGrowNestedPaths(), getAutoGrowCollectionLimit());

		if (this.conversionService != null) {
			result.initConversion(this.conversionService);
		}
		if (this.messageCodesResolver != null) {
			result.setMessageCodesResolver(this.messageCodesResolver);
		}

		return result;
	}

	/**
	 * 为此 DataBinder 初始化直接字段访问，作为默认 Bean 属性访问的替代方案。
	 * @see #initBeanPropertyAccess()
	 * @see #createDirectFieldBindingResult()
	 */
	public void initDirectFieldAccess() {
		Assert.state(this.bindingResult == null,
				"DataBinder is already initialized - call initDirectFieldAccess before other configuration methods");
		this.directFieldAccess = true;
	}

	/**
	 * 使用直接字段访问创建 {@link AbstractPropertyBindingResult} 实例。
	 * @since 4.2.1
	 */
	protected AbstractPropertyBindingResult createDirectFieldBindingResult() {
		DirectFieldBindingResult result = new DirectFieldBindingResult(getTarget(),
				getObjectName(), isAutoGrowNestedPaths());

		if (this.conversionService != null) {
			result.initConversion(this.conversionService);
		}
		if (this.messageCodesResolver != null) {
			result.setMessageCodesResolver(this.messageCodesResolver);
		}

		return result;
	}

	/**
	 * 返回此 DataBinder 持有的内部 BindingResult，作为 AbstractPropertyBindingResult。
	 */
	protected AbstractPropertyBindingResult getInternalBindingResult() {
		if (this.bindingResult == null) {
			this.bindingResult = (this.directFieldAccess ?
					createDirectFieldBindingResult(): createBeanPropertyBindingResult());
		}
		return this.bindingResult;
	}

	/**
	 * 返回此绑定器 BindingResult 的底层 PropertyAccessor。
	 */
	protected ConfigurablePropertyAccessor getPropertyAccessor() {
		return getInternalBindingResult().getPropertyAccessor();
	}

	/**
	 * 返回此绑定器的底层 SimpleTypeConverter。
	 */
	protected SimpleTypeConverter getSimpleTypeConverter() {
		if (this.typeConverter == null) {
			this.typeConverter = new SimpleTypeConverter();
			if (this.conversionService != null) {
				this.typeConverter.setConversionService(this.conversionService);
			}
		}
		return this.typeConverter;
	}

	/**
	 * 返回此绑定器 BindingResult 的底层 TypeConverter。
	 */
	protected PropertyEditorRegistry getPropertyEditorRegistry() {
		if (getTarget() != null) {
			return getInternalBindingResult().getPropertyAccessor();
		}
		else {
			return getSimpleTypeConverter();
		}
	}

	/**
	 * 返回此绑定器 BindingResult 的底层 TypeConverter。
	 */
	protected TypeConverter getTypeConverter() {
		if (getTarget() != null) {
			return getInternalBindingResult().getPropertyAccessor();
		}
		else {
			return getSimpleTypeConverter();
		}
	}

	/**
	 * 返回此 DataBinder 创建的 BindingResult 实例。这样可以方便地在绑定操作之后访问绑定结果。
	 * @return the BindingResult instance, to be treated as BindingResult
	 * or as Errors instance (Errors is a super-interface of BindingResult)
	 * @see Errors
	 * @see #bind
	 */
	public BindingResult getBindingResult() {
		return getInternalBindingResult();
	}


	/**
	 * 设置是否忽略未知字段，即是否忽略在目标对象中没有对应字段的绑定参数。<p>默认为 "true"。关闭此选项
	 * 可强制所有绑定参数都必须在目标对象中有匹配的字段。<p>请注意，此设置仅适用于此 DataBinder 上的<i>
	 * 绑定</i>操作，不适用于通过其 {@link #getBindingResult() BindingResult} <i>检索</i>值。
	 * @see #bind
	 */
	public void setIgnoreUnknownFields(boolean ignoreUnknownFields) {
		this.ignoreUnknownFields = ignoreUnknownFields;
	}

	/**
	 * 返回绑定时是否忽略未知字段。
	 */
	public boolean isIgnoreUnknownFields() {
		return this.ignoreUnknownFields;
	}

	/**
	 * 设置是否忽略无效字段，即是否忽略在目标对象中具有对应字段但这些字段不可访问（例如由于嵌套路径中的
	 * null 值）的绑定参数。<p>默认为 "false"。开启此选项可忽略针对目标对象图中不存在部分的嵌套对象的绑
	 * 定参数。<p>请注意，此设置仅适用于此 DataBinder 上的<i>绑定</i>操作，不适用于通过其
	 * {@link #getBindingResult() BindingResult} <i>检索</i>值。
	 * @see #bind
	 */
	public void setIgnoreInvalidFields(boolean ignoreInvalidFields) {
		this.ignoreInvalidFields = ignoreInvalidFields;
	}

	/**
	 * 返回绑定时是否忽略无效字段。
	 */
	public boolean isIgnoreInvalidFields() {
		return this.ignoreInvalidFields;
	}

	/**
	 * 注册应允许用于绑定的字段模式。<p>默认为所有字段。<p>例如，可限制此范围以避免在绑定 HTTP 请求参数
	 * 时被恶意用户进行不必要的修改。<p>支持 {@code "xxx*"}、{@code "*xxx"}、{@code "*xxx*"} 和
	 * {@code "xxx*yyy"} 匹配（模式部分数量任意），以及直接相等匹配。<p>此方法的默认实现以
	 * {@linkplain PropertyAccessorUtils#canonicalPropertyName(String) canonical}形式存储允许的字段模式。
	 * 因此，重写此方法的子类必须考虑到这一点。<p>可以通过重写 {@link #isAllowed} 方法来实现更复杂的
	 * 匹配。<p>或者，指定一组<i>不允许</i>的字段模式。
	 * @param allowedFields array of allowed field patterns
	 * @see #setDisallowedFields
	 * @see #isAllowed(String)
	 */
	public void setAllowedFields(@Nullable String... allowedFields) {
		this.allowedFields = PropertyAccessorUtils.canonicalPropertyNames(allowedFields);
	}

	/**
	 * 返回应允许用于绑定的字段模式。
	 * @return array of allowed field patterns
	 * @see #setAllowedFields(String...)
	 */
	@Nullable
	public String[] getAllowedFields() {
		return this.allowedFields;
	}

	/**
	 * 注册<i>不</i>允许用于绑定的字段模式。<p>默认没有。<p>将字段标记为不允许，例如以避免在绑定 HTTP
	 * 请求参数时被恶意用户进行不必要的修改。<p>支持 {@code "xxx*"}、{@code "*xxx"}、{@code "*xxx*"} 和
	 * {@code "xxx*yyy"} 匹配（模式部分数量任意），以及直接相等匹配。<p>此方法的默认实现以
	 * {@linkplain PropertyAccessorUtils#canonicalPropertyName(String) canonical}形式存储不允许的字段模
	 * 式。从 Spring Framework 5.2.21 起，默认实现还会将不允许的字段模式转换为
	 * {@linkplain String#toLowerCase() lowercase}，以支持 {@link #isAllowed} 中不区分大小写的模式匹配。
	 * 因此，重写此方法的子类必须考虑这两种转换。<p>可以通过重写 {@link #isAllowed} 方法来实现更复杂
	 * 的匹配。<p>或者，指定一组<i>允许</i>的字段模式。
	 * @param disallowedFields array of disallowed field patterns
	 * @see #setAllowedFields
	 * @see #isAllowed(String)
	 */
	public void setDisallowedFields(@Nullable String... disallowedFields) {
		if (disallowedFields == null) {
			this.disallowedFields = null;
		}
		else {
			String[] fieldPatterns = new String[disallowedFields.length];
			for (int i = 0; i < fieldPatterns.length; i++) {
				fieldPatterns[i] = PropertyAccessorUtils.canonicalPropertyName(disallowedFields[i]).toLowerCase();
			}
			this.disallowedFields = fieldPatterns;
		}
	}

	/**
	 * 返回<i>不</i>应允许用于绑定的字段模式。
	 * @return array of disallowed field patterns
	 * @see #setDisallowedFields(String...)
	 */
	@Nullable
	public String[] getDisallowedFields() {
		return this.disallowedFields;
	}

	/**
	 * 注册每次绑定过程所需的字段。<p>如果指定的某个字段未包含在传入属性值列表中，则将创建相应的“缺失字
	 * 段”错误，错误代码为 "required"（由默认的绑定错误处理器创建）。
	 * @param requiredFields array of field names
	 * @see #setBindingErrorProcessor
	 * @see DefaultBindingErrorProcessor#MISSING_FIELD_ERROR_CODE
	 */
	public void setRequiredFields(@Nullable String... requiredFields) {
		this.requiredFields = PropertyAccessorUtils.canonicalPropertyNames(requiredFields);
		if (logger.isDebugEnabled()) {
			logger.debug("DataBinder requires binding of required fields [" +
					StringUtils.arrayToCommaDelimitedString(requiredFields) + "]");
		}
	}

	/**
	 * 返回每次绑定过程所需的字段。
	 * @return array of field names
	 */
	@Nullable
	public String[] getRequiredFields() {
		return this.requiredFields;
	}

	/**
	 * 设置用于将错误解析为消息代码的策略。将给定策略应用于底层错误持有者。<p>默认是
	 * DefaultMessageCodesResolver。
	 * @see BeanPropertyBindingResult#setMessageCodesResolver
	 * @see DefaultMessageCodesResolver
	 */
	public void setMessageCodesResolver(@Nullable MessageCodesResolver messageCodesResolver) {
		Assert.state(this.messageCodesResolver == null, "DataBinder is already initialized with MessageCodesResolver");
		this.messageCodesResolver = messageCodesResolver;
		if (this.bindingResult != null && messageCodesResolver != null) {
			this.bindingResult.setMessageCodesResolver(messageCodesResolver);
		}
	}

	/**
	 * 设置用于处理绑定错误的策略，即必填字段错误和 {@code PropertyAccessException}。<p>默认是
	 * DefaultBindingErrorProcessor。
	 * @see DefaultBindingErrorProcessor
	 */
	public void setBindingErrorProcessor(BindingErrorProcessor bindingErrorProcessor) {
		Assert.notNull(bindingErrorProcessor, "BindingErrorProcessor must not be null");
		this.bindingErrorProcessor = bindingErrorProcessor;
	}

	/**
	 * 返回用于处理绑定错误的策略。
	 */
	public BindingErrorProcessor getBindingErrorProcessor() {
		return this.bindingErrorProcessor;
	}

	/**
	 * 设置每次绑定步骤后要应用的 Validator。
	 * @see #addValidators(Validator...)
	 * @see #replaceValidators(Validator...)
	 */
	public void setValidator(@Nullable Validator validator) {
		assertValidators(validator);
		this.validators.clear();
		if (validator != null) {
			this.validators.add(validator);
		}
	}

	private void assertValidators(Validator... validators) {
		Object target = getTarget();
		for (Validator validator : validators) {
			if (validator != null && (target != null && !validator.supports(target.getClass()))) {
				throw new IllegalStateException("Invalid target for Validator [" + validator + "]: " + target);
			}
		}
	}

	/**
	 * 添加每次绑定步骤后要应用的 Validator。
	 * @see #setValidator(Validator)
	 * @see #replaceValidators(Validator...)
	 */
	public void addValidators(Validator... validators) {
		assertValidators(validators);
		this.validators.addAll(Arrays.asList(validators));
	}

	/**
	 * 替换每次绑定步骤后要应用的 Validator。
	 * @see #setValidator(Validator)
	 * @see #addValidators(Validator...)
	 */
	public void replaceValidators(Validator... validators) {
		assertValidators(validators);
		this.validators.clear();
		this.validators.addAll(Arrays.asList(validators));
	}

	/**
	 * 返回每次绑定步骤后要应用的主要 Validator（如果有）。
	 */
	@Nullable
	public Validator getValidator() {
		return (!this.validators.isEmpty() ? this.validators.get(0) : null);
	}

	/**
	 * 返回数据绑定后要应用的 Validator。
	 */
	public List<Validator> getValidators() {
		return Collections.unmodifiableList(this.validators);
	}


	//---------------------------------------------------------------------
	// Implementation of PropertyEditorRegistry/TypeConverter interface
	//---------------------------------------------------------------------

	/**
	 * 指定要用于转换属性值的 Spring 3.0 ConversionService，作为 JavaBeans PropertyEditor 的替代方案。
	 */
	public void setConversionService(@Nullable ConversionService conversionService) {
		Assert.state(this.conversionService == null, "DataBinder is already initialized with ConversionService");
		this.conversionService = conversionService;
		if (this.bindingResult != null && conversionService != null) {
			this.bindingResult.initConversion(conversionService);
		}
	}

	/**
	 * 返回关联的 ConversionService（如果有）。
	 */
	@Nullable
	public ConversionService getConversionService() {
		return this.conversionService;
	}

	/**
	 * 添加自定义格式化器，将其应用于与 {@link Formatter} 声明类型匹配的所有字段。<p>在底层注册相应的
	 * {@link PropertyEditor} 适配器。
	 * @param formatter the formatter to add, generically declared for a specific type
	 * @since 4.2
	 * @see #registerCustomEditor(Class, PropertyEditor)
	 */
	public void addCustomFormatter(Formatter<?> formatter) {
		FormatterPropertyEditorAdapter adapter = new FormatterPropertyEditorAdapter(formatter);
		getPropertyEditorRegistry().registerCustomEditor(adapter.getFieldType(), adapter);
	}

	/**
	 * 为 {@link Formatter} 类中指定的字段类型添加自定义格式化器，仅将其应用于指定的字段（如果有），否
	 * 则应用于所有字段。<p>在底层注册相应的 {@link PropertyEditor} 适配器。
	 * @param formatter the formatter to add, generically declared for a specific type
	 * @param fields the fields to apply the formatter to, or none if to be applied to all
	 * @since 4.2
	 * @see #registerCustomEditor(Class, String, PropertyEditor)
	 */
	public void addCustomFormatter(Formatter<?> formatter, String... fields) {
		FormatterPropertyEditorAdapter adapter = new FormatterPropertyEditorAdapter(formatter);
		Class<?> fieldType = adapter.getFieldType();
		if (ObjectUtils.isEmpty(fields)) {
			getPropertyEditorRegistry().registerCustomEditor(fieldType, adapter);
		}
		else {
			for (String field : fields) {
				getPropertyEditorRegistry().registerCustomEditor(fieldType, field, adapter);
			}
		}
	}

	/**
	 * 添加自定义格式化器，仅将其应用于指定的字段类型（如果有），否则应用于与 {@link Formatter} 声明类
	 * 型匹配的所有字段。<p>在底层注册相应的 {@link PropertyEditor} 适配器。
	 * @param formatter the formatter to add (does not need to generically declare a
	 * field type if field types are explicitly specified as parameters)
	 * @param fieldTypes the field types to apply the formatter to, or none if to be
	 * derived from the given {@link Formatter} implementation class
	 * @since 4.2
	 * @see #registerCustomEditor(Class, PropertyEditor)
	 */
	public void addCustomFormatter(Formatter<?> formatter, Class<?>... fieldTypes) {
		FormatterPropertyEditorAdapter adapter = new FormatterPropertyEditorAdapter(formatter);
		if (ObjectUtils.isEmpty(fieldTypes)) {
			getPropertyEditorRegistry().registerCustomEditor(adapter.getFieldType(), adapter);
		}
		else {
			for (Class<?> fieldType : fieldTypes) {
				getPropertyEditorRegistry().registerCustomEditor(fieldType, adapter);
			}
		}
	}

	@Override
	public void registerCustomEditor(Class<?> requiredType, PropertyEditor propertyEditor) {
		getPropertyEditorRegistry().registerCustomEditor(requiredType, propertyEditor);
	}

	@Override
	public void registerCustomEditor(@Nullable Class<?> requiredType, @Nullable String field, PropertyEditor propertyEditor) {
		getPropertyEditorRegistry().registerCustomEditor(requiredType, field, propertyEditor);
	}

	@Override
	@Nullable
	public PropertyEditor findCustomEditor(@Nullable Class<?> requiredType, @Nullable String propertyPath) {
		return getPropertyEditorRegistry().findCustomEditor(requiredType, propertyPath);
	}

	@Override
	@Nullable
	public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType) throws TypeMismatchException {
		return getTypeConverter().convertIfNecessary(value, requiredType);
	}

	@Override
	@Nullable
	public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
			@Nullable MethodParameter methodParam) throws TypeMismatchException {

		return getTypeConverter().convertIfNecessary(value, requiredType, methodParam);
	}

	@Override
	@Nullable
	public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType, @Nullable Field field)
			throws TypeMismatchException {

		return getTypeConverter().convertIfNecessary(value, requiredType, field);
	}

	@Nullable
	@Override
	public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
			@Nullable TypeDescriptor typeDescriptor) throws TypeMismatchException {

		return getTypeConverter().convertIfNecessary(value, requiredType, typeDescriptor);
	}


	/**
	 * 将给定的属性值绑定到此绑定器的目标。<p>此调用可能创建字段错误，表示基本的绑定错误，如必填字段
	 * （代码 "required"）或值与 Bean 属性之间类型不匹配（代码 "typeMismatch"）。<p>请注意，给定的
	 * PropertyValues 应是可丢弃的实例：为提高效率，如果它实现了 MutablePropertyValues 接口，它将被修改
	 * 为仅包含允许的字段；否则，将为此目的创建内部的可变副本。如果您希望原始实例在任何情况下都保持不变，
	 * 请传入 PropertyValues 的副本。
	 * @param pvs property values to bind
	 * @see #doBind(org.springframework.beans.MutablePropertyValues)
	 */
	public void bind(PropertyValues pvs) {
		MutablePropertyValues mpvs = (pvs instanceof MutablePropertyValues ?
				(MutablePropertyValues) pvs : new MutablePropertyValues(pvs));
		doBind(mpvs);
	}

	/**
	 * 绑定过程的实际实现，使用传入的 MutablePropertyValues 实例。
	 * @param mpvs the property values to bind,
	 * as MutablePropertyValues instance
	 * @see #checkAllowedFields
	 * @see #checkRequiredFields
	 * @see #applyPropertyValues
	 */
	protected void doBind(MutablePropertyValues mpvs) {
		checkAllowedFields(mpvs);
		checkRequiredFields(mpvs);
		applyPropertyValues(mpvs);
	}

	/**
	 * 根据允许的字段检查给定的属性值，移除不允许字段的值。
	 * @param mpvs the property values to be bound (can be modified)
	 * @see #getAllowedFields
	 * @see #isAllowed(String)
	 */
	protected void checkAllowedFields(MutablePropertyValues mpvs) {
		PropertyValue[] pvs = mpvs.getPropertyValues();
		for (PropertyValue pv : pvs) {
			String field = PropertyAccessorUtils.canonicalPropertyName(pv.getName());
			if (!isAllowed(field)) {
				mpvs.removePropertyValue(pv);
				getBindingResult().recordSuppressedField(field);
				if (logger.isDebugEnabled()) {
					logger.debug("Field [" + field + "] has been removed from PropertyValues " +
							"and will not be bound, because it has not been found in the list of allowed fields");
				}
			}
		}
	}

	/**
	 * 确定给定字段是否允许用于绑定。<p>对每个传入的属性值调用。<p>在配置的允许字段模式列表和不允许字段
	 * 模式列表中检查 {@code "xxx*"}、{@code "*xxx"}、{@code "*xxx*"} 和 {@code "xxx*yyy"} 匹配（模式部
	 * 分数量任意），以及直接相等匹配。<p>与允许字段模式的匹配区分大小写；而与不允许字段模式的匹配不区
	 * 分大小写。<p>与不允许模式匹配的字段即使也恰好匹配允许列表中的模式，也不会被接受。<p>可以在子类中
	 * 重写，但必须注意遵守上述约定。
	 * @param field the field to check
	 * @return {@code true} if the field is allowed
	 * @see #setAllowedFields
	 * @see #setDisallowedFields
	 * @see org.springframework.util.PatternMatchUtils#simpleMatch(String, String)
	 */
	protected boolean isAllowed(String field) {
		String[] allowed = getAllowedFields();
		String[] disallowed = getDisallowedFields();
		return ((ObjectUtils.isEmpty(allowed) || PatternMatchUtils.simpleMatch(allowed, field)) &&
				(ObjectUtils.isEmpty(disallowed) || !PatternMatchUtils.simpleMatch(disallowed, field.toLowerCase())));
	}

	/**
	 * 根据必填字段检查给定的属性值，并在适当的时候生成缺失字段错误。
	 * @param mpvs the property values to be bound (can be modified)
	 * @see #getRequiredFields
	 * @see #getBindingErrorProcessor
	 * @see BindingErrorProcessor#processMissingFieldError
	 */
	protected void checkRequiredFields(MutablePropertyValues mpvs) {
		String[] requiredFields = getRequiredFields();
		if (!ObjectUtils.isEmpty(requiredFields)) {
			Map<String, PropertyValue> propertyValues = new HashMap<>();
			PropertyValue[] pvs = mpvs.getPropertyValues();
			for (PropertyValue pv : pvs) {
				String canonicalName = PropertyAccessorUtils.canonicalPropertyName(pv.getName());
				propertyValues.put(canonicalName, pv);
			}
			for (String field : requiredFields) {
				PropertyValue pv = propertyValues.get(field);
				boolean empty = (pv == null || pv.getValue() == null);
				if (!empty) {
					if (pv.getValue() instanceof String) {
						empty = !StringUtils.hasText((String) pv.getValue());
					}
					else if (pv.getValue() instanceof String[]) {
						String[] values = (String[]) pv.getValue();
						empty = (values.length == 0 || !StringUtils.hasText(values[0]));
					}
				}
				if (empty) {
					// Use bind error processor to create FieldError.
					getBindingErrorProcessor().processMissingFieldError(field, getInternalBindingResult());
					// Remove property from property values to bind:
					// It has already caused a field error with a rejected value.
					if (pv != null) {
						mpvs.removePropertyValue(pv);
						propertyValues.remove(field);
					}
				}
			}
		}
	}

	/**
	 * 将给定的属性值应用到目标对象。<p>默认实现将所有提供的属性值作为 Bean 属性值应用。默认情况下，未
	 * 知字段将被忽略。
	 * @param mpvs the property values to be bound (can be modified)
	 * @see #getTarget
	 * @see #getPropertyAccessor
	 * @see #isIgnoreUnknownFields
	 * @see #getBindingErrorProcessor
	 * @see BindingErrorProcessor#processPropertyAccessException
	 */
	protected void applyPropertyValues(MutablePropertyValues mpvs) {
		try {
			// Bind request parameters onto target object.
			getPropertyAccessor().setPropertyValues(mpvs, isIgnoreUnknownFields(), isIgnoreInvalidFields());
		}
		catch (PropertyBatchUpdateException ex) {
			// Use bind error processor to create FieldErrors.
			for (PropertyAccessException pae : ex.getPropertyAccessExceptions()) {
				getBindingErrorProcessor().processPropertyAccessException(pae, getInternalBindingResult());
			}
		}
	}


	/**
	 * 调用指定的 Validator（如果有）。
	 * @see #setValidator(Validator)
	 * @see #getBindingResult()
	 */
	public void validate() {
		Object target = getTarget();
		Assert.state(target != null, "No target to validate");
		BindingResult bindingResult = getBindingResult();
		// Call each validator with the same binding result
		for (Validator validator : getValidators()) {
			validator.validate(target, bindingResult);
		}
	}

	/**
	 * 使用给定的验证提示调用指定的 Validator（如果有）。<p>注意：验证提示可能会被实际的目标 Validator
	 * 忽略。
	 * @param validationHints one or more hint objects to be passed to a {@link SmartValidator}
	 * @since 3.1
	 * @see #setValidator(Validator)
	 * @see SmartValidator#validate(Object, Errors, Object...)
	 */
	public void validate(Object... validationHints) {
		Object target = getTarget();
		Assert.state(target != null, "No target to validate");
		BindingResult bindingResult = getBindingResult();
		// Call each validator with the same binding result
		for (Validator validator : getValidators()) {
			if (!ObjectUtils.isEmpty(validationHints) && validator instanceof SmartValidator) {
				((SmartValidator) validator).validate(target, bindingResult, validationHints);
			}
			else if (validator != null) {
				validator.validate(target, bindingResult);
			}
		}
	}

	/**
	 * 关闭此 DataBinder，如果遇到任何错误，可能会抛出 BindException。
	 * @return the model Map, containing target object and Errors instance
	 * @throws BindException if there were any errors in the bind operation
	 * @see BindingResult#getModel()
	 */
	public Map<?, ?> close() throws BindException {
		if (getBindingResult().hasErrors()) {
			throw new BindException(getBindingResult());
		}
		return getBindingResult().getModel();
	}

}
