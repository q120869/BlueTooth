package win.kymco.bluetooth.ble;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.Arrays;
import java.util.UUID;

import win.kymco.bluetooth.APP;
import win.kymco.bluetooth.R;

/**
 * BLE客戶端(主機/中心設備/Central)
 */
public class BleClientActivity extends Activity implements
        OnChartValueSelectedListener {

    // custom service
    public static final UUID UUID_SERVICE = UUID.fromString("00001523-1212-efde-1523-785feabcd123");
    // button
    public static final UUID UUID_CHAR_READ_NOTIFY_BUTTON = UUID.fromString("00001524-1212-efde-1523-785feabcd123");
    // variable resistor電阻
    public static final UUID UUID_CHAR_READ_NOTIFY_RESISTOR = UUID.fromString("00001526-1212-efde-1523-785feabcd123");

    public static final UUID UUID_DESC_NOTITY = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    // unuse
    public static final UUID UUID_CHAR_READ_NOTIFY = UUID.fromString("00000000-0000-0000-0000-000000000000");
    // LED ON/OFF
    public static final UUID UUID_CHAR_WRITE = UUID.fromString("00001525-1212-efde-1523-785feabcd123");

    // battery service
    public static final UUID BATTERY_SERVICE = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    // battery level
    public static final UUID BATTERY_LEVEL_CHAR_READ_NOTIFY = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");

    private static final String TAG = BleClientActivity.class.getSimpleName();
    private EditText mWriteET;
    private TextView mTips, mBtnValue, mVRvalue, mBatteryValue;
    private BleDevAdapter mBleDevAdapter;
    private BluetoothGatt mBluetoothGatt;
    private boolean isConnected = false;
    private TextView mTxtResult;

    private Switch sw;

    private LineChart mChart;
    private ProgressBar mProgressBar;

    // 與服務端連接的Callback
    public BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            BluetoothDevice dev = gatt.getDevice();
            Log.i(TAG, String.format("onConnectionStateChange:%s,%s,%s,%s", dev.getName(), dev.getAddress(), status, newState));
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                isConnected = true;
                gatt.discoverServices(); //啟動服務發現
            } else {
                isConnected = false;
                closeConn();
            }
            APP.toast(String.format(status == 0 ? (newState == 2 ? "與[%s]連接成功" : "與[%s]連接斷開") : ("與[%s]連接出錯,錯誤碼:" + status), dev), 0);
//            logTv(String.format(status == 0 ? (newState == 2 ? "與[%s]連接成功" : "與[%s]連接斷開") : ("與[%s]連接出錯,錯誤碼:" + status), dev));
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i(TAG, String.format("onServicesDiscovered:%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), status));
            if (status == BluetoothGatt.GATT_SUCCESS) { //BLE服務發現成功
                // 遍歷獲取BLE服務Services/Characteristics/Descriptors的全部UUID
                for (BluetoothGattService service : gatt.getServices()) {
                    StringBuilder allUUIDs = new StringBuilder("UUIDs={\nS=" + service.getUuid().toString());
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        allUUIDs.append(",\nC=").append(characteristic.getUuid());
                        for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors())
                            allUUIDs.append(",\nD=").append(descriptor.getUuid());
                    }
                    allUUIDs.append("}");
                    Log.i(TAG, "onServicesDiscovered:" + allUUIDs.toString());
                    logTv("發現服務" + allUUIDs);
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            UUID uuid = characteristic.getUuid();
            String valueStr = new String(characteristic.getValue());
            Log.i(TAG, String.format("onCharacteristicRead:%s,%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr, status));
            logTv("讀取Characteristic[" + uuid + "]:\n" + valueStr);

            // FIXME : ky5680
            if (uuid.equals(UUID_CHAR_READ_NOTIFY_BUTTON)) {
                setBtnValueTv(valueStr);
            }
            if (uuid.equals(UUID_CHAR_READ_NOTIFY_RESISTOR)) {
//                setVRValueTv(valueStr);
//                setVRValueTv(Arrays.toString(characteristic.getValue()));
                setVRValueTv((characteristic.getValue()[0] & 0xFF) +"");
            }
            if (uuid.equals(BATTERY_LEVEL_CHAR_READ_NOTIFY)) {
                setBatteryValue((characteristic.getValue()[0] & 0xFF));
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            UUID uuid = characteristic.getUuid();
            String valueStr = new String(characteristic.getValue());
            Log.i(TAG, String.format("onCharacteristicWrite:%s,%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr, status));
            logTv("寫入Characteristic[" + uuid + "]:\n" + valueStr);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            UUID uuid = characteristic.getUuid();
            String valueStr = new String(characteristic.getValue());
            Log.i(TAG, String.format("onCharacteristicChanged:%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr));
            logTv("通知Characteristic[" + uuid + "]:\n" + valueStr);

            // FIXME : ky5680
            if (uuid.equals(UUID_CHAR_READ_NOTIFY_BUTTON)) {
                setBtnValueTv((characteristic.getValue()[0]) + "");
            }
            if (uuid.equals(UUID_CHAR_READ_NOTIFY_RESISTOR)) {
//                setVRValueTv(valueStr);
//                setVRValueTv(Arrays.toString(characteristic.getValue()));
                setVRValueTv((characteristic.getValue()[0] & 0xFF) + "");
                addEntry((characteristic.getValue()[0] & 0xFF));
            }
            if (uuid.equals(BATTERY_LEVEL_CHAR_READ_NOTIFY)) {
                setBatteryValue((characteristic.getValue()[0] & 0xFF));
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            UUID uuid = descriptor.getUuid();
            String valueStr = Arrays.toString(descriptor.getValue());
            Log.i(TAG, String.format("onDescriptorRead:%s,%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr, status));
            logTv("讀取Descriptor[" + uuid + "]:\n" + valueStr);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            UUID uuid = descriptor.getUuid();
            String valueStr = Arrays.toString(descriptor.getValue());
            Log.i(TAG, String.format("onDescriptorWrite:%s,%s,%s,%s,%s", gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuid, valueStr, status));
            logTv("寫入Descriptor[" + uuid + "]:\n" + valueStr);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bleclient);

        sw = findViewById(R.id.sw);
        sw.setChecked(false);

        //CompoundButton.OnCheckedChangeListener swOnCheckedChangeListener;
        sw.setOnCheckedChangeListener(swOnCheckedChangeListener);

        RecyclerView rv = findViewById(R.id.rv_ble);
        //mWriteET = findViewById(R.id.et_write);
        mTips = findViewById(R.id.tv_tips);
        mBtnValue = findViewById(R.id.tv_button_value);
        mVRvalue = findViewById(R.id.tv_vr_value);
        mBatteryValue = findViewById(R.id.tv_battery_value);
        mTxtResult = findViewById(R.id.txtResult);
        //mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        rv.setLayoutManager(new LinearLayoutManager(this));
        mBleDevAdapter = new BleDevAdapter(new BleDevAdapter.Listener() {
            @Override
            public void onItemClick(BluetoothDevice dev) {
                closeConn();
                mBluetoothGatt = dev.connectGatt(BleClientActivity.this, false, mBluetoothGattCallback); // 連接藍牙設備
                logTv(String.format("與[%s]開始連接............", dev));
            }
        });
        rv.setAdapter(mBleDevAdapter);

        // FIXME
        mChart = findViewById(R.id.chart1);
        mChart.setOnChartValueSelectedListener(this);

        // enable description text
        mChart.getDescription().setEnabled(true);

        // enable touch gestures
        mChart.setTouchEnabled(true);

        // enable scaling and dragging
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(false);

        // if disabled, scaling can be done on x- and y-axis separately
        mChart.setPinchZoom(true);

        // set an alternative background color
        mChart.setBackgroundColor(Color.LTGRAY);

        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);

        // add empty data
        mChart.setData(data);

        // get the legend (only possible after setting data)
        Legend l = mChart.getLegend();

        // modify the legend ...
        l.setForm(Legend.LegendForm.LINE);
//        l.setTypeface(Typeface.createFromAsset(getAssets(), "OpenSans-Light.ttf"));
        l.setTextColor(Color.WHITE);

        XAxis xl = mChart.getXAxis();
//        xl.setTypeface(Typeface.createFromAsset(getAssets(), "OpenSans-Light.ttf"));
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis leftAxis = mChart.getAxisLeft();
//        leftAxis.setTypeface(Typeface.createFromAsset(getAssets(), "OpenSans-Light.ttf"));
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMaximum(300f);
        leftAxis.setAxisMinimum(-50f);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    private void addEntry(float f) {

        LineData data = mChart.getData();

        if (data != null) {

            ILineDataSet set = data.getDataSetByIndex(0);
            // set.addEntry(...); // can be called as well

            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

            data.addEntry(new Entry(set.getEntryCount(), f), 0);
            data.notifyDataChanged();

            // let the chart know it's data has changed
            mChart.notifyDataSetChanged();

            // limit the number of visible entries
            mChart.setVisibleXRangeMaximum(60);
            // chart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            mChart.moveViewToX(data.getEntryCount());

            // this automatically refreshes the chart (calls invalidate())
            // chart.moveViewTo(data.getXValCount()-7, 55f,
            // AxisDependency.LEFT);
        }
    }

    private LineDataSet createSet() {

        LineDataSet set = new LineDataSet(null, "Dynamic Data");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(Color.WHITE);
        set.setLineWidth(2f);
        set.setCircleRadius(4f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        return set;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeConn();
    }

    // BLE中心設備連接外圍設備的數量有限(大概2~7個)，在建立新連接之前必須釋放舊連接資源，否則容易出現連接錯誤133
    private void closeConn() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
        }
    }

    // 掃描BLE
    public void reScan(View view) {
        if (mBleDevAdapter.isScanning)
            APP.toast("正在掃描...", 0);
        else
            mBleDevAdapter.reScan();
    }

    // 注意：連續頻繁讀寫數據容易失敗，讀寫操作間隔最好200ms以上，或等待上次回調完成後再進行下次讀寫操作！
    // 讀取數據成功會回調->onCharacteristicChanged()
    public void read(View view) {
        BluetoothGattService service = getGattService(UUID_SERVICE);
        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID_CHAR_READ_NOTIFY_BUTTON);//通過UUID獲取可讀的Characteristic
            mBluetoothGatt.readCharacteristic(characteristic);
        }
    }

    public void readButtonValue(View view) {
        BluetoothGattService service = getGattService(UUID_SERVICE);
        if (service != null) {
            BluetoothGattCharacteristic button_characteristic = service.getCharacteristic(UUID_CHAR_READ_NOTIFY_BUTTON);//通過UUID獲取可讀的Characteristic
            mBluetoothGatt.readCharacteristic(button_characteristic);
        }
    }

    public void readVRValue(View view) {
        BluetoothGattService service = getGattService(UUID_SERVICE);
        if (service != null) {
            BluetoothGattCharacteristic resistor_characteristic = service.getCharacteristic(UUID_CHAR_READ_NOTIFY_RESISTOR);//通過UUID獲取可讀的Characteristic
            mBluetoothGatt.readCharacteristic(resistor_characteristic);
        }
    }

    public void readBatteryValue(View view) {
        BluetoothGattService service = getGattService(BATTERY_SERVICE);
        if (service != null) {
            BluetoothGattCharacteristic battery_level_characteristic = service.getCharacteristic(BATTERY_LEVEL_CHAR_READ_NOTIFY);//通過UUID獲取可讀的Characteristic
            mBluetoothGatt.readCharacteristic(battery_level_characteristic);
        }
    }

    // 注意：連續頻繁讀寫數據容易失敗，讀寫操作間隔最好200ms以上，或等待上次回調完成後再進行下次讀寫操作！
    // 寫入數據成功會回調->onCharacteristicWrite()
    public void write(View view) {
        BluetoothGattService service = getGattService(UUID_SERVICE);
        if (service != null) {
            String text = mWriteET.getText().toString();
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID_CHAR_WRITE);//通過UUID獲取可寫的Characteristic
            characteristic.setValue(text.getBytes()); //單次最多20個字節
            mBluetoothGatt.writeCharacteristic(characteristic);
        }
    }

    private CompoundButton.OnCheckedChangeListener swOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            BluetoothGattService service = getGattService(UUID_SERVICE);
            if (service != null){
                if (isChecked == true){
                    byte b[] = new byte[1];
                    b[0] = (byte) 1 ;
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID_CHAR_WRITE);//通過UUID獲取可寫的Characteristic
                    characteristic.setValue(b); //單次最多20個字節
                    mBluetoothGatt.writeCharacteristic(characteristic);
                    mTxtResult.setText("LED is ON");
                }
                else {
                    byte b[] = new byte[1];
                    b[0] = (byte) 0 ;
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID_CHAR_WRITE);//通過UUID獲取可寫的Characteristic
                    characteristic.setValue(b); //單次最多20個字節
                    mBluetoothGatt.writeCharacteristic(characteristic);
                    mTxtResult.setText("LED is OFF");
                }
            }

        }
    };

//    public void ledOn(View view) {
//        BluetoothGattService service = getGattService(UUID_SERVICE);
//        if (service != null) {
//            byte b[] = new byte[1];
//            b[0] = (byte) 1 ;
//            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID_CHAR_WRITE);//通過UUID獲取可寫的Characteristic
//            characteristic.setValue(b); //單次最多20個字節
//            mBluetoothGatt.writeCharacteristic(characteristic);
//            mTxtResult.setText("LED is ON");
//        }
//    }
//
//    public void ledOff(View view) {
//        BluetoothGattService service = getGattService(UUID_SERVICE);
//        if (service != null) {
//            byte b[] = new byte[1];
//            b[0] = (byte) 0 ;
//            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID_CHAR_WRITE);//通過UUID獲取可寫的Characteristic
//            characteristic.setValue(b); //單次最多20個字節
//            mBluetoothGatt.writeCharacteristic(characteristic);
//            mTxtResult.setText("LED is OFF");
//        }
//    }

    // 設置通知Characteristic變化會回調->onCharacteristicChanged()
    public void setNotify(View view) {
        BluetoothGattService service = getGattService(UUID_SERVICE);
        if (service != null) {
            // 設置Characteristic通知
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID_CHAR_READ_NOTIFY_BUTTON);//通過UUID獲取可通知的Characteristic
            mBluetoothGatt.setCharacteristicNotification(characteristic, true);

            // 向Characteristic的Descriptor屬性寫入通知開關，使藍牙設備主動向手機發送數據
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID_DESC_NOTITY);
            // descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);//和通知類似,但服務端不主動發數據,只指示客戶端讀取數據
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    //設定按鈕通知
    public void setButtonNotify(View view) {
        BluetoothGattService service = getGattService(UUID_SERVICE);
        if (service != null) {
            BluetoothGattCharacteristic button_characteristic = service.getCharacteristic(UUID_CHAR_READ_NOTIFY_BUTTON);//通過UUID獲取可通知的Characteristic
            mBluetoothGatt.setCharacteristicNotification(button_characteristic, true);

            BluetoothGattDescriptor descriptor = button_characteristic.getDescriptor(UUID_DESC_NOTITY);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    //設定可變電阻通知
    public void setVRNotify(View view) {
        BluetoothGattService service = getGattService(UUID_SERVICE);
        if (service != null) {
            BluetoothGattCharacteristic resistor_characteristic = service.getCharacteristic(UUID_CHAR_READ_NOTIFY_RESISTOR);//通過UUID獲取可通知的Characteristic
            mBluetoothGatt.setCharacteristicNotification(resistor_characteristic, true);

            BluetoothGattDescriptor descriptor = resistor_characteristic.getDescriptor(UUID_DESC_NOTITY);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    //設定模擬充電通知
    public void setBatteryNotify(View view) {
        BluetoothGattService service = getGattService(BATTERY_SERVICE);
        if (service != null) {
            BluetoothGattCharacteristic battery_level_characteristic = service.getCharacteristic(BATTERY_LEVEL_CHAR_READ_NOTIFY);//通過UUID獲取可通知的Characteristic
            mBluetoothGatt.setCharacteristicNotification(battery_level_characteristic, true);

            BluetoothGattDescriptor descriptor = battery_level_characteristic.getDescriptor(UUID_DESC_NOTITY);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }


    // 獲取Gatt服務
    private BluetoothGattService getGattService(UUID uuid) {
        if (!isConnected) {
            APP.toast("沒有連接", 0);
            return null;
        }
        BluetoothGattService service = mBluetoothGatt.getService(uuid);
        if (service == null)
            APP.toast("沒有找到服務UUID=" + uuid, 0);
        return service;
    }

    // 輸出日誌
    private void logTv(final String msg) {
        if (isDestroyed())
            return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                APP.toast(msg, 0);
                mTips.append(msg + "\n\n");
            }
        });
    }

    //設定按鈕的值，依照韌體內的設定寫判斷條件
    private void setBtnValueTv(final String msg) {
        if (isDestroyed())
            return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (msg.equals("1")){
                    mBtnValue.setText("Press");
                }
                else if (msg.equals("0")){
                    mBtnValue.setText("Release");
                }
               //mBtnValue.setText(msg);
                Log.e(TAG, msg );
            }
        });
    }

    private void setVRValueTv(final String msg) {
        if (isDestroyed())
            return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVRvalue.setText(msg);
            }
        });
    }

    private void setBatteryValue(final int i) {
        if (isDestroyed())
            return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                mBatteryValue.setText(String.valueOf(i));
//                mProgressBar.setProgress(i);
            }
        });
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        Toast toast = Toast.makeText(this, e.toString(), Toast.LENGTH_LONG);
        toast.show();
        Log.i(" selected", e.toString());
    }

    @Override
    public void onNothingSelected() {
        Log.i("Nothing selected", "Nothing selected.");
    }
}