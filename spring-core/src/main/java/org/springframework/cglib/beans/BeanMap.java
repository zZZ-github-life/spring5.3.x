/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cglib.beans;

import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.asm.ClassVisitor;
import org.springframework.cglib.core.AbstractClassGenerator;
import org.springframework.cglib.core.KeyFactory;
import org.springframework.cglib.core.ReflectUtils;

/**
 * 基于 <code>Map</code> 的 JavaBean 视图。默认的键集合是所有属性名（getter 或 setter）的并集。尝试
 * 设置只读属性将被忽略，只写属性将返回 <code>null</code>。不支持移除对象（键集合是固定的）。
 * @author Chris Nokleberg
 */
@SuppressWarnings({"rawtypes", "unchecked"})
abstract public class BeanMap implements Map {
    /**
     * 将映射键集合中反映的属性限制为可读属性。
     * @see BeanMap.Generator#setRequire
     */
    public static final int REQUIRE_GETTER = 1;

    /**
     * 将映射键集合中反映的属性限制为可写属性。
     * @see BeanMap.Generator#setRequire
     */
    public static final int REQUIRE_SETTER = 2;
    
    /**
     * 用于创建新 <code>BeanMap</code> 的辅助方法。若要更精细地控制生成的实例，请使用新的 <code>
     * BeanMap.Generator</code> 实例而不是此静态方法。
     * @param bean the JavaBean underlying the map
     * @return a new <code>BeanMap</code> instance
     */
    public static BeanMap create(Object bean) {
        Generator gen = new Generator();
        gen.setBean(bean);
        return gen.create();
    }

    public static class Generator extends AbstractClassGenerator {
        private static final Source SOURCE = new Source(BeanMap.class.getName());

        private static final BeanMapKey KEY_FACTORY =
          (BeanMapKey)KeyFactory.create(BeanMapKey.class, KeyFactory.CLASS_BY_NAME);

        interface BeanMapKey {
            public Object newInstance(Class type, int require);
        }
        
        private Object bean;
        private Class beanClass;
        private int require;
        
        public Generator() {
            super(SOURCE);
        }

        /**
         * 设置生成的映射应反映的 Bean。可以使用 {@link #setBean} 将其替换为同一类型的另一个 Bean。调用此方
         * 法会覆盖之前通过 {@link #setBeanClass} 设置的值。必须在调用 {@link #create} 之前调用此方法或
         * {@link #setBeanClass}。
         * @param bean the initial bean
         */
        public void setBean(Object bean) {
            this.bean = bean;
            if (bean != null) {
				beanClass = bean.getClass();
				// SPRING PATCH BEGIN
				setContextClass(beanClass);
				// SPRING PATCH END
			}
        }

        /**
         * 设置生成的映射应支持的 Bean 类。必须在调用 {@link #create} 之前调用此方法或
         * {@link #setBeanClass}。
         * @param beanClass the class of the bean
         */
        public void setBeanClass(Class beanClass) {
            this.beanClass = beanClass;
        }

        /**
         * 限制生成的映射所反映的属性。
         * @param require any combination of {@link #REQUIRE_GETTER} and
         * {@link #REQUIRE_SETTER}; default is zero (any property allowed)
         */
        public void setRequire(int require) {
            this.require = require;
        }

        protected ClassLoader getDefaultClassLoader() {
            return beanClass.getClassLoader();
        }

        protected ProtectionDomain getProtectionDomain() {
        	return ReflectUtils.getProtectionDomain(beanClass);
        }

        /**
         * 创建 <code>BeanMap</code> 的新实例。如果可能，将复用已生成的类。
         */
        public BeanMap create() {
            if (beanClass == null)
                throw new IllegalArgumentException("Class of bean unknown");
            setNamePrefix(beanClass.getName());
            return (BeanMap)super.create(KEY_FACTORY.newInstance(beanClass, require));
        }

        public void generateClass(ClassVisitor v) throws Exception {
            new BeanMapEmitter(v, getClassName(), beanClass, require);
        }

        protected Object firstInstance(Class type) {
            return ((BeanMap)ReflectUtils.newInstance(type)).newInstance(bean);
        }

        protected Object nextInstance(Object instance) {
            return ((BeanMap)instance).newInstance(bean);
        }
    }

    /**
     * 使用指定的 Bean 创建新的 <code>BeanMap</code> 实例。这比使用 {@link #create} 静态方法更快。
     * @param bean the JavaBean underlying the map
     * @return a new <code>BeanMap</code> instance
     */
    abstract public BeanMap newInstance(Object bean);

    /**
     * 获取属性的类型。
     * @param name the name of the JavaBean property
     * @return the type of the property, or null if the property does not exist
     */
    abstract public Class getPropertyType(String name);

    protected Object bean;

    protected BeanMap() {
    }

    protected BeanMap(Object bean) {
        setBean(bean);
    }

    public Object get(Object key) {
        return get(bean, key);
    }

    public Object put(Object key, Object value) {
        return put(bean, key, value);
    }

    /**
     * 获取 Bean 的属性。这使得 <code>BeanMap</code> 可以静态地用于多个 Bean——与映射绑定的 Bean 实例会
     * 被忽略，转而使用传给此方法的 Bean。
     * @param bean the bean to query; must be compatible with the type of
     * this <code>BeanMap</code>
     * @param key must be a String
     * @return the current value, or null if there is no matching property
     */
    abstract public Object get(Object bean, Object key);

    /**
     * 设置 Bean 的属性。这使得 <code>BeanMap</code> 可以静态地用于多个 Bean——与映射绑定的 Bean 实例会
     * 被忽略，转而使用传给此方法的 Bean。
     * @param key must be a String
     * @return the old value, if there was one, or null
     */
    abstract public Object put(Object bean, Object key, Object value);

    /**
     * 更改此映射应使用的底层 Bean。
     * @param bean the new JavaBean
     * @see #getBean
     */
    public void setBean(Object bean) {
        this.bean = bean;
    }

    /**
     * 返回此映射当前使用的 Bean。
     * @return the current JavaBean
     * @see #setBean
     */
    public Object getBean() {
        return bean;
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public boolean containsKey(Object key) {
        return keySet().contains(key);
    }

    public boolean containsValue(Object value) {
        for (Iterator it = keySet().iterator(); it.hasNext();) {
            Object v = get(it.next());
            if (((value == null) && (v == null)) || (value != null && value.equals(v)))
                return true;
        }
        return false;
    }

    public int size() {
        return keySet().size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public Object remove(Object key) {
        throw new UnsupportedOperationException();
    }

    public void putAll(Map t) {
        for (Iterator it = t.keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            put(key, t.get(key));
        }
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof Map)) {
            return false;
        }
        Map other = (Map)o;
        if (size() != other.size()) {
            return false;
        }
        for (Iterator it = keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            if (!other.containsKey(key)) {
                return false;
            }
            Object v1 = get(key);
            Object v2 = other.get(key);
            if (!((v1 == null) ? v2 == null : v1.equals(v2))) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int code = 0;
        for (Iterator it = keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            Object value = get(key);
            code += ((key == null) ? 0 : key.hashCode()) ^
                ((value == null) ? 0 : value.hashCode());
        }
        return code;
    }

    // TODO: optimize
    public Set entrySet() {
        HashMap copy = new HashMap();
        for (Iterator it = keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            copy.put(key, get(key));
        }
        return Collections.unmodifiableMap(copy).entrySet();
    }

    public Collection values() {
        Set keys = keySet();
        List values = new ArrayList(keys.size());
        for (Iterator it = keys.iterator(); it.hasNext();) {
            values.add(get(it.next()));
        }
        return Collections.unmodifiableCollection(values);
    }

    /*
     * @see java.util.AbstractMap#toString
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append('{');
        for (Iterator it = keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            sb.append(key);
            sb.append('=');
            sb.append(get(key));
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append('}');
        return sb.toString();
    }
}
