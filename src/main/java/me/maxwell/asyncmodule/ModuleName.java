package me.maxwell.asyncmodule;

import java.lang.annotation.*;

/**
 * 指定moduleName
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleName {
    String value();
}
