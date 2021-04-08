package com.wzb.rest.cache;

/**
 * 方法参数顺序
 */
public class ParameterSort {

    /**
     * ParameterSort
     */
    public ParameterSort() {

    }

    /**
     * ParameterSort
     *
     * @param index          顺序索引
     * @param name           参数名称
     * @param type           参数类型
     * @param annotationName 注解名称
     * @param clazz          class
     * @param path           路径
     */
    private ParameterSort(int index,
                          String name,
                          ParameterType type,
                          String annotationName,
                          Class clazz,
                          String path) {
        this.index = index;
        this.name = name;
        this.type = type;
        this.annotationName = annotationName;
        this.clazz = clazz;
        this.path = path;
    }

    /**
     * 索引
     */
    private int index;

    /**
     * 参数名
     */
    private String name;

    /**
     * 参数类型
     */
    private ParameterType type;

    /**
     * 注解名称
     */
    private String annotationName;

    /**
     * class
     */
    private Class clazz;

    /**
     * 路径
     */
    private String path;

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public ParameterType getType() {
        return type;
    }

    public String getAnnotationName() {
        return annotationName;
    }

    public Class getClazz() {
        return clazz;
    }

    public String getPath() {
        return path;
    }

    /**
     * 建设者
     *
     * @return {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder
     */
    public static class Builder {
        /**
         * 索引
         */
        private int index;

        /**
         * 参数名
         */
        private String name;

        /**
         * 参数类型
         */
        private ParameterType type;

        /**
         * 注解名称
         */
        private String annotationName;

        /**
         * class
         */
        private Class clazz;

        /**
         * 路径
         */
        private String path;

        /**
         * 索引
         *
         * @param index 索引
         * @return {@link Builder}
         */
        public Builder index(int index) {
            this.index = index;
            return this;
        }

        /**
         * 名称
         *
         * @param name 名称
         * @return {@link Builder}
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * 类型
         *
         * @param type 类型
         * @return {@link Builder}
         */
        public Builder type(ParameterType type) {
            this.type = type;
            return this;
        }

        /**
         * 注解名称
         *
         * @param annotationName 注解名称
         * @return {@link Builder}
         */
        public Builder annotationName(String annotationName) {
            this.annotationName = annotationName;
            return this;
        }

        /**
         * 类
         *
         * @param clazz 类
         * @return {@link Builder}
         */
        public Builder clazz(Class clazz) {
            this.clazz = clazz;
            return this;
        }

        /**
         * 路径
         *
         * @param path 路径
         * @return {@link Builder}
         */
        public Builder path(String path) {
            this.path = path;
            return this;
        }

        /**
         * 建造
         *
         * @return {@link ParameterSort}
         */
        public ParameterSort build() {
            return new ParameterSort(this.index, this.name, this.type,
                    this.annotationName, this.clazz, this.path);
        }
    }
}
