package com.wzb.rest.resolver;

import com.wzb.rest.cache.ClientCacheFactory;
import com.wzb.rest.cache.DynamicParameter;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * 动态参数解析
 */
public final class DynamicParameterResolver {

    private static final String URL_SYMBOL_START = "{";

    private static final String URL_SYMBOL_END = "}";

    private static final int FIND_INDEX = -1;

    private static DynamicParameterResolver resolver = new DynamicParameterResolver();

    /**
     * init
     */
    private DynamicParameterResolver() {

    }

    /**
     * 获取实例
     *
     * @return {@link DynamicParameterResolver}
     */
    public static DynamicParameterResolver getInstance() {
        return resolver;
    }

    /**
     * 解析动态URL参数
     *
     * @param factory    缓存工厂
     * @param methodKey  方法key
     * @param dynamicUrl 动态URL
     */
    public void resolverDynamicParameter(ClientCacheFactory factory, String methodKey, String dynamicUrl) {
        if (!factory.hasDynamicParameter(methodKey)) {
            List<DynamicParameter> dynamicParameterList = new LinkedList<>();
            DynamicParameter dynamicParameter;
            int fromIndex = 0;
            do {
                dynamicParameter = findNextDynamicParameter(dynamicUrl, fromIndex);
                fromIndex = dynamicParameter.getEndIndex() + 1;
                dynamicParameterList.add(dynamicParameter);
            } while (Objects.nonNull(dynamicParameter.getName()));
            factory.putDynamicParameterIfAbsent(methodKey, dynamicParameterList);
        }
    }

    /**
     * 查找下一个动态参数
     *
     * @param dynamicUrl 动态URL
     * @param fromIndex  索引开始位置
     * @return {@link DynamicParameter}
     */
    private DynamicParameter findNextDynamicParameter(String dynamicUrl, int fromIndex) {
        int startIndex = dynamicUrl.indexOf(URL_SYMBOL_START, fromIndex);
        int endIndex = dynamicUrl.indexOf(URL_SYMBOL_END, fromIndex);
        if (startIndex > FIND_INDEX && startIndex >= endIndex) {
            throw new IllegalArgumentException(
                    String.format("format url[%s] dynamic parameter error at { index %d", dynamicUrl, startIndex));
        }
        DynamicParameter dynamicParameter = null;
        if (startIndex > FIND_INDEX) {
            String placeholder = dynamicUrl.substring(startIndex, endIndex + URL_SYMBOL_END.length());
            int checkIndex = placeholder.indexOf(URL_SYMBOL_START, URL_SYMBOL_START.length());
            if (checkIndex > FIND_INDEX) {
                throw new IllegalArgumentException(
                        String.format("format url[%s] dynamic parameter error at { index %d",
                                dynamicUrl, checkIndex + startIndex + URL_SYMBOL_START.length()));
            }
            String dynamicName = placeholder.substring(URL_SYMBOL_START.length(),
                    placeholder.length() - URL_SYMBOL_END.length()).trim();
            if (dynamicName.isEmpty()) {
                throw new IllegalArgumentException(
                        String.format("format url[%s] dynamic parameter error at %s", dynamicUrl, placeholder));
            }
            StringBuilder subURL = new StringBuilder(dynamicUrl.substring(fromIndex, startIndex));
            dynamicParameter = new DynamicParameter(dynamicName, subURL, endIndex);
        }
        //解析完动态参数
        if (null == dynamicParameter) {
            StringBuilder subURL = new StringBuilder();
            if (dynamicUrl.length() > fromIndex) {
                subURL.append(dynamicUrl.substring(fromIndex));
            }
            dynamicParameter = new DynamicParameter(subURL);
        }
        return dynamicParameter;
    }
}
