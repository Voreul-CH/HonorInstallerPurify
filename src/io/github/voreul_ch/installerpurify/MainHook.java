package io.github.voreul_ch.installerpurify;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {
    private static final String TARGET_PKG = "com.android.packageinstaller";
    private static final String TAG = "[InstallBypass] ";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET_PKG.equals(lpparam.packageName)) {
            return;
        }
        final ClassLoader cl = lpparam.classLoader;
        hookSecurityCheck(cl);
        hookFingerprintAuth(cl);
    }

    private void hookSecurityCheck(final ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod("ly1", cl, "h", boolean.class, "sy1",
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) {
                            try {
                                final Object callback = param.args[1];
                                if (callback == null) {
                                    return null;
                                }
                                boolean isHn = (Boolean) XposedHelpers.callMethod(param.thisObject, "d");
                                Class<?> ly1Class = param.thisObject.getClass();
                                final Object passResult = XposedHelpers.callStaticMethod(ly1Class, "a", isHn);
                                final Class<?> oy1Class = cl.loadClass("oy1");
                                final Class<?> jz1Class = cl.loadClass("jz1");
                                final Object jz1Proxy = Proxy.newProxyInstance(cl, new Class<?>[]{jz1Class},
                                        new InvocationHandler() {
                                            @Override
                                            public Object invoke(Object proxy, Method method, Object[] args) {
                                                String name = method.getName();
                                                if ("hashCode".equals(name)) {
                                                    return System.identityHashCode(proxy);
                                                }
                                                if ("equals".equals(name)) {
                                                    return proxy == (args != null && args.length > 0 ? args[0] : null);
                                                }
                                                if ("toString".equals(name)) {
                                                    return "Jz1Noop";
                                                }
                                                return null;
                                            }
                                        });
                                final Method deliver = callback.getClass().getMethod("b", oy1Class, jz1Class);
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            deliver.invoke(callback, passResult, jz1Proxy);
                                            XposedBridge.log(TAG + "security check skipped");
                                        } catch (Throwable t) {
                                            XposedBridge.log(TAG + "deliver result error: " + t);
                                        }
                                    }
                                });
                            } catch (Throwable t) {
                                XposedBridge.log(TAG + "check skip error: " + t);
                            }
                            return null;
                        }
                    });
            XposedBridge.log(TAG + "hook ly1.h ok");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "hook ly1.h failed: " + t);
        }
    }

    private void hookFingerprintAuth(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod("qf2", cl, "g", Context.class, "qf2$a",
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) {
                            try {
                                Object cb = param.args[1];
                                if (cb != null) {
                                    cb.getClass().getMethod("c").invoke(cb);
                                    cb.getClass().getMethod("a", boolean.class).invoke(cb, true);
                                    XposedBridge.log(TAG + "fingerprint auth bypassed");
                                }
                            } catch (Throwable t) {
                                XposedBridge.log(TAG + "fingerprint bypass error: " + t);
                            }
                            return null;
                        }
                    });
            XposedBridge.log(TAG + "hook qf2.g ok");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "hook qf2.g failed: " + t);
        }
    }
}
