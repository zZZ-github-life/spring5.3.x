/*
 * Copyright 2002-2019 the original author or authors.
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
 * 表示一个正在进行的响应式事务。
 * 目前这是一个扩展 {@link TransactionExecution} 的标记接口，
 * 但在未来的版本中可能会增加更多方法。
 *
 * <p>事务代码可以使用它来获取状态信息，
 * 并以编程方式请求回滚（而不是抛出导致隐式回滚的异常）。
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @since 5.2
 * @see #setRollbackOnly()
 * @see ReactiveTransactionManager#getReactiveTransaction
 */
public interface ReactiveTransaction extends TransactionExecution {

}
