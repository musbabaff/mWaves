package blockstock.plugin.mWaves.utils.color.impl;

import blockstock.plugin.mWaves.mWaves;
import blockstock.plugin.mWaves.utils.color.Colorizer;

public class VanillaColorizer implements Colorizer {

    private final mWaves plugin;


    public VanillaColorizer(mWaves plugin) {
        this.plugin = plugin;
    }

    @Override
    public String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        return plugin.getUtils().translateAlternateColorCodes('&', message);
    }
}
