package com.tuxprogrammer.lostcities.statics;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The parts of a class file the Java 8 API check needs: the class's name, its supertypes, and the names and
 * descriptors of its fields and methods. A small reader of its own rather than ASM, because the JDK's {@code ct.sym}
 * signature files use the newest class-file version, which the ASM on the 1.7.10 test classpath refuses to read.
 *
 * <p>
 * Format: JVM Specification §4.1 (ClassFile), §4.4 (constant pool), §4.5 and §4.6 (fields and methods).
 */
final class ClassFileSummary {

    private static final int MAGIC = 0xCAFEBABE;

    final String name;
    final String superName;
    final List<String> interfaces;
    /** {@code name + ":" + descriptor} of every field. */
    final Set<String> fields;
    /** {@code name + descriptor} of every method. */
    final Set<String> methods;
    /** Names of every method, for signature-polymorphic lookups. */
    final Set<String> methodNames;

    private ClassFileSummary(String name, String superName, List<String> interfaces, Set<String> fields,
        Set<String> methods, Set<String> methodNames) {
        this.name = name;
        this.superName = superName;
        this.interfaces = interfaces;
        this.fields = fields;
        this.methods = methods;
        this.methodNames = methodNames;
    }

    /**
     * Parses a class file.
     *
     * @param bytes the class file
     * @return its summary
     * @throws IOException when the bytes are not a class file
     */
    static ClassFileSummary parse(byte[] bytes) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        if (in.readInt() != MAGIC) {
            throw new IOException("not a class file");
        }
        in.readUnsignedShort(); // minor version
        in.readUnsignedShort(); // major version
        int count = in.readUnsignedShort();
        String[] utf8 = new String[count];
        int[] classNameIndex = new int[count];
        for (int i = 1; i < count; i++) {
            int tag = in.readUnsignedByte();
            switch (tag) {
                case 1: // Utf8
                    utf8[i] = in.readUTF();
                    break;
                case 7: // Class
                    classNameIndex[i] = in.readUnsignedShort();
                    break;
                case 8: // String
                case 16: // MethodType
                case 19: // Module
                case 20: // Package
                    in.readUnsignedShort();
                    break;
                case 15: // MethodHandle
                    in.readUnsignedByte();
                    in.readUnsignedShort();
                    break;
                case 3: // Integer
                case 4: // Float
                case 9: // Fieldref
                case 10: // Methodref
                case 11: // InterfaceMethodref
                case 12: // NameAndType
                case 17: // Dynamic
                case 18: // InvokeDynamic
                    in.readInt();
                    break;
                case 5: // Long
                case 6: // Double
                    in.readLong();
                    i++; // eight-byte constants take two slots
                    break;
                default:
                    throw new IOException("unknown constant-pool tag " + tag + " at entry " + i);
            }
        }
        in.readUnsignedShort(); // access flags
        String name = utf8[classNameIndex[in.readUnsignedShort()]];
        int superIndex = in.readUnsignedShort();
        String superName = superIndex == 0 ? null : utf8[classNameIndex[superIndex]];
        int interfaceCount = in.readUnsignedShort();
        List<String> interfaces = new ArrayList<>();
        for (int i = 0; i < interfaceCount; i++) {
            interfaces.add(utf8[classNameIndex[in.readUnsignedShort()]]);
        }
        Set<String> fields = new HashSet<>();
        int fieldCount = in.readUnsignedShort();
        for (int i = 0; i < fieldCount; i++) {
            in.readUnsignedShort(); // access flags
            String fieldName = utf8[in.readUnsignedShort()];
            String desc = utf8[in.readUnsignedShort()];
            fields.add(fieldName + ":" + desc);
            skipAttributes(in);
        }
        Set<String> methods = new HashSet<>();
        Set<String> methodNames = new HashSet<>();
        int methodCount = in.readUnsignedShort();
        for (int i = 0; i < methodCount; i++) {
            in.readUnsignedShort(); // access flags
            String methodName = utf8[in.readUnsignedShort()];
            String desc = utf8[in.readUnsignedShort()];
            methods.add(methodName + desc);
            methodNames.add(methodName);
            skipAttributes(in);
        }
        return new ClassFileSummary(
            name,
            superName,
            Collections.unmodifiableList(interfaces),
            fields,
            methods,
            methodNames);
    }

    private static void skipAttributes(DataInputStream in) throws IOException {
        int count = in.readUnsignedShort();
        for (int i = 0; i < count; i++) {
            in.readUnsignedShort(); // name
            int length = in.readInt();
            int skipped = 0;
            while (skipped < length) {
                int n = in.skipBytes(length - skipped);
                if (n <= 0) {
                    throw new IOException("truncated attribute");
                }
                skipped += n;
            }
        }
    }
}
