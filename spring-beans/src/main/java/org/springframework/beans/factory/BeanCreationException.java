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

package org.springframework.beans.factory;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.FatalBeanException;
import org.springframework.core.NestedRuntimeException;
import org.springframework.lang.Nullable;

/**
 * 当 BeanFactory 尝试根据 Bean 定义创建 Bean 时遇到错误所抛出的异常。
 *
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class BeanCreationException extends FatalBeanException {

	@Nullable
	private final String beanName;

	@Nullable
	private final String resourceDescription;

	@Nullable
	private List<Throwable> relatedCauses;


	/**
	 * 创建新的 BeanCreationException。
	 * @param msg the detail message
	 */
	public BeanCreationException(String msg) {
		super(msg);
		this.beanName = null;
		this.resourceDescription = null;
	}

	/**
	 * 创建新的 BeanCreationException。
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public BeanCreationException(String msg, Throwable cause) {
		super(msg, cause);
		this.beanName = null;
		this.resourceDescription = null;
	}

	/**
	 * 创建新的 BeanCreationException。
	 * @param beanName the name of the bean requested
	 * @param msg the detail message
	 */
	public BeanCreationException(String beanName, String msg) {
		super("Error creating bean with name '" + beanName + "': " + msg);
		this.beanName = beanName;
		this.resourceDescription = null;
	}

	/**
	 * 创建新的 BeanCreationException。
	 * @param beanName the name of the bean requested
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public BeanCreationException(String beanName, String msg, Throwable cause) {
		this(beanName, msg);
		initCause(cause);
	}

	/**
	 * 创建新的 BeanCreationException。
	 * @param resourceDescription description of the resource
	 * that the bean definition came from
	 * @param beanName the name of the bean requested
	 * @param msg the detail message
	 */
	public BeanCreationException(@Nullable String resourceDescription, @Nullable String beanName, String msg) {
		super("Error creating bean with name '" + beanName + "'" +
				(resourceDescription != null ? " defined in " + resourceDescription : "") + ": " + msg);
		this.resourceDescription = resourceDescription;
		this.beanName = beanName;
		this.relatedCauses = null;
	}

	/**
	 * 创建新的 BeanCreationException。
	 * @param resourceDescription description of the resource
	 * that the bean definition came from
	 * @param beanName the name of the bean requested
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public BeanCreationException(@Nullable String resourceDescription, String beanName, String msg, Throwable cause) {
		this(resourceDescription, beanName, msg);
		initCause(cause);
	}


	/**
	 * 返回 Bean 定义所来自资源的描述（如果有）。
	 */
	@Nullable
	public String getResourceDescription() {
		return this.resourceDescription;
	}

	/**
	 * 返回所请求的 Bean 名称（如果有）。
	 */
	@Nullable
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * 为此 Bean 创建异常添加一个相关原因，它不是失败的直接原因，而是在同一 Bean 实例的创建过程中更早发
	 * 生的。
	 * @param ex the related cause to add
	 */
	public void addRelatedCause(Throwable ex) {
		if (this.relatedCauses == null) {
			this.relatedCauses = new ArrayList<>();
		}
		this.relatedCauses.add(ex);
	}

	/**
	 * 返回相关原因（如果有）。
	 * @return the array of related causes, or {@code null} if none
	 */
	@Nullable
	public Throwable[] getRelatedCauses() {
		if (this.relatedCauses == null) {
			return null;
		}
		return this.relatedCauses.toArray(new Throwable[0]);
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString());
		if (this.relatedCauses != null) {
			for (Throwable relatedCause : this.relatedCauses) {
				sb.append("\nRelated cause: ");
				sb.append(relatedCause);
			}
		}
		return sb.toString();
	}

	@Override
	public void printStackTrace(PrintStream ps) {
		synchronized (ps) {
			super.printStackTrace(ps);
			if (this.relatedCauses != null) {
				for (Throwable relatedCause : this.relatedCauses) {
					ps.println("Related cause:");
					relatedCause.printStackTrace(ps);
				}
			}
		}
	}

	@Override
	public void printStackTrace(PrintWriter pw) {
		synchronized (pw) {
			super.printStackTrace(pw);
			if (this.relatedCauses != null) {
				for (Throwable relatedCause : this.relatedCauses) {
					pw.println("Related cause:");
					relatedCause.printStackTrace(pw);
				}
			}
		}
	}

	@Override
	public boolean contains(@Nullable Class<?> exClass) {
		if (super.contains(exClass)) {
			return true;
		}
		if (this.relatedCauses != null) {
			for (Throwable relatedCause : this.relatedCauses) {
				if (relatedCause instanceof NestedRuntimeException &&
						((NestedRuntimeException) relatedCause).contains(exClass)) {
					return true;
				}
			}
		}
		return false;
	}

}
