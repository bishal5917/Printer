package com.example.printer;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.printer.databinding.ActivityMainBinding;
import com.usdk.apiservice.aidl.UDeviceService;
import com.usdk.apiservice.aidl.constants.LogLevel;
import com.usdk.apiservice.aidl.printer.AlignMode;
import com.usdk.apiservice.aidl.printer.OnPrintListener;
import com.usdk.apiservice.aidl.printer.UPrinter;
import com.usdk.apiservice.aidl.scanner.CameraId;
import com.usdk.apiservice.aidl.scanner.OnScanListener;
import com.usdk.apiservice.aidl.scanner.UScanner;

public class MainActivity extends AppCompatActivity {

    private UDeviceService deviceService;
    private UPrinter printer;
    private String TAG = "tag";
    private SparseArray<UScanner> scanners = new SparseArray<UScanner>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindService(this);
        Button scanBtn = findViewById(R.id.btnScan);
        scanBtn.setOnClickListener(v -> {
            try {
                startBackScan();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public UScanner getScanner(final int cameraId) throws RemoteException {
        UScanner scanner = scanners.get(cameraId);
        if (scanner == null) {
            IBinder binder = deviceService.getScanner(cameraId);
            scanner = UScanner.Stub.asInterface(binder);
            scanners.put(cameraId, scanner);
        }
        return scanner;
    }

    public void startBackScan() throws RemoteException {
        UScanner scanner = getScanner(CameraId.BACK);
        Bundle bundle = new Bundle();
//        bundle.putInt(ScannerData.TIMEOUT, 30);
//        bundle.putString(ScannerData.TITLE, "Customize the title");
//        bundle.putBoolean(ScannerData.IS_SHOW_HAND_INPUT_BUTTON, true);
        scanner.startScan(bundle, new OnScanListener.Stub() {
            @Override
            public void onSuccess(String barcode) throws RemoteException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), barcode, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancel() throws RemoteException {
            }

            @Override
            public void onTimeout() throws RemoteException {
            }

            @Override
            public void onError(int error) throws RemoteException {
            }
        });
    }

    public void bindService(Activity context) {
        Intent service = new Intent("com.usdk.apiservice");
        service.setPackage("com.usdk.apiservice");
        context.bindService(service, new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                // Bind success, then get the object of device service
                deviceService = UDeviceService.Stub.asInterface(service);
                Log.d(TAG, "Service Binding Successful");
                //register
                try {
                    deviceService.register(null, new Binder());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                //set log level
                try {
                    deviceService.setLogLevel(LogLevel.EMVLOG_REALTIME, LogLevel.USDKLOG_VERBOSE);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                //start Printing
                Button printBtn = findViewById(R.id.btnPrint);
                EditText editText = findViewById(R.id.etValToPrint);

                printBtn.setOnClickListener(v -> {
                    try {
                        getPrinter();
                        Log.d(TAG, "Printer btn clicked successfully");
                        printer.addText(AlignMode.LEFT, editText.getText().toString());
                        printer.startPrint(new OnPrintListener.Stub() {
                            @Override
                            public void onFinish() throws RemoteException {
                            }

                            @Override
                            public void onError(int error) throws RemoteException {
                            }
                        });
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                });
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {

            }
        }, Context.BIND_AUTO_CREATE);
    }

    public UPrinter getPrinter() throws RemoteException {
        if (printer == null) {
            printer = UPrinter.Stub.asInterface(deviceService.getPrinter());
        }
        return printer;
    }
}