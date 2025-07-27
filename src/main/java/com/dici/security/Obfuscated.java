package com.dici.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Use this annotation in conjunction with [SafeToStringBuilder] to mark sensitive fields and configure how they should be logged
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Obfuscated {
    /// Controls the length of the sensitive field that will be shown in plan. Must be positive. If it's negative, [SafeToStringBuilder]
    /// will default to the safest behaviour, i.e. 0.
    int plainPrefixLength() default 0;
}
