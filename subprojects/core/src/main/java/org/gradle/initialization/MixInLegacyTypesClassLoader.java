/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.initialization;

import groovy.lang.GroovyObject;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import groovy.lang.MetaClassRegistry;
import org.gradle.api.Nullable;
import org.gradle.internal.classloader.TransformingClassLoader;
import org.gradle.internal.classpath.ClassPath;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class MixInLegacyTypesClassLoader extends TransformingClassLoader {
    private static final Type GROOVY_OBJECT_TYPE = Type.getType(GroovyObject.class);
    private static final Type META_CLASS_REGISTRY_TYPE = Type.getType(MetaClassRegistry.class);
    private static final Type GROOVY_SYSTEM_TYPE = Type.getType(GroovySystem.class);
    private static final Type META_CLASS_TYPE = Type.getType(MetaClass.class);
    private static final Type OBJECT_TYPE = Type.getType(Object.class);
    private static final Type CLASS_TYPE = Type.getType(Class.class);
    private static final Type STRING_TYPE = Type.getType(String.class);

    private static final String RETURN_OBJECT_FROM_OBJECT_STRING_OBJECT = Type.getMethodDescriptor(OBJECT_TYPE, OBJECT_TYPE, STRING_TYPE, OBJECT_TYPE);
    private static final String RETURN_OBJECT_FROM_STRING_OBJECT = Type.getMethodDescriptor(OBJECT_TYPE, STRING_TYPE, OBJECT_TYPE);
    private static final String RETURN_OBJECT_FROM_STRING = Type.getMethodDescriptor(OBJECT_TYPE, STRING_TYPE);
    private static final String RETURN_OBJECT_FROM_OBJECT_STRING = Type.getMethodDescriptor(OBJECT_TYPE, OBJECT_TYPE, STRING_TYPE);
    private static final String RETURN_VOID_FROM_OBJECT_STRING_OBJECT = Type.getMethodDescriptor(Type.VOID_TYPE, OBJECT_TYPE, STRING_TYPE, OBJECT_TYPE);
    private static final String RETURN_VOID_FROM_STRING_OBJECT = Type.getMethodDescriptor(Type.VOID_TYPE, STRING_TYPE, OBJECT_TYPE);
    private static final String RETURN_META_CLASS_REGISTRY = Type.getMethodDescriptor(META_CLASS_REGISTRY_TYPE);
    private static final String RETURN_META_CLASS_FROM_CLASS = Type.getMethodDescriptor(META_CLASS_TYPE, CLASS_TYPE);
    private static final String RETURN_META_CLASS = Type.getMethodDescriptor(META_CLASS_TYPE);
    private static final String RETURN_CLASS = Type.getMethodDescriptor(CLASS_TYPE);

    private static final String META_CLASS_FIELD = "__meta_class__";

    private final Set<String> classesToMixInGroovyObject;
    private final Set<String> syntheticClasses;

    public MixInLegacyTypesClassLoader(ClassLoader parent, ClassPath classPath) {
        super(parent, classPath);
        classesToMixInGroovyObject = readClassNames("converted-types.txt");
        syntheticClasses = readClassNames("removed-types.txt");
    }

    private Set<String> readClassNames(String resourceName) {
        Set<String> classNames = new HashSet<String>();
        URL resource = getClass().getResource(resourceName);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream()));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    classNames.add(line.trim());
                }
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not load class names from '" + resource + "'.", e);
        }
        return classNames;
    }

    @Nullable
    @Override
    protected byte[] generateMissingClass(String name) {
        if (!syntheticClasses.contains(name)) {
            return null;
        }
        ClassWriter visitor = new ClassWriter(0);
        visitor.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC, name.replace('.', '/'), null, OBJECT_TYPE.getInternalName(), null);
        visitor.visitEnd();
        return visitor.toByteArray();
    }

    @Override
    protected boolean shouldTransform(String className) {
        return classesToMixInGroovyObject.contains(className) || syntheticClasses.contains(className);
    }

    @Override
    protected byte[] transform(String className, byte[] bytes) {
        ClassReader classReader = new ClassReader(bytes);
        ClassWriter classWriter = new ClassWriter(0);
        classReader.accept(new TransformingAdapter(classWriter), 0);
        bytes = classWriter.toByteArray();
        return bytes;
    }

    private static class TransformingAdapter extends ClassVisitor {
        private String className;

        TransformingAdapter(ClassVisitor cv) {
            super(Opcodes.ASM5, cv);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.className = name;

            Set<String> interfaceNames = new LinkedHashSet<String>(Arrays.asList(interfaces));
            interfaceNames.add(GROOVY_OBJECT_TYPE.getInternalName());
            cv.visit(version, access, name, signature, superName, interfaceNames.toArray(new String[0]));
        }

        @Override
        public void visitEnd() {
            addMetaClassField();
            addGetMetaClass();
            addSetMetaClass();
            addGetProperty();
            addSetProperty();
            addInvokeMethod();
            cv.visitEnd();
        }

        private void addMetaClassField() {
            cv.visitField(Opcodes.ACC_PRIVATE, META_CLASS_FIELD, META_CLASS_TYPE.getDescriptor(), null, null);
        }

        private void addGetProperty() {
            MethodVisitor methodVisitor = cv.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, "getProperty", RETURN_OBJECT_FROM_STRING, null, null);
            methodVisitor.visitCode();

            // this.getMetaClass()
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, "getMetaClass", RETURN_META_CLASS, false);

            // getProperty(this, name)
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, META_CLASS_TYPE.getInternalName(), "getProperty", RETURN_OBJECT_FROM_OBJECT_STRING, true);

            // return
            methodVisitor.visitInsn(Opcodes.ARETURN);
            methodVisitor.visitMaxs(3, 2);
            methodVisitor.visitEnd();
        }

        private void addSetProperty() {
            MethodVisitor methodVisitor = cv.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, "setProperty",  RETURN_VOID_FROM_STRING_OBJECT, null, null);
            methodVisitor.visitCode();

            // this.getMetaClass()
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, "getMetaClass", RETURN_META_CLASS, false);

            // setProperty(this, name, value)
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, META_CLASS_TYPE.getInternalName(), "setProperty",  RETURN_VOID_FROM_OBJECT_STRING_OBJECT, true);

            methodVisitor.visitInsn(Opcodes.RETURN);
            methodVisitor.visitMaxs(4, 3);
            methodVisitor.visitEnd();
        }

        private void addInvokeMethod() {
            MethodVisitor methodVisitor = cv.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, "invokeMethod", RETURN_OBJECT_FROM_STRING_OBJECT, null, null);
            methodVisitor.visitCode();

            // this.getMetaClass()
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, "getMetaClass", RETURN_META_CLASS, false);

            // invokeMethod(this, name, args)
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, META_CLASS_TYPE.getInternalName(), "invokeMethod",  RETURN_OBJECT_FROM_OBJECT_STRING_OBJECT, true);

            // return
            methodVisitor.visitInsn(Opcodes.ARETURN);
            methodVisitor.visitMaxs(4, 3);
            methodVisitor.visitEnd();
        }

        private void addGetMetaClass() {
            Label lookup = new Label();

            MethodVisitor methodVisitor = cv.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, "getMetaClass", RETURN_META_CLASS, null, null);
            methodVisitor.visitCode();

            // if (this.metaClass != null) { return this.metaClass; }
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitFieldInsn(Opcodes.GETFIELD, className, META_CLASS_FIELD, META_CLASS_TYPE.getDescriptor());
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitJumpInsn(Opcodes.IFNULL, lookup);
            methodVisitor.visitInsn(Opcodes.ARETURN);

            methodVisitor.visitLabel(lookup);
            methodVisitor.visitFrame(Opcodes.F_NEW, 1, new Object[]{className}, 1, new Object[]{META_CLASS_TYPE.getInternalName()});
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0); // for storing to field

            // GroovySystem.getMetaClassRegistry()
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, GROOVY_SYSTEM_TYPE.getInternalName(), "getMetaClassRegistry", RETURN_META_CLASS_REGISTRY, false);

            // this.getClass()
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, OBJECT_TYPE.getInternalName(), "getClass", RETURN_CLASS, false);

            // getMetaClass(..)
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, META_CLASS_REGISTRY_TYPE.getInternalName(), "getMetaClass", RETURN_META_CLASS_FROM_CLASS, true);

            // this.metaClass = <value>
            methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, className, META_CLASS_FIELD, META_CLASS_TYPE.getDescriptor());

            // return this.metaClass
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitFieldInsn(Opcodes.GETFIELD, className, META_CLASS_FIELD, META_CLASS_TYPE.getDescriptor());

            methodVisitor.visitInsn(Opcodes.ARETURN);
            methodVisitor.visitMaxs(4, 1);
            methodVisitor.visitEnd();
        }

        private void addSetMetaClass() {
            MethodVisitor methodVisitor = cv.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, "setMetaClass", Type.getMethodDescriptor(Type.VOID_TYPE, META_CLASS_TYPE), null, null);
            methodVisitor.visitCode();

            // this.metaClass = <value>
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, className, META_CLASS_FIELD, META_CLASS_TYPE.getDescriptor());

            methodVisitor.visitInsn(Opcodes.RETURN);
            methodVisitor.visitMaxs(2, 2);
            methodVisitor.visitEnd();
        }

    }
}
