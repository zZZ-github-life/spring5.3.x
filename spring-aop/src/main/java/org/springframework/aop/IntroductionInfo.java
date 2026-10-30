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

package org.springframework.aop;

/**
 * 提供描述引入所需信息的接口。
 *
 * <p>{@link IntroductionAdvisor IntroductionAdvisors} 必须实现此接口。如果某个
 * {@link org.aopalliance.aop.Advice} 实现了此接口，那么它可以在没有 {@link IntroductionAdvisor} 的
 * 情况下用作引入。在这种情况下，通知是自描述的，它不仅提供必要的行为，还描述它所引入的接口。
 *
 * @author Rod Johnson
 * @since 1.1.1
 */
public interface IntroductionInfo {

	/**
	 * 返回此 Advisor 或 Advice 所引入的附加接口。
	 * @return 被引入的接口
	 */
	Class<?>[] getInterfaces();

}
