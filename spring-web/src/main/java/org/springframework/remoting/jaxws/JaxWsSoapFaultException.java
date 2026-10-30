/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.remoting.jaxws;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPFault;
import javax.xml.ws.soap.SOAPFaultException;

import org.springframework.remoting.soap.SoapFaultException;

/**
 * 用于 JAX-WS {@link javax.xml.ws.soap.SOAPFaultException} 类的
 * Spring SoapFaultException 适配器。
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
@SuppressWarnings("serial")
public class JaxWsSoapFaultException extends SoapFaultException {

	/**
	 * JaxWsSoapFaultException 的构造函数。
	 * @param original 要包装的原始 JAX-WS SOAPFaultException
	 */
	public JaxWsSoapFaultException(SOAPFaultException original) {
		super(original.getMessage(), original);
	}

	/**
	 * 返回包装的 JAX-WS SOAPFault。
	 */
	public final SOAPFault getFault() {
		return ((SOAPFaultException) getCause()).getFault();
	}


	@Override
	public String getFaultCode() {
		return getFault().getFaultCode();
	}

	@Override
	public QName getFaultCodeAsQName() {
		return getFault().getFaultCodeAsQName();
	}

	@Override
	public String getFaultString() {
		return getFault().getFaultString();
	}

	@Override
	public String getFaultActor() {
		return getFault().getFaultActor();
	}

}
