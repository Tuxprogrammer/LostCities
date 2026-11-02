package com.tuxprogrammer.lostcities.statics;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Collects what compiled code refers to: the classes a class, field or method names (supertypes, descriptors,
 * annotations, every type an instruction names) and the fields and methods its instructions use. Generic signatures
 * are ignored, since they never load a class.
 */
final class TypeRefs {

    /** Descriptor of FML's {@code @SideOnly}. */
    static final String SIDE_ONLY = "Lcpw/mods/fml/relauncher/SideOnly;";

    private TypeRefs() {}

    /** A field or method an instruction or method handle uses. */
    static final class MemberRef {

        final String owner;
        final String name;
        final String desc;
        final boolean field;

        MemberRef(String owner, String name, String desc, boolean field) {
            this.owner = owner;
            this.name = name;
            this.desc = desc;
            this.field = field;
        }

        @Override
        public String toString() {
            return owner.replace('/', '.') + (field ? "." : "#") + name + (field ? "" : desc);
        }
    }

    /** Returns the types the class header names: superclass, interfaces and class annotations. */
    static Set<String> ofHeader(ClassNode node) {
        Set<String> out = new TreeSet<>();
        if (node.superName != null) {
            out.add(node.superName);
        }
        if (node.interfaces != null) {
            out.addAll(node.interfaces);
        }
        annotations(node.visibleAnnotations, out);
        annotations(node.invisibleAnnotations, out);
        return out;
    }

    /** Returns the types a field names. */
    static Set<String> of(FieldNode field) {
        Set<String> out = new TreeSet<>();
        desc(field.desc, out);
        annotations(field.visibleAnnotations, out);
        annotations(field.invisibleAnnotations, out);
        return out;
    }

    /** Returns the types a method names in its descriptor, annotations, exceptions, locals and code. */
    static Set<String> of(MethodNode method) {
        Set<String> out = new TreeSet<>();
        methodDesc(method.desc, out);
        if (method.exceptions != null) {
            out.addAll(method.exceptions);
        }
        annotations(method.visibleAnnotations, out);
        annotations(method.invisibleAnnotations, out);
        if (method.tryCatchBlocks != null) {
            for (TryCatchBlockNode block : method.tryCatchBlocks) {
                if (block.type != null) {
                    out.add(block.type);
                }
            }
        }
        if (method.localVariables != null) {
            for (LocalVariableNode local : method.localVariables) {
                desc(local.desc, out);
            }
        }
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof TypeInsnNode) {
                internalOrArray(((TypeInsnNode) insn).desc, out);
            } else if (insn instanceof FieldInsnNode) {
                FieldInsnNode f = (FieldInsnNode) insn;
                internalOrArray(f.owner, out);
                desc(f.desc, out);
            } else if (insn instanceof MethodInsnNode) {
                MethodInsnNode m = (MethodInsnNode) insn;
                internalOrArray(m.owner, out);
                methodDesc(m.desc, out);
            } else if (insn instanceof InvokeDynamicInsnNode) {
                InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
                methodDesc(indy.desc, out);
                handle(indy.bsm, out);
                for (Object arg : indy.bsmArgs) {
                    constant(arg, out);
                }
            } else if (insn instanceof LdcInsnNode) {
                constant(((LdcInsnNode) insn).cst, out);
            } else if (insn instanceof MultiANewArrayInsnNode) {
                desc(((MultiANewArrayInsnNode) insn).desc, out);
            }
        }
        return out;
    }

    /** Returns every field and method a method's instructions and method handles use. */
    static List<MemberRef> members(MethodNode method) {
        List<MemberRef> out = new ArrayList<>();
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof FieldInsnNode) {
                FieldInsnNode f = (FieldInsnNode) insn;
                out.add(new MemberRef(f.owner, f.name, f.desc, true));
            } else if (insn instanceof MethodInsnNode) {
                MethodInsnNode m = (MethodInsnNode) insn;
                out.add(new MemberRef(m.owner, m.name, m.desc, false));
            } else if (insn instanceof InvokeDynamicInsnNode) {
                InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
                addHandle(indy.bsm, out);
                for (Object arg : indy.bsmArgs) {
                    if (arg instanceof Handle) {
                        addHandle((Handle) arg, out);
                    }
                }
            } else if (insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof Handle) {
                addHandle((Handle) ((LdcInsnNode) insn).cst, out);
            }
        }
        return out;
    }

    /** Returns {@code true} when the annotations hold {@code @SideOnly(Side.CLIENT)}. */
    static boolean isClientOnly(List<AnnotationNode> annotations) {
        if (annotations == null) {
            return false;
        }
        for (AnnotationNode annotation : annotations) {
            if (!SIDE_ONLY.equals(annotation.desc) || annotation.values == null) {
                continue;
            }
            for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
                Object value = annotation.values.get(i + 1);
                if ("value".equals(annotation.values.get(i)) && value instanceof String[]
                    && "CLIENT".equals(((String[]) value)[1])) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void addHandle(Handle handle, List<MemberRef> out) {
        boolean field = handle.getTag() <= Opcodes.H_PUTSTATIC;
        out.add(new MemberRef(handle.getOwner(), handle.getName(), handle.getDesc(), field));
    }

    private static void annotations(List<AnnotationNode> list, Set<String> out) {
        if (list == null) {
            return;
        }
        for (AnnotationNode annotation : list) {
            desc(annotation.desc, out);
        }
    }

    private static void constant(Object constant, Set<String> out) {
        if (constant instanceof Type) {
            Type type = (Type) constant;
            if (type.getSort() == Type.METHOD) {
                methodDesc(type.getDescriptor(), out);
            } else {
                type(type, out);
            }
        } else if (constant instanceof Handle) {
            handle((Handle) constant, out);
        }
    }

    private static void handle(Handle handle, Set<String> out) {
        internalOrArray(handle.getOwner(), out);
        if (handle.getDesc()
            .startsWith("(")) {
            methodDesc(handle.getDesc(), out);
        } else {
            desc(handle.getDesc(), out);
        }
    }

    private static void methodDesc(String desc, Set<String> out) {
        Type method = Type.getMethodType(desc);
        type(method.getReturnType(), out);
        for (Type argument : method.getArgumentTypes()) {
            type(argument, out);
        }
    }

    private static void desc(String desc, Set<String> out) {
        if (desc != null) {
            type(Type.getType(desc), out);
        }
    }

    private static void internalOrArray(String name, Set<String> out) {
        if (name.startsWith("[")) {
            desc(name, out);
        } else {
            out.add(name);
        }
    }

    private static void type(Type type, Set<String> out) {
        Type element = type.getSort() == Type.ARRAY ? type.getElementType() : type;
        if (element.getSort() == Type.OBJECT) {
            out.add(element.getInternalName());
        }
    }
}
