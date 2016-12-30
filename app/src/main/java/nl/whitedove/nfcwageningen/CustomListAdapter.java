package nl.whitedove.nfcwageningen;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

@SuppressLint("SimpleDateFormat")
class CustomListAdapter extends BaseAdapter {

    private ArrayList<LogItem> listData;

    private LayoutInflater layoutInflater;

    CustomListAdapter(Context context, ArrayList<LogItem> listData) {

        this.listData = listData;
        layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return listData.size();
    }

    @Override
    public Object getItem(int position) {
        return listData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.row_list_layout, parent, false);
            holder = new ViewHolder();
            holder.cacherView = (TextView) convertView.findViewById(R.id.cacher);
            holder.datumView = (TextView) convertView.findViewById(R.id.datum);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.cacherView.setText(String.format("%s%s", listData.get(position).getCacher(), listData.get(position).getFtf()));
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        holder.datumView.setText(dateFormat.format(listData.get(position).getDatum()));
        return convertView;
    }

    private static class ViewHolder {
        TextView cacherView;
        TextView datumView;
    }
}