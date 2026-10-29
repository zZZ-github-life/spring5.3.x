/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.ui.freemarker;

import java.io.IOException;
import java.io.StringWriter;

import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * FreeMarker 工具类。
 * 提供使用模型处理 FreeMarker 模板的便捷方法。
 *
 * @author Juergen Hoeller
 * @since 14.03.2004
 */
public abstract class FreeMarkerTemplateUtils {

	/**
	 * 使用给定的模型处理指定的 FreeMarker 模板，并将结果写入给定的 Writer。
	 * <p>当使用此方法准备要通过 Spring 邮件支持发送的邮件文本时，
	 * 请考虑将 IO/TemplateException 包装为 MailPreparationException。
	 * @param model the model object, typically a Map that contains model names
	 * as keys and model objects as values
	 * @return the result as String
	 * @throws IOException if the template wasn't found or couldn't be read
	 * @throws freemarker.template.TemplateException if rendering failed
	 * @see org.springframework.mail.MailPreparationException
	 */
	public static String processTemplateIntoString(Template template, Object model)
			throws IOException, TemplateException {

		StringWriter result = new StringWriter(1024);
		template.process(model, result);
		return result.toString();
	}

}
