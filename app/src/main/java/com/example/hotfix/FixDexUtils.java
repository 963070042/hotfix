package com.example.hotfix;

import android.content.Context;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashSet;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

public class FixDexUtils {
    private static final String DEX_SUFFIX = ".dex";
    private static final String APK_SUFFIX = ".apk";
    private static final String JAR_SUFFIX = ".jar";
    private static final String ZIP_SUFFIX = ".zip";
    private static final String DEX_DIR = "odex";
    private static final String OPTIMIZE_DEX_DIR = "optimize_dex";
    private static HashSet<File> loadedDex = new HashSet<>();

    static {
        loadedDex.clear();
    }
    public static void loadFixedDex(Context context){
        loadFixedDex(context,null);
    }

    public static void loadFixedDex(Context context, File patchFilesDir) {
        if (context == null){
            return;
        }
        File fileDir = patchFilesDir != null ? patchFilesDir : new File(context.getFilesDir(), DEX_DIR);
        File[] listFiles = fileDir.listFiles();
        for (File file: listFiles) {
            if (file.getName().startsWith("classes")
                    && (file.getName().endsWith(DEX_SUFFIX)
                        ||file.getName().endsWith(APK_SUFFIX)
                        ||file.getName().endsWith(JAR_SUFFIX)
                        ||file.getName().endsWith(ZIP_SUFFIX))){
                loadedDex.add(file);
            }
        }
        doDexInject(context,loadedDex);
    }

    private static void doDexInject(Context appContext, HashSet<File> loadedDex) {
        String optimizeDir = appContext.getFilesDir().getAbsolutePath() + File.separator + OPTIMIZE_DEX_DIR;
        File fopt = new File(optimizeDir);
        if (!fopt.exists()){
            fopt.mkdirs();
        }
        try {
            PathClassLoader pathLoader = (PathClassLoader) appContext.getClassLoader();
            for (File dex : loadedDex){
                DexClassLoader dexLoader = new DexClassLoader(
                        dex.getAbsolutePath(),
                        fopt.getAbsolutePath(),
                        null,
                        pathLoader
                );
                Object dexPathList = getPathList(dexLoader);
                Object pathPathList = getPathList(pathLoader);
                Object leftDexElements = getDexElements(dexPathList);
                Object rightDexElements = getDexElements(pathPathList);
                Object dexElements = combineArray(leftDexElements, rightDexElements);
                // ?????????PathList?????????Element[] dexElements;??????
                Object pathList = getPathList(pathLoader);// ?????????????????????????????????pathPathList????????????
                setField(pathList, pathList.getClass(), "dexElements", dexElements);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
    /**
     * ???????????????????????????????????????
     */
    private static void setField(Object obj, Class<?> cl, String field, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field declaredField = cl.getDeclaredField(field);
        declaredField.setAccessible(true);
        declaredField.set(obj, value);
    }

    /**
     * ?????????????????????????????????
     */
    private static Object getField(Object obj, Class<?> cl, String field) throws NoSuchFieldException, IllegalAccessException {
        Field localField = cl.getDeclaredField(field);
        localField.setAccessible(true);
        return localField.get(obj);
    }


    /**
     * ??????????????????????????????pathList??????
     */
    private static Object getPathList(Object baseDexClassLoader) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        return getField(baseDexClassLoader, Class.forName("dalvik.system.BaseDexClassLoader"), "pathList");
    }

    /**
     * ????????????pathList??????dexElements
     */
    private static Object getDexElements(Object pathList) throws NoSuchFieldException, IllegalAccessException {
        return getField(pathList, pathList.getClass(), "dexElements");
    }

    /**
     * ????????????
     */
    private static Object combineArray(Object arrayLhs, Object arrayRhs) {
        Class<?> componentType = arrayLhs.getClass().getComponentType();
        int i = Array.getLength(arrayLhs);// ???????????????????????????????????????
        int j = Array.getLength(arrayRhs);// ?????????dex????????????
        int k = i + j;// ????????????????????????????????????+???dex?????????
        Object result = Array.newInstance(componentType, k);// ?????????????????????componentType????????????k????????????
        System.arraycopy(arrayLhs, 0, result, 0, i);
        System.arraycopy(arrayRhs, 0, result, i, j);
        return result;
    }
}
