

package com.TVCastLib.sampler.widget;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.TVCastLib.sampler.R;
import com.TVCastLib.core.AppInfo;

import java.util.Comparator;

public class AppAdapter extends ArrayAdapter<AppInfo> {
    Context context; 
    int resourceId; 
    String runningAppId;

    public AppAdapter(Context context, int resource) {
        super(context, resource);
        this.context = context;
        this.resourceId = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (convertView == null) {
            view = View.inflate(getContext(), resourceId, null);
        }

        AppInfo app = this.getItem(position);

        TextView textView = (TextView) view.findViewById(R.id.itemTitle);
        TextView subTextView = (TextView) view.findViewById(R.id.itemSubTitle);
        ImageView imageView = (ImageView) view.findViewById(R.id.itemCheck);
        textView.setText(app.getName());
        subTextView.setText(app.getId());
        if (runningAppId != null && runningAppId.equals(app.getId())) {
            System.out.println("runningAppId: " + runningAppId + ", id: " + app.getId());
            textView.setTextColor(Color.RED);
            imageView.setVisibility(View.VISIBLE);
        } else {
            textView.setTextColor(Color.BLACK);
            imageView.setVisibility(View.INVISIBLE);
        }

        return view;
    }

    public void setRunningAppId(String appId) {
        runningAppId = appId;
    }

    public void sort() {
        this.sort(new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo lhs, AppInfo rhs) {
                return lhs.getName().compareTo(rhs.getName());
            }
        });
    }
}
