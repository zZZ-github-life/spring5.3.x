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

package org.springframework.beans;

import java.beans.PropertyChangeEvent;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.CollectionFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 一个基本的 {@link ConfigurablePropertyAccessor}，为所有典型用例提供所需的基础设施。
 *
 * <p>该访问器会在必要时将集合和数组值转换为对应的目标集合或数组。处理集合或数组的自定义属性编辑器，
 * 既可以通过 PropertyEditor 的 {@code setValue} 编写，也可以针对逗号分隔的 String 通过
 * {@code setAsText} 编写，因为当数组本身不可赋值时，String 数组会按这种格式进行转换。
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Rod Johnson
 * @author Rob Harrop
 * @since 4.2
 * @see #registerCustomEditor
 * @see #setPropertyValues
 * @see #setPropertyValue
 * @see #getPropertyValue
 * @see #getPropertyType
 * @see BeanWrapper
 * @see PropertyEditorRegistrySupport
 */
public abstract class AbstractNestablePropertyAccessor extends AbstractPropertyAccessor {

	/**
	 * 我们会创建大量此类对象，因此不希望每次都新建一个 logger。
	 */
	private static final Log logger = LogFactory.getLog(AbstractNestablePropertyAccessor.class);

	private int autoGrowCollectionLimit = Integer.MAX_VALUE;

	@Nullable
	Object wrappedObject;

	private String nestedPath = "";

	@Nullable
	Object rootObject;

	/** 缓存嵌套 Accessor 的 Map：嵌套路径 -> Accessor 实例。 */
	@Nullable
	private Map<String, AbstractNestablePropertyAccessor> nestedPropertyAccessors;


	/**
	 * 创建一个新的空访问器。之后需要设置被包装的实例。会注册默认编辑器。
	 * @see #setWrappedInstance
	 */
	protected AbstractNestablePropertyAccessor() {
		this(true);
	}

	/**
	 * 创建一个新的空访问器。之后需要设置被包装的实例。
	 * @param registerDefaultEditors 是否注册默认编辑器
	 * （如果该访问器不需要任何类型转换，可以禁用）
	 * @see #setWrappedInstance
	 */
	protected AbstractNestablePropertyAccessor(boolean registerDefaultEditors) {
		if (registerDefaultEditors) {
			registerDefaultEditors();
		}
		this.typeConverterDelegate = new TypeConverterDelegate(this);
	}

	/**
	 * 为给定对象创建一个新的访问器。
	 * @param object 该访问器所包装的对象
	 */
	protected AbstractNestablePropertyAccessor(Object object) {
		registerDefaultEditors();
		setWrappedInstance(object);
	}

	/**
	 * 创建一个新的访问器，包装指定类的新实例。
	 * @param clazz 要实例化并包装的类
	 */
	protected AbstractNestablePropertyAccessor(Class<?> clazz) {
		registerDefaultEditors();
		setWrappedInstance(BeanUtils.instantiateClass(clazz));
	}

	/**
	 * 为给定对象创建一个新的访问器，并注册该对象所处的嵌套路径。
	 * @param object 该访问器所包装的对象
	 * @param nestedPath 该对象的嵌套路径
	 * @param rootObject 路径顶部的根对象
	 */
	protected AbstractNestablePropertyAccessor(Object object, String nestedPath, Object rootObject) {
		registerDefaultEditors();
		setWrappedInstance(object, nestedPath, rootObject);
	}

	/**
	 * 为给定对象创建一个新的访问器，并注册该对象所处的嵌套路径。
	 * @param object 该访问器所包装的对象
	 * @param nestedPath 该对象的嵌套路径
	 * @param parent 包含该对象的访问器（不能为 {@code null}）
	 */
	protected AbstractNestablePropertyAccessor(Object object, String nestedPath, AbstractNestablePropertyAccessor parent) {
		setWrappedInstance(object, nestedPath, parent.getWrappedInstance());
		setExtractOldValueForEditor(parent.isExtractOldValueForEditor());
		setAutoGrowNestedPaths(parent.isAutoGrowNestedPaths());
		setAutoGrowCollectionLimit(parent.getAutoGrowCollectionLimit());
		setConversionService(parent.getConversionService());
	}


	/**
	 * 指定数组和集合自动增长的限值。<p>在普通访问器上默认不限制。
	 */
	public void setAutoGrowCollectionLimit(int autoGrowCollectionLimit) {
		this.autoGrowCollectionLimit = autoGrowCollectionLimit;
	}

	/**
	 * 返回数组和集合自动增长的限值。
	 */
	public int getAutoGrowCollectionLimit() {
		return this.autoGrowCollectionLimit;
	}

	/**
	 * 切换目标对象，仅当新对象的类与被替换对象的类不同时，才替换缓存的内省结果。
	 * @param object 新的目标对象
	 */
	public void setWrappedInstance(Object object) {
		setWrappedInstance(object, "", null);
	}

	/**
	 * 切换目标对象，仅当新对象的类与被替换对象的类不同时，才替换缓存的内省结果。
	 * @param object 新的目标对象
	 * @param nestedPath 该对象的嵌套路径
	 * @param rootObject 路径顶部的根对象
	 */
	public void setWrappedInstance(Object object, @Nullable String nestedPath, @Nullable Object rootObject) {
		this.wrappedObject = ObjectUtils.unwrapOptional(object);
		Assert.notNull(this.wrappedObject, "Target object must not be null");
		this.nestedPath = (nestedPath != null ? nestedPath : "");
		this.rootObject = (!this.nestedPath.isEmpty() ? rootObject : this.wrappedObject);
		this.nestedPropertyAccessors = null;
		this.typeConverterDelegate = new TypeConverterDelegate(this, this.wrappedObject);
	}

	public final Object getWrappedInstance() {
		Assert.state(this.wrappedObject != null, "No wrapped object");
		return this.wrappedObject;
	}

	public final Class<?> getWrappedClass() {
		return getWrappedInstance().getClass();
	}

	/**
	 * 返回此访问器所包装对象的嵌套路径。
	 */
	public final String getNestedPath() {
		return this.nestedPath;
	}

	/**
	 * 返回此访问器路径顶部的根对象。
	 * @see #getNestedPath
	 */
	public final Object getRootInstance() {
		Assert.state(this.rootObject != null, "No root object");
		return this.rootObject;
	}

	/**
	 * 返回此访问器路径顶部根对象的类。
	 * @see #getNestedPath
	 */
	public final Class<?> getRootClass() {
		return getRootInstance().getClass();
	}

	@Override
	public void setPropertyValue(String propertyName, @Nullable Object value) throws BeansException {
		AbstractNestablePropertyAccessor nestedPa;
		try {
			nestedPa = getPropertyAccessorForPropertyPath(propertyName);
		}
		catch (NotReadablePropertyException ex) {
			throw new NotWritablePropertyException(getRootClass(), this.nestedPath + propertyName,
					"Nested property in path '" + propertyName + "' does not exist", ex);
		}
		PropertyTokenHolder tokens = getPropertyNameTokens(getFinalPath(nestedPa, propertyName));
		nestedPa.setPropertyValue(tokens, new PropertyValue(propertyName, value));
	}

	@Override
	public void setPropertyValue(PropertyValue pv) throws BeansException {
		PropertyTokenHolder tokens = (PropertyTokenHolder) pv.resolvedTokens;
		if (tokens == null) {
			String propertyName = pv.getName();
			AbstractNestablePropertyAccessor nestedPa;
			try {
				nestedPa = getPropertyAccessorForPropertyPath(propertyName);
			}
			catch (NotReadablePropertyException ex) {
				throw new NotWritablePropertyException(getRootClass(), this.nestedPath + propertyName,
						"Nested property in path '" + propertyName + "' does not exist", ex);
			}
			tokens = getPropertyNameTokens(getFinalPath(nestedPa, propertyName));
			if (nestedPa == this) {
				pv.getOriginalPropertyValue().resolvedTokens = tokens;
			}
			nestedPa.setPropertyValue(tokens, pv);
		}
		else {
			setPropertyValue(tokens, pv);
		}
	}

	protected void setPropertyValue(PropertyTokenHolder tokens, PropertyValue pv) throws BeansException {
		if (tokens.keys != null) {
			processKeyedProperty(tokens, pv);
		}
		else {
			processLocalProperty(tokens, pv);
		}
	}

	@SuppressWarnings("unchecked")
	private void processKeyedProperty(PropertyTokenHolder tokens, PropertyValue pv) {
		Object propValue = getPropertyHoldingValue(tokens);
		PropertyHandler ph = getLocalPropertyHandler(tokens.actualName);
		if (ph == null) {
			throw new InvalidPropertyException(
					getRootClass(), this.nestedPath + tokens.actualName, "No property handler found");
		}
		Assert.state(tokens.keys != null, "No token keys");
		String lastKey = tokens.keys[tokens.keys.length - 1];

		if (propValue.getClass().isArray()) {
			Class<?> requiredType = propValue.getClass().getComponentType();
			int arrayIndex = Integer.parseInt(lastKey);
			Object oldValue = null;
			try {
				if (isExtractOldValueForEditor() && arrayIndex < Array.getLength(propValue)) {
					oldValue = Array.get(propValue, arrayIndex);
				}
				Object convertedValue = convertIfNecessary(tokens.canonicalName, oldValue, pv.getValue(),
						requiredType, ph.nested(tokens.keys.length));
				int length = Array.getLength(propValue);
				if (arrayIndex >= length && arrayIndex < this.autoGrowCollectionLimit) {
					Class<?> componentType = propValue.getClass().getComponentType();
					Object newArray = Array.newInstance(componentType, arrayIndex + 1);
					System.arraycopy(propValue, 0, newArray, 0, length);
					int lastKeyIndex = tokens.canonicalName.lastIndexOf('[');
					String propName = tokens.canonicalName.substring(0, lastKeyIndex);
					setPropertyValue(propName, newArray);
					propValue = getPropertyValue(propName);
				}
				Array.set(propValue, arrayIndex, convertedValue);
			}
			catch (IndexOutOfBoundsException ex) {
				throw new InvalidPropertyException(getRootClass(), this.nestedPath + tokens.canonicalName,
						"Invalid array index in property path '" + tokens.canonicalName + "'", ex);
			}
		}

		else if (propValue instanceof List) {
			Class<?> requiredType = ph.getCollectionType(tokens.keys.length);
			List<Object> list = (List<Object>) propValue;
			int index = Integer.parseInt(lastKey);
			Object oldValue = null;
			if (isExtractOldValueForEditor() && index < list.size()) {
				oldValue = list.get(index);
			}
			Object convertedValue = convertIfNecessary(tokens.canonicalName, oldValue, pv.getValue(),
					requiredType, ph.nested(tokens.keys.length));
			int size = list.size();
			if (index >= size && index < this.autoGrowCollectionLimit) {
				for (int i = size; i < index; i++) {
					try {
						list.add(null);
					}
					catch (NullPointerException ex) {
						throw new InvalidPropertyException(getRootClass(), this.nestedPath + tokens.canonicalName,
								"Cannot set element with index " + index + " in List of size " +
								size + ", accessed using property path '" + tokens.canonicalName +
								"': List does not support filling up gaps with null elements");
					}
				}
				list.add(convertedValue);
			}
			else {
				try {
					list.set(index, convertedValue);
				}
				catch (IndexOutOfBoundsException ex) {
					throw new InvalidPropertyException(getRootClass(), this.nestedPath + tokens.canonicalName,
							"Invalid list index in property path '" + tokens.canonicalName + "'", ex);
				}
			}
		}

		else if (propValue instanceof Map) {
			Class<?> mapKeyType = ph.getMapKeyType(tokens.keys.length);
			Class<?> mapValueType = ph.getMapValueType(tokens.keys.length);
			Map<Object, Object> map = (Map<Object, Object>) propValue;
			// IMPORTANT: Do not pass full property name in here - property editors
			// must not kick in for map keys but rather only for map values.
			TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(mapKeyType);
			Object convertedMapKey = convertIfNecessary(null, null, lastKey, mapKeyType, typeDescriptor);
			Object oldValue = null;
			if (isExtractOldValueForEditor()) {
				oldValue = map.get(convertedMapKey);
			}
			// Pass full property name and old value in here, since we want full
			// conversion ability for map values.
			Object convertedMapValue = convertIfNecessary(tokens.canonicalName, oldValue, pv.getValue(),
					mapValueType, ph.nested(tokens.keys.length));
			map.put(convertedMapKey, convertedMapValue);
		}

		else {
			throw new InvalidPropertyException(getRootClass(), this.nestedPath + tokens.canonicalName,
					"Property referenced in indexed property path '" + tokens.canonicalName +
					"' is neither an array nor a List nor a Map; returned value was [" + propValue + "]");
		}
	}

	private Object getPropertyHoldingValue(PropertyTokenHolder tokens) {
		// Apply indexes and map keys: fetch value for all keys but the last one.
		Assert.state(tokens.keys != null, "No token keys");
		PropertyTokenHolder getterTokens = new PropertyTokenHolder(tokens.actualName);
		getterTokens.canonicalName = tokens.canonicalName;
		getterTokens.keys = new String[tokens.keys.length - 1];
		System.arraycopy(tokens.keys, 0, getterTokens.keys, 0, tokens.keys.length - 1);

		Object propValue;
		try {
			propValue = getPropertyValue(getterTokens);
		}
		catch (NotReadablePropertyException ex) {
			throw new NotWritablePropertyException(getRootClass(), this.nestedPath + tokens.canonicalName,
					"Cannot access indexed value in property referenced " +
					"in indexed property path '" + tokens.canonicalName + "'", ex);
		}

		if (propValue == null) {
			// null map value case
			if (isAutoGrowNestedPaths()) {
				int lastKeyIndex = tokens.canonicalName.lastIndexOf('[');
				getterTokens.canonicalName = tokens.canonicalName.substring(0, lastKeyIndex);
				propValue = setDefaultValue(getterTokens);
			}
			else {
				throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + tokens.canonicalName,
						"Cannot access indexed value in property referenced " +
						"in indexed property path '" + tokens.canonicalName + "': returned null");
			}
		}
		return propValue;
	}

	private void processLocalProperty(PropertyTokenHolder tokens, PropertyValue pv) {
		PropertyHandler ph = getLocalPropertyHandler(tokens.actualName);
		if (ph == null || !ph.isWritable()) {
			if (pv.isOptional()) {
				if (logger.isDebugEnabled()) {
					logger.debug("Ignoring optional value for property '" + tokens.actualName +
							"' - property not found on bean class [" + getRootClass().getName() + "]");
				}
				return;
			}
			if (this.suppressNotWritablePropertyException) {
				// Optimization for common ignoreUnknown=true scenario since the
				// exception would be caught and swallowed higher up anyway...
				return;
			}
			throw createNotWritablePropertyException(tokens.canonicalName);
		}

		Object oldValue = null;
		try {
			Object originalValue = pv.getValue();
			Object valueToApply = originalValue;
			if (!Boolean.FALSE.equals(pv.conversionNecessary)) {
				if (pv.isConverted()) {
					valueToApply = pv.getConvertedValue();
				}
				else {
					if (isExtractOldValueForEditor() && ph.isReadable()) {
						try {
							oldValue = ph.getValue();
						}
						catch (Exception ex) {
							if (ex instanceof PrivilegedActionException) {
								ex = ((PrivilegedActionException) ex).getException();
							}
							if (logger.isDebugEnabled()) {
								logger.debug("Could not read previous value of property '" +
										this.nestedPath + tokens.canonicalName + "'", ex);
							}
						}
					}
					valueToApply = convertForProperty(
							tokens.canonicalName, oldValue, originalValue, ph.toTypeDescriptor());
				}
				pv.getOriginalPropertyValue().conversionNecessary = (valueToApply != originalValue);
			}
			ph.setValue(valueToApply);
		}
		catch (TypeMismatchException ex) {
			throw ex;
		}
		catch (InvocationTargetException ex) {
			PropertyChangeEvent propertyChangeEvent = new PropertyChangeEvent(
					getRootInstance(), this.nestedPath + tokens.canonicalName, oldValue, pv.getValue());
			if (ex.getTargetException() instanceof ClassCastException) {
				throw new TypeMismatchException(propertyChangeEvent, ph.getPropertyType(), ex.getTargetException());
			}
			else {
				Throwable cause = ex.getTargetException();
				if (cause instanceof UndeclaredThrowableException) {
					// May happen e.g. with Groovy-generated methods
					cause = cause.getCause();
				}
				throw new MethodInvocationException(propertyChangeEvent, cause);
			}
		}
		catch (Exception ex) {
			PropertyChangeEvent pce = new PropertyChangeEvent(
					getRootInstance(), this.nestedPath + tokens.canonicalName, oldValue, pv.getValue());
			throw new MethodInvocationException(pce, ex);
		}
	}

	@Override
	@Nullable
	public Class<?> getPropertyType(String propertyName) throws BeansException {
		try {
			PropertyHandler ph = getPropertyHandler(propertyName);
			if (ph != null) {
				return ph.getPropertyType();
			}
			else {
				// Maybe an indexed/mapped property...
				Object value = getPropertyValue(propertyName);
				if (value != null) {
					return value.getClass();
				}
				// Check to see if there is a custom editor,
				// which might give an indication on the desired target type.
				Class<?> editorType = guessPropertyTypeFromEditors(propertyName);
				if (editorType != null) {
					return editorType;
				}
			}
		}
		catch (InvalidPropertyException ex) {
			// Consider as not determinable.
		}
		return null;
	}

	@Override
	@Nullable
	public TypeDescriptor getPropertyTypeDescriptor(String propertyName) throws BeansException {
		try {
			AbstractNestablePropertyAccessor nestedPa = getPropertyAccessorForPropertyPath(propertyName);
			String finalPath = getFinalPath(nestedPa, propertyName);
			PropertyTokenHolder tokens = getPropertyNameTokens(finalPath);
			PropertyHandler ph = nestedPa.getLocalPropertyHandler(tokens.actualName);
			if (ph != null) {
				if (tokens.keys != null) {
					if (ph.isReadable() || ph.isWritable()) {
						return ph.nested(tokens.keys.length);
					}
				}
				else {
					if (ph.isReadable() || ph.isWritable()) {
						return ph.toTypeDescriptor();
					}
				}
			}
		}
		catch (InvalidPropertyException ex) {
			// Consider as not determinable.
		}
		return null;
	}

	@Override
	public boolean isReadableProperty(String propertyName) {
		try {
			PropertyHandler ph = getPropertyHandler(propertyName);
			if (ph != null) {
				return ph.isReadable();
			}
			else {
				// Maybe an indexed/mapped property...
				getPropertyValue(propertyName);
				return true;
			}
		}
		catch (InvalidPropertyException ex) {
			// Cannot be evaluated, so can't be readable.
		}
		return false;
	}

	@Override
	public boolean isWritableProperty(String propertyName) {
		try {
			PropertyHandler ph = getPropertyHandler(propertyName);
			if (ph != null) {
				return ph.isWritable();
			}
			else {
				// Maybe an indexed/mapped property...
				getPropertyValue(propertyName);
				return true;
			}
		}
		catch (InvalidPropertyException ex) {
			// Cannot be evaluated, so can't be writable.
		}
		return false;
	}

	@Nullable
	private Object convertIfNecessary(@Nullable String propertyName, @Nullable Object oldValue,
			@Nullable Object newValue, @Nullable Class<?> requiredType, @Nullable TypeDescriptor td)
			throws TypeMismatchException {

		Assert.state(this.typeConverterDelegate != null, "No TypeConverterDelegate");
		try {
			return this.typeConverterDelegate.convertIfNecessary(propertyName, oldValue, newValue, requiredType, td);
		}
		catch (ConverterNotFoundException | IllegalStateException ex) {
			PropertyChangeEvent pce =
					new PropertyChangeEvent(getRootInstance(), this.nestedPath + propertyName, oldValue, newValue);
			throw new ConversionNotSupportedException(pce, requiredType, ex);
		}
		catch (ConversionException | IllegalArgumentException ex) {
			PropertyChangeEvent pce =
					new PropertyChangeEvent(getRootInstance(), this.nestedPath + propertyName, oldValue, newValue);
			throw new TypeMismatchException(pce, requiredType, ex);
		}
	}

	@Nullable
	protected Object convertForProperty(
			String propertyName, @Nullable Object oldValue, @Nullable Object newValue, TypeDescriptor td)
			throws TypeMismatchException {

		return convertIfNecessary(propertyName, oldValue, newValue, td.getType(), td);
	}

	@Override
	@Nullable
	public Object getPropertyValue(String propertyName) throws BeansException {
		AbstractNestablePropertyAccessor nestedPa = getPropertyAccessorForPropertyPath(propertyName);
		PropertyTokenHolder tokens = getPropertyNameTokens(getFinalPath(nestedPa, propertyName));
		return nestedPa.getPropertyValue(tokens);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	protected Object getPropertyValue(PropertyTokenHolder tokens) throws BeansException {
		String propertyName = tokens.canonicalName;
		String actualName = tokens.actualName;
		PropertyHandler ph = getLocalPropertyHandler(actualName);
		if (ph == null || !ph.isReadable()) {
			throw new NotReadablePropertyException(getRootClass(), this.nestedPath + propertyName);
		}
		try {
			Object value = ph.getValue();
			if (tokens.keys != null) {
				if (value == null) {
					if (isAutoGrowNestedPaths()) {
						value = setDefaultValue(new PropertyTokenHolder(tokens.actualName));
					}
					else {
						throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + propertyName,
								"Cannot access indexed value of property referenced in indexed " +
										"property path '" + propertyName + "': returned null");
					}
				}
				StringBuilder indexedPropertyName = new StringBuilder(tokens.actualName);
				// apply indexes and map keys
				for (int i = 0; i < tokens.keys.length; i++) {
					String key = tokens.keys[i];
					if (value == null) {
						throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + propertyName,
								"Cannot access indexed value of property referenced in indexed " +
										"property path '" + propertyName + "': returned null");
					}
					else if (value.getClass().isArray()) {
						int index = Integer.parseInt(key);
						value = growArrayIfNecessary(value, index, indexedPropertyName.toString());
						value = Array.get(value, index);
					}
					else if (value instanceof List) {
						int index = Integer.parseInt(key);
						List<Object> list = (List<Object>) value;
						growCollectionIfNecessary(list, index, indexedPropertyName.toString(), ph, i + 1);
						value = list.get(index);
					}
					else if (value instanceof Set) {
						// Apply index to Iterator in case of a Set.
						Set<Object> set = (Set<Object>) value;
						int index = Integer.parseInt(key);
						if (index < 0 || index >= set.size()) {
							throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
									"Cannot get element with index " + index + " from Set of size " +
											set.size() + ", accessed using property path '" + propertyName + "'");
						}
						Iterator<Object> it = set.iterator();
						for (int j = 0; it.hasNext(); j++) {
							Object elem = it.next();
							if (j == index) {
								value = elem;
								break;
							}
						}
					}
					else if (value instanceof Map) {
						Map<Object, Object> map = (Map<Object, Object>) value;
						Class<?> mapKeyType = ph.getResolvableType().getNested(i + 1).asMap().resolveGeneric(0);
						// IMPORTANT: Do not pass full property name in here - property editors
						// must not kick in for map keys but rather only for map values.
						TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(mapKeyType);
						Object convertedMapKey = convertIfNecessary(null, null, key, mapKeyType, typeDescriptor);
						value = map.get(convertedMapKey);
					}
					else {
						throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
								"Property referenced in indexed property path '" + propertyName +
										"' is neither an array nor a List nor a Set nor a Map; returned value was [" + value + "]");
					}
					indexedPropertyName.append(PROPERTY_KEY_PREFIX).append(key).append(PROPERTY_KEY_SUFFIX);
				}
			}
			return value;
		}
		catch (IndexOutOfBoundsException ex) {
			throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
					"Index of out of bounds in property path '" + propertyName + "'", ex);
		}
		catch (NumberFormatException | TypeMismatchException ex) {
			throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
					"Invalid index in property path '" + propertyName + "'", ex);
		}
		catch (InvocationTargetException ex) {
			throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
					"Getter for property '" + actualName + "' threw exception", ex);
		}
		catch (Exception ex) {
			throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
					"Illegal attempt to get property '" + actualName + "' threw exception", ex);
		}
	}


	/**
	 * 返回指定 {@code propertyName} 的 {@link PropertyHandler}，必要时进行导航。如果未找到则返回
	 * {@code null}，而不是抛出异常。
	 * @param propertyName 要获取其描述符的属性
	 * @return 指定属性的属性描述符，
	 * 如果未找到则为 {@code null}
	 * @throws BeansException 如果内省失败
	 */
	@Nullable
	protected PropertyHandler getPropertyHandler(String propertyName) throws BeansException {
		Assert.notNull(propertyName, "Property name must not be null");
		AbstractNestablePropertyAccessor nestedPa = getPropertyAccessorForPropertyPath(propertyName);
		return nestedPa.getLocalPropertyHandler(getFinalPath(nestedPa, propertyName));
	}

	/**
	 * 返回指定本地 {@code propertyName} 的 {@link PropertyHandler}。仅用于访问当前上下文中可用的属性。
	 * @param propertyName 本地属性的名称
	 * @return 该属性的处理器，如果尚未找到则为 {@code null}
	 */
	@Nullable
	protected abstract PropertyHandler getLocalPropertyHandler(String propertyName);

	/**
	 * 创建一个新的嵌套属性访问器实例。可在子类中覆盖，以创建 PropertyAccessor 的子类。
	 * @param object 该 PropertyAccessor 所包装的对象
	 * @param nestedPath 该对象的嵌套路径
	 * @return 嵌套的 PropertyAccessor 实例
	 */
	protected abstract AbstractNestablePropertyAccessor newNestedPropertyAccessor(Object object, String nestedPath);

	/**
	 * 为指定属性创建一个 {@link NotWritablePropertyException}。
	 */
	protected abstract NotWritablePropertyException createNotWritablePropertyException(String propertyName);


	private Object growArrayIfNecessary(Object array, int index, String name) {
		if (!isAutoGrowNestedPaths()) {
			return array;
		}
		int length = Array.getLength(array);
		if (index >= length && index < this.autoGrowCollectionLimit) {
			Class<?> componentType = array.getClass().getComponentType();
			Object newArray = Array.newInstance(componentType, index + 1);
			System.arraycopy(array, 0, newArray, 0, length);
			for (int i = length; i < Array.getLength(newArray); i++) {
				Array.set(newArray, i, newValue(componentType, null, name));
			}
			setPropertyValue(name, newArray);
			Object defaultValue = getPropertyValue(name);
			Assert.state(defaultValue != null, "Default value must not be null");
			return defaultValue;
		}
		else {
			return array;
		}
	}

	private void growCollectionIfNecessary(Collection<Object> collection, int index, String name,
			PropertyHandler ph, int nestingLevel) {

		if (!isAutoGrowNestedPaths()) {
			return;
		}
		int size = collection.size();
		if (index >= size && index < this.autoGrowCollectionLimit) {
			Class<?> elementType = ph.getResolvableType().getNested(nestingLevel).asCollection().resolveGeneric();
			if (elementType != null) {
				for (int i = collection.size(); i < index + 1; i++) {
					collection.add(newValue(elementType, null, name));
				}
			}
		}
	}

	/**
	 * 获取路径的最后一个组成部分。即使不是嵌套的也适用。
	 * @param pa 要操作的属性访问器
	 * @param nestedPath 已知为嵌套的属性路径
	 * @return 路径的最后一个组成部分（目标 Bean 上的属性）
	 */
	protected String getFinalPath(AbstractNestablePropertyAccessor pa, String nestedPath) {
		if (pa == this) {
			return nestedPath;
		}
		return nestedPath.substring(PropertyAccessorUtils.getLastNestedPropertySeparatorIndex(nestedPath) + 1);
	}

	/**
	 * 递归导航，返回嵌套属性路径的属性访问器。
	 * @param propertyPath 属性路径，可能是嵌套的
	 * @return 目标 Bean 的属性访问器
	 */
	protected AbstractNestablePropertyAccessor getPropertyAccessorForPropertyPath(String propertyPath) {
		int pos = PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex(propertyPath);
		// Handle nested properties recursively.
		if (pos > -1) {
			String nestedProperty = propertyPath.substring(0, pos);
			String nestedPath = propertyPath.substring(pos + 1);
			AbstractNestablePropertyAccessor nestedPa = getNestedPropertyAccessor(nestedProperty);
			return nestedPa.getPropertyAccessorForPropertyPath(nestedPath);
		}
		else {
			return this;
		}
	}

	/**
	 * 获取给定嵌套属性的 Property 访问器。如果缓存中未找到，则创建一个新的。<p>注意：现在必须缓存嵌套
	 * 的 PropertyAccessor，以保留为嵌套属性注册的自定义编辑器。
	 * @param nestedProperty 要为其创建 PropertyAccessor 的属性
	 * @return PropertyAccessor 实例，可能是缓存的或新建的
	 */
	private AbstractNestablePropertyAccessor getNestedPropertyAccessor(String nestedProperty) {
		if (this.nestedPropertyAccessors == null) {
			this.nestedPropertyAccessors = new HashMap<>();
		}
		// Get value of bean property.
		PropertyTokenHolder tokens = getPropertyNameTokens(nestedProperty);
		String canonicalName = tokens.canonicalName;
		Object value = getPropertyValue(tokens);
		if (value == null || (value instanceof Optional && !((Optional<?>) value).isPresent())) {
			if (isAutoGrowNestedPaths()) {
				value = setDefaultValue(tokens);
			}
			else {
				throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + canonicalName);
			}
		}

		// Lookup cached sub-PropertyAccessor, create new one if not found.
		AbstractNestablePropertyAccessor nestedPa = this.nestedPropertyAccessors.get(canonicalName);
		if (nestedPa == null || nestedPa.getWrappedInstance() != ObjectUtils.unwrapOptional(value)) {
			if (logger.isTraceEnabled()) {
				logger.trace("Creating new nested " + getClass().getSimpleName() + " for property '" + canonicalName + "'");
			}
			nestedPa = newNestedPropertyAccessor(value, this.nestedPath + canonicalName + NESTED_PROPERTY_SEPARATOR);
			// Inherit all type-specific PropertyEditors.
			copyDefaultEditorsTo(nestedPa);
			copyCustomEditorsTo(nestedPa, canonicalName);
			this.nestedPropertyAccessors.put(canonicalName, nestedPa);
		}
		else {
			if (logger.isTraceEnabled()) {
				logger.trace("Using cached nested property accessor for property '" + canonicalName + "'");
			}
		}
		return nestedPa;
	}

	private Object setDefaultValue(PropertyTokenHolder tokens) {
		PropertyValue pv = createDefaultPropertyValue(tokens);
		setPropertyValue(tokens, pv);
		Object defaultValue = getPropertyValue(tokens);
		Assert.state(defaultValue != null, "Default value must not be null");
		return defaultValue;
	}

	private PropertyValue createDefaultPropertyValue(PropertyTokenHolder tokens) {
		TypeDescriptor desc = getPropertyTypeDescriptor(tokens.canonicalName);
		if (desc == null) {
			throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + tokens.canonicalName,
					"Could not determine property type for auto-growing a default value");
		}
		Object defaultValue = newValue(desc.getType(), desc, tokens.canonicalName);
		return new PropertyValue(tokens.canonicalName, defaultValue);
	}

	private Object newValue(Class<?> type, @Nullable TypeDescriptor desc, String name) {
		try {
			if (type.isArray()) {
				Class<?> componentType = type.getComponentType();
				// TODO - only handles 2-dimensional arrays
				if (componentType.isArray()) {
					Object array = Array.newInstance(componentType, 1);
					Array.set(array, 0, Array.newInstance(componentType.getComponentType(), 0));
					return array;
				}
				else {
					return Array.newInstance(componentType, 0);
				}
			}
			else if (Collection.class.isAssignableFrom(type)) {
				TypeDescriptor elementDesc = (desc != null ? desc.getElementTypeDescriptor() : null);
				return CollectionFactory.createCollection(type, (elementDesc != null ? elementDesc.getType() : null), 16);
			}
			else if (Map.class.isAssignableFrom(type)) {
				TypeDescriptor keyDesc = (desc != null ? desc.getMapKeyTypeDescriptor() : null);
				return CollectionFactory.createMap(type, (keyDesc != null ? keyDesc.getType() : null), 16);
			}
			else {
				Constructor<?> ctor = type.getDeclaredConstructor();
				if (Modifier.isPrivate(ctor.getModifiers())) {
					throw new IllegalAccessException("Auto-growing not allowed with private constructor: " + ctor);
				}
				return BeanUtils.instantiateClass(ctor);
			}
		}
		catch (Throwable ex) {
			throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + name,
					"Could not instantiate property type [" + type.getName() + "] to auto-grow nested property path", ex);
		}
	}

	/**
	 * 将给定属性名解析为相应的属性名标记。
	 * @param propertyName 要解析的属性名称
	 * @return 解析后的属性标记的表示形式
	 */
	private PropertyTokenHolder getPropertyNameTokens(String propertyName) {
		String actualName = null;
		List<String> keys = new ArrayList<>(2);
		int searchIndex = 0;
		while (searchIndex != -1) {
			int keyStart = propertyName.indexOf(PROPERTY_KEY_PREFIX, searchIndex);
			searchIndex = -1;
			if (keyStart != -1) {
				int keyEnd = getPropertyNameKeyEnd(propertyName, keyStart + PROPERTY_KEY_PREFIX.length());
				if (keyEnd != -1) {
					if (actualName == null) {
						actualName = propertyName.substring(0, keyStart);
					}
					String key = propertyName.substring(keyStart + PROPERTY_KEY_PREFIX.length(), keyEnd);
					if (key.length() > 1 && (key.startsWith("'") && key.endsWith("'")) ||
							(key.startsWith("\"") && key.endsWith("\""))) {
						key = key.substring(1, key.length() - 1);
					}
					keys.add(key);
					searchIndex = keyEnd + PROPERTY_KEY_SUFFIX.length();
				}
			}
		}
		PropertyTokenHolder tokens = new PropertyTokenHolder(actualName != null ? actualName : propertyName);
		if (!keys.isEmpty()) {
			tokens.canonicalName += PROPERTY_KEY_PREFIX +
					StringUtils.collectionToDelimitedString(keys, PROPERTY_KEY_SUFFIX + PROPERTY_KEY_PREFIX) +
					PROPERTY_KEY_SUFFIX;
			tokens.keys = StringUtils.toStringArray(keys);
		}
		return tokens;
	}

	private int getPropertyNameKeyEnd(String propertyName, int startIndex) {
		int unclosedPrefixes = 0;
		int length = propertyName.length();
		for (int i = startIndex; i < length; i++) {
			switch (propertyName.charAt(i)) {
				case PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR:
					// The property name contains opening prefix(es)...
					unclosedPrefixes++;
					break;
				case PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR:
					if (unclosedPrefixes == 0) {
						// No unclosed prefix(es) in the property name (left) ->
						// this is the suffix we are looking for.
						return i;
					}
					else {
						// This suffix does not close the initial prefix but rather
						// just one that occurred within the property name.
						unclosedPrefixes--;
					}
					break;
			}
		}
		return -1;
	}


	@Override
	public String toString() {
		String className = getClass().getName();
		if (this.wrappedObject == null) {
			return className + ": no wrapped object set";
		}
		return className + ": wrapping object [" + ObjectUtils.identityToString(this.wrappedObject) + ']';
	}


	/**
	 * 特定属性的处理器。
	 */
	protected abstract static class PropertyHandler {

		private final Class<?> propertyType;

		private final boolean readable;

		private final boolean writable;

		public PropertyHandler(Class<?> propertyType, boolean readable, boolean writable) {
			this.propertyType = propertyType;
			this.readable = readable;
			this.writable = writable;
		}

		public Class<?> getPropertyType() {
			return this.propertyType;
		}

		public boolean isReadable() {
			return this.readable;
		}

		public boolean isWritable() {
			return this.writable;
		}

		public abstract TypeDescriptor toTypeDescriptor();

		public abstract ResolvableType getResolvableType();

		@Nullable
		public Class<?> getMapKeyType(int nestingLevel) {
			return getResolvableType().getNested(nestingLevel).asMap().resolveGeneric(0);
		}

		@Nullable
		public Class<?> getMapValueType(int nestingLevel) {
			return getResolvableType().getNested(nestingLevel).asMap().resolveGeneric(1);
		}

		@Nullable
		public Class<?> getCollectionType(int nestingLevel) {
			return getResolvableType().getNested(nestingLevel).asCollection().resolveGeneric();
		}

		@Nullable
		public abstract TypeDescriptor nested(int level);

		@Nullable
		public abstract Object getValue() throws Exception;

		public abstract void setValue(@Nullable Object value) throws Exception;
	}


	/**
	 * 用于存储属性标记的持有者类。
	 */
	protected static class PropertyTokenHolder {

		public PropertyTokenHolder(String name) {
			this.actualName = name;
			this.canonicalName = name;
		}

		public String actualName;

		public String canonicalName;

		@Nullable
		public String[] keys;
	}

}
