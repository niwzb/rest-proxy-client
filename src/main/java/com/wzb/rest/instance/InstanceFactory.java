package com.wzb.rest.instance;


import com.wzb.rest.cache.ClientCacheFactory;
import com.wzb.rest.invoke.Invoker;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 实例工厂
 */
public final class InstanceFactory {

    private static final Logger logger = LoggerFactory.getLogger(InstanceFactory.class);

    private static final ClientCacheFactory factory = ClientCacheFactory.getInstance();

    /**
     * init
     */
    private InstanceFactory() {

    }

    /**
     * 创建代理
     *
     * @param interfaceClass 接口类
     * @param <T>            泛型
     * @return {@link T}
     */
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(Class<T> interfaceClass) {
        return (T) Proxy.newProxyInstance(getClassLoader(), new Class<?>[]{interfaceClass},
                (proxy, method, args) -> {
                    String methodKey = factory.generateMethodKey(method);
                    List<Object> argsList = null == args
                            ? new ArrayList<>()
                            : Stream.of(args).collect(Collectors.toList());
                    return Invoker.invoke(methodKey, argsList, method.getReturnType());
                });
    }

    /**
     * 创建实例
     *
     * @param interfaceClass 接口类
     * @param <T>            泛型
     * @return {@link T}
     */
    public static <T> T createInstance(Class<T> interfaceClass) {
        Method[] methods = interfaceClass.getMethods();
        //过滤default、static修饰方法
        List<Method> methodList = Stream.of(methods)
                .filter(method -> !method.isDefault() && !Modifier.isStatic(method.getModifiers()))
                .collect(Collectors.toList());
        //包路径
        String packageStr = interfaceClass.getName().substring(0, interfaceClass.getName().lastIndexOf('.'));
        //随机包路径及文件名
        int packageRandInt = new Random().nextInt(100);
        while (packageRandInt < 1) {
            packageRandInt = new Random().nextInt(100);
        }
        int nameRandInt = new Random().nextInt(packageRandInt);
        //实例类名
        String instanceClassName = String.format(packageStr.concat(".RestClientProxy%d"), packageRandInt, nameRandInt);
        //实例实现方法定义
        List<String> methodDefinitionList = methodList.stream()
                .map(method -> definitionMethod(interfaceClass, method)).collect(Collectors.toList());

        ClassLoader classLoader = getClassLoader();
        ClassPool classPool = new ClassPool(true);
        classPool.appendClassPath(new LoaderClassPath(classLoader));
        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) toClass(classPool, InstanceFactory.class.getProtectionDomain(),
                classLoader, instanceClassName, methodDefinitionList, interfaceClass);
        if (null != clazz) {
            try {
                return clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                logger.warn(String.format(">>>>>>>>>>>>>>实例化接口[%s]异常", interfaceClass.getName()), e);
            }
        }
        logger.info(">>>>>>>>>>>>>>实例化接口[{}]异常, 降级处理成 java 代理", interfaceClass.getName());
        //降级处理
        return createProxy(interfaceClass);
    }

    /**
     * 定义方法
     *
     * @param clazz  接口
     * @param method 方法
     * @return {@link String}
     */
    private static String definitionMethod(Class<?> clazz, Method method) {
        //方法key
        String methodKey = clazz.getName().concat("#").concat(method.getName());
        //实现接口方法
        StringBuilder methodDefinition = new StringBuilder("public ");
        Class<?> returnType = method.getReturnType();
        if (returnType.equals(Void.class)) {
            methodDefinition.append("void ");
        } else {
            methodDefinition.append(returnType.getName()).append(" ");
        }
        methodDefinition.append(method.getName()).append("(");
        methodDefinition.append(definitionParameter(method.getParameters())).append(") { ");
        methodDefinition.append("java.lang.String methodKey = ");
        methodDefinition.append("\"").append(methodKey).append("\"; ");
        methodDefinition.append("java.lang.Class returnType = ");
        methodDefinition.append(method.getReturnType().getName()).append(".class; ");
        methodDefinition.append("java.util.List args = ");
        if (0 == method.getParameterCount()) {
            methodDefinition.append("null; ");
        } else {
            methodDefinition.append("new java.util.LinkedList(); ");
            for (int i = 0; i < method.getParameterCount(); i++) {
                methodDefinition.append("args.add(").append(method.getParameters()[i].getName()).append("); ");
            }
        }
        if (returnType.equals(Void.class)) {
            methodDefinition.append("com.wzb.rest.invoke.Invoker.invoke(methodKey, args); ");
        } else {
            methodDefinition.append("return (").append(returnType.getName()).append(") com.wzb.rest.invoke.Invoker.invoke(methodKey, args, returnType); ");
        }
        methodDefinition.append("} ");
        return methodDefinition.toString();
    }

    /**
     * 定义参数
     *
     * @param parameters 参数
     * @return {@link String}
     */
    private static String definitionParameter(Parameter[] parameters) {
        StringBuilder parameterDefinition = new StringBuilder();
        for (Parameter parameter : parameters) {
            if (parameterDefinition.length() > 0) {
                parameterDefinition.append(", ");
            }
            parameterDefinition.append(parameter.getType().getName())
                    .append(" ").append(parameter.getName());
        }
        return parameterDefinition.toString();
    }

    /**
     * 获取类加载器
     *
     * @return {@link ClassLoader}
     */
    private static ClassLoader getClassLoader() {
        /*ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
        }*/
        ClassLoader classLoader = InstanceFactory.class.getClassLoader();
        if (null == classLoader) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        return classLoader;
    }

    /**
     * 转换成类文件
     *
     * @param classPool      类池
     * @param pd             保护域
     * @param classLoader    类加载器
     * @param className      类名
     * @param methodList     方法列表
     * @param interfaceClass 接口
     * @return {@link Class}
     */
    private static Class<?> toClass(ClassPool classPool,
                                    ProtectionDomain pd,
                                    ClassLoader classLoader,
                                    String className,
                                    List<String> methodList,
                                    Class<?> interfaceClass) {
        try {
            CtClass mCtc = classPool.makeClass(className);
            mCtc.addInterface(classPool.get(interfaceClass.getName()));
            for (String method : methodList) {
                mCtc.addMethod(CtMethod.make(method, mCtc));
            }
            mCtc.addConstructor(CtNewConstructor.defaultConstructor(mCtc));
            return mCtc.toClass(classLoader, pd);
        } catch (CannotCompileException | NotFoundException | RuntimeException ex) {
            logger.warn(">>>>>>>>>>>>>代理类实现接口异常", ex);
        }
        return null;
    }
}
