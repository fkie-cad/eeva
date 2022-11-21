package com.damnvulnerableapp.common;

/**
 * Loader used to dynamically load classes by name. It also provides sanity checks for class names.
 *
 * @author Pascal Kühnemann
 * @version 1.0
 * */
public final class DynamicClassLoader {

    /**
     * Sole instance of this class.
     * */
    private static DynamicClassLoader instance;

    /**
     * Construct sole instance.
     * */
    private DynamicClassLoader() {

    }

    /**
     * Returns sole instance of this class.
     *
     * @return Sole instance.
     * */
    public static DynamicClassLoader getInstance() {
        if (DynamicClassLoader.instance == null)
            DynamicClassLoader.instance = new DynamicClassLoader();
        return DynamicClassLoader.instance;
    }

    /**
     * Checks whether a class with a given class name exists in the specified package and inherits
     * from the specified parent class.
     *
     * @param classPackage Package that contains the class to check for.
     * @param className Name of the class to check for.
     * @param parent Superclass of class to check for.
     * @return <code>true</code>, if specified class exists; <code>false</code> otherwise.
     * */
    public final boolean checkClass(Package classPackage, String className, Class<?> parent) {

        if (classPackage == null || className == null || parent == null)
            return false;

        try {
            final Class<?> target = Class.forName(classPackage.getName() + "." + className);
            if (!parent.isAssignableFrom(target))
                return false;
        } catch (ClassNotFoundException e) {
            return false;
        }

        return true;
    }

    /**
     * Loads class based on its name. To that end, the class has to be located in a specified package
     * and have a specified superclass.
     *
     * @param classPackage Package that contains the class to load.
     * @param className Name of the class to load.
     * @param parent Superclass of class to load.
     * @return New instance of the class, if class exists; <code>null</code> otherwise.
     * */
    public final Object loadClass(Package classPackage, String className, Class<?> parent) {

        if (classPackage == null || className == null || parent == null)
            return null;

        if (this.checkClass(classPackage, className, parent)) {

            try {

                final Class<?> target = Class.forName(classPackage.getName() + "." + className);
                return target.newInstance();
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException ignored) {}
        }

        return null;
    }
}
