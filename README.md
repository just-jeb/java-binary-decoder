# java-binary-decoder [![Build Status](https://travis-ci.org/meltedspark/java-binary-decoder.svg?branch=master)](https://travis-ci.org/meltedspark/java-binary-decoder)
Simple library for decoding C++\C-like structures serialized to a binary buffer and converting them to Java classes.  

## Purpose
The library is intended to ease the maintenance of protocol between Java and the side that defines the binary structure.  
The decoding is reflection based, thus it eliminates the need for chaning the decoding code when the binary structure changes. The only thing that has to be changed is a corresponding Java class.

## Features
* Signed\Unsigned integer types
* Hierarchical structures
* Nested structures
* Arrays (primitives and complex structures)
	* Arrays with constant size
	* Arrays with size determined by another structure member

## How to use

### Simple structure:
Given the following structure being serialized to a binary buffer:
```c++
struct mystruct 
{
	bool cBoolean;
	char cChar;
    unsigned char cByte; 
    short cShort;
    int cInt;
    long cLong;
    float cFloat;
    double cDouble;    
}
```
Assuming that serialization is done by the following rules: `boolean` is 1 byte, `char` is 1 byte, `short` is 2 bytes, `int` is 4 bytes, `long` is 8 bytes, `float` is 4 bytes and `double` is 8 bytes, the specified binary buffer consists of `29` bytes and includes all the data according to its apearance in the structure.  
On the Java side the corresponding class will look like this:
```java
@Decodable
public class MyJavaClass {
	public boolean javaBoolean;
    public char javaChar;
    public byte javaByte;
    public short javaShort;
    public int javaInt;
    public long javaLong;
    public float javaFloat;
    public double javaDouble;
}
```
The decoding itself:
```java
IDataInputDecoder decoder = new BinaryDataDecoder();
try(DataInput dis = new DataInputStream(new BufferedInputStream(new ByteArrayInputStream(buffer)))){
	MyJavaClass instance = decoder.decode(MyJavaClass.class, dis);
}
```
