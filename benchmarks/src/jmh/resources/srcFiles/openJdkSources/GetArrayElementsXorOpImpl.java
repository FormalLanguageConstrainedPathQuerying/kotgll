package org.openjdk.bench.java.lang.foreign.xor;

public class GetArrayElementsXorOpImpl implements XorOp {

    static {
        System.loadLibrary("jnitest");
    }

    public native void xor(byte[] src, int sOff, byte[] dst, int dOff, int len);
}
