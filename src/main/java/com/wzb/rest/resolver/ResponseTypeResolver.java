package com.wzb.rest.resolver;

import com.wzb.rest.cache.ClientCacheFactory;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.List;

/**
 * 返回类型解析类
 */
public final class ResponseTypeResolver {

    private static ResponseTypeResolver resolver = new ResponseTypeResolver();

    /**
     * init
     */
    private ResponseTypeResolver() {

    }

    /**
     * 获取实例
     *
     * @return {@link ResponseTypeResolver}
     */
    public static ResponseTypeResolver getInstance() {
        return resolver;
    }

    /**
     * 解析程序响应类型
     *
     * @param factory           缓存工厂
     * @param methodKey         方法key
     * @param type              类型
     * @return {@link Class}    方法返回泛型顺序
     */
    public List<Class<?>> resolverResponseType(ClientCacheFactory factory,
                                               String methodKey,
                                               Type type) {
        if (factory.hasResponseClass(methodKey)) {
            return factory.getResponseClass(methodKey);
        }
        List<Class<?>> classes = new ArrayList<>();
        if (type instanceof ParameterizedType
                || type instanceof TypeVariable
                || type instanceof WildcardType
                || type instanceof GenericArrayType) {
            resolverActualType(new Type[]{type}, classes);
        } else {
            classes.add((Class<?>) type);
        }
        factory.putIfAbsent(methodKey, classes);
        factory.putIfAbsent(methodKey, type);
        return classes;
    }

    /**
     * 解析程序实际类型
     *
     * @param type        类型
     * @param classes     顺序存放泛型
     * @return Class<?>   此层级的泛型
     */
    private Class<?> resolverActualType(ParameterizedType type, List<Class<?>> classes) {
        Type[] innerActualTypes = type.getActualTypeArguments();
        if (null == innerActualTypes || innerActualTypes.length == 0) {
            classes.add((Class<?>) type.getRawType());
            return (Class<?>) type.getRawType();
        }
        return resolverActualType(innerActualTypes, classes);
    }

    /**
     * 解析程序实际类型
     *
     * @param type        类型
     * @param classes     顺序存放泛型
     * @return Class<?>   此层级的泛型
     */
    private Class<?> resolverActualType(TypeVariable type, List<Class<?>> classes) {
        Type[] types = type.getBounds();
        if (null == types || types.length == 0) {
            classes.add(Object.class);
            return Object.class;
        }
        return resolverActualType(types, classes);
    }

    /**
     * 解析程序实际类型
     *
     * @param type        类型
     * @param classes     顺序存放泛型
     * @return Class<?>   此层级的泛型
     */
    private Class<?> resolverActualType(GenericArrayType type, List<Class<?>> classes) {
        classes.add((Class<?>) type.getGenericComponentType());
        return (Class<?>) type.getGenericComponentType();
    }

    /**
     * 解析程序实际类型
     *
     * @param type        类型
     * @param classes     顺序存放泛型
     * @return Class<?>   此层级的泛型
     */
    private Class<?> resolverActualType(WildcardType type, List<Class<?>> classes) {
        Type[] types = type.getUpperBounds();
        if (null == types || types.length == 0) {
            return null;
        }
        return resolverActualType(types, classes);
    }

    /**
     * 解析程序实际响应类型
     *
     * @param actualTypes 实际类型
     * @param classes     顺序存放泛型
     * @return Class<?>   此层级的泛型
     */
    private Class<?> resolverActualType(Type[] actualTypes, List<Class<?>> classes) {
        Type type = actualTypes[0];
        if (type instanceof ParameterizedType) {
            classes.add((Class<?>) ((ParameterizedType) type).getRawType());
            return resolverActualType((ParameterizedType) type, classes);
        }
        if (type instanceof TypeVariable) {
            return resolverActualType((TypeVariable) type, classes);
        }
        if (type instanceof WildcardType) {
            return resolverActualType((WildcardType) type, classes);
        }
        if (type instanceof GenericArrayType) {
            return resolverActualType((GenericArrayType) type, classes);
        }
        classes.add((Class<?>) type);
        return (Class<?>) type;
    }
}
