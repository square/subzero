package com.squareup.subzero;

import com.google.common.base.Strings;
import com.ncipher.nfast.NFException;
import com.squareup.subzero.ncipher.NCipher;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
  public String dataSignerKey;       // should be "subzerodatasigner", unless a different name was given to the key.

  public static SubzeroConfig load(boolean nCipher, String configFile)
      throws IOException, NFException {
    if (nCipher) {
      String securityWorld = new NCipher().getSecurityWorld();
      System.out.println(format("Security world: %s", securityWorld));
    } else {
      System.out.println("skipping nCipher");
    }

    // Load the right config file
    InputStream file;
    if (Strings.isNullOrEmpty(configFile)) {
      ClassLoader classLoader = SubzeroGui.class.getClassLoader();
      URL resource = classLoader.getResource("sample.yaml");
      file = resource.openStream();
    } else {
      file = new FileInputStream(configFile);
    }
    return new Yaml().loadAs(file, SubzeroConfig.class);
  }
}
