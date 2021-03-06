package com.guard.security.crc.crc_guard_app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.guard.security.crc.crc_guard_app.R;
import com.guard.security.crc.crc_guard_app.model.Marca;

import java.util.List;

public class MarcaAdapter extends BaseAdapter {
    private Context context;
    private List<Marca> list;
    private int layout;

    public MarcaAdapter(Context context, List<Marca> list, int layout){
        this.context = context;
        this.list = list;
        this.layout = layout;
    }
    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int id) {
        return id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        ViewHolder vh;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(layout, null);
            vh = new ViewHolder();
            vh.numMarca = (TextView) convertView.findViewById(R.id.textViewNumMarca);
            //vh.idDevice = (TextView) convertView.findViewById(R.id.textViewIdDevice);
            //vh.nfcData = (TextView) convertView.findViewById(R.id.textViewNfcData);
            vh.horaMarca = (TextView) convertView.findViewById(R.id.textViewHoraMarca);
            //vh.latitud = (TextView) convertView.findViewById(R.id.textViewLatitud);
            //vh.longitud = (TextView) convertView.findViewById(R.id.textViewLongitud);
            vh.estado = (TextView) convertView.findViewById(R.id.textViewIndEstado);
            convertView.setTag(vh);
        } else {
            vh = (ViewHolder) convertView.getTag();
        }

        Marca currentMarca = list.get(position);

        vh.numMarca.setText(currentMarca.getDbId()+"");
        //vh.idDevice.setText(currentMarca.getImei());
        //vh.nfcData.setText(currentMarca.getNfcData());
        vh.horaMarca.setText(currentMarca.getHoraMarca());
        //vh.latitud.setText(currentMarca.getLat());
        //vh.longitud.setText(currentMarca.getLng());
        vh.estado.setText(currentMarca.getEstado());

        return convertView;
    }

    public class ViewHolder {
        TextView numMarca;
        //TextView idDevice;
        //TextView nfcData;
        TextView horaMarca;
        //TextView latitud;
        //TextView longitud;
        TextView estado;
    }
}
