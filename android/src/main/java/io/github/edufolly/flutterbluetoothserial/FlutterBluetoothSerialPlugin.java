package io.github.edufolly.flutterbluetoothserial;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

public class FlutterBluetoothSerialPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {

    private MethodChannel channel;
    private Activity activity;
    private Context context;
    private ActivityResultLauncher<Intent> enableBtLauncher;
    private Result pendingEnableBtResult;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "flutter_bluetooth_serial");
        channel.setMethodCallHandler(this);
        context = flutterPluginBinding.getApplicationContext();
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("requestEnableBluetooth")) {
            requestEnableBluetooth(result);
        } else {
            result.notImplemented();
        }
    }

    private void requestEnableBluetooth(Result result) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            result.error("UNAVAILABLE", "Bluetooth is not available on this device", null);
            return;
        }

        if (bluetoothAdapter.isEnabled()) {
            result.success(true);
        } else {
            pendingEnableBtResult = result;
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtLauncher.launch(enableBtIntent);
        }
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();

        enableBtLauncher = binding.getActivityResultRegistry().register(
                "enableBluetooth",
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (pendingEnableBtResult != null) {
                        int resultCode = result.getResultCode();
                        pendingEnableBtResult.success(resultCode == Activity.RESULT_OK);
                        pendingEnableBtResult = null;
                    }
                }
        );
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        activity = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        onAttachedToActivity(binding);
    }

    @Override
    public void onDetachedFromActivity() {
        activity = null;
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        channel = null;
        context = null;
    }
}
