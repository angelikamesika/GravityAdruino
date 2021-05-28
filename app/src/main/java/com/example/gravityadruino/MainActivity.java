package com.example.gravityadruino;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.provider.MediaStore;
import android.view.View;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import android.net.Uri;

public class MainActivity extends AppCompatActivity {
    private static final int READ_STORAGE_PERMISSION_REQUEST_CODE = 1;
    private static final int WRITE_STORAGE_PERMISSION_REQUEST_CODE = 2;
    public final String ACTION_USB_PERMISSION = "com.hariharan.arduinousb.USB_PERMISSION";
    private Button startButton, sendButton, clearButton, stopButton, debugButton, backButton;
    private TextView textView;
    private EditText editText;
    private RelativeLayout mLayoutFirst;
    private UsbManager usbManager;
    private UsbDevice device;
    private UsbSerialDevice serialPort;
    private UsbDeviceConnection connection;
    private VideoView mVideoView;
    private QueueImpl<Integer> mQueueData = new QueueImpl<Integer>();
    boolean mIsChangeVideo = false;
    private VideoProcess mVideoProcess;
    private final int mMaxQueueSize = 4;
    private final int mDeltaWeigth = 12;
    private MyMultiClickListener mMyMultiClickListener;

    enum GravityState {
        NONE,
        NEUTRAL,
        TUNING,
        RELOAD,
        ESTABLISH_20,
        ESTABLISH_30,
        ESTABLISH_40,
        ESTABLISH_50,
        ESTABLISH_60,
        ESTABLISH_70,
        ESTABLISH_80,
        ESTABLISH_90,
    }

    private GravityState mGravityState = GravityState.NONE;
    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            try {
                data = new String(arg0, "UTF-8");
                data.concat("/n");
                Log.d("AAA_GRAV", "data = " + data + "***");
                processData(data);
                tvAppend(textView, data);

                // parseData(data);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            Log.d("AAA_GRAV", "END RECEIVE");
        }
    };
    StringBuilder mFirstData = new StringBuilder();

    private String processData(String aData) {
        mFirstData.append(aData);
        Log.d("AAA_GRAV", "processData 111 firstData = " + mFirstData);
        if (aData.endsWith("\n")) {
            String str = mFirstData.substring(0, mFirstData.length() - 2);
            Log.d("AAA_GRAV", "processData str = " + str);
            Log.d("AAA_GRAV", "processData 222 firstData = " + mFirstData);

            //mFirstData = new StringBuilder();
            String data = mFirstData.toString();
            parseData(data);


        }
        return null;
    }


    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            // setUiEnabled(true);
                            serialPort.setBaudRate(115200);//9600
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                            tvAppend(textView, "Serial Connection Opened!\n");

                        } else {
                            Log.d("AAA_GRAVITY", "PORT NOT OPEN");
                        }
                    } else {
                        Log.d("AAA_GRAVITY", "PORT IS NULL");
                    }
                } else {
                    Log.d("AAA_GRAVITY", "PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                // onClickStart(startButton);
                onStartUsb();
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                //onClickStop(stopButton);
                onStopUsb();

            }
        }

        ;
    };

    private void setWindowParametres() {
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        int UI_OPTIONS = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(UI_OPTIONS);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_land);
        setWindowParametres();
        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);
        startButton = (Button) findViewById(R.id.buttonStart);
        sendButton = (Button) findViewById(R.id.buttonSend);
        clearButton = (Button) findViewById(R.id.buttonClear);
        stopButton = (Button) findViewById(R.id.buttonStop);
        debugButton = findViewById(R.id.buttonDebug);
        backButton = findViewById(R.id.buttonBack);
        mLayoutFirst = findViewById(R.id.layoutFirst);
        editText = (EditText) findViewById(R.id.editText);
        textView = (TextView) findViewById(R.id.textView);

        mMyMultiClickListener = new MyMultiClickListener(mLayoutFirst);
        debugButton.setOnClickListener(mMyMultiClickListener);

        mVideoView = (VideoView) findViewById(R.id.videoView);
        mVideoProcess = new VideoProcess(this);
        //setUiEnabled(false);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);

//        String filePath = Utils.getExternalStorage(VideoProcess.NEUTRAL[0]);
//
//       // mVideoView.setVideoURI(Uri.fromFile(new File (filePath)));
//        mVideoView.setVideoPath(filePath);
//
//        mVideoView.start();

        testQuequeInitial();

    }

    @Override
    protected void onResume() {
        //onStartUsb();
        requestPermissionForReadExtertalStorage();
        // testExternalStorage();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        onStopUsb();
        super.onDestroy();
    }

    public void setUiEnabled(boolean bool) {
        startButton.setEnabled(!bool);
        sendButton.setEnabled(bool);
        stopButton.setEnabled(bool);
        textView.setEnabled(bool);
    }

    private void testExternalStorage() {
        String filePath = Utils.getExternalStorage(VideoProcess.TEST[0]);
        Log.d("AAA_GRAV", "filePath = " + filePath);
        tvAppend(textView, filePath + "\n");
        File f = new File(filePath);
        if (f.exists()) {
            tvAppend(textView, "File exist!");
            Log.d("AAA_GRAV", "EXIST =" + filePath);
        } else {
            tvAppend(textView, "File NOT exist!");
            Log.d("AAA_GRAV", "NOT EXIST =" + filePath);
        }
    }

    public boolean checkPermissionForReadExtertalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    public void requestPermissionForReadExtertalStorage() {
        try {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    READ_STORAGE_PERMISSION_REQUEST_CODE);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }


    private void onStartUsb() {
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 0x2341)//Arduino Vendor ID
                {
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }
                if (!keep)
                    break;
            }
        }
    }

    public void onStopUsb() {
        if (serialPort != null) {
            serialPort.close();
        }
        if (textView != null)
            tvAppend(textView, "\nSerial Connection Closed! \n");
    }

    public void onClear() {
        textView.setText(" ");
    }

    public void onClickStart(View view) {
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 0x2341)//Arduino Vendor ID
                {
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }
                if (!keep)
                    break;
            }
        }
    }

    public void onClickSend(View view) {
        String string = editText.getText().toString();
        serialPort.write(string.getBytes());
        tvAppend(textView, "\nData Sent : " + string + "\n");

    }

    public void onClickStop(View view) {
        //setUiEnabled(false);
        if (serialPort != null) {
            serialPort.close();
        }
        tvAppend(textView, "\nSerial Connection Closed! \n");

    }

    private void getFileFromAssetsUri() {
        //  String filePath = "android.resource://" + getPackageName() + "/" + "gravity_scale_tuning.mp4";
//        String filePath = "android.resource://" + getPackageName() + "/" +
//                R.raw.gravity_scale_tuning;

        int id = this.getResources().getIdentifier("gravity_scale_tuning", "raw", this.getPackageName());
        Log.d("AAA_GRAV", "id = " + id);
//        String filePath = "android.resource://" + getPackageName() + "/" +
//                id;

        String filePath = "android.resource://" + getPackageName() + "/" +
                id;
        File f = new File(filePath);


        Log.d("AAA_GRAV", "ON CLICK = " + filePath + "\n");
        if (f.exists()) {
            Log.d("AAA_GRAV", "EXIST! ");
            textView.append("YES " + filePath + "\n");
        } else {
            Log.d("AAA_GRAV", " NOT EXIST! ");
            textView.append("NO " + filePath + "\n");
        }
        textView.setText(filePath);
        AssetManager am = getAssets();
        String[] list = new String[0];
        try {
            list = am.list("");
            Log.d("AAA_GRAV", " list.size() =  " + list.length);
            for (String s : list) {
                Log.d("AAA_GRAV", s);
            }
        } catch (IOException aE) {
            Log.d("AAA_GRAV", "EXCEPTION " + aE);
            aE.printStackTrace();
        }
        mVideoView.setVideoURI(Uri.parse(filePath));
        mVideoView.start();
    }

    private void getFileFromContentProviderUri() {
        Uri collection;
        Uri fileUri = Uri.EMPTY;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        }
        Log.d("AAA_GRAV", "MEDIA collection = " + collection);
        String[] projection = new String[]{
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE
        };
//        String[] projection = {MediaStore.Images.Media._ID};
//// Create the cursor pointing to the SDCard
//        cursor = managedQuery( MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//                projection,
//                MediaStore.Images.Media.DATA + " like ? ",
//                new String[] {"%myimagesfolder%"},
//                null);


        // String selection = MediaStore.Video.Media.DISPLAY_NAME + " like ? ";
        String selection = MediaStore.Video.Media.DATA + " like ? ";

        //String[] selectionArgs = new String[] {"%"+"GRAVITY_video_for_programming"+"%"};
        String[] selectionArgs = new String[]{"%" + "g1" + "%"};
        String sortOrder = MediaStore.Video.Media.DISPLAY_NAME + " ASC";

        try (Cursor cursor = getApplicationContext().getContentResolver().query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
        )) {
            // Cache column indices.
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            int nameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);


            while (cursor.moveToNext()) {
                // Get values of columns for a given video.
                long id = cursor.getLong(idColumn);
                String name = cursor.getString(nameColumn);
                Log.d("AAA_GRAV", "MEDIA name = " + name);

                Uri contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);


                fileUri = contentUri;
                Log.d("AAA_GRAV", "MEDIA fileUri = " + fileUri);

            }
        }

        mVideoView.setVideoURI(fileUri);
        mVideoView.start();
    }


    public void onClickClear(View view) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(" ");
            }
        });

//        VideoProcess vp = new VideoProcess(this);
//        Uri once = vp.getFileURI(VideoProcess.RELOADING[0]);
//        Uri loop = vp.getFileURI(VideoProcess.RELOADING[1]);
//        vp.playOnceAndLoop(mVideoView,
//                once,
//                loop);

        // vp.playOnce(mVideoView, once);
        // getFileFromAssetsUri();

        // testPlayVideoAndStatus(10);
        //  getFileFromContentProviderUri();
    }

    public void onClickBack(View view) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLayoutFirst.setVisibility(View.GONE);
            }
        });
    }


    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("AAA_GRAV", "tvAppend ftext = " + ftext + "*");
                ftv.append(ftext);

            }
        });
    }

    public GravityState getGravityState() {
        // TO DO
        return mGravityState;
    }

    public void parseData(String aData) {
        Log.d("AAA_GRAV", "process parseData = " + "*" + aData + "*");
        if (!aData.startsWith("scale")) {
            //  Log.d ("AAA_GRAV", "process parseData 111 weight = " + "*" + aData + "*");
            mFirstData = new StringBuilder();
            // mFirstData.append("\n");
        } else {
            String[] dataArr = aData.split(":");
            Log.d("AAA_GRAV", "process parseData weight STR = " + dataArr[dataArr.length - 1].trim() + "*");
            //int weight = Integer.parseInt(dataArr[dataArr.length - 1]);
            String weight = dataArr[dataArr.length - 1].trim();
            Log.d("AAA_GRAV", "process parseData weight = " + weight);
            int gravity = Integer.parseInt(weight);
            Log.d("AAA_GRAV", "process parseData gravity = " + gravity);
            gravity /= 10;
            Log.d("AAA_GRAV", "process parseData gravity = " + gravity);

            mFirstData = new StringBuilder();
            mFirstData.append("\n");

            processGravity(gravity);

            if (mIsChangeVideo)
                playGravityVideo(mGravityState);
        }
    }

    private void processGravity(int gravity) {
        Log.d("AAA_GRAV", "******processGravity******");
        Log.d("AAA_GRAV", "mQueueData.size() = " + mQueueData.size());
        GravityState gravityState = GravityState.NEUTRAL;
        if (mQueueData.size() < mMaxQueueSize) {
            mQueueData.enqueue(gravity);
            Log.d("AAA_GRAV", "mQueueData.size() = " + mQueueData.size());
        } else {
            int gravPrev = mQueueData.getLast();
            Log.d("AAA_GRAV", "gravPrev = " + gravPrev);
            Log.d("AAA_GRAV", "gravity = " + gravity);
            mQueueData.dequeue();
            mQueueData.enqueue(gravity);

            if (gravPrev == 0 && gravity > 0) {
                gravityState = GravityState.TUNING;
            } else if (gravPrev != 0 && gravity == 0) {
                gravityState = GravityState.RELOAD;
            } else if (gravPrev != 0 && gravity != 0) {
                int prev = mQueueData.peek();
                int var = prev;
                Log.d("AAA_GRAV", "prev = " + prev);
                Iterator it = mQueueData.iterator();
                while (it.hasNext()) {

                    int next = (int) it.next();

                    Log.d("AAA_GRAV", "prev = " + prev + "next = " + next);
                    if (Math.abs(next - prev) > mDeltaWeigth) {
                        gravityState = GravityState.TUNING;

                    } else if (/*prev == var*/Math.abs(var - prev) <= mDeltaWeigth) {
                        gravityState = establishGravityState(gravity);
                    }
                    prev = next;
                }
            }
        }
        Log.d("AAA_GRAV", "gravityState = " + gravityState +
                " mGravityState = " + mGravityState);
        if (gravityState != mGravityState) {
            mGravityState = gravityState;
            mIsChangeVideo = true;
        } else {
            mIsChangeVideo = false;
        }

        Log.d("AAA_GRAV", "mIsChangeVideo = " + mIsChangeVideo);
    }

    private GravityState establishGravityState(int gravity) {
        GravityState state = GravityState.ESTABLISH_20;
        if (gravity <= 20)
            state = GravityState.ESTABLISH_20;
        else if (gravity > 20 && gravity <= 30)
            state = GravityState.ESTABLISH_30;
        else if (gravity > 30 && gravity <= 40)
            state = GravityState.ESTABLISH_40;
        else if (gravity > 40 && gravity <= 50)
            state = GravityState.ESTABLISH_50;
        else if (gravity > 50 && gravity <= 60)
            state = GravityState.ESTABLISH_60;
        else if (gravity > 60 && gravity <= 70)
            state = GravityState.ESTABLISH_70;
        else if (gravity > 70 && gravity <= 80)
            state = GravityState.ESTABLISH_80;
        else if (gravity > 80 && gravity <= 90)
            state = GravityState.ESTABLISH_90;
        else if (gravity > 90)
            state = GravityState.ESTABLISH_90;
        return state;
    }


    private void playVideo(String[] arr) {
        int length = arr.length;
        if (length == 1) {
            Uri loop = mVideoProcess.getFileURI(arr[0]);
            mVideoProcess.playLoop(mVideoView, loop);
        } else if (length == 2) {
            Uri once = mVideoProcess.getFileURI(arr[0]);
            Uri loop = mVideoProcess.getFileURI(arr[1]);
            mVideoProcess.playOnceAndLoop(mVideoView,
                    once,
                    loop);
        }
    }


    private void playGravityVideo(GravityState state) {
        Log.d("AAA_GRAV", "playGravityVideo state = " + state);
        switch (state) {
            case NEUTRAL:
                playVideo(VideoProcess.NEUTRAL);
                break;
            case RELOAD:
                playVideo(VideoProcess.RELOADING);
                break;
            case TUNING:
                playVideo(VideoProcess.TUNING);
                break;
            case ESTABLISH_20:
                playVideo(VideoProcess.KG_20);
                break;
            case ESTABLISH_30:
                playVideo(VideoProcess.KG_30);
                break;
            case ESTABLISH_40:
                playVideo(VideoProcess.KG_40);
                break;
            case ESTABLISH_50:
                playVideo(VideoProcess.KG_50);
                break;
            case ESTABLISH_60:
                playVideo(VideoProcess.KG_60);
                break;
            case ESTABLISH_70:
                playVideo(VideoProcess.KG_70);
                break;
            case ESTABLISH_80:
                playVideo(VideoProcess.KG_80);
                break;
            case ESTABLISH_90:
                playVideo(VideoProcess.KG_90);
                break;
        }

    }

    private void testPlayVideoAndStatus(int newGravity) {
        processGravity(newGravity);
        if (mIsChangeVideo)
            playGravityVideo(mGravityState);
        Log.d("AAA_GRAV", "==========================");

    }

    private void testQuequeInitial() {
        mQueueData.enqueue(0);
        mQueueData.enqueue(0);
        mQueueData.enqueue(0);
    }


    public class MyMultiClickListener implements View.OnClickListener {
        private boolean isRunning = false;
        private int resetInTime = 2500;
        private int counter = 0;
        private final int COUNT = 4;
        private RelativeLayout mLayout;

        public MyMultiClickListener(RelativeLayout aLayout) {

            mLayout = aLayout;
        }

        @Override
        public void onClick(View v) {
            Log.d("AAA_grav", "onClick counter = " + counter + "isRunning = " + isRunning);
            if (isRunning) {
                if (counter == COUNT) {
                    onMultiCliclkListener();
                    // return;
                }
                if (counter == 2) {
                    Toast.makeText(MainActivity.this, "You tap 3 times from 5", Toast.LENGTH_LONG).show();
                }

            }
            counter++;
            if (!isRunning) {
                isRunning = true;
                Log.d("AAA_grav", "onClick counter = " + counter);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(resetInTime);
                            isRunning = false;
                            counter = 0;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }

        }

        private void onMultiCliclkListener() {
            Log.d("AAA_grav", "onMultiCliclkListener!!!");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLayout.setVisibility(View.VISIBLE);
                }
            });
        }

    }



    }