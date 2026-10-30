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

package org.springframework.expression.spel.support;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.asm.MethodVisitor;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.Property;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.CompilablePropertyAccessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * 一个功能强大的 {@link PropertyAccessor}，通过反射在目标实例上读取属性，也可能写入属性。
 *
 * <p>属性可以通过公共 getter 方法（读取时）或公共 setter 方法（写入时）引用，也可以作为公共字段引
 * 用。
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 3.0
 * @see StandardEvaluationContext
 * @see SimpleEvaluationContext
 * @see DataBindingPropertyAccessor
 */
public class ReflectivePropertyAccessor implements PropertyAccessor {

	private static final Set<Class<?>> ANY_TYPES = Collections.emptySet();

	private static final Set<Class<?>> BOOLEAN_TYPES;

	static {
		Set<Class<?>> booleanTypes = new HashSet<>(4);
		booleanTypes.add(Boolean.class);
		booleanTypes.add(Boolean.TYPE);
		BOOLEAN_TYPES = Collections.unmodifiableSet(booleanTypes);
	}


	private final boolean allowWrite;

	private final Map<PropertyCacheKey, InvokerPair> readerCache = new ConcurrentHashMap<>(64);

	private final Map<PropertyCacheKey, Member> writerCache = new ConcurrentHashMap<>(64);

	private final Map<PropertyCacheKey, TypeDescriptor> typeDescriptorCache = new ConcurrentHashMap<>(64);

	private final Map<Class<?>, Method[]> sortedMethodsCache = new ConcurrentHashMap<>(64);

	@Nullable
	private volatile InvokerPair lastReadInvokerPair;


	/**
	 * 创建一个新的属性访问器，用于读取和写入。
	 * @see #ReflectivePropertyAccessor(boolean)
	 */
	public ReflectivePropertyAccessor() {
		this.allowWrite = true;
	}

	/**
	 * 创建一个新的属性访问器，用于读取，也可能用于写入。
	 * @param allowWrite 是否允许对目标实例进行写操作
	 * @since 4.3.15
	 * @see #canWrite
	 */
	public ReflectivePropertyAccessor(boolean allowWrite) {
		this.allowWrite = allowWrite;
	}


	/**
	 * 返回 {@code null}，表示这是一个通用访问器。
	 */
	@Override
	@Nullable
	public Class<?>[] getSpecificTargetClasses() {
		return null;
	}

	@Override
	public boolean canRead(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
		if (target == null) {
			return false;
		}

		Class<?> type = (target instanceof Class ? (Class<?>) target : target.getClass());
		if (type.isArray() && name.equals("length")) {
			return true;
		}

		PropertyCacheKey cacheKey = new PropertyCacheKey(type, name, target instanceof Class);
		if (this.readerCache.containsKey(cacheKey)) {
			return true;
		}

		Method method = findGetterForProperty(name, type, target);
		if (method != null) {
			// Treat it like a property...
			// The readerCache will only contain gettable properties (let's not worry about setters for now).
			Property property = new Property(type, method, null);
			TypeDescriptor typeDescriptor = new TypeDescriptor(property);
			method = ClassUtils.getInterfaceMethodIfPossible(method, type);
			this.readerCache.put(cacheKey, new InvokerPair(method, typeDescriptor));
			this.typeDescriptorCache.put(cacheKey, typeDescriptor);
			return true;
		}
		else {
			Field field = findField(name, type, target);
			if (field != null) {
				TypeDescriptor typeDescriptor = new TypeDescriptor(field);
				this.readerCache.put(cacheKey, new InvokerPair(field, typeDescriptor));
				this.typeDescriptorCache.put(cacheKey, typeDescriptor);
				return true;
			}
		}

		return false;
	}

	@Override
	public TypedValue read(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
		Assert.state(target != null, "Target must not be null");
		Class<?> type = (target instanceof Class ? (Class<?>) target : target.getClass());

		if (type.isArray() && name.equals("length")) {
			if (target instanceof Class) {
				throw new AccessException("Cannot access length on array class itself");
			}
			return new TypedValue(Array.getLength(target));
		}

		PropertyCacheKey cacheKey = new PropertyCacheKey(type, name, target instanceof Class);
		InvokerPair invoker = this.readerCache.get(cacheKey);
		this.lastReadInvokerPair = invoker;

		if (invoker == null || invoker.member instanceof Method) {
			Method method = (Method) (invoker != null ? invoker.member : null);
			if (method == null) {
				method = findGetterForProperty(name, type, target);
				if (method != null) {
					// Treat it like a property...
					// The readerCache will only contain gettable properties (let's not worry about setters for now).
					Property property = new Property(type, method, null);
					TypeDescriptor typeDescriptor = new TypeDescriptor(property);
					method = ClassUtils.getInterfaceMethodIfPossible(method, type);
					invoker = new InvokerPair(method, typeDescriptor);
					this.lastReadInvokerPair = invoker;
					this.readerCache.put(cacheKey, invoker);
				}
			}
			if (method != null) {
				try {
					ReflectionUtils.makeAccessible(method);
					Object value = method.invoke(target);
					return new TypedValue(value, invoker.typeDescriptor.narrow(value));
				}
				catch (Exception ex) {
					throw new AccessException("Unable to access property '" + name + "' through getter method", ex);
				}
			}
		}

		if (invoker == null || invoker.member instanceof Field) {
			Field field = (Field) (invoker == null ? null : invoker.member);
			if (field == null) {
				field = findField(name, type, target);
				if (field != null) {
					invoker = new InvokerPair(field, new TypeDescriptor(field));
					this.lastReadInvokerPair = invoker;
					this.readerCache.put(cacheKey, invoker);
				}
			}
			if (field != null) {
				try {
					ReflectionUtils.makeAccessible(field);
					Object value = field.get(target);
					return new TypedValue(value, invoker.typeDescriptor.narrow(value));
				}
				catch (Exception ex) {
					throw new AccessException("Unable to access field '" + name + "'", ex);
				}
			}
		}

		throw new AccessException("Neither getter method nor field found for property '" + name + "'");
	}

	@Override
	public boolean canWrite(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
		if (!this.allowWrite || target == null) {
			return false;
		}

		Class<?> type = (target instanceof Class ? (Class<?>) target : target.getClass());
		PropertyCacheKey cacheKey = new PropertyCacheKey(type, name, target instanceof Class);
		if (this.writerCache.containsKey(cacheKey)) {
			return true;
		}

		Method method = findSetterForProperty(name, type, target);
		if (method != null) {
			// Treat it like a property
			Property property = new Property(type, null, method);
			TypeDescriptor typeDescriptor = new TypeDescriptor(property);
			method = ClassUtils.getInterfaceMethodIfPossible(method, type);
			this.writerCache.put(cacheKey, method);
			this.typeDescriptorCache.put(cacheKey, typeDescriptor);
			return true;
		}
		else {
			Field field = findField(name, type, target);
			if (field != null) {
				this.writerCache.put(cacheKey, field);
				this.typeDescriptorCache.put(cacheKey, new TypeDescriptor(field));
				return true;
			}
		}

		return false;
	}

	@Override
	public void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object newValue)
			throws AccessException {

		if (!this.allowWrite) {
			throw new AccessException("PropertyAccessor for property '" + name +
					"' on target [" + target + "] does not allow write operations");
		}

		Assert.state(target != null, "Target must not be null");
		Class<?> type = (target instanceof Class ? (Class<?>) target : target.getClass());

		Object possiblyConvertedNewValue = newValue;
		TypeDescriptor typeDescriptor = getTypeDescriptor(context, target, name);
		if (typeDescriptor != null) {
			try {
				possiblyConvertedNewValue = context.getTypeConverter().convertValue(
						newValue, TypeDescriptor.forObject(newValue), typeDescriptor);
			}
			catch (EvaluationException evaluationException) {
				throw new AccessException("Type conversion failure", evaluationException);
			}
		}

		PropertyCacheKey cacheKey = new PropertyCacheKey(type, name, target instanceof Class);
		Member cachedMember = this.writerCache.get(cacheKey);

		if (cachedMember == null || cachedMember instanceof Method) {
			Method method = (Method) cachedMember;
			if (method == null) {
				method = findSetterForProperty(name, type, target);
				if (method != null) {
					method = ClassUtils.getInterfaceMethodIfPossible(method, type);
					cachedMember = method;
					this.writerCache.put(cacheKey, cachedMember);
				}
			}
			if (method != null) {
				try {
					ReflectionUtils.makeAccessible(method);
					method.invoke(target, possiblyConvertedNewValue);
					return;
				}
				catch (Exception ex) {
					throw new AccessException("Unable to access property '" + name + "' through setter method", ex);
				}
			}
		}

		if (cachedMember == null || cachedMember instanceof Field) {
			Field field = (Field) cachedMember;
			if (field == null) {
				field = findField(name, type, target);
				if (field != null) {
					cachedMember = field;
					this.writerCache.put(cacheKey, cachedMember);
				}
			}
			if (field != null) {
				try {
					ReflectionUtils.makeAccessible(field);
					field.set(target, possiblyConvertedNewValue);
					return;
				}
				catch (Exception ex) {
					throw new AccessException("Unable to access field '" + name + "'", ex);
				}
			}
		}

		throw new AccessException("Neither setter method nor field found for property '" + name + "'");
	}

	/**
	 * 获取最近一次读取的调用器对。
	 * @deprecated as of 4.3.15 since it is not used within the framework anymore
	 */
	@Deprecated
	@Nullable
	public Member getLastReadInvokerPair() {
		InvokerPair lastReadInvoker = this.lastReadInvokerPair;
		return (lastReadInvoker != null ? lastReadInvoker.member : null);
	}


	@Nullable
	private TypeDescriptor getTypeDescriptor(EvaluationContext context, Object target, String name) {
		Class<?> type = (target instanceof Class ? (Class<?>) target : target.getClass());

		if (type.isArray() && name.equals("length")) {
			return TypeDescriptor.valueOf(Integer.TYPE);
		}
		PropertyCacheKey cacheKey = new PropertyCacheKey(type, name, target instanceof Class);
		TypeDescriptor typeDescriptor = this.typeDescriptorCache.get(cacheKey);
		if (typeDescriptor == null) {
			// Attempt to populate the cache entry
			try {
				if (canRead(context, target, name) || canWrite(context, target, name)) {
					typeDescriptor = this.typeDescriptorCache.get(cacheKey);
				}
			}
			catch (AccessException ex) {
				// Continue with null type descriptor
			}
		}
		return typeDescriptor;
	}

	@Nullable
	private Method findGetterForProperty(String propertyName, Class<?> clazz, Object target) {
		Method method = findGetterForProperty(propertyName, clazz, target instanceof Class);
		if (method == null && target instanceof Class) {
			method = findGetterForProperty(propertyName, target.getClass(), false);
		}
		return method;
	}

	@Nullable
	private Method findSetterForProperty(String propertyName, Class<?> clazz, Object target) {
		Method method = findSetterForProperty(propertyName, clazz, target instanceof Class);
		if (method == null && target instanceof Class) {
			method = findSetterForProperty(propertyName, target.getClass(), false);
		}
		return method;
	}

	/**
	 * 查找指定属性的 getter 方法。
	 */
	@Nullable
	protected Method findGetterForProperty(String propertyName, Class<?> clazz, boolean mustBeStatic) {
		Method method = findMethodForProperty(getPropertyMethodSuffixes(propertyName),
				"get", clazz, mustBeStatic, 0, ANY_TYPES);
		if (method == null) {
			method = findMethodForProperty(getPropertyMethodSuffixes(propertyName),
					"is", clazz, mustBeStatic, 0, BOOLEAN_TYPES);
			if (method == null) {
				// Record-style plain accessor method, e.g. name()
				method = findMethodForProperty(new String[] {propertyName},
						"", clazz, mustBeStatic, 0, ANY_TYPES);
			}
		}
		return method;
	}

	/**
	 * 查找指定属性的 setter 方法。
	 */
	@Nullable
	protected Method findSetterForProperty(String propertyName, Class<?> clazz, boolean mustBeStatic) {
		return findMethodForProperty(getPropertyMethodSuffixes(propertyName),
				"set", clazz, mustBeStatic, 1, ANY_TYPES);
	}

	@Nullable
	private Method findMethodForProperty(String[] methodSuffixes, String prefix, Class<?> clazz,
			boolean mustBeStatic, int numberOfParams, Set<Class<?>> requiredReturnTypes) {

		Method[] methods = getSortedMethods(clazz);
		for (String methodSuffix : methodSuffixes) {
			for (Method method : methods) {
				if (isCandidateForProperty(method, clazz) && method.getName().equals(prefix + methodSuffix) &&
						method.getParameterCount() == numberOfParams &&
						(!mustBeStatic || Modifier.isStatic(method.getModifiers())) &&
						(requiredReturnTypes.isEmpty() || requiredReturnTypes.contains(method.getReturnType()))) {
					return method;
				}
			}
		}
		return null;
	}

	/**
	 * 返回类的方法，其中非桥接方法排在前面。
	 */
	private Method[] getSortedMethods(Class<?> clazz) {
		return this.sortedMethodsCache.computeIfAbsent(clazz, key -> {
			Method[] methods = key.getMethods();
			Arrays.sort(methods, (o1, o2) -> (o1.isBridge() == o2.isBridge() ? 0 : (o1.isBridge() ? 1 : -1)));
			return methods;
		});
	}

	/**
	 * 判断给定的 {@code Method} 是否为在给定目标类实例上进行属性访问的候选方法。<p>默认实现将任何方法
	 * 都视为候选方法，即使是 {@link Object} 基类上非用户声明的属性。
	 * @param method 要评估的 Method
	 * @param targetClass 正在被内省的具体目标类
	 * @since 4.3.15
	 */
	protected boolean isCandidateForProperty(Method method, Class<?> targetClass) {
		return true;
	}

	/**
	 * 返回给定属性名的方法后缀。默认实现使用 JavaBean 约定，并额外支持形如 'xY' 的属性，此时优先使用方
	 * 法 'getXY()'，而不是 JavaBean 约定的 'getxY()'。
	 */
	protected String[] getPropertyMethodSuffixes(String propertyName) {
		String suffix = getPropertyMethodSuffix(propertyName);
		if (suffix.length() > 0 && Character.isUpperCase(suffix.charAt(0))) {
			return new String[] {suffix};
		}
		return new String[] {suffix, StringUtils.capitalize(suffix)};
	}

	/**
	 * 返回给定属性名的方法后缀。默认实现使用 JavaBean 约定。
	 */
	protected String getPropertyMethodSuffix(String propertyName) {
		if (propertyName.length() > 1 && Character.isUpperCase(propertyName.charAt(1))) {
			return propertyName;
		}
		return StringUtils.capitalize(propertyName);
	}

	@Nullable
	private Field findField(String name, Class<?> clazz, Object target) {
		Field field = findField(name, clazz, target instanceof Class);
		if (field == null && target instanceof Class) {
			field = findField(name, target.getClass(), false);
		}
		return field;
	}

	/**
	 * 在指定类上查找具有特定名称的字段。
	 */
	@Nullable
	protected Field findField(String name, Class<?> clazz, boolean mustBeStatic) {
		Field[] fields = clazz.getFields();
		for (Field field : fields) {
			if (field.getName().equals(name) && (!mustBeStatic || Modifier.isStatic(field.getModifiers()))) {
				return field;
			}
		}
		// We'll search superclasses and implemented interfaces explicitly,
		// although it shouldn't be necessary - however, see SPR-10125.
		if (clazz.getSuperclass() != null) {
			Field field = findField(name, clazz.getSuperclass(), mustBeStatic);
			if (field != null) {
				return field;
			}
		}
		for (Class<?> implementedInterface : clazz.getInterfaces()) {
			Field field = findField(name, implementedInterface, mustBeStatic);
			if (field != null) {
				return field;
			}
		}
		return null;
	}

	/**
	 * 尝试为特定类上的特定属性创建定制的优化属性访问器。通用的 ReflectivePropertyAccessor 总是可用，但
	 * 由于每次调用 read() 时都需要查找要使用的反射成员（方法/字段），因此并非最优。如果无法构建更优的
	 * 访问器，此方法只会返回 ReflectivePropertyAccessor 实例。<p>注意：优化访问器目前仅可用于读取操作。
	 * 如果需要读写访问器，请不要调用此方法。
	 * @see OptimalPropertyAccessor
	 */
	public PropertyAccessor createOptimalAccessor(EvaluationContext context, @Nullable Object target, String name) {
		// Don't be clever for arrays or a null target...
		if (target == null) {
			return this;
		}
		Class<?> type = (target instanceof Class ? (Class<?>) target : target.getClass());
		if (type.isArray()) {
			return this;
		}

		PropertyCacheKey cacheKey = new PropertyCacheKey(type, name, target instanceof Class);
		InvokerPair invocationTarget = this.readerCache.get(cacheKey);

		if (invocationTarget == null || invocationTarget.member instanceof Method) {
			Method method = (Method) (invocationTarget != null ? invocationTarget.member : null);
			if (method == null) {
				method = findGetterForProperty(name, type, target);
				if (method != null) {
					TypeDescriptor typeDescriptor = new TypeDescriptor(new MethodParameter(method, -1));
					method = ClassUtils.getInterfaceMethodIfPossible(method, type);
					invocationTarget = new InvokerPair(method, typeDescriptor);
					ReflectionUtils.makeAccessible(method);
					this.readerCache.put(cacheKey, invocationTarget);
				}
			}
			if (method != null) {
				return new OptimalPropertyAccessor(invocationTarget);
			}
		}

		if (invocationTarget == null || invocationTarget.member instanceof Field) {
			Field field = (invocationTarget != null ? (Field) invocationTarget.member : null);
			if (field == null) {
				field = findField(name, type, target instanceof Class);
				if (field != null) {
					invocationTarget = new InvokerPair(field, new TypeDescriptor(field));
					ReflectionUtils.makeAccessible(field);
					this.readerCache.put(cacheKey, invocationTarget);
				}
			}
			if (field != null) {
				return new OptimalPropertyAccessor(invocationTarget);
			}
		}

		return this;
	}


	/**
	 * 封装用于通过反射调用以访问属性值的成员（方法/字段），以及该反射调用所返回值的类型描述符。
	 */
	private static class InvokerPair {

		final Member member;

		final TypeDescriptor typeDescriptor;

		public InvokerPair(Member member, TypeDescriptor typeDescriptor) {
			this.member = member;
			this.typeDescriptor = typeDescriptor;
		}
	}


	private static final class PropertyCacheKey implements Comparable<PropertyCacheKey> {

		private final Class<?> clazz;

		private final String property;

		private final boolean targetIsClass;

		public PropertyCacheKey(Class<?> clazz, String name, boolean targetIsClass) {
			this.clazz = clazz;
			this.property = name;
			this.targetIsClass = targetIsClass;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof PropertyCacheKey)) {
				return false;
			}
			PropertyCacheKey otherKey = (PropertyCacheKey) other;
			return (this.clazz == otherKey.clazz && this.property.equals(otherKey.property) &&
					this.targetIsClass == otherKey.targetIsClass);
		}

		@Override
		public int hashCode() {
			return (this.clazz.hashCode() * 29 + this.property.hashCode());
		}

		@Override
		public String toString() {
			return "PropertyCacheKey [clazz=" + this.clazz.getName() + ", property=" + this.property +
					", targetIsClass=" + this.targetIsClass + "]";
		}

		@Override
		public int compareTo(PropertyCacheKey other) {
			int result = this.clazz.getName().compareTo(other.clazz.getName());
			if (result == 0) {
				result = this.property.compareTo(other.property);
			}
			return result;
		}
	}


	/**
	 * PropertyAccessor 的一种优化形式，它使用反射，但只知道如何访问特定类上的特定属性。这与通用的
	 * ReflectivePropertyResolver 不同，后者管理着可在不同类上调用以访问不同属性的方法和字段缓存。之所
	 * 以存在这种优化访问器，是因为每次读取时按类/名称查找相应的反射对象并不便宜。
	 */
	public static class OptimalPropertyAccessor implements CompilablePropertyAccessor {

		/**
		 * 被访问的成员。
		 */
		public final Member member;

		private final TypeDescriptor typeDescriptor;

		OptimalPropertyAccessor(InvokerPair target) {
			this.member = target.member;
			this.typeDescriptor = target.typeDescriptor;
		}

		@Override
		@Nullable
		public Class<?>[] getSpecificTargetClasses() {
			throw new UnsupportedOperationException("Should not be called on an OptimalPropertyAccessor");
		}

		@Override
		public boolean canRead(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
			if (target == null) {
				return false;
			}
			Class<?> type = (target instanceof Class ? (Class<?>) target : target.getClass());
			if (type.isArray()) {
				return false;
			}

			if (this.member instanceof Method) {
				Method method = (Method) this.member;
				String getterName = "get" + StringUtils.capitalize(name);
				if (getterName.equals(method.getName())) {
					return true;
				}
				getterName = "is" + StringUtils.capitalize(name);
				if (getterName.equals(method.getName())) {
					return true;
				}
			}
			return this.member.getName().equals(name);
		}

		@Override
		public TypedValue read(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
			if (this.member instanceof Method) {
				Method method = (Method) this.member;
				try {
					ReflectionUtils.makeAccessible(method);
					Object value = method.invoke(target);
					return new TypedValue(value, this.typeDescriptor.narrow(value));
				}
				catch (Exception ex) {
					throw new AccessException("Unable to access property '" + name + "' through getter method", ex);
				}
			}
			else {
				Field field = (Field) this.member;
				try {
					ReflectionUtils.makeAccessible(field);
					Object value = field.get(target);
					return new TypedValue(value, this.typeDescriptor.narrow(value));
				}
				catch (Exception ex) {
					throw new AccessException("Unable to access field '" + name + "'", ex);
				}
			}
		}

		@Override
		public boolean canWrite(EvaluationContext context, @Nullable Object target, String name) {
			throw new UnsupportedOperationException("Should not be called on an OptimalPropertyAccessor");
		}

		@Override
		public void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object newValue) {
			throw new UnsupportedOperationException("Should not be called on an OptimalPropertyAccessor");
		}

		@Override
		public boolean isCompilable() {
			return (Modifier.isPublic(this.member.getModifiers()) &&
					Modifier.isPublic(this.member.getDeclaringClass().getModifiers()));
		}

		@Override
		public Class<?> getPropertyType() {
			if (this.member instanceof Method) {
				return ((Method) this.member).getReturnType();
			}
			else {
				return ((Field) this.member).getType();
			}
		}

		@Override
		public void generateCode(String propertyName, MethodVisitor mv, CodeFlow cf) {
			boolean isStatic = Modifier.isStatic(this.member.getModifiers());
			String descriptor = cf.lastDescriptor();
			String classDesc = this.member.getDeclaringClass().getName().replace('.', '/');

			if (!isStatic) {
				if (descriptor == null) {
					cf.loadTarget(mv);
				}
				if (descriptor == null || !classDesc.equals(descriptor.substring(1))) {
					mv.visitTypeInsn(CHECKCAST, classDesc);
				}
			}
			else {
				if (descriptor != null) {
					// A static field/method call will not consume what is on the stack,
					// it needs to be popped off.
					mv.visitInsn(POP);
				}
			}

			if (this.member instanceof Method) {
				Method method = (Method) this.member;
				boolean isInterface = method.getDeclaringClass().isInterface();
				int opcode = (isStatic ? INVOKESTATIC : isInterface ? INVOKEINTERFACE : INVOKEVIRTUAL);
				mv.visitMethodInsn(opcode, classDesc, method.getName(),
						CodeFlow.createSignatureDescriptor(method), isInterface);
			}
			else {
				mv.visitFieldInsn((isStatic ? GETSTATIC : GETFIELD), classDesc, this.member.getName(),
						CodeFlow.toJvmDescriptor(((Field) this.member).getType()));
			}
		}
	}

}
