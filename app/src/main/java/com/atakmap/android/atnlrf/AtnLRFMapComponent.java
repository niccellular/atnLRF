
package com.atakmap.android.atnlrf;

import android.content.Context;
import android.content.Intent;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.dropdown.DropDownMapComponent;

import com.atakmap.coremap.log.Log;
import com.atakmap.android.atnlrf.plugin.R;

public class AtnLRFMapComponent extends DropDownMapComponent {

    private static final String TAG = "AtnLRFMapComponent";

    private Context pluginContext;

    private AtnLRFDropDownReceiver ddr;

    public void onCreate(final Context context, Intent intent,
            final MapView view) {

        context.setTheme(R.style.ATAKPluginTheme);
        super.onCreate(context, intent, view);
        pluginContext = context;

        ddr = new AtnLRFDropDownReceiver(
                view, context);

        Log.d(TAG, "registering the plugin filter");
        DocumentedIntentFilter ddFilter = new DocumentedIntentFilter();
        ddFilter.addAction(AtnLRFDropDownReceiver.SHOW_PLUGIN);
        registerDropDownReceiver(ddr, ddFilter);





    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        super.onDestroyImpl(context, view);
    }

}
