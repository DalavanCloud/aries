/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.spifly;

import java.util.Arrays;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class TCCLSetterVisitor extends ClassAdapter implements ClassVisitor, Opcodes {
    private static final String GENERATED_METHOD_NAME = "$$fixContextClassLoader$$";

    private static final String UTIL_CLASS = Util.class.getName().replace('.', '/'); 
    private static final String VOID_RETURN_TYPE = "()V";
    
    private final String targetClass;

    public TCCLSetterVisitor(ClassVisitor cv, String className) {
        super(cv);
        this.targetClass = className.replace('.', '/');
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
            String signature, String[] exceptions) {
        System.out.println("@@@ " + access + ": " + name + "#" + desc + "#" + signature + "~" + Arrays.toString(exceptions));

        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        return new TCCLSetterMethodVisitor(mv);
    }
    
    @Override
    public void visitEnd() {
        // Add generated static method
        MethodVisitor mv = cv.visitMethod(ACC_PRIVATE + ACC_STATIC,
                GENERATED_METHOD_NAME, "()V", null, null);
        mv.visitCode();
        mv.visitLdcInsn("java.util.ServiceLoader");
        mv.visitLdcInsn("load");
        String typeIdentifier = "L" + targetClass + ";";
        mv.visitLdcInsn(Type.getType(typeIdentifier));
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class",
                "getClassLoader", "()Ljava/lang/ClassLoader;");
        mv.visitMethodInsn(INVOKESTATIC, "org/apache/aries/spifly/Util",
                "fixContextClassloader",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/ClassLoader;)V");
        mv.visitInsn(RETURN);
        mv.visitMaxs(3, 0);
        mv.visitEnd();
        
        super.visitEnd();
    }
    


    private class TCCLSetterMethodVisitor extends MethodAdapter implements MethodVisitor
    {
        public TCCLSetterMethodVisitor(MethodVisitor mv) {
            super(mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name,
                String desc) {
            System.out.println("### " + opcode + ": " + owner + "#" + name + "#" + desc);
            
            if (opcode == INVOKESTATIC &&
                "java/util/ServiceLoader".equals(owner) &&
                "load".equals(name)) {
                System.out.println("+++ Gotcha!");
                                
                mv.visitMethodInsn(INVOKESTATIC, UTIL_CLASS,
                        "storeContextClassloader", VOID_RETURN_TYPE);
                mv.visitMethodInsn(INVOKESTATIC, targetClass,
                        GENERATED_METHOD_NAME, VOID_RETURN_TYPE);

                super.visitMethodInsn(opcode, owner, name, desc);

                mv.visitMethodInsn(INVOKESTATIC, UTIL_CLASS,
                        "restoreContextClassloader", VOID_RETURN_TYPE);
            } else {                
                super.visitMethodInsn(opcode, owner, name, desc);
            }
        }
    }
}
