package jp.rmitkt.xposed.fakedevicedata;

import android.app.AndroidAppHelper;
import android.content.ContentResolver;
import android.provider.Settings;
import android.util.Log;

import java.io.FileNotFoundException;

import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XposedBridge;

public class FakeDeviceData implements IXposedHookLoadPackage {
	private boolean DEBUG_MODE = true;
	private static XSharedPreferences pref;
	private LoadPackageParam lpparam;
	private static final String LogSource = "fakedevicedata";

	@Override
	public void handleLoadPackage(final LoadPackageParam lpp) throws Throwable {
		lpparam = lpp;

		try {
			pref = new XSharedPreferences("jp.rmitkt.xposed.fakedevicedata", "pref");

			if (pref.getAll().size() == 0) {
				String message = "Error loading prefs.xml (no settings found). Does the file exist and have read permissions?";
				Log.d(LogSource, message);
				XposedBridge.log(LogSource + ": " + message);
			}

			if (!pref.getBoolean(lpparam.packageName, false))
				return;

			// Values like Build.MODEL
			handle();
		} catch (Exception e) {
			if (DEBUG_MODE) {
				String message = "Error loading prefs.xml. Does the file exist and have read permissions?";
				Log.d(LogSource, message);
				XposedBridge.log(LogSource + ": " + message);
			}
			return;
		}

		final String pkg = lpparam.packageName;

		// Values from build.prop, like getProperty("ro.build.id")
		try{
			// TODO: Refactor XC_MethodHook into static methods to avoid accidental inclusion of global parameters?
			XposedHelpers.findAndHookMethod("java.lang.System", lpparam.classLoader, "getProperty", String.class, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					String message = pkg + ": System.getProperty(" + param.args[0] + ") = " + param.getResult();
					if (param.args[0] == "os.version") {
						String override = "1";
						message += " -> " + override;
						param.setResult(override);
					}

					if (DEBUG_MODE) Log.d(LogSource, message);
				}
			});
		} catch (NoSuchMethodError e){
			if (DEBUG_MODE) {
				String message = "couldn't hook method System.getProperty()";
				Log.d(LogSource, message);
				XposedBridge.log(LogSource + ": " + message);
			}

			return;
		}
		try{
			XposedHelpers.findAndHookMethod("android.os.SystemProperties", lpparam.classLoader, "get", String.class, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					String message = pkg + ": SystemProperties.get(" + param.args[0] + ") = " + param.getResult();

					String override = null;
					if (param.args[0] == "ro.product.display") {
						override = "NoMoto";
					} else if (param.args[0] == "ro.boot.hardware.sku") {
						override = "Fake";
					} else if (param.args[0] == "gsm.sim.operator.numeric") {
						override = "1";
					}

					if (override != null) {
						message += " -> " + override;
						param.setResult(override);
					}

					if (DEBUG_MODE) Log.d(LogSource, message);
				}
			});
		} catch (NoSuchMethodError e){
			if (DEBUG_MODE) {
				String message = "couldn't hook method SystemProperties.get()";
				Log.d(LogSource, message);
				XposedBridge.log(LogSource + ": " + message);
			}

			return;
		}
		// Source: https://android.googlesource.com/platform/frameworks/base/+/84e2756c0f3794c6efe5568a9d09101ba689fb39/core/java/android/provider/Settings.java
		// Reference: https://developer.android.com/reference/android/provider/Settings.Global.html#getString(android.content.ContentResolver,%20java.lang.String)
		try{
			XposedHelpers.findAndHookMethod("android.provider.Settings.Global", lpparam.classLoader, "getString", ContentResolver.class, String.class, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					String message = pkg + ": Settings.Global.getString(" + param.args[1] + ") = " + param.getResult();

					String override = null;
					if (param.args[1] == "device_provisioned") {
						override = "0";
					}

					if (override != null) {
						message += " -> " + override;
						param.setResult(override);
					}

					if (DEBUG_MODE) Log.d(LogSource, message);
				}
			});
		} catch (NoSuchMethodError e){
			if (DEBUG_MODE) {
				String message = "couldn't hook method Settings.Global.getString()";
				Log.d(LogSource, message);
				XposedBridge.log(LogSource + ": " + message);
			}

			return;
		}
		// Reference: https://developer.android.com/reference/android/provider/Settings.Secure.html#getString(android.content.ContentResolver,%20java.lang.String)
		try{
			XposedHelpers.findAndHookMethod("android.provider.Settings.Secure", lpparam.classLoader, "getString", ContentResolver.class, String.class, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					String message = pkg + ": Settings.Secure.getString(" + param.args[1] + ") = " + param.getResult();

					String override = null;
					if (param.args[1] == Settings.Secure.ANDROID_ID) {
						override = "1111222233334444";
					}

					if (override != null) {
						message += " -> " + override;
						param.setResult(override);
					}

					if (DEBUG_MODE) Log.d(LogSource, message);
				}
			});
		} catch (NoSuchMethodError e){
			if (DEBUG_MODE) {
				String message = "couldn't hook method Settings.Secure.getString()";
				Log.d(LogSource, message);
				XposedBridge.log(LogSource + ": " + message);
			}

			return;
		}

		// IMEI
		// Source: https://android.googlesource.com/platform/frameworks/base/+/master/telephony/java/android/telephony/TelephonyManager.java
		// Reference: https://developer.android.com/reference/android/telephony/TelephonyManager.html
		try{
			XposedHelpers.findAndHookMethod("android.telephony.TelephonyManager", lpparam.classLoader, "getDeviceId", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					String message = pkg + ": getDeviceId.get() = " + param.getResult();
					String override = "111222333444555";
					message += " -> " + override;
					param.setResult(override);
					if (DEBUG_MODE) Log.d(LogSource, message);
				}
			});
		} catch (NoSuchMethodError e){
			if (DEBUG_MODE) {
				String message = "couldn't hook method getDeviceId()";
				Log.d(LogSource, message);
				XposedBridge.log(LogSource + ": " + message);
			}

			return;
		}
		try{
			XposedHelpers.findAndHookMethod("android.telephony.TelephonyManager", lpparam.classLoader, "getDeviceId", int.class, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					String message = pkg + ": getDeviceId.get(" + param.args[0] + ") = " + param.getResult();
					String override = "111222333444555";
					message += " -> " + override;
					param.setResult(override);
					if (DEBUG_MODE) Log.d(LogSource, message);
				}
			});
		} catch (NoSuchMethodError e){
			if (DEBUG_MODE) {
				String message = "couldn't hook method getDeviceId(int)";
				Log.d(LogSource, message);
				XposedBridge.log(LogSource + ": " + message);
			}

			return;
		}
	}

	public void handle() {
		Class<?> classBuild = XposedHelpers.findClass("android.os.Build", lpparam.classLoader);

		HandleFakeStaticString(classBuild, "BOARD", "fake_board");
		HandleFakeStaticString(classBuild, "BOOTLOADER", "fake_bootloader");
		HandleFakeStaticString(classBuild, "BRAND", "fake_brand");
		HandleFakeStaticString(classBuild, "CPU_ABI", "fake_cpu_abi");
		HandleFakeStaticString(classBuild, "CPU_ABI2", "fake_cpu_abi2");
		HandleFakeStaticString(classBuild, "DEVICE", "fake_device");
		HandleFakeStaticString(classBuild, "DISPLAY", "fake_display");
		HandleFakeStaticString(classBuild, "FINGERPRINT", "fake_fingerprint");
		HandleFakeStaticString(classBuild, "HARDWARE", "fake_hardware");
		HandleFakeStaticString(classBuild, "HOST", "fake_host");
		HandleFakeStaticString(classBuild, "ID", "fake_id");
		HandleFakeStaticString(classBuild, "MANUFACTURER", "fake_manufacturer");
		HandleFakeStaticString(classBuild, "MODEL", "fake_model");
		HandleFakeStaticString(classBuild, "PRODUCT", "fake_product");
		HandleFakeStaticString(classBuild, "RADIO", "fake_radio");
		HandleFakeStaticString(classBuild, "TAGS", "fake_tags");
		HandleFakeStaticInt(classBuild, "TIME", "fake_time");
		HandleFakeStaticString(classBuild, "TYPE", "fake_type");
		HandleFakeStaticString(classBuild, "USER", "fake_user");

		Class<?> classBuild2 = XposedHelpers.findClass("android.os.Build.VERSION", lpparam.classLoader);

		HandleFakeStaticString(classBuild2, "CODENAME", "fake_codename");
		HandleFakeStaticString(classBuild2, "INCREMENTAL", "fake_incremental");
		HandleFakeStaticString(classBuild2, "RELEASE", "fake_release");
		HandleFakeStaticString(classBuild2, "SDK", "fake_sdk");
		HandleFakeStaticInt(classBuild2, "SDK_INT", "fake_sdk_int");
	}

	private void HandleFakeStaticString(Class<?> myClass, String fieldName, String fakeKey) {
		String defValue = "";
		String fakeEnabledKey = fakeKey + "_key";
		String fakeValueKey = fakeKey + "_value";
		if (pref.getBoolean(fakeEnabledKey, false) && (pref.getString(fakeValueKey, defValue) != defValue)) {
			if (DEBUG_MODE) Log.d(LogSource, lpparam.packageName + ": " + myClass.getCanonicalName() + "." + fieldName + " = " + XposedHelpers.getStaticObjectField(myClass, fieldName) + " -> " + pref.getString(fakeValueKey, defValue));
			XposedHelpers.setStaticObjectField(myClass, fieldName, pref.getString(fakeValueKey, defValue));
		}
	}

	private void HandleFakeStaticInt(Class<?> myClass, String fieldName, String fakeKey) {
		int defValue = 0;
		String fakeEnabledKey = fakeKey + "_key";
		String fakeValueKey = fakeKey + "_value";
		if (pref.getBoolean(fakeEnabledKey, false) && (pref.getInt(fakeValueKey, defValue) != defValue)) {
			if (DEBUG_MODE) Log.d(LogSource, lpparam.packageName + ": " + myClass.getCanonicalName() + "." + fieldName + " = " + XposedHelpers.getStaticObjectField(myClass, fieldName) + " -> " + pref.getInt(fakeValueKey, defValue));
			XposedHelpers.setStaticObjectField(myClass, fieldName, pref.getInt(fakeValueKey, defValue));
		}
	}
}