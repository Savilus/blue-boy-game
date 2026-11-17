package org.savilusGame.config;

/**
 * Release switch for development-only input: god mode, the debug overlay and map reloading.
 * <p>Controlled by the {@code dev.enabled} value in {@code config.yml}. It is disabled by
 * default so normal builds never expose cheat entry points. Immutable and safe to share.
 */
public record DevMode(boolean enabled) {

  private static final DevMode DISABLED = new DevMode(false);
  @YamlValue("dev.enabled")
  private static boolean enabledInConfig;

  static {
    YamlConfigLoader.loadConfig(DevMode.class, "/config.yml");
  }

  /**
   * Explicit value, primarily for tests and callers that own configuration.
   */
  public DevMode {
  }

  public static DevMode fromConfig() {
    return enabledInConfig ? new DevMode(true) : DISABLED;
  }
}
