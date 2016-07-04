package com.evgenbar.binarydecoder;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DecodeUnsigned {
	public enum IntegerType { UnsignedByte, UnsignedShort, UnsignedInt }
	IntegerType value() default IntegerType.UnsignedByte;
}
