package org.example.async.log.client.permit;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Permit {
    private static final long ACCESSIBLE_OVERRIDE_FIELD_OFFSET;
    private static final IllegalAccessException INIT_ERROR;
    private static final sun.misc.Unsafe UNSAFE = (sun.misc.Unsafe) reflectiveStaticFieldAccess(sun.misc.Unsafe.class, "theUnsafe");
    static {
        Field f;
        long g;
        Throwable ex;

        try {
            g = getOverrideFieldOffset();
            ex = null;
        } catch (Throwable t) {
            f = null;
            g = -1L;
            ex = t;
        }

        ACCESSIBLE_OVERRIDE_FIELD_OFFSET = g;
        if (ex == null) INIT_ERROR = null;
        else if (ex instanceof IllegalAccessException) INIT_ERROR = (IllegalAccessException) ex;
        else {
            INIT_ERROR = new IllegalAccessException("Cannot initialize Unsafe-based permit");
            INIT_ERROR.initCause(ex);
        }
    }

    static class Fake {
        boolean override;
        Object accessCheckCache;
    }

    public static Method getMethod(Class<?> c, String mName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method m = null;
        Class<?> oc = c;
        while (c != null) {
            try {
                m = c.getDeclaredMethod(mName, parameterTypes);
                break;
            } catch (NoSuchMethodException e) {}
            c = c.getSuperclass();
        }

        if (m == null) throw new NoSuchMethodException(oc.getName() + " :: " + mName + "(args)");
        return setAccessible(m);
    }

    public static <T extends AccessibleObject> T setAccessible(T accessor) {
        if (INIT_ERROR == null) {
            UNSAFE.putBoolean(accessor, ACCESSIBLE_OVERRIDE_FIELD_OFFSET, true);
        } else {
            accessor.setAccessible(true);
        }

        return accessor;
    }

    private static Object reflectiveStaticFieldAccess(Class<?> c, String fName) {
        try {
            Field f = c.getDeclaredField(fName);
            f.setAccessible(true);
            return f.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    private static long getOverrideFieldOffset() throws Throwable {
        Field f = null;
        Throwable saved = null;
        try {
            f = AccessibleObject.class.getDeclaredField("override");
        } catch (Throwable t) {
            saved = t;
        }

        if (f != null) {
            return UNSAFE.objectFieldOffset(f);
        }
        // The below seems very risky, but for all AccessibleObjects in java today it does work, and starting with JDK12, making the field accessible is no longer possible.
        try {
            return UNSAFE.objectFieldOffset(Fake.class.getDeclaredField("override"));
        } catch (Throwable t) {
            throw saved;
        }
    }

}
