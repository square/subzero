package com.squareup.plutus;

import com.squareup.testing.SlowTests;
import java.net.URL;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static java.lang.String.format;
import static org.assertj.core.api.Java6Assertions.assertThat;

@Category(SlowTests.class)
public class PlutusCliTest {
  // Check that the resource files exist for all the possible environments
  @Test public void checkPerEnvironmentResources() {
    for (PlutusConfig.EnvironmentsMap.Environments env : PlutusConfig.EnvironmentsMap.Environments.values()) {
      ClassLoader classLoader = PlutusCli.class.getClassLoader();
      URL resource = classLoader.getResource(format("plutus-%s.yaml", env.name()));
      assertThat(resource).as("resource for %s", env).isNotNull();
    }
  }

  // Check that loadEnvMap doesn't throw any exception
  @Test public void checkEnvMap() throws Exception {
    PlutusConfig.loadEnvMap();
  }
}
