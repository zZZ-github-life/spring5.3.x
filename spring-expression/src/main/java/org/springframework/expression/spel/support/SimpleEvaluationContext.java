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

package org.springframework.expression.spel.support;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.ConstructorResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.OperatorOverloader;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypeComparator;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypeLocator;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.lang.Nullable;

/**
 * {@link EvaluationContext} 的基本实现，聚焦于 SpEL 核心特性和定制选项的一个子集，面向简单的条件求
 * 值，尤其是数据绑定场景。
 *
 * <p>在许多情况下，并不需要 SpEL 语言的全部能力，而应有意义地加以限制。示例包括但不限于数据绑定表
 * 达式、基于属性的过滤器等。为此，{@code SimpleEvaluationContext} 仅支持 SpEL 语言语法的一个子集，
 * 例如排除对 Java 类型、构造器和 Bean 引用的引用。
 *
 * <p>创建 {@code SimpleEvaluationContext} 时，需要选择 SpEL 表达式中属性访问所需的支持级别：
 * <ul>
 * <li>自定义的 {@code PropertyAccessor}（通常不是基于反射的），可以与 {@link DataBindingPropertyAccessor} 结合使用</li>
 * <li>用于只读访问的数据绑定属性</li>
 * <li>用于读写的数据绑定属性</li>
 * </ul>
 *
 * <p>方便的是，{@link SimpleEvaluationContext#forReadOnlyDataBinding()} 可通过
 * {@link DataBindingPropertyAccessor} 启用对属性的读访问；当还需要写访问时，
 * {@link SimpleEvaluationContext#forReadWriteDataBinding()} 同样如此。或者，通过
 * {@link SimpleEvaluationContext#forPropertyAccessors} 配置自定义访问器，并可通过构建器启用方法解
 * 析和/或类型转换器。
 *
 * <p>请注意，{@code SimpleEvaluationContext} 通常不配置默认根对象。相反，它旨在创建一次，然后通过
 * 对预编译的 {@link org.springframework.expression.Expression} 反复调用 {@code getValue} 来重复使
 * 用，调用时同时传入 {@code EvaluationContext} 和根对象作为参数：
 * {@link org.springframework.expression.Expression#getValue(EvaluationContext, Object)}。
 *
 * <p>如需更强大的功能和灵活性，特别是在内部配置场景中，请考虑改用
 * {@link StandardEvaluationContext}。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.3.15
 * @see #forPropertyAccessors
 * @see #forReadOnlyDataBinding()
 * @see #forReadWriteDataBinding()
 * @see StandardEvaluationContext
 * @see StandardTypeConverter
 * @see DataBindingPropertyAccessor
 */
public final class SimpleEvaluationContext implements EvaluationContext {

	private static final TypeLocator typeNotFoundTypeLocator = typeName -> {
		throw new SpelEvaluationException(SpelMessage.TYPE_NOT_FOUND, typeName);
	};


	private final TypedValue rootObject;

	private final List<PropertyAccessor> propertyAccessors;

	private final List<MethodResolver> methodResolvers;

	private final TypeConverter typeConverter;

	private final TypeComparator typeComparator = new StandardTypeComparator();

	private final OperatorOverloader operatorOverloader = new StandardOperatorOverloader();

	private final Map<String, Object> variables = new HashMap<>();


	private SimpleEvaluationContext(List<PropertyAccessor> accessors, List<MethodResolver> resolvers,
			@Nullable TypeConverter converter, @Nullable TypedValue rootObject) {

		this.propertyAccessors = accessors;
		this.methodResolvers = resolvers;
		this.typeConverter = (converter != null ? converter : new StandardTypeConverter());
		this.rootObject = (rootObject != null ? rootObject : TypedValue.NULL);
	}


	/**
	 * 返回指定的根对象（如果有）。
	 */
	@Override
	public TypedValue getRootObject() {
		return this.rootObject;
	}

	/**
	 * 返回指定的 {@link PropertyAccessor} 委托（如果有）。
	 * @see #forPropertyAccessors
	 */
	@Override
	public List<PropertyAccessor> getPropertyAccessors() {
		return this.propertyAccessors;
	}

	/**
	 * 始终返回空列表，因为此上下文不支持使用类型引用。
	 */
	@Override
	public List<ConstructorResolver> getConstructorResolvers() {
		return Collections.emptyList();
	}

	/**
	 * 返回指定的 {@link MethodResolver} 委托（如果有）。
	 * @see Builder#withMethodResolvers
	 */
	@Override
	public List<MethodResolver> getMethodResolvers() {
		return this.methodResolvers;
	}

	/**
	 * {@code SimpleEvaluationContext} 不支持使用 Bean 引用。
	 * @return 始终为 {@code null}
	 */
	@Override
	@Nullable
	public BeanResolver getBeanResolver() {
		return null;
	}

	/**
	 * {@code SimpleEvaluationContext} 不支持使用类型引用。
	 * @return {@code TypeLocator} 实现，会抛出
	 * 带有 {@link SpelMessage#TYPE_NOT_FOUND} 的 {@link SpelEvaluationException}。
	 */
	@Override
	public TypeLocator getTypeLocator() {
		return typeNotFoundTypeLocator;
	}

	/**
	 * 所配置的 {@link TypeConverter}。<p>默认情况下为 {@link StandardTypeConverter}。
	 * @see Builder#withTypeConverter
	 * @see Builder#withConversionService
	 */
	@Override
	public TypeConverter getTypeConverter() {
		return this.typeConverter;
	}

	/**
	 * 返回 {@link StandardTypeComparator} 的实例。
	 */
	@Override
	public TypeComparator getTypeComparator() {
		return this.typeComparator;
	}

	/**
	 * 返回 {@link StandardOperatorOverloader} 的实例。
	 */
	@Override
	public OperatorOverloader getOperatorOverloader() {
		return this.operatorOverloader;
	}

	/**
	 * {@code SimpleEvaluationContext} 不支持在表达式中进行变量赋值。
	 * @throws SpelEvaluationException 带有 {@link SpelMessage#VARIABLE_ASSIGNMENT_NOT_SUPPORTED} 消息
	 * @since 5.2.24
	 */
	@Override
	public TypedValue assignVariable(String name, Supplier<TypedValue> valueSupplier) {
		throw new SpelEvaluationException(SpelMessage.VARIABLE_ASSIGNMENT_NOT_SUPPORTED, "#" + name);
	}

	@Override
	public void setVariable(String name, @Nullable Object value) {
		this.variables.put(name, value);
	}

	@Override
	@Nullable
	public Object lookupVariable(String name) {
		return this.variables.get(name);
	}


	/**
	 * 为指定的 {@link PropertyAccessor} 委托创建一个 {@code SimpleEvaluationContext}：通常是针对某个用
	 * 例的自定义 {@code PropertyAccessor}（例如在自定义数据结构中解析属性），如果还需要属性解引用，也
	 * 可以与 {@link DataBindingPropertyAccessor} 组合使用。
	 * @param accessors 要使用的访问器委托
	 * @see DataBindingPropertyAccessor#forReadOnlyAccess()
	 * @see DataBindingPropertyAccessor#forReadWriteAccess()
	 */
	public static Builder forPropertyAccessors(PropertyAccessor... accessors) {
		for (PropertyAccessor accessor : accessors) {
			if (accessor.getClass() == ReflectivePropertyAccessor.class) {
				throw new IllegalArgumentException("SimpleEvaluationContext is not designed for use with a plain " +
						"ReflectivePropertyAccessor. Consider using DataBindingPropertyAccessor or a custom subclass.");
			}
		}
		return new Builder(accessors);
	}

	/**
	 * 创建一个 {@code SimpleEvaluationContext}，通过 {@link DataBindingPropertyAccessor} 以只读方式访
	 * 问公共属性。
	 * @see DataBindingPropertyAccessor#forReadOnlyAccess()
	 * @see #forPropertyAccessors
	 */
	public static Builder forReadOnlyDataBinding() {
		return new Builder(DataBindingPropertyAccessor.forReadOnlyAccess());
	}

	/**
	 * 创建一个 {@code SimpleEvaluationContext}，通过 {@link DataBindingPropertyAccessor} 以读写方式访
	 * 问公共属性。
	 * @see DataBindingPropertyAccessor#forReadWriteAccess()
	 * @see #forPropertyAccessors
	 */
	public static Builder forReadWriteDataBinding() {
		return new Builder(DataBindingPropertyAccessor.forReadWriteAccess());
	}


	/**
	 * {@code SimpleEvaluationContext} 的构建器。
	 */
	public static class Builder {

		private final List<PropertyAccessor> accessors;

		private List<MethodResolver> resolvers = Collections.emptyList();

		@Nullable
		private TypeConverter typeConverter;

		@Nullable
		private TypedValue rootObject;

		public Builder(PropertyAccessor... accessors) {
			this.accessors = Arrays.asList(accessors);
		}

		/**
		 * 注册指定的 {@link MethodResolver} 委托，以组合属性访问和方法解析。
		 * @param resolvers 要使用的解析器委托
		 * @see #withInstanceMethods()
		 * @see SimpleEvaluationContext#forPropertyAccessors
		 */
		public Builder withMethodResolvers(MethodResolver... resolvers) {
			for (MethodResolver resolver : resolvers) {
				if (resolver.getClass() == ReflectiveMethodResolver.class) {
					throw new IllegalArgumentException("SimpleEvaluationContext is not designed for use with a plain " +
							"ReflectiveMethodResolver. Consider using DataBindingMethodResolver or a custom subclass.");
				}
			}
			this.resolvers = Arrays.asList(resolvers);
			return this;
		}

		/**
		 * 在指定属性访问器之外，再注册一个用于实例方法调用的 {@link DataBindingMethodResolver}（即不支持静
		 * 态方法），通常与 {@link DataBindingPropertyAccessor} 组合使用。
		 * @see #withMethodResolvers
		 * @see SimpleEvaluationContext#forReadOnlyDataBinding()
		 * @see SimpleEvaluationContext#forReadWriteDataBinding()
		 */
		public Builder withInstanceMethods() {
			this.resolvers = Collections.singletonList(DataBindingMethodResolver.forInstanceMethodInvocation());
			return this;
		}


		/**
		 * 注册自定义的 {@link ConversionService}。<p>默认使用由
		 * {@link org.springframework.core.convert.support.DefaultConversionService} 支持的
		 * {@link StandardTypeConverter}。
		 * @see #withTypeConverter
		 * @see StandardTypeConverter#StandardTypeConverter(ConversionService)
		 */
		public Builder withConversionService(ConversionService conversionService) {
			this.typeConverter = new StandardTypeConverter(conversionService);
			return this;
		}
		/**
		 * 注册自定义的 {@link TypeConverter}。<p>默认使用由
		 * {@link org.springframework.core.convert.support.DefaultConversionService} 支持的
		 * {@link StandardTypeConverter}。
		 * @see #withConversionService
		 * @see StandardTypeConverter#StandardTypeConverter()
		 */
		public Builder withTypeConverter(TypeConverter converter) {
			this.typeConverter = converter;
			return this;
		}

		/**
		 * 指定用于解析的默认根对象。<p>默认没有，求值时需要传入对象参数。
		 * @see org.springframework.expression.Expression#getValue(EvaluationContext)
		 * @see org.springframework.expression.Expression#getValue(EvaluationContext, Object)
		 */
		public Builder withRootObject(Object rootObject) {
			this.rootObject = new TypedValue(rootObject);
			return this;
		}

		/**
		 * 指定用于解析的带类型根对象。<p>默认没有，求值时需要传入对象参数。
		 * @see org.springframework.expression.Expression#getValue(EvaluationContext)
		 * @see org.springframework.expression.Expression#getValue(EvaluationContext, Object)
		 */
		public Builder withTypedRootObject(Object rootObject, TypeDescriptor typeDescriptor) {
			this.rootObject = new TypedValue(rootObject, typeDescriptor);
			return this;
		}

		public SimpleEvaluationContext build() {
			return new SimpleEvaluationContext(this.accessors, this.resolvers, this.typeConverter, this.rootObject);
		}
	}

}
