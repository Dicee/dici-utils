package com.dici.exceptions;

import static com.dici.check.Check.notNull;

public class UnknownEnumValueException extends RuntimeException {
	private static final long serialVersionUID = 1L; 

	public static <ENUM extends Enum<ENUM>> RuntimeException unknownEnumValue(Class<ENUM> enumeration, ENUM value) {
		return new UnknownEnumValueException(enumeration, value);
	}
	
	public <ENUM extends Enum<ENUM>> UnknownEnumValueException(Class<ENUM> enumeration, ENUM value) {
		super(String.format("Unknown value <%s> for enumeration type <%s>", notNull(value.name()), notNull(enumeration.getCanonicalName())));
	}
}
