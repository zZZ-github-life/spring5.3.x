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

package org.springframework.test.web.client;

import java.io.IOException;
import java.util.Iterator;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 简单的 {@code RequestExpectationManager}，按顺序（即预期的声明顺序）将请求匹配到预期。
 *
 * <p>当请求预期的预期次数大于一时，
 * 只有第一次执行需要遵循声明顺序。
 * 后续请求执行可以插入到其后的任意位置。
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.3
 */
public class SimpleRequestExpectationManager extends AbstractRequestExpectationManager {

	/** 按声明顺序排列的预期（计数可能 > 1）。 */
	@Nullable
	private Iterator<RequestExpectation> expectationIterator;

	/** 跟踪仍有剩余次数的预期。 */
	private final RequestExpectationGroup repeatExpectations = new RequestExpectationGroup();


	@Override
	protected void afterExpectationsDeclared() {
		Assert.state(this.expectationIterator == null, "Expectations already declared");
		this.expectationIterator = getExpectations().iterator();
	}

	@Override
	protected RequestExpectation matchRequest(ClientHttpRequest request) throws IOException {
		RequestExpectation expectation = this.repeatExpectations.findExpectation(request);
		if (expectation == null) {
			if (this.expectationIterator == null || !this.expectationIterator.hasNext()) {
				throw createUnexpectedRequestError(request);
			}
			expectation = this.expectationIterator.next();
			expectation.match(request);
		}
		this.repeatExpectations.update(expectation);
		return expectation;
	}

	@Override
	public void reset() {
		super.reset();
		this.expectationIterator = null;
		this.repeatExpectations.reset();
	}

}
