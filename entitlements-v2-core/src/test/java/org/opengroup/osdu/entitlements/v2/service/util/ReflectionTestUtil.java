package org.opengroup.osdu.entitlements.v2.service.util;

import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

public class ReflectionTestUtil {

    public static <T> void setFieldValueForClass(T targetObject, String fieldName, Object fieldValue) {
        Field field = ReflectionUtils.findField(targetObject.getClass(), fieldName);
        field.setAccessible(true);
        ReflectionUtils.setField(field, targetObject, fieldValue);
    }
}
