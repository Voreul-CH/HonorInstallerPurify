package io.github.voreul_ch.installerpurify;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

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
        hookSafeModeBanner(cl);
        hookAdRecommend(cl);
        hookSafeResultBanner(cl);
        hookCleanCacheCard(cl);
        hookDiversionWait(cl);
        hookNetwork(cl);
    }

    private void hookDiversionWait(ClassLoader cl) {
        try {
            final Class<?> respClz = cl.loadClass("com.hihonor.packageinstaller.entity.resp.DiversionResp");
            XposedHelpers.findAndHookMethod("g82", cl, "f",
                    String.class, String.class, String.class, String.class, long.class, String.class,
                    int.class, long.class, int.class, int.class, "g82$d", Runnable.class,
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            Object dVar = param.args[10];
                            java.lang.reflect.Method m = dVar.getClass().getMethod("a", respClz);
                            m.setAccessible(true);
                            m.invoke(dVar, new Object[]{null});
                            return null;
                        }
                    });
            XposedBridge.log(TAG + "hook g82.f ok");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "hook g82.f failed: " + t);
        }
    }

    private void hookCleanCacheCard(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod("z62", cl, "E",
                    "com.hihonor.packageinstaller.entity.resp.SafeModeCards",
                    XC_MethodReplacement.returnConstant(true));
            XposedBridge.log(TAG + "hook z62.E ok");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "hook z62.E failed: " + t);
        }
    }

    private void hookNetwork(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod("java.net.Socket", cl, "connect", java.net.SocketAddress.class, int.class,
                    new de.robv.android.xposed.XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            param.setThrowable(new java.net.SocketException("blocked by HonorInstallerPurify"));
                        }
                    });
            XposedBridge.log(TAG + "hook Socket.connect ok");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "hook Socket.connect failed: " + t);
        }
    }

    private void hookSafeModeBanner(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod("r12", cl, "j",
                    "com.hihonor.packageinstaller.utils.CardTipsRecord", String.class, boolean.class, String.class,
                    XC_MethodReplacement.returnConstant(false));
            XposedBridge.log(TAG + "hook r12.j ok");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "hook r12.j failed: " + t);
        }
        try {
            XposedHelpers.findAndHookMethod("r12", cl, "s",
                    "com.android.packageinstaller.PackageInstallerActivity", "com.hihonor.packageinstaller.rbi.RbiTempBean",
                    XC_MethodReplacement.returnConstant(null));
            XposedBridge.log(TAG + "hook r12.s ok");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "hook r12.s failed: " + t);
        }
    }

    private void hookAdRecommend(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod("com.hihonor.packageinstaller.presenter.AbstractAdBusinessPresenter", cl, "l",
                    "kt1", "com.hihonor.packageinstaller.rbi.RbiTempBean", "ef2", boolean.class,
                    XC_MethodReplacement.returnConstant(null));
            XposedBridge.log(TAG + "hook ad presenter l ok");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "hook ad presenter l failed: " + t);
        }
    }

    private void hookSafeResultBanner(ClassLoader cl) {
        hideAfter(cl, "e2", "W0");
        hideAfter(cl, "k2", "Y0");
    }

    private void hideAfter(ClassLoader cl, String methodName, final String fieldName) {
        try {
            XposedHelpers.findAndHookMethod("com.android.packageinstaller.PackageInstallerActivity", cl, methodName,
                    new de.robv.android.xposed.XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                Object v = XposedHelpers.getObjectField(param.thisObject, fieldName);
                                if (v instanceof View) {
                                    ((View) v).setVisibility(View.GONE);
                                    XposedBridge.log(TAG + "hidden result banner via " + fieldName);
                                }
                            } catch (Throwable t) {
                                XposedBridge.log(TAG + "hide " + fieldName + " error: " + t);
                            }
                        }
                    });
            XposedBridge.log(TAG + "hook " + methodName + " ok");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "hook " + methodName + " failed: " + t);
        }
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
