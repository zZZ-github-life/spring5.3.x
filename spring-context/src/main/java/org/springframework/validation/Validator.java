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

package org.springframework.validation;

/**
 * 用于应用程序特定对象的校验器。
 *
 * <p>此接口完全独立于任何基础设施或上下文；也就是说，它并不局限于只校验 Web 层、数据访问层或任何其
 * 他层中的对象。因此，它适用于应用程序的任何层，并支持将校验逻辑本身作为一等公民进行封装。
 *
 * <p>下面是一个简单但完整的 {@code Validator} 实现，它校验 {@code UserLogin} 实例的各个
 * {@link String} 属性是否非空（即不为 {@code null} 且不完全是空白字符），以及所存在的任何密码长度
 * 至少为 {@code 'MINIMUM_PASSWORD_LENGTH'} 个字符。
 *
 * <pre class="code">public class UserLoginValidator implements Validator {
 *
 *    private static final int MINIMUM_PASSWORD_LENGTH = 6;
 *
 *    public boolean supports(Class clazz) {
 *       return UserLogin.class.isAssignableFrom(clazz);
 *    }
 *
 *    public void validate(Object target, Errors errors) {
 *       ValidationUtils.rejectIfEmptyOrWhitespace(errors, "userName", "field.required");
 *       ValidationUtils.rejectIfEmptyOrWhitespace(errors, "password", "field.required");
 *       UserLogin login = (UserLogin) target;
 *       if (login.getPassword() != null
 *             &amp;&amp; login.getPassword().trim().length() &lt; MINIMUM_PASSWORD_LENGTH) {
 *          errors.rejectValue("password", "field.min.length",
 *                new Object[]{Integer.valueOf(MINIMUM_PASSWORD_LENGTH)},
 *                "The password must be at least [" + MINIMUM_PASSWORD_LENGTH + "] characters in length.");
 *       }
 *    }
 * }</pre>
 *
 * <p>关于 {@code Validator} 接口及其在企业应用程序中角色的更全面讨论，另请参见 Spring 参考手册。
 *
 * @author Rod Johnson
 * @see SmartValidator
 * @see Errors
 * @see ValidationUtils
 */
public interface Validator {

	/**
	 * 此 {@link Validator} 能否{@link #validate(Object, Errors) validate}所提供的 {@code clazz} 的实例？
	 *
	 * <p>此方法<i>通常</i>这样实现：
	 * <pre class="code">return Foo.class.isAssignableFrom(clazz);</pre>
	 * （其中 {@code Foo} 是要被{@link #validate(Object, Errors) validated} 的实际对象实例的类（或超类）。）
	 * @param clazz the {@link Class} that this {@link Validator} is
	 * being asked if it can {@link #validate(Object, Errors) validate}
	 * @return {@code true} if this {@link Validator} can indeed
	 * {@link #validate(Object, Errors) validate} instances of the
	 * supplied {@code clazz}
	 */
	boolean supports(Class<?> clazz);

	/**
	 * 校验所提供的 {@code target} 对象，该对象必须是某个 {@link Class} 的实例，并且对于该类，
	 * {@link #supports(Class)} 方法通常已经（或将会）返回 {@code true}。
	 *
	 * <p>所提供的 {@link Errors errors} 实例可用于报告任何产生的校验错误。
	 * @param target the object that is to be validated
	 * @param errors contextual state about the validation process
	 * @see ValidationUtils
	 */
	void validate(Object target, Errors errors);

}
