package com.evgenbar.binarydecoder.tests;

import static org.junit.Assert.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import static org.hamcrest.CoreMatchers.*;

import com.evgenbar.binarydecoder.BinaryDataDecoder;
import com.evgenbar.binarydecoder.Decodable;
import com.evgenbar.binarydecoder.DecodeUnsigned;
import com.evgenbar.binarydecoder.DecodeUnsigned.IntegerType;
import com.evgenbar.binarydecoder.IDataInputDecoder;
import com.evgenbar.binarydecoder.SizedBy;

public class BinaryDataDecoderTest {

	IDataInputDecoder decoder = new BinaryDataDecoder();
	DataInputStream mInputStream;
	DataOutputStream mOutStream;


	@Before
	public void setUp() throws IOException {
		PipedInputStream input = new PipedInputStream();
		mOutStream = new DataOutputStream(new PipedOutputStream(input));
		mInputStream = new DataInputStream(input);
	}

	@After
	public void shutDown() throws IOException {
		mOutStream.close();
		mInputStream.close();
	}
	
	@Test
	public void testDecodeClassWithTransient() throws IOException {
		double d = 1.0;
		long l = 10;
		DecodableWithTransient.b = l;
		mOutStream.writeDouble(d);
		mOutStream.writeLong(20L);
		mOutStream.flush();
		DecodableWithTransient actual = decoder.decode(DecodableWithTransient.class, mInputStream);
		assertEquals(d, actual.a, 1E-15);
		assertEquals(l, DecodableWithTransient.b);
	}
	
	@Test
	public void testDecodeClassWithPrimitives() throws IOException {
		Primitives expected = new Primitives(true, 'g', (byte) 0x7, (short) 344, 654, 0xFFDA, 65.67f, 436.546d);
		writePrimitives(expected);
		mOutStream.flush();
		Primitives actual = decoder.decode(Primitives.class, mInputStream);
		assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
	}

	@Test
	public void testDecodeIntPrimitiveArray() throws IOException {
		int[] expected = new int[] { 1, 5, -7, 3, 0, -244 };
		writeIntArray(expected);
		Integer[] actual = decoder.decode(Integer.class, expected.length, mInputStream);
		assertArrayEquals(expected, ArrayUtils.toPrimitive(actual));
	}

	@Test
	public void testDecodeDoubleNonPrimitiveArray() throws IOException {
		double[] expected = new double[] { 1.1d, 5.6d, -78.32d, 3d, 0d, -244.32d };
		writeDoubleArray(expected);
		Double[] actual = decoder.decode(Double.class, expected.length, mInputStream);
		assertArrayEquals(ArrayUtils.toObject(expected), actual);
	}

	@Test
	public void testDecodeUnsigned() throws IOException {
		SignedUnsigned expected = new SignedUnsigned(0.54f, 0XFF, -1, 0xFFFF, 0xFFFFFFFFl);
		writeSignedUsigned(expected);
		mOutStream.flush();
		SignedUnsigned actual = decoder.decode(SignedUnsigned.class, mInputStream);
		assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
	}

	@Test
	public void testDecodeInheritance() throws IOException {
		mOutStream.writeFloat(6.6f);
		mOutStream.writeDouble(7.7d);
		mOutStream.flush();
		DecodableChild child = decoder.decode(DecodableChild.class, mInputStream);
		assertEquals(5, child.a);
		assertEquals(6.6f, child.b, 1E-15);
		assertEquals(7.7d, child.c, 1E-15);
	}

	@Test
	public void testDecodeUnsignedArray() throws IOException {
		int[] array = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, -1 };
		writeIntArray(array);
		mOutStream.flush();
		UnsignedArrayContainer container = decoder.decode(UnsignedArrayContainer.class, mInputStream);
		long[] expected = new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0xFFFFFFFFl };		
		assertArrayEquals(expected, container.array);
	}

	@Test
	public void testDecodeComplexObjectSkipNonDecodable() throws IOException {
		ComplexDecodable expected = writeComplexDecodable();
		mOutStream.flush();
		ComplexDecodable actual = decoder.decode(ComplexDecodable.class, mInputStream);
		assertEquals(expected, actual);
	}
	
	@Rule
    public ExpectedException thrown = ExpectedException.none();
	
	@Test
	public void testSizedByException() throws IOException {
		int[] array = new int[]{1,2,3,4};
		writeIntArray(array);
		mOutStream.flush();
		thrown.expectCause(isA(NoSuchFieldException.class));
		//thrown.expect(RuntimeException.class,"l");
		decoder.decode(SizedByWithoutSize.class, mInputStream);
	}
	
	@Test
	public void testUnsignedSizedBy() throws IOException {
		int[] array = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, -1 };
		mOutStream.writeInt(array.length);
		writeIntArray(array);
		mOutStream.flush();
		SizedByWithSize sized = decoder.decode(SizedByWithSize.class, mInputStream);
		long[] expected =  new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0xFFFFFFFFl };
		assertArrayEquals(expected, sized.array);
	}

	@Test
	public void testDecodeArrayOfComplexObjects() throws IOException {
		ComplexDecodable[] expected = new ComplexDecodable[] { writeComplexDecodable(), writeComplexDecodable(), writeComplexDecodable() };
		mOutStream.flush();
		ComplexDecodable[] actual = decoder.decode(ComplexDecodable.class, 3, mInputStream);
		assertArrayEquals(expected, actual);
	}

	private ComplexDecodable writeComplexDecodable() throws IOException {
		ComplexDecodable decodable = new ComplexDecodable();
		decodable.array = new double[] { 1.3, 65.7, -345.8 };
		writeDoubleArray(decodable.array);
		decodable.primitives = new Primitives(true, 'f', (byte) 0xFF, (short) 344, 654, 0xFFDA, 65.67f, 436.546d);
		writePrimitives(decodable.primitives);
		decodable.container = new UnsignedArrayContainer();
		decodable.container.array = new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0xFFFFFFFFl };
		writeIntArray(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, -1 });
		decodable.child = new DecodableChild();
		decodable.child.b = 6.5f;
		mOutStream.writeFloat(decodable.child.b);
		decodable.child.c = -9.8d;
		mOutStream.writeDouble(decodable.child.c);

		return decodable;
	}

	private void writePrimitives(Primitives p) throws IOException {
		mOutStream.writeBoolean(p.mBool);
		mOutStream.writeChar(p.mChar);
		mOutStream.writeByte(p.mByte);
		mOutStream.writeShort(p.mShort);
		mOutStream.writeInt(p.mInt);
		mOutStream.writeLong(p.mLong);
		mOutStream.writeFloat(p.mFloat);
		mOutStream.writeDouble(p.mDouble);
	}

	private void writeSignedUsigned(SignedUnsigned su) throws IOException {
		mOutStream.writeFloat(su.mFloat);
		mOutStream.writeByte(su.mUnsignedByte);
		mOutStream.writeInt(su.mSignedInt);
		mOutStream.writeShort(su.mUnsignedShort);
		mOutStream.writeInt((int) su.mUnsignedInt);
	}

	private void writeIntArray(int[] array) throws IOException {
		for (int i : array) {
			mOutStream.writeInt(i);
		}
	}

	private void writeDoubleArray(double[] array) throws IOException {
		for (double i : array) {
			mOutStream.writeDouble(i);
		}
	}
	
	@Decodable
	public static class SizedByWithoutSize {
		@SizedBy("size")
		int[] array;
	}
	
	@Decodable
	public static class SizedByWithSize {
		int size;
		@SizedBy("size")
		@DecodeUnsigned(IntegerType.UnsignedInt)
		long[] array;
	}


	@Decodable
	public static class ComplexDecodable {
		double[] array = new double[3];
		Primitives primitives;
		UnsignedArrayContainer container;
		String nonDecodable = "don't touch me";
		DecodableChild child;

		@Override
		public boolean equals(Object o) {
			ComplexDecodable c = (ComplexDecodable) o;
			return EqualsBuilder.reflectionEquals(primitives, c.primitives) && EqualsBuilder.reflectionEquals(container, c.container)
					&& nonDecodable.equals(c.nonDecodable) && EqualsBuilder.reflectionEquals(child, c.child) && Arrays.equals(array, c.array);

		}
	}

	@Decodable
	public static class UnsignedArrayContainer {
		@DecodeUnsigned(IntegerType.UnsignedInt)
		long[] array = new long[10];
	}

	public static class NonDecodable {
		int a = 5;
	}

	@Decodable
	public static class DecodeableParent extends NonDecodable {
		float b;
	}

	@Decodable
	public static class DecodableChild extends DecodeableParent {
		double c;
	}
	
	@Decodable
	public static class DecodableWithTransient {
		double a;
		transient static long b;
	}

	@Decodable
	public static class SignedUnsigned {
		public SignedUnsigned() {
		};

		public SignedUnsigned(float f, int unsignedByte, int signedInt, int unsignedShort, long unsignedInt) {
			mFloat = f;
			mUnsignedByte = unsignedByte;
			mSignedInt = signedInt;
			mUnsignedShort = unsignedShort;
			mUnsignedInt = unsignedInt;
		}

		Float mFloat;
		@DecodeUnsigned(IntegerType.UnsignedByte)
		int mUnsignedByte;
		int mSignedInt;
		@DecodeUnsigned(IntegerType.UnsignedShort)
		int mUnsignedShort;
		@DecodeUnsigned(IntegerType.UnsignedInt)
		long mUnsignedInt;
	}

	@Decodable
	public static class Primitives {
		public Primitives() {
		};

		public Primitives(boolean b, char c, byte y, short s, int i, long l, float f, double d) {
			mBool = b;
			mChar = c;
			mByte = y;
			mShort = s;
			mInt = i;
			mLong = l;
			mFloat = f;
			mDouble = d;
		}

		boolean mBool;
		char mChar;
		byte mByte;
		short mShort;
		int mInt;
		long mLong;
		float mFloat;
		double mDouble;
	}
}
