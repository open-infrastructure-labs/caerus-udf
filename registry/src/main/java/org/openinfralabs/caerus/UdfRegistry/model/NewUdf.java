package org.openinfralabs.caerus.UdfRegistry.model;


import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@ApiModel(description = "Details about new UDF configuration, only used by post request to create a new UDF.")
public class NewUdf implements Serializable {
  private String name;
  private int pkg; //0 = function; 1 = docker
  private String language;
  private BigDecimal interfaceVersion;
  private BigDecimal languageVersion;
  private String main;
  private List<String> invocationEvents;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getPkg() {
    return pkg;
  }

  public void setPkg(int pkg) {
    this.pkg = pkg;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public BigDecimal getInterfaceVersion() {
    return interfaceVersion;
  }

  public void setInterfaceVersion(BigDecimal interfaceVersion) {
    this.interfaceVersion = interfaceVersion;
  }

  public BigDecimal getLanguageVersion() {
    return languageVersion;
  }

  public void setLanguageVersion(BigDecimal languageVersion) {
    this.languageVersion = languageVersion;
  }

  public String getMain() {
    return main;
  }

  public void setMain(String main) {
    this.main = main;
  }

  public List<String> getInvocationEvents() {
    return invocationEvents;
  }

  public void setInvocationEvents(List<String> invocationEvents) {
    this.invocationEvents = invocationEvents;
  }
}
