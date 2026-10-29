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

package org.springframework.jmx.export.metadata;

import org.springframework.lang.Nullable;

/**
 * 指示将给定 Bean 属性作为 JMX 属性暴露的元数据。仅在对 JavaBean 的 getter 或 setter 使用时有效。
 *
 * @author Rob Harrop
 * @since 1.2
 * @see org.springframework.jmx.export.assembler.MetadataMBeanInfoAssembler
 * @see org.springframework.jmx.export.MBeanExporter
 */
public class ManagedAttribute extends AbstractJmxAttribute {

	/**
	 * 空的属性。
	 */
	public static final ManagedAttribute EMPTY = new ManagedAttribute();


	@Nullable
	private Object defaultValue;

	@Nullable
	private String persistPolicy;

	private int persistPeriod = -1;


	/**
	 * 设置此属性的默认值。
	 */
	public void setDefaultValue(@Nullable Object defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * 返回此属性的默认值。
	 */
	@Nullable
	public Object getDefaultValue() {
		return this.defaultValue;
	}

	public void setPersistPolicy(@Nullable String persistPolicy) {
		this.persistPolicy = persistPolicy;
	}

	@Nullable
	public String getPersistPolicy() {
		return this.persistPolicy;
	}

	public void setPersistPeriod(int persistPeriod) {
		this.persistPeriod = persistPeriod;
	}

	public int getPersistPeriod() {
		return this.persistPeriod;
	}

}
