package com.squareup.subzero;

import com.squareup.subzero.ncipher.NCipher;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

import static java.lang.String.format;

public class SubzeroConfig {
  private final static String FILE_PREFIX = "!!";

  public String framebuffer;         // path
  public String framebufferSize;     // either "<width>,<height>" or "!!<path>"
  public boolean window;             // set to true to render framebuffer in an AWT window.
  public String softcard;            // hex or name
  public String softcardPassword;    // either "<password>" or "!!<path>"
  public String pubKeyEncryptionKey; // hex
  public String teamName;            // either "<name>" or "!!<path>"

  /**
   * SubzeroGui does not use ServiceContainer, so we implement our own config loading.
   */
  public static SubzeroConfig load() throws IOException {
    // load subzero.yaml
    EnvironmentsMap environmentsMap = loadEnvMap();

    // Get HSM's security world. Default to dev.
    EnvironmentsMap.Environments env;
    String securityWorld = new NCipher().getSecurityWorld();
    env = environmentsMap.environments.get(securityWorld);
    if (env == null) {
      env = EnvironmentsMap.Environments.development;
    }
    System.out.printf("Env: %s\n", env.name());

    // Load the right config file
    ClassLoader classLoader = SubzeroGui.class.getClassLoader();
    URL resource = classLoader.getResource(format("subzero-%s.yaml", env.name()));
    return new Yaml().loadAs(resource.openStream(), SubzeroConfig.class);
  }

  protected static EnvironmentsMap loadEnvMap() throws IOException {
    ClassLoader classLoader = SubzeroGui.class.getClassLoader();
    URL resource = classLoader.getResource("subzero.yaml");
    return new Yaml().loadAs(resource.openStream(), EnvironmentsMap.class);
  }

  private String getStringOrLoadFile(String parameter) throws IOException {
    if (parameter.startsWith(FILE_PREFIX)) {
      List<String> fileContent =
          Files.readAllLines(Paths.get(parameter.substring(FILE_PREFIX.length())));
      if (fileContent.size() > 0) {
        return fileContent.get(0);
      }
    }
    return parameter;
  }

  public String getSoftcardPassword() throws IOException {
    return getStringOrLoadFile(softcardPassword);
  }

  public String getTeamName() throws IOException {
    return getStringOrLoadFile(teamName);
  }

  public String getFramebufferSize() throws IOException {
    return getStringOrLoadFile(framebufferSize);
  }

  public static class EnvironmentsMap {
    public Map<String, Environments> environments;

    enum Environments {
      development
    }
  }
}
