package org.savilusGame.config;

import org.apache.commons.lang3.StringUtils;

/**
 * Release switch for development-only input: god mode, the debug overlay and map reloading.
 * <p>Disabled by default. Start the game with {@code -Dblueboy.dev=true} to enable those
 * keys; normal builds never expose cheat entry points. Immutable and safe to share.
 */
public final class DevMode {

  /** System property that enables development-only input. */
  public static final String PROPERTY = "blueboy.dev";

  private static final DevMode DISABLED = new DevMode(false);

  private final boolean enabled;

  /** Explicit value, primarily for tests and callers that own configuration. */
  public DevMode(boolean enabled) {
    this.enabled = enabled;
  }

  /** Value of {@value #PROPERTY}; anything but {@code true} keeps release behaviour. */
  public static DevMode fromSystemProperties() {
    String property = StringUtils.trimToEmpty(System.getProperty(PROPERTY));
    return Boolean.parseBoolean(property) ? new DevMode(true) : DISABLED;
  }

  public boolean isEnabled() {
    return enabled;
  }
}
