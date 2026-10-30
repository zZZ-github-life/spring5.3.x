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

package org.springframework.web.jsf.el;

import java.beans.FeatureDescriptor;
import java.util.Iterator;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.jsf.FacesContextUtils;

/**
 * 特殊的 JSF {@code ELResolver}，在名为 "webApplicationContext" 的变量下
 * 暴露 Spring {@code WebApplicationContext} 实例。
 *
 * <p>与 {@link SpringBeanFacesELResolver} 不同，此 ELResolver 变体<i>不会</i>
 * 将 JSF 变量名解析为 Spring Bean 名称。它而是在一个特殊名称下暴露 Spring 的根
 * WebApplicationContext <i>本身</i>，并且能够将
 * "webApplicationContext.mySpringManagedBusinessObject" 解引用解析为该应用上下文中
 * Spring 定义的 Bean。
 *
 * <p>在 {@code faces-config.xml} 文件中按如下方式配置此解析器：
 *
 * <pre class="code">
 * &lt;application&gt;
 *   ...
 *   &lt;el-resolver&gt;org.springframework.web.jsf.el.WebApplicationContextFacesELResolver&lt;/el-resolver&gt;
 * &lt;/application&gt;</pre>
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see SpringBeanFacesELResolver
 * @see org.springframework.web.jsf.FacesContextUtils#getWebApplicationContext
 */
public class WebApplicationContextFacesELResolver extends ELResolver {

	/**
	 * 所暴露的 WebApplicationContext 变量名称："webApplicationContext"。
	 */
	public static final String WEB_APPLICATION_CONTEXT_VARIABLE_NAME = "webApplicationContext";


	/** 可供子类使用的 Logger。 */
	protected final Log logger = LogFactory.getLog(getClass());


	@Override
	@Nullable
	public Object getValue(ELContext elContext, @Nullable Object base, Object property) throws ELException {
		if (base != null) {
			if (base instanceof WebApplicationContext) {
				WebApplicationContext wac = (WebApplicationContext) base;
				String beanName = property.toString();
				if (logger.isTraceEnabled()) {
					logger.trace("Attempting to resolve property '" + beanName + "' in root WebApplicationContext");
				}
				if (wac.containsBean(beanName)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Successfully resolved property '" + beanName + "' in root WebApplicationContext");
					}
					elContext.setPropertyResolved(true);
					try {
						return wac.getBean(beanName);
					}
					catch (BeansException ex) {
						throw new ELException(ex);
					}
				}
				else {
					// Mimic standard JSF/JSP behavior when base is a Map by returning null.
					return null;
				}
			}
		}
		else {
			if (WEB_APPLICATION_CONTEXT_VARIABLE_NAME.equals(property)) {
				elContext.setPropertyResolved(true);
				return getWebApplicationContext(elContext);
			}
		}

		return null;
	}

	@Override
	@Nullable
	public Class<?> getType(ELContext elContext, @Nullable Object base, Object property) throws ELException {
		if (base != null) {
			if (base instanceof WebApplicationContext) {
				WebApplicationContext wac = (WebApplicationContext) base;
				String beanName = property.toString();
				if (logger.isDebugEnabled()) {
					logger.debug("Attempting to resolve property '" + beanName + "' in root WebApplicationContext");
				}
				if (wac.containsBean(beanName)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Successfully resolved property '" + beanName + "' in root WebApplicationContext");
					}
					elContext.setPropertyResolved(true);
					try {
						return wac.getType(beanName);
					}
					catch (BeansException ex) {
						throw new ELException(ex);
					}
				}
				else {
					// Mimic standard JSF/JSP behavior when base is a Map by returning null.
					return null;
				}
			}
		}
		else {
			if (WEB_APPLICATION_CONTEXT_VARIABLE_NAME.equals(property)) {
				elContext.setPropertyResolved(true);
				return WebApplicationContext.class;
			}
		}

		return null;
	}

	@Override
	public void setValue(ELContext elContext, Object base, Object property, Object value) throws ELException {
	}

	@Override
	public boolean isReadOnly(ELContext elContext, Object base, Object property) throws ELException {
		if (base instanceof WebApplicationContext) {
			elContext.setPropertyResolved(true);
			return true;
		}
		return false;
	}

	@Override
	@Nullable
	public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext elContext, Object base) {
		return null;
	}

	@Override
	public Class<?> getCommonPropertyType(ELContext elContext, Object base) {
		return Object.class;
	}


	/**
	 * 获取要暴露的 {@link WebApplicationContext} 引用。
	 * <p>默认实现委托给 {@link FacesContextUtils}，
	 * 如果未找到 {@code WebApplicationContext} 则返回 {@code null}。
	 * @param elContext 当前的 JSF ELContext
	 * @return Spring Web 应用上下文
	 * @see org.springframework.web.jsf.FacesContextUtils#getWebApplicationContext
	 */
	@Nullable
	protected WebApplicationContext getWebApplicationContext(ELContext elContext) {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		return FacesContextUtils.getRequiredWebApplicationContext(facesContext);
	}

}
