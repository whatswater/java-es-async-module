package me.maxwell.asyncmodule;

import java.lang.annotation.*;

/**
 * 一个Module的路径由以下部分组成，moduleVersion + ":" + ModuleName。
 * 此注解用于指定本模块的moduleVersion，用于在模块工厂中和其他的模块区分开。
 * 当其他模块依赖于此模块时，会向模块工厂传递一个Class对象，模块工厂会根据Class对象的ModuleVersion和ClassName找到具体的模块信息。
 * 如果classpath中存在路径相同的Class文件，某模块加载此Class的时候，会加载哪一个取决于类加载器的设置
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleVersion {
    String value();
}
