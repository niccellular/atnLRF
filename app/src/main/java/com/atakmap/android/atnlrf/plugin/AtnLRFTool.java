
package com.atakmap.android.atnlrf.plugin;

import android.content.Context;

import com.atak.plugins.impl.AbstractPluginTool;
import gov.tak.api.util.Disposable;

public class AtnLRFTool extends AbstractPluginTool implements Disposable {

    public AtnLRFTool(Context context) {
        super(context,
                context.getString(R.string.app_name),
                context.getString(R.string.app_name),
                context.getResources().getDrawable(R.drawable.ic_launcher),
                "com.atakmap.android.atnlrf.SHOW_PLUGIN");
        PluginNativeLoader.init(context);
    }

    @Override
    public void dispose() {
    }

}
