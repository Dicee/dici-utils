package com.dici.security;

import lombok.Value;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SafeToStringBuilderTest {
    private static final String STRING = "hello";
    private static final List<String> LIST = List.of("my", "friends");
    private static final String SHORT_SENSITIVE_STR = "bla";
    private static final String LONG_SENSITIVE_STR = "I'm gonna tell you my secretest secret";

    @Test
    void testToString_null() {
        assertThat(SafeToStringBuilder.toString(null)).isEqualTo("null");
    }

    @Test
    void testToString_noSensitiveField_null() {
        Pojo pojo = new Pojo(null, LIST);
        assertThat(SafeToStringBuilder.toString(pojo)).isEqualTo("Pojo(string=null, list=[my, friends])");
    }

    @Test
    void testToString_noSensitiveField() {
        Pojo pojo = new Pojo(STRING, LIST);
        assertThat(SafeToStringBuilder.toString(pojo)).isEqualTo("Pojo(string=hello, list=[my, friends])");
    }

    @Test
    void testToString_sensitiveField_null() {
        SensitivePojo pojo = new SensitivePojo(STRING, LIST, null);
        assertThat(SafeToStringBuilder.toString(pojo)).isEqualTo("SensitivePojo(string=hello, list=[my, friends], sensitive=null)");
    }

    @Test
    void testToString_sensitiveField_fullyHidden() {
        SensitivePojo pojo = new SensitivePojo(STRING, LIST, LONG_SENSITIVE_STR);
        assertThat(SafeToStringBuilder.toString(pojo)).isEqualTo("SensitivePojo(string=hello, list=[my, friends], sensitive=****)");
    }

    @Test
    void testToString_sensitiveField_javaRecord() {
        SensitiveRecord record = new SensitiveRecord(STRING, LIST, LONG_SENSITIVE_STR);
        assertThat(SafeToStringBuilder.toString(record)).isEqualTo("SensitiveRecord(string=hello, list=[my, friends], sensitive=****)");
    }

    @Test
    void testToString_sensitiveField_withPrefix_sensitiveValueIsShorter() {
        var pojo = new SensitivePojoWithPlainPrefixLength(STRING, LIST, SHORT_SENSITIVE_STR);
        assertThat(SafeToStringBuilder.toString(pojo)).isEqualTo("SensitivePojoWithPlainPrefixLength(string=hello, list=[my, friends], sensitive=****)");
    }

    @Test
    void testToString_sensitiveField_withPrefix_sensitiveValueIsLonger() {
        var pojo = new SensitivePojoWithPlainPrefixLength(STRING, LIST, LONG_SENSITIVE_STR);
        assertThat(SafeToStringBuilder.toString(pojo)).isEqualTo("SensitivePojoWithPlainPrefixLength(string=hello, list=[my, friends], sensitive=I'm g****)");
    }

    @ParameterizedTest
    @ValueSource(strings = {SHORT_SENSITIVE_STR, LONG_SENSITIVE_STR})
    void testToString_sensitiveField_withPrefix_negativeLength(String sensitiveValue) {
        var pojo = new SensitivePojoWithNegativePlainPrefixLength(STRING, LIST, sensitiveValue);
        assertThat(SafeToStringBuilder.toString(pojo)).isEqualTo("SensitivePojoWithNegativePlainPrefixLength(string=hello, list=[my, friends], sensitive=****)");
    }

    @Test
    void testToString_sensitiveField_nested() {
        var pojo = new SensitiveNestedPojo(STRING, LIST, LONG_SENSITIVE_STR, new SensitivePojoWithToString(SHORT_SENSITIVE_STR));
        assertThat(SafeToStringBuilder.toString(pojo)).isEqualTo(
                "SensitiveNestedPojo(string=hello, list=[my, friends], sensitive=I'm****, " +
                        "sensitivePojo=SensitivePojoWithToString(sensitive=****))"
        );
    }

    @Value
    public static class Pojo {
        String string;
        List<String> list; // just to ensure we support more than simple strings
    }

    @Value
    public static class SensitivePojo {
        String string;
        List<String> list; // just to ensure we support more than simple strings

        @Obfuscated
        String sensitive;
    }

    // make sure it works for Java records too
    public record SensitiveRecord(
            String string,
            List<String> list,
            @Obfuscated String sensitive
    ) {
    }

    @Value
    public static class SensitiveNestedPojo {
        String string;
        List<String> list;

        @Obfuscated(plainPrefixLength = 3)
        String sensitive;

        SensitivePojoWithToString sensitivePojo;
    }

    @Value
    public static class SensitivePojoWithToString {
        @Obfuscated
        String sensitive;

        @Override
        public String toString() {
            return SafeToStringBuilder.toString(this);
        }
    }

    @Value
    public static class SensitivePojoWithPlainPrefixLength {
        String string;
        List<String> list;

        @Obfuscated(plainPrefixLength = 5)
        String sensitive;
    }

    @Value
    public static class SensitivePojoWithNegativePlainPrefixLength {
        String string;
        List<String> list;

        @Obfuscated(plainPrefixLength = -5)
        String sensitive;
    }


}