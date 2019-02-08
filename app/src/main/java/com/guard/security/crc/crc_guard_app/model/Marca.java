package com.guard.security.crc.crc_guard_app.model;

import java.util.Date;

public class Marca {
    private String imei;
    private String nfcData;
    private Date horaMarca;
    private double lat;
    private double lng;
    private String estado;

    public Marca(String pImei, String pNfcData, Date pHoraMarca, double pLat, double pLng){
        this.imei = pImei;
        this.nfcData = pNfcData;
        this.horaMarca = pHoraMarca;
        this.lat = pLat;
        this.lng = pLng;
    }

    public String getNfcData() {
        return nfcData;
    }

    public void setNfcData(String nfcData) {
        this.nfcData = nfcData;
    }

    public Date getHoraMarca() {
        return horaMarca;
    }

    public void setHoraMarca(Date horaMarca) {
        this.horaMarca = horaMarca;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }
}
