/**
 * Spring 对 <a href="http://objenesis.org">Objenesis 3.2</a> 的重新打包
 * （入口点为 SpringObjenesis；仅供内部使用）。
 *
 * <p>这种重新打包技术可避免在应用层面或来自第三方库和框架的、对不同 Objenesis 版本的依赖产生任何潜
 * 在冲突。
 *
 * <p>由于这种重新打包发生在 class 文件级别，因此这里不提供源码和 javadoc。在使用这些类时，详细信息
 * 请参阅原始的 <a href="http://objenesis.org/tutorial.html">Objenesis 文档</a>。
 */
package org.springframework.objenesis;
