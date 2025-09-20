package com.fanconn.rideOS.menu;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.fanconn.rideOS.R;

public class MenuAdapter extends ArrayAdapter<String> {

    private final Context context;
    private final String[] menuItems;
    private final int[] icons;
    private int selectedPosition = 0;

    public MenuAdapter(Context context, String[] menuItems, int[] icons) {
        super(context, R.layout.item_menu_tab, menuItems);
        this.context = context;
        this.menuItems = menuItems;
        this.icons = icons;
    }

    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged(); // Refresh ListView to apply style changes
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View rowView = LayoutInflater.from(context).inflate(R.layout.item_menu_tab, parent, false);

        TextView label = rowView.findViewById(R.id.tabLabel);
        ImageView icon = rowView.findViewById(R.id.tabIcon);
        View indicator = rowView.findViewById(R.id.selectedIndicator);
        LinearLayout layout = rowView.findViewById(R.id.tabLayout);

        // Set label and icon
        label.setText(menuItems[position]);
        icon.setImageResource(icons[position]);

        // Set selection state
        if (position == selectedPosition) {
            indicator.setVisibility(View.VISIBLE);              // Show blue bar
            label.setTextColor(Color.WHITE);                    // Bright label
//            label.setTypeface(null, Typeface.BOLD);             // Bold if selected
            layout.setSelected(true);                           // For selector background
        } else {
            indicator.setVisibility(View.GONE);                 // Hide blue bar
            label.setTextColor(Color.parseColor("#6F6F6F"));    // Dim label
            label.setTypeface(null, Typeface.NORMAL);           // Normal if not selected
            layout.setSelected(false);
        }

        return rowView; // ✅ Required to show item
    }
}
