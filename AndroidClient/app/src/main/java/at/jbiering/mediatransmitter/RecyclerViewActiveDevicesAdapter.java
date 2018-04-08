package at.jbiering.mediatransmitter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;
import java.util.Set;

import at.jbiering.mediatransmitter.model.Device;

public class RecyclerViewActiveDevicesAdapter
        extends RecyclerView.Adapter<RecyclerViewActiveDevicesAdapter.ViewHolder>{

    private List<Device> activeDevices;

    public RecyclerViewActiveDevicesAdapter(List<Device> activeDevices) {
        this.activeDevices = activeDevices;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView textViewPersonName;
        private TextView textViewIp;

        public ViewHolder(View openChatsRowItem) {
            super(openChatsRowItem);

            textViewPersonName = openChatsRowItem.findViewById(R.id.textViewPersonName);
            textViewIp = openChatsRowItem.findViewById(R.id.textViewDeviceIp);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View newChatRowItem = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.recycler_view_active_devices_row_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(newChatRowItem);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Device device = activeDevices.get(position);

        holder.textViewIp.setText(device.getIp());
        holder.textViewPersonName.setText(device.getName());
    }

    @Override
    public int getItemCount() {
        return activeDevices.size();
    }
}
