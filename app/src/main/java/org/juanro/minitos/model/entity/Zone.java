package org.juanro.minitos.model.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "zones")
public class Zone {
    @PrimaryKey
    @ColumnInfo(name = "id")
    private int id;

    @ColumnInfo(name = "fill_color")
    private String fillColor;

    @ColumnInfo(name = "color")
    private String color;

    @ColumnInfo(name = "width")
    private int width;

    @ColumnInfo(name = "points")
    private String points; // JSON string of points

    public Zone(int id, String fillColor, String color, int width, String points) {
        this.id = id;
        this.fillColor = fillColor;
        this.color = color;
        this.width = width;
        this.points = points;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFillColor() {
        return fillColor;
    }

    public void setFillColor(String fillColor) {
        this.fillColor = fillColor;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public String getPoints() {
        return points;
    }

    public void setPoints(String points) {
        this.points = points;
    }
}
