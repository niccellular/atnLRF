
package com.atakmap.android.atnlrf.plugin;

import com.atak.plugins.impl.AbstractPlugin;
import com.atak.plugins.impl.PluginContextProvider;

import gov.tak.api.plugin.IPlugin;
import gov.tak.api.plugin.IServiceController;
import com.atakmap.android.atnlrf.AtnLRFMapComponent;

public class AtnLRFLifecycle extends AbstractPlugin implements IPlugin {

    public AtnLRFLifecycle(IServiceController serviceController) {
        super(serviceController, new AtnLRFTool(serviceController.getService(PluginContextProvider.class).getPluginContext()), new AtnLRFMapComponent());

    }
}
