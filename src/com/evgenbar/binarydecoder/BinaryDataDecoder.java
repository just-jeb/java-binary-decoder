package com.evgenbar.binarydecoder;


import java.io.DataInput;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

import org.apache.commons.lang3.ClassUtils;
import com.google.common.collect.ImmutableMap;

public class BinaryDataDecoder implements IDataInputDecoder {
	final static Map<Class<?>, ThrowingFunction<DataInput, Object>> PrimitiveDecoders = ImmutableMap
			.<Class<?>, ThrowingFunction<DataInput, Object>> builder().put(Boolean.class, (DataInput input) -> input.readBoolean())
			.put(Character.class, (DataInput input) -> input.readChar()).put(Byte.class, (DataInput input) -> input.readByte())
			.put(Short.class, (DataInput input) -> input.readShort()).put(Integer.class, (DataInput input) -> input.readInt())
			.put(Long.class, (DataInput input) -> input.readLong()).put(Float.class, (DataInput input) -> input.readFloat())
			.put(Double.class, (DataInput input) -> input.readDouble()).build();
	final static Map<DecodeUnsigned.IntegerType, ThrowingFunction<DataInput, Object>> UnsignedDecoders = ImmutableMap.<DecodeUnsigned.IntegerType, ThrowingFunction<DataInput, Object>> builder()
			.put(DecodeUnsigned.IntegerType.UnsignedByte, (DataInput input) -> input.readUnsignedByte())
			.put(DecodeUnsigned.IntegerType.UnsignedShort, (DataInput input) -> input.readUnsignedShort())
			.put(DecodeUnsigned.IntegerType.UnsignedInt, (DataInput input) -> Integer.toUnsignedLong(input.readInt())).build();

	@Override
	public <T> T decode(Class<T> type, DataInput input) {
		T object;
		try {
			object = type.newInstance();
			decodeObject(object, type, input);
		} catch (InstantiationException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			throw new RuntimeException(e);
		}
		return object;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T[] decode(Class<T> type, int arrayLength, DataInput input) {
		T[] array;
		try {
			array = (T[]) Array.newInstance(type, arrayLength);
			decodeArray(array, type, null, input);
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		return array;
	}

	private void decodeArray(Object array, Class<?> elementType, DecodeUnsigned decodeUnsigned, DataInput input)
			throws InstantiationException, IllegalAccessException {
		elementType = ClassUtils.primitiveToWrapper(elementType);
		int length = Array.getLength(array);
		for (int i = 0; i < length; i++) {
			Object element;
			if (PrimitiveDecoders.containsKey(elementType)) {
				element = decodePrimitive(input, elementType, decodeUnsigned); // Handle array of primitives
			} else {
				element = decode(elementType, input);
			}
			Array.set(array, i, element);
		}
	}

	private void decodeObject(Object o, Class<?> objectType, DataInput input)
			throws InstantiationException, IllegalAccessException, NoSuchFieldException, SecurityException {
		if (objectType.getDeclaredAnnotation(Decodable.class) == null)
			return;
		Class<?> superType = objectType.getSuperclass();
		if (superType != null) {
			decodeObject(o, superType, input);
		}
		for (Field field : objectType.getDeclaredFields()) {
			if (Modifier.isTransient(field.getModifiers()))
				continue;
			Class<?> fieldType = ClassUtils.primitiveToWrapper(field.getType());

			Object fieldObject;
			field.setAccessible(true);
			if (PrimitiveDecoders.containsKey(fieldType)) {// Field is a primitive
				// Can decode from stream
				fieldObject = decodePrimitive(input, fieldType, field.getAnnotation(DecodeUnsigned.class));
			} else if (!fieldType.isArray()) { // Field is not array nor primitive
				// Skip non-decodable fields
				if (fieldType.getDeclaredAnnotation(Decodable.class) == null)
					continue;
				// Otherwise, decodable, can create a new instance and pass on
				fieldObject = decode(fieldType, input);
			} else {
				SizedBy sizedBy = field.getAnnotation(SizedBy.class);
				if (sizedBy == null) {//Assuming array is initialized with some length
					fieldObject = field.get(o);
				} else {
					Field sizeField = objectType.getDeclaredField(sizedBy.value());
					sizeField.setAccessible(true);
					int size = (int) sizeField.get(o);
					fieldObject = Array.newInstance(fieldType.getComponentType(), size);
				}
				decodeArray(fieldObject, fieldType.getComponentType(), field.getAnnotation(DecodeUnsigned.class), input);
			}
			field.set(o, fieldObject);
		}
	}

	private Object decodePrimitive(DataInput input, Class<?> fieldType, DecodeUnsigned annotation) {

		if (annotation != null) {
			return UnsignedDecoders.get(annotation.value()).apply(input);
		}
		return PrimitiveDecoders.get(fieldType).apply(input);
	}
}
