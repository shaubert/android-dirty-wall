package com.shaubert.dirty.db;

import android.content.ContentValues;
import android.os.Parcel;

import java.util.Map.Entry;
import java.util.Set;

public class ContentValuesState {

    private ContentValues contentValues;

    public ContentValuesState(ContentValues contentValues) {
        this.contentValues = contentValues;
    }
    
    public ContentValues asContentValues() {
        return contentValues;
    }
    
    public boolean equals(Object object) {
        return contentValues.equals(object);
    }

    public int hashCode() {
        return contentValues.hashCode();
    }

    public void put(String key, String value) {
        contentValues.put(key, value);
    }

    public void putAll(ContentValues other) {
        contentValues.putAll(other);
    }

    public void put(String key, Byte value) {
        contentValues.put(key, value);
    }

    public void put(String key, Short value) {
        contentValues.put(key, value);
    }

    public void put(String key, Integer value) {
        contentValues.put(key, value);
    }

    public void put(String key, Long value) {
        contentValues.put(key, value);
    }

    public void put(String key, Float value) {
        contentValues.put(key, value);
    }

    public void put(String key, Double value) {
        contentValues.put(key, value);
    }

    public void put(String key, Boolean value) {
        contentValues.put(key, value);
    }
    
    public void putBooleanAsInt(String key, Boolean value) {
        contentValues.put(key, value != null ? (value ? 1 : 0) : null);
    }

    public void put(String key, byte[] value) {
        contentValues.put(key, value);
    }

    public void putNull(String key) {
        contentValues.putNull(key);
    }

    public int size() {
        return contentValues.size();
    }

    public void remove(String key) {
        contentValues.remove(key);
    }

    public void clear() {
        contentValues.clear();
    }

    public boolean containsKey(String key) {
        return contentValues.containsKey(key);
    }

    public Object get(String key) {
        return contentValues.get(key);
    }

    public String getAsString(String key) {
        return contentValues.getAsString(key);
    }

    public Long getAsLong(String key) {
        return contentValues.getAsLong(key);
    }

    public Integer getAsInteger(String key) {
        return contentValues.getAsInteger(key);
    }

    public Short getAsShort(String key) {
        return contentValues.getAsShort(key);
    }

    public Byte getAsByte(String key) {
        return contentValues.getAsByte(key);
    }

    public Double getAsDouble(String key) {
        return contentValues.getAsDouble(key);
    }

    public Float getAsFloat(String key) {
        return contentValues.getAsFloat(key);
    }

    public Boolean getAsBoolean(String key) {
        return contentValues.getAsBoolean(key);
    }

    public byte[] getAsByteArray(String key) {
        return contentValues.getAsByteArray(key);
    }

    public Object get(String key, Object def) {
        return contentValues.containsKey(key) && contentValues.get(key) != null ? contentValues.get(key) : def;
    }

    public String getAsString(String key, String def) {
        return contentValues.containsKey(key) && contentValues.get(key) != null ? contentValues.getAsString(key) : def;
    }

    public Long getAsLong(String key, Long def) {
        return contentValues.containsKey(key) && contentValues.get(key) != null ? contentValues.getAsLong(key) : def;
    }

    public Integer getAsInteger(String key, Integer def) {
        return contentValues.containsKey(key) && contentValues.get(key) != null ? contentValues.getAsInteger(key) : def;
    }

    public Short getAsShort(String key, Short def) {
        return contentValues.containsKey(key) && contentValues.get(key) != null ? contentValues.getAsShort(key) : def;
    }

    public Byte getAsByte(String key, Byte def) {
        return contentValues.containsKey(key) && contentValues.get(key) != null ? contentValues.getAsByte(key) : def;
    }

    public Double getAsDouble(String key, Double def) {
        return contentValues.containsKey(key) && contentValues.get(key) != null ? contentValues.getAsDouble(key) : def;
    }

    public Float getAsFloat(String key, Float def) {
        return contentValues.containsKey(key) && contentValues.get(key) != null ? contentValues.getAsFloat(key) : def;
    }

    public Boolean getAsBoolean(String key, Boolean def) {
        return contentValues.containsKey(key) && contentValues.get(key) != null ? contentValues.getAsBoolean(key) : def;
    }

    public byte[] getAsByteArray(String key, byte[] def) {
        return contentValues.containsKey(key) && contentValues.get(key) != null ? contentValues.getAsByteArray(key) : def;
    }

    public Boolean getIntAsBoolean(String key, Boolean def) {
        return contentValues.containsKey(key) && contentValues.get(key) != null ? (contentValues.getAsInteger(key) != 0) : def;
    }
    
    public Set<Entry<String, Object>> valueSet() {
        return contentValues.valueSet();
    }

    public int describeContents() {
        return contentValues.describeContents();
    }

    public void writeToParcel(Parcel parcel, int flags) {
        contentValues.writeToParcel(parcel, flags);
    }

    public String toString() {
        return contentValues.toString();
    }

}
