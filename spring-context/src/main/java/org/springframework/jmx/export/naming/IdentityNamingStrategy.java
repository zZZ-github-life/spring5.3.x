/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jmx.export.naming;

import java.util.Hashtable;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@code ObjectNamingStrategy} 接口的实现，它基于给定实例的标识创建名称。
 *
 * <p>生成的 {@code ObjectName} 形式为 <i>package</i>:class=<i>类名</i>,hashCode=<i>标识哈希值（十六进制）</i>
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 */
public class IdentityNamingStrategy implements ObjectNamingStrategy {

	/**
	 * 类型键。
	 */
	public static final String TYPE_KEY = "type";

	/**
	 * 哈希码键。
	 */
	public static final String HASH_CODE_KEY = "hashCode";


	/**
	 * 基于受管资源的标识返回一个 {@code ObjectName} 实例。
	 */
	@Override
	public ObjectName getObjectName(Object managedBean, @Nullable String beanKey) throws MalformedObjectNameException {
		String domain = ClassUtils.getPackageName(managedBean.getClass());
		Hashtable<String, String> keys = new Hashtable<>();
		keys.put(TYPE_KEY, ClassUtils.getShortName(managedBean.getClass()));
		keys.put(HASH_CODE_KEY, ObjectUtils.getIdentityHexString(managedBean));
		return ObjectNameManager.getInstance(domain, keys);
	}

}
