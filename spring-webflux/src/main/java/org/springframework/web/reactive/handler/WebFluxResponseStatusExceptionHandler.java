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

package org.springframework.web.reactive.handler;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.handler.ResponseStatusExceptionHandler;

/**
 * 通用的 WebFlux 异常处理器，检测 {@link org.springframework.web.server.ResponseStatusException} 的实例（继承自基类）以及带有 {@link ResponseStatus @ResponseStatus} 注解的异常，为它们确定 HTTP 状态并相应地更新响应状态。
 *
 * <p>如果响应已提交，则该错误保持未解决状态并被传播。
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 5.0.5
 */
public class WebFluxResponseStatusExceptionHandler extends ResponseStatusExceptionHandler {

	@Override
	protected int determineRawStatusCode(Throwable ex) {
		int status = super.determineRawStatusCode(ex);
		if (status == -1) {
			ResponseStatus ann = AnnotatedElementUtils.findMergedAnnotation(ex.getClass(), ResponseStatus.class);
			if (ann != null) {
				status = ann.code().value();
			}
		}
		return status;
	}

}
