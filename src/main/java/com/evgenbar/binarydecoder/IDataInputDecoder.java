package com.evgenbar.binarydecoder;


import java.io.DataInput;

public interface IDataInputDecoder {
	/**
	 * Decodes data input to the object of given type
	 * @param type type to decode
	 * @param input input to decode from
	 */
	<T> T decode(Class<T> type, DataInput input);
	<T> T[] decode(Class<T> type, int arrayLength, DataInput input);
}
