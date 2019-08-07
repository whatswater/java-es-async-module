package me.maxwell.asyncmodule;

import java.lang.annotation.*;

/**
 * ModuleFactory读取ClassVersion的值，
 * 在require的时候，
 * 判断依赖哪个版本的模块。
 * 此处的ClassVersion不用于类加载器，类加载器的逻辑需要另外指定
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleVersion {
    String value();
}
