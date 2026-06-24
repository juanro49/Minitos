package org.juanro.minitos.gui.adapter;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.core.content.ContextCompat;
import android.widget.TextView;

import java.util.ArrayList;

import org.juanro.minitos.gui.activity.MapActivity;
import org.juanro.minitos.model.entity.VehicleWithFavorite;

/**
 * Cursor adapter to display search results in a dropdown list
 */
public class SearchVehicleAdapter extends CursorAdapter {

    private final ArrayList<VehicleWithFavorite> vehicles;

    public SearchVehicleAdapter(Context context, Cursor cursor, ArrayList<VehicleWithFavorite> vehicles) {
        super(context, cursor, false);
        this.vehicles = vehicles;
    }

    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        final int position = cursor.getPosition();
        TextView textView = view.findViewById(android.R.id.text1);
        textView.setText(vehicles.get(position).getVehicle().getName());
        textView.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
        view.setBackgroundColor(Color.rgb(243, 243, 243)); // background_holo_light
        view.setOnClickListener(v -> {
            Intent intent = new Intent(context, MapActivity.class);
            intent.putExtra("vehicle", vehicles.get(position).getVehicle());
            context.startActivity(intent);
        });
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        return inflater.inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
    }

}
