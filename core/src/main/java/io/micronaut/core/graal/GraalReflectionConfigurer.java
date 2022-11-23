/*
 * Copyright 2017-2022 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.core.graal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;

import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationMetadataProvider;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.annotation.ReflectionConfig;
import io.micronaut.core.annotation.TypeHint;
import io.micronaut.core.reflect.ReflectionUtils;
import io.micronaut.core.util.CollectionUtils;

/**
 * Interface that allows dynamic configuration of reflection generated by the GraalTypeElementVisitor.
 *
 * @author graemerocher
 * @since 3.5.0
 * @see io.micronaut.core.annotation.ReflectionConfig
 */
@Internal
public interface GraalReflectionConfigurer extends AnnotationMetadataProvider {
    /**
     * The suffix used for generated classes.
     */
    String CLASS_SUFFIX = "$ReflectConfig";

    /**
     * Configure reflection for this type.
     * @param context The feature access
     */
    @SuppressWarnings({"unused", "java:S3776"})
    default void configure(ReflectionConfigurationContext context) {
        final AnnotationMetadata annotationMetadata = getAnnotationMetadata();
        final List<AnnotationValue<ReflectionConfig>> values = annotationMetadata.getAnnotationValuesByType(
                ReflectionConfig.class);
        for (AnnotationValue<ReflectionConfig> reflectConfig : values) {
            reflectConfig.stringValue("type").ifPresent(className -> {
                Class<?> t = context.findClassByName(className);
                if (t == null) {
                    return;
                }
                context.register(t);
                final Set<TypeHint.AccessType> accessType = CollectionUtils.setOf(
                    reflectConfig.enumValues("accessType", TypeHint.AccessType.class)
                );
                if (accessType.contains(TypeHint.AccessType.ALL_PUBLIC_METHODS)) {
                    final Method[] methods = t.getMethods();
                    for (Method method : methods) {
                        if (Modifier.isPublic(method.getModifiers())) {
                            context.register(method);
                        }
                    }
                }
                if (accessType.contains(TypeHint.AccessType.ALL_DECLARED_METHODS)) {
                    final Method[] declaredMethods = t.getDeclaredMethods();
                    context.register(declaredMethods);
                }
                if (accessType.contains(TypeHint.AccessType.ALL_PUBLIC_FIELDS)) {
                    final Field[] fields = t.getFields();
                    for (Field field : fields) {
                        if (Modifier.isPublic(field.getModifiers())) {
                            context.register(field);
                        }
                    }
                }
                if (accessType.contains(TypeHint.AccessType.ALL_DECLARED_FIELDS)) {
                    final Field[] fields = t.getDeclaredFields();
                    context.register(fields);
                }
                if (accessType.contains(TypeHint.AccessType.ALL_PUBLIC_CONSTRUCTORS)) {
                    final Constructor<?>[] constructors = t.getConstructors();
                    for (Constructor<?> constructor : constructors) {
                        if (Modifier.isPublic(constructor.getModifiers())) {
                            context.register(constructor);
                        }
                    }
                }
                if (accessType.contains(TypeHint.AccessType.ALL_DECLARED_CONSTRUCTORS)) {
                    final Constructor<?>[] constructors = t.getDeclaredConstructors();
                    context.register(constructors);
                }

                final List<AnnotationValue<ReflectionConfig.ReflectiveMethodConfig>> methodConfig =
                        reflectConfig.getAnnotations("methods", ReflectionConfig.ReflectiveMethodConfig.class);
                for (AnnotationValue<ReflectionConfig.ReflectiveMethodConfig> mrc :
                        methodConfig) {
                    mrc.stringValue("name").ifPresent(n -> {
                        final String[] typeNames = mrc.stringValues("parameterTypes");
                        final Class<?>[] parameterTypes = new Class<?>[typeNames.length];
                        for (int i = 0; i < typeNames.length; i++) {
                            String typeName = typeNames[i];
                            final Class<?> pt = context.findClassByName(typeName);
                            if (pt == null) {
                                // bail out
                                return;
                            } else {
                                parameterTypes[i] = pt;
                            }
                        }
                        if (n.equals("<init>")) {
                            try {
                                Constructor<?> c = t.getDeclaredConstructor(parameterTypes);
                                context.register(c);
                            } catch (NoSuchMethodException e) {
                                // ignore
                            }
                        } else {
                            try {
                                Method method = t.getDeclaredMethod(n, parameterTypes);
                                context.register(method);
                            } catch (NoSuchMethodException e) {
                                // ignore
                            }
                        }
                    });
                }

                final List<AnnotationValue<ReflectionConfig.ReflectiveFieldConfig>> fields =
                        reflectConfig.getAnnotations(
                        "fields",
                        ReflectionConfig.ReflectiveFieldConfig.class
                );

                for (AnnotationValue<ReflectionConfig.ReflectiveFieldConfig> field : fields) {
                    field.stringValue("name")
                            .flatMap(n -> ReflectionUtils.findField(t, n))
                            .ifPresent(context::register);
                }
            });
        }
    }

    /**
     * Context object for the configuration.
     */
    interface ReflectionConfigurationContext {
        /**
         * Finds a class by name.
         * @param name The name
         * @return The class or null
         */
        @Nullable
        Class<?> findClassByName(@NonNull String name);

        /**
         * Register the given types for reflection.
         * @param types The types
         */
        void register(Class<?>...types);

        /**
         * Register the given methods for reflection.
         * @param methods The methods
         */
        void register(Method... methods);

        /**
         * Register the given fields for reflection.
         * @param fields The fields
         */
        void register(Field... fields);

        /**
         * Register the given constructors for reflection.
         * @param constructors The constructors
         */
        void register(Constructor<?>... constructors);
    }
}