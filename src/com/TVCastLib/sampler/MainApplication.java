

package com.TVCastLib.sampler;

import android.app.Application;

import com.TVCastLib.discovery.DiscoveryManager;
import com.TVCastLib.service.DIALService;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        DIALService.registerApp("Levak");
        DiscoveryManager.init(getApplicationContext());

        super.onCreate();
    }
}
