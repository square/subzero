package com.squareup.subzero;

import java.net.URL;
import org.junit.Test;

import static java.lang.String.format;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class SubzeroGuiTest {
  // Check that the resource files exist for all the possible environments
  @Test public void checkPerEnvironmentResources() {
    for (SubzeroConfig.EnvironmentsMap.Environments env : SubzeroConfig.EnvironmentsMap.Environments.values()) {
      ClassLoader classLoader = SubzeroGui.class.getClassLoader();
      URL resource = classLoader.getResource(format("subzero-%s.yaml", env.name()));
      assertThat(resource).as("resource for %s", env).isNotNull();
    }
  }

  // Check that loadEnvMap doesn't throw any exception
  @Test public void checkEnvMap() throws Exception {
    SubzeroConfig.loadEnvMap();
  }
}
