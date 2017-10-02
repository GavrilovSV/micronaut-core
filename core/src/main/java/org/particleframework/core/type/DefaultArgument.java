package org.particleframework.core.type;

import org.particleframework.core.annotation.AnnotationUtil;
import org.particleframework.core.annotation.Internal;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents an argument to a constructor or method
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@Internal
class DefaultArgument<T> implements Argument<T> {
    private final Class type;
    private final String name;
    private final Annotation qualifier;
    private final Annotation[] annotations;
    private final Map<String, Argument<?>> typeParameters;

    DefaultArgument(Class type, String name, Annotation qualifier, Annotation[] annotations, Argument... genericTypes) {
        this.type = type;
        this.name = name;
        this.annotations = annotations;
        this.qualifier = qualifier;
        this.typeParameters = initializeTypeParameters(genericTypes);
    }

    DefaultArgument(Class type, String name, Annotation qualifier, Argument... genericTypes) {
        this.type = type;
        this.name = name;
        this.annotations = new Annotation[0];
        this.qualifier = qualifier;
        this.typeParameters = initializeTypeParameters(genericTypes);
    }

    @Override
    public Optional<Argument<?>> getFirstTypeVariable() {
        if(!typeParameters.isEmpty()) {
            return typeParameters.values().stream().findFirst();
        }
        return Optional.empty();
    }

    @Override
    public Map<String, Argument<?>> getTypeVariables() {
        return this.typeParameters;
    }

    @Override
    public Class getType() {
        return type;
    }

    @Override
    public Annotation getQualifier() {
        return this.qualifier;
    }

    @Override
    public <A extends Annotation> A findAnnotation(Class<A> stereotype) {
        return AnnotationUtil.findAnnotationWithStereoType(stereotype, annotations);
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        for (Annotation annotation : annotations) {
            if(annotation.annotationType().equals(annotationClass)) {
                return (A) annotation;
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return type.getSimpleName() + " " + name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DefaultArgument that = (DefaultArgument) o;

        if (!type.equals(that.type)) return false;
        if (!name.equals(that.name)) return false;
        return qualifier != null ? qualifier.equals(that.qualifier) : that.qualifier == null;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + (qualifier != null ? qualifier.hashCode() : 0);
        return result;
    }

    @Override
    public Annotation[] getAnnotations() {
        return annotations;
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return annotations;
    }



    private Map<String, Argument<?>> initializeTypeParameters(Argument[] genericTypes) {
        Map<String, Argument<?>> typeParameters;
        if(genericTypes != null && genericTypes.length > 0) {
            typeParameters = new LinkedHashMap<>(genericTypes.length);
            for (Argument genericType : genericTypes) {
                typeParameters.put(genericType.getName(), genericType);
            }
        }
        else {
            typeParameters = Collections.emptyMap();
        }
        return typeParameters;
    }


}