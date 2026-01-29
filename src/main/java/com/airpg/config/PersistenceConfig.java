package com.airpg.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration for game persistence settings.
 * Maps to persistence.* properties in application.properties.
 */
@ConfigMapping(prefix = "persistence")
public interface PersistenceConfig {

    /**
     * Whether persistence is enabled
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Maximum number of saves to keep
     */
    @WithDefault("10")
    int maxSaves();
}
