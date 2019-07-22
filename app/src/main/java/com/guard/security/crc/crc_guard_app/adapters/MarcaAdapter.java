package com.guard.security.crc.crc_guard_app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.guard.security.crc.crc_guard_app.R;
import com.guard.security.crc.crc_guard_app.model.Marca;

import org.w3c.dom.Text;

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
            vh.estado =  convertView.findViewById(R.id.txtEstado);
            vh.horaMarca =  convertView.findViewById(R.id.txtTitulo_Hora);
            vh.numMarca = convertView.findViewById(R.id.txtTitulo_Marca);
            vh.Imei = convertView.findViewById(R.id.txtImei);
            convertView.setTag(vh);

        } else {
            vh = (ViewHolder) convertView.getTag();
        }

        Marca currentMarca = list.get(position);
        String Resultado_Estado;
        if (currentMarca.getEstado().contains("PRC")){
            Resultado_Estado = "Procesado.";
        }else if(currentMarca.getEstado().contains("PEN")){
            Resultado_Estado = "Pendiente.";
        }else{
            Resultado_Estado = currentMarca.getEstado();
        }
        vh.estado.setText(Resultado_Estado);
        vh.numMarca.setText("Marca #"+currentMarca.getDbId());
        vh.horaMarca.setText("Hora: "+currentMarca.getHoraMarca());
        vh.Imei.setText("Imei:" + currentMarca.getImei());


        return convertView;
    }

    public class ViewHolder {
        TextView numMarca;
        TextView horaMarca;
        TextView estado;
        TextView Imei;
    }
}
