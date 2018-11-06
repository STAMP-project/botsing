package eu.stamp.botsing_model_generation.testcase.carving;

import eu.stamp.botsing_model_generation.BotsingTestGenerationContext;
import org.evosuite.classpath.ResourceList;

public class CarvingHelper {

    public static  Class<?> getClassForName(String type) {
        try {
            if (type.equals("boolean") || type.equals("java.lang.Boolean")) {
                return Boolean.TYPE;
            } else if (type.equals("byte") || type.equals("java.lang.Byte")) {
                return Byte.TYPE;
            } else if (type.equals("char") || type.equals("java.lang.Character")) {
                return Character.TYPE;
            } else if (type.equals("double") || type.equals("java.lang.Double")) {
                return Double.TYPE;
            } else if (type.equals("float") || type.equals("java.lang.Float")) {
                return Float.TYPE;
            } else if (type.equals("int") || type.equals("java.lang.Integer")) {
                return Integer.TYPE;
            } else if (type.equals("long") || type.equals("java.lang.Long")) {
                return Long.TYPE;
            } else if (type.equals("short") || type.equals("java.lang.Short")) {
                return Short.TYPE;
            } else if (type.equals("String")) {
                return Class.forName("java.lang." + type, true,
                        BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
            }

            if (type.endsWith("[]")) {
                // see http://stackoverflow.com/questions/3442090/java-what-is-this-ljava-lang-object

                final StringBuilder arrayTypeNameBuilder = new StringBuilder(30);

                int index = 0;
                while ((index = type.indexOf('[', index)) != -1) {
                    arrayTypeNameBuilder.append('[');
                    index++;
                }

                arrayTypeNameBuilder.append('L'); // always needed for Object arrays

                // remove bracket from type name get array component type
                type = type.replace("[]", "");
                arrayTypeNameBuilder.append(type);

                arrayTypeNameBuilder.append(';'); // finalize object array name

                return Class.forName(arrayTypeNameBuilder.toString(), true,
                        BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
            } else {
                return Class.forName(ResourceList.getClassNameFromResourcePath(type), true,
                        BotsingTestGenerationContext.getInstance().getClassLoaderForSUT());
            }
        } catch (final ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
