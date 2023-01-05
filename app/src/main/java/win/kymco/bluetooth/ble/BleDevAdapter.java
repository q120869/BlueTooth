package win.kymco.bluetooth.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import win.kymco.bluetooth.R;

public class BleDevAdapter extends RecyclerView.Adapter<BleDevAdapter.VH> {
    private static final String TAG = BleDevAdapter.class.getSimpleName();
    private final Listener mListener;
    private final Handler mHandler = new Handler();
    private final List<BleDev> mDevices = new ArrayList<>();
    public boolean isScanning;
    private final ScanCallback mScanCallback = new ScanCallback() {// 掃描Callback
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BleDev dev = new BleDev(result.getDevice(), result);
            if (!mDevices.contains(dev)
                    && dev.dev.getName() != null
                    && dev.dev.getName().contains("KYMCO")) {
                mDevices.add(dev);
                notifyDataSetChanged();
                Log.i(TAG, "onScanResult: " + result); // result.getScanRecord() 獲取BLE廣播數據
            }
        }
    };

    BleDevAdapter(Listener listener) {
        mListener = listener;
        scanBle();
    }

    // 重新掃描
    public void reScan() {
        mDevices.clear();
        notifyDataSetChanged();
        scanBle();
    }

    // 掃描BLE藍牙(不會掃描經典藍牙)
    private void scanBle() {
        isScanning = true;
//        BluetoothAdapter bluetoothAdapter = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE).getDefaultAdapter();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        final BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        // Android5.0新增的掃描API，掃描返回的結果更友好，比如BLE廣播數據以前是byte[] scanRecord，而新API幫我們解析成ScanRecord類
        bluetoothLeScanner.startScan(mScanCallback);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bluetoothLeScanner.stopScan(mScanCallback); //停止掃描
                isScanning = false;
            }
        }, 3000);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dev, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final VH holder, int position) {
        BleDev dev = mDevices.get(position);
        String name = dev.dev.getName();
        String address = dev.dev.getAddress();
        holder.name.setText(
                String.format(" Device Name = %s \n Mac Address = %s \n Rssi = %s",
                        name, address, dev.scanResult.getRssi()));
//        holder.address.setText(String.format("廣播數據{%s}", dev.scanResult.getScanRecord()));
    }

    @Override
    public int getItemCount() {
        return mDevices.size();
    }

    class VH extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView name;
        final TextView address;

        VH(final View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            name = itemView.findViewById(R.id.name);
            address = itemView.findViewById(R.id.address);
        }

        @Override
        public void onClick(View v) {
            int pos = getAdapterPosition();
            Log.d(TAG, "onClick, getAdapterPosition=" + pos);
            if (pos >= 0 && pos < mDevices.size())
                mListener.onItemClick(mDevices.get(pos).dev);
        }
    }

    public interface Listener {
        void onItemClick(BluetoothDevice dev);
    }

    public static class BleDev {
        public BluetoothDevice dev;
        ScanResult scanResult;

        BleDev(BluetoothDevice device, ScanResult result) {
            dev = device;
            scanResult = result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BleDev bleDev = (BleDev) o;
            return Objects.equals(dev, bleDev.dev);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dev);
        }
    }
}