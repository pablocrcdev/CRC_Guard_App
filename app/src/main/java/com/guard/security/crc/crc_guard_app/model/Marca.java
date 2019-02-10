package com.guard.security.crc.crc_guard_app.model;

import java.util.Date;

public class Marca {
    private int dbId;
    private String imei;
    private String nfcData;
    private String horaMarca;
    private String lat;
    private String lng;
    private String estado;

    public Marca(int pDbId, String pImei, String pNfcData, String pHoraMarca, String pLat, String pLng){
        this.dbId = pDbId;
        this.imei = pImei;
        this.nfcData = pNfcData;
        this.horaMarca = pHoraMarca;
        this.lat = pLat;
        this.lng = pLng;
    }
    public Marca(String pImei, String pNfcData, String pHoraMarca, String pLat, String pLng){
        this.imei = pImei;
        this.nfcData = pNfcData;
        this.horaMarca = pHoraMarca;
        this.lat = pLat;
        this.lng = pLng;
    }

    public int getDbId() {
        return dbId;
    }

    public void setDbId(int dbId) {
        this.dbId = dbId;
    }

    public String getNfcData() {
        return nfcData;
    }

    public void setNfcData(String nfcData) {
        this.nfcData = nfcData;
    }

    public String getHoraMarca() {
        return horaMarca;
    }

    public void setHoraMarca(String horaMarca) {
        this.horaMarca = horaMarca;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLng() {
        return lng;
    }

    public void setLng(String lng) {
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
