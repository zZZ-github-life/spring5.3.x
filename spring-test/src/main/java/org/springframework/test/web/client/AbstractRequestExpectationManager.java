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

package org.springframework.test.web.client;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 负责存储预期与实际请求，并在结束时检查未满足预期的
 * {@code RequestExpectationManager} 实现的基类。
 *
 * <p>子类负责通过将每个请求与预期进行匹配来验证请求，
 * 匹配可以遵循声明顺序，也可以不遵循。
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.3
 */
public abstract class AbstractRequestExpectationManager implements RequestExpectationManager {

	private final List<RequestExpectation> expectations = new ArrayList<>();

	private final List<ClientHttpRequest> requests = new ArrayList<>();

	private final Map<ClientHttpRequest, Throwable> requestFailures = new LinkedHashMap<>();


	/**
	 * 返回预期的只读列表。
	 */
	protected List<RequestExpectation> getExpectations() {
		return Collections.unmodifiableList(this.expectations);
	}

	/**
	 * 返回截至目前已执行请求的只读列表。
	 */
	protected List<ClientHttpRequest> getRequests() {
		return Collections.unmodifiableList(this.requests);
	}


	@Override
	public ResponseActions expectRequest(ExpectedCount count, RequestMatcher matcher) {
		Assert.state(this.requests.isEmpty(), "Cannot add more expectations after actual requests are made");
		RequestExpectation expectation = new DefaultRequestExpectation(count, matcher);
		this.expectations.add(expectation);
		return expectation;
	}

	@Override
	public ClientHttpResponse validateRequest(ClientHttpRequest request) throws IOException {
		RequestExpectation expectation;
		synchronized (this.requests) {
			if (this.requests.isEmpty()) {
				afterExpectationsDeclared();
			}
			try {
				// Try this first for backwards compatibility
				ClientHttpResponse response = validateRequestInternal(request);
				if (response != null) {
					return response;
				}
				else {
					expectation = matchRequest(request);
				}
			}
			catch (Throwable ex) {
				this.requestFailures.put(request, ex);
				throw ex;
			}
			finally {
				this.requests.add(request);
			}
		}
		return expectation.createResponse(request);
	}

	/**
	 * 在第一个实际请求到达时调用，这实际上意味着预期声明阶段已经结束。
	 */
	protected void afterExpectationsDeclared() {
	}

	/**
	 * 子类必须实现将请求与已声明预期进行匹配的实际验证逻辑。
	 * @deprecated as of 5.0.3, subclasses should implement {@link #matchRequest(ClientHttpRequest)}
	 * instead and return only the matched expectation, leaving the call to create the response
	 * as a separate step (to be invoked by this class).
	 */
	@Deprecated
	@Nullable
	protected ClientHttpResponse validateRequestInternal(ClientHttpRequest request) throws IOException {
		return null;
	}

	/**
	 * 从 5.0.3 开始，子类应实现此方法而不是
	 * {@link #validateRequestInternal(ClientHttpRequest)}，以便将请求与预期进行匹配，
	 * 而将创建响应的调用留作单独的步骤（由此类调用）。
	 * @param request 当前请求
	 * @return 匹配的期望，其请求计数已通过
	 * {@link RequestExpectation#incrementAndValidate()} 更新。
	 * @since 5.0.3
	 */
	protected RequestExpectation matchRequest(ClientHttpRequest request) throws IOException {
		throw new UnsupportedOperationException(
				"It looks like neither the deprecated \"validateRequestInternal\"" +
				"nor its replacement (this method) are implemented.");
	}

	@Override
	public void verify() {
		int count = verifyInternal();
		if (count > 0) {
			String message = "Further request(s) expected leaving " + count + " unsatisfied expectation(s).\n";
			throw new AssertionError(message + getRequestDetails());
		}
	}

	@Override
	public void verify(Duration timeout) {
		Instant endTime = Instant.now().plus(timeout);
		do {
			if (verifyInternal() == 0) {
				return;
			}
		}
		while (Instant.now().isBefore(endTime));
		verify();
	}

	private int verifyInternal() {
		if (this.expectations.isEmpty()) {
			return 0;
		}
		if (!this.requestFailures.isEmpty()) {
			throw new AssertionError("Some requests did not execute successfully.\n" +
					this.requestFailures.entrySet().stream()
							.map(entry -> "Failed request:\n" + entry.getKey() + "\n" + entry.getValue())
							.collect(Collectors.joining("\n", "\n", "")));
		}
		int count = 0;
		for (RequestExpectation expectation : this.expectations) {
			if (!expectation.isSatisfied()) {
				count++;
			}
		}
		return count;
	}

	/**
	 * 返回已执行请求的详细信息。
	 */
	protected String getRequestDetails() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.requests.size()).append(" request(s) executed");
		if (!this.requests.isEmpty()) {
			sb.append(":\n");
			for (ClientHttpRequest request : this.requests) {
				sb.append(request.toString()).append('\n');
			}
		}
		else {
			sb.append(".\n");
		}
		return sb.toString();
	}

	/**
	 * 返回一个 {@code AssertionError}，子类可在遇到意外请求时抛出。
	 */
	protected AssertionError createUnexpectedRequestError(ClientHttpRequest request) {
		HttpMethod method = request.getMethod();
		URI uri = request.getURI();
		String message = "No further requests expected: HTTP " + method + " " + uri + "\n";
		return new AssertionError(message + getRequestDetails());
	}

	@Override
	public void reset() {
		this.expectations.clear();
		this.requests.clear();
		this.requestFailures.clear();
	}


	/**
	 * 用于管理一组剩余预期的辅助类。
	 */
	protected static class RequestExpectationGroup {

		private final Set<RequestExpectation> expectations = new LinkedHashSet<>();

		public void addAllExpectations(Collection<RequestExpectation> expectations) {
			this.expectations.addAll(expectations);
		}

		public Set<RequestExpectation> getExpectations() {
			return this.expectations;
		}

		/**
		 * 返回匹配的预期，如果没有匹配则返回 {@code null}。
		 */
		@Nullable
		public RequestExpectation findExpectation(ClientHttpRequest request) throws IOException {
			for (RequestExpectation expectation : this.expectations) {
				try {
					expectation.match(request);
					return expectation;
				}
				catch (AssertionError error) {
					// We're looking to find a match or return null.
				}
			}
			return null;
		}

		/**
		 * 为已匹配的预期调用此方法。
		 * <p>给定预期的计数会先递增，如果 remainingCount &gt; 0 则保留，否则移除。
		 */
		public void update(RequestExpectation expectation) {
			expectation.incrementAndValidate();
			updateInternal(expectation);
		}

		private void updateInternal(RequestExpectation expectation) {
			if (expectation.hasRemainingCount()) {
				this.expectations.add(expectation);
			}
			else {
				this.expectations.remove(expectation);
			}
		}

		/**
		 * 将预期添加到此组。
		 * @deprecated as of 5.0.3, if favor of {@link #addAllExpectations}
		 */
		@Deprecated
		public void updateAll(Collection<RequestExpectation> expectations) {
			expectations.forEach(this::updateInternal);
		}

		/**
		 * 重置此组中的所有预期。
		 */
		public void reset() {
			this.expectations.clear();
		}
	}

}
