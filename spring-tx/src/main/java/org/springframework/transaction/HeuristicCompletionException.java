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

package org.springframework.transaction;

/**
 * 表示事务协调器一方因启发式决策而导致事务失败的异常。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 17.03.2003
 */
@SuppressWarnings("serial")
public class HeuristicCompletionException extends TransactionException {

	/**
	 * 未知结果状态。
	 */
	public static final int STATE_UNKNOWN = 0;

	/**
	 * 已提交结果状态。
	 */
	public static final int STATE_COMMITTED = 1;

	/**
	 * 已回滚结果状态。
	 */
	public static final int STATE_ROLLED_BACK = 2;

	/**
	 * 混合结果状态。
	 */
	public static final int STATE_MIXED = 3;


	public static String getStateString(int state) {
		switch (state) {
			case STATE_COMMITTED:
				return "committed";
			case STATE_ROLLED_BACK:
				return "rolled back";
			case STATE_MIXED:
				return "mixed";
			default:
				return "unknown";
		}
	}


	/**
	 * 事务的结果状态：部分或全部资源是否已被提交？
	 */
	private final int outcomeState;


	/**
	 * HeuristicCompletionException 的构造函数。
	 * @param outcomeState the outcome state of the transaction
	 * @param cause the root cause from the transaction API in use
	 */
	public HeuristicCompletionException(int outcomeState, Throwable cause) {
		super("Heuristic completion: outcome state is " + getStateString(outcomeState), cause);
		this.outcomeState = outcomeState;
	}

	/**
	 * 返回事务状态的结果状态，
	 * 为本类中的常量之一。
	 * @see #STATE_UNKNOWN
	 * @see #STATE_COMMITTED
	 * @see #STATE_ROLLED_BACK
	 * @see #STATE_MIXED
	 */
	public int getOutcomeState() {
		return this.outcomeState;
	}

}
