package vn.com.lcx.common.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ObjectUtils {

    private ObjectUtils() {
    }

    public static <SOURCE, TARGET> TARGET mapObjects(SOURCE source, Class<TARGET> target) {
        // Get all field of source class and target class
        List<Field> sourceClassFields = new ArrayList<>(Arrays.asList(source.getClass().getDeclaredFields()));
        List<Field> targetClassFields = new ArrayList<>(Arrays.asList(target.getDeclaredFields()));
        // Get all field of super class of both
        if (source.getClass().getSuperclass() != null) {
            List<Field> superClassField = Arrays.asList(source.getClass().getSuperclass().getDeclaredFields());
            sourceClassFields.addAll(superClassField);
        }
        if (target.getSuperclass() != null) {
            List<Field> superClassField = Arrays.asList(target.getSuperclass().getDeclaredFields());
            targetClassFields.addAll(superClassField);
        }
        // Filter: only get fields in source that its name is included in target
        List<String> targetClassFieldNames = targetClassFields.stream().map(Field::getName).collect(Collectors.toList());
        sourceClassFields = sourceClassFields
                .stream()
                .filter(field -> targetClassFieldNames.contains(field.getName()))
                .collect(Collectors.toList());
        targetClassFields.sort(Comparator.comparing(Field::getName));
        sourceClassFields.sort(Comparator.comparing(Field::getName));
        TARGET result;
        try {
            result = target.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return null;
        }
        // Start mapping
        // Lưu ý: khi dùng field.set(ObjectToSet, valueToSetToFeild) thì ObjectToSet phải là Object mà field đó thuộc về, nó hoạt động giống như setter,
        // tương tự với value = field.get(ObjectToGet), nó hoạt động giống như getter (phải dùng hàm field.setAccessible(true) trước)
        sourceClassFields.forEach(field -> {
            field.setAccessible(true);
            Object value;
            try {
                if (!Modifier.isFinal(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())) {
                    value = field.get(source);
                    if (value != null) {
                        Field fieldToSet = targetClassFields.stream().filter(field1 -> field1.getName().equals(field.getName())).collect(Collectors.toList()).get(0);
                        fieldToSet.setAccessible(true);
                        fieldToSet.set(result, value);
                    }
                }
            } catch (Exception e) {
                LogUtils.writeLog(e.getMessage(), e);
            }
        });
        return result;
    }

    public static boolean isNullOrEmpty(Object object) {
        if (!Optional.ofNullable(object).isPresent()) {
            return true;
        }
        if (object instanceof Iterable) {
            Collection<?> objectChecking = (Collection<?>) object;
            return objectChecking.isEmpty();
        }
        if (object instanceof String) {
            String objectChecking = (String) object;
            return objectChecking.trim().isEmpty();
        }
        return false;
    }

}
