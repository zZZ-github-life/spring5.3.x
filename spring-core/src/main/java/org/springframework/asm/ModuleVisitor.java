// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
package org.springframework.asm;

/**
 * 用于访问 Java 模块的访问者。该类的方法必须按以下顺序调用：( {@code visitMainClass} | (
 * {@code visitPackage} | {@code visitRequire} | {@code visitExport} | {@code visitOpen} |
 * {@code visitUse} | {@code visitProvide} )* ) {@code visitEnd}。
 *
 * @author Remi Forax
 * @author Eric Bruneton
 */
public abstract class ModuleVisitor {
  /**
   * 此访问者实现的 ASM API 版本。其值必须是 {@link Opcodes#ASM6} 或 {@link Opcodes#ASM7} 之一。
   */
  protected final int api;

  /**
   * 此访问者必须将方法调用委托给的模块访问者。可以为 {@literal null}。
   */
  protected ModuleVisitor mv;

  /**
   * 构造一个新的 {@link ModuleVisitor}。
   *
   * @param api the ASM API version implemented by this visitor. Must be one of {@link Opcodes#ASM6}
   *     or {@link Opcodes#ASM7}.
   */
  protected ModuleVisitor(final int api) {
    this(api, null);
  }

  /**
   * 构造一个新的 {@link ModuleVisitor}。
   *
   * @param api the ASM API version implemented by this visitor. Must be one of {@link Opcodes#ASM6}
   *     or {@link Opcodes#ASM7}.
   * @param moduleVisitor the module visitor to which this visitor must delegate method calls. May
   *     be null.
   */
  protected ModuleVisitor(final int api, final ModuleVisitor moduleVisitor) {
    if (api != Opcodes.ASM9
        && api != Opcodes.ASM8
        && api != Opcodes.ASM7
        && api != Opcodes.ASM6
        && api != Opcodes.ASM5
        && api != Opcodes.ASM4
        && api != Opcodes.ASM10_EXPERIMENTAL) {
      throw new IllegalArgumentException("Unsupported api " + api);
    }
    // SPRING PATCH: no preview mode check for ASM experimental
    this.api = api;
    this.mv = moduleVisitor;
  }

  /**
   * 访问当前模块的主类。
   *
   * @param mainClass the internal name of the main class of the current module.
   */
  public void visitMainClass(final String mainClass) {
    if (mv != null) {
      mv.visitMainClass(mainClass);
    }
  }

  /**
   * 访问当前模块的一个包。
   *
   * @param packaze the internal name of a package.
   */
  public void visitPackage(final String packaze) {
    if (mv != null) {
      mv.visitPackage(packaze);
    }
  }

  /**
   * 访问当前模块的一个依赖。
   *
   * @param module the fully qualified name (using dots) of the dependence.
   * @param access the access flag of the dependence among {@code ACC_TRANSITIVE}, {@code
   *     ACC_STATIC_PHASE}, {@code ACC_SYNTHETIC} and {@code ACC_MANDATED}.
   * @param version the module version at compile time, or {@literal null}.
   */
  public void visitRequire(final String module, final int access, final String version) {
    if (mv != null) {
      mv.visitRequire(module, access, version);
    }
  }

  /**
   * 访问当前模块的一个导出包。
   *
   * @param packaze the internal name of the exported package.
   * @param access the access flag of the exported package, valid values are among {@code
   *     ACC_SYNTHETIC} and {@code ACC_MANDATED}.
   * @param modules the fully qualified names (using dots) of the modules that can access the public
   *     classes of the exported package, or {@literal null}.
   */
  public void visitExport(final String packaze, final int access, final String... modules) {
    if (mv != null) {
      mv.visitExport(packaze, access, modules);
    }
  }

  /**
   * 访问当前模块的一个开放包。
   *
   * @param packaze the internal name of the opened package.
   * @param access the access flag of the opened package, valid values are among {@code
   *     ACC_SYNTHETIC} and {@code ACC_MANDATED}.
   * @param modules the fully qualified names (using dots) of the modules that can use deep
   *     reflection to the classes of the open package, or {@literal null}.
   */
  public void visitOpen(final String packaze, final int access, final String... modules) {
    if (mv != null) {
      mv.visitOpen(packaze, access, modules);
    }
  }

  /**
   * 访问当前模块使用的一个服务。名称必须是接口或类的内部名称。
   *
   * @param service the internal name of the service.
   */
  public void visitUse(final String service) {
    if (mv != null) {
      mv.visitUse(service);
    }
  }

  /**
   * 访问一个服务的实现。
   *
   * @param service the internal name of the service.
   * @param providers the internal names of the implementations of the service (there is at least
   *     one provider).
   */
  public void visitProvide(final String service, final String... providers) {
    if (mv != null) {
      mv.visitProvide(service, providers);
    }
  }

  /**
   * 访问模块的结尾。此方法是最后一个被调用的方法，用于通知访问者所有内容都已访问完毕。
   */
  public void visitEnd() {
    if (mv != null) {
      mv.visitEnd();
    }
  }
}
