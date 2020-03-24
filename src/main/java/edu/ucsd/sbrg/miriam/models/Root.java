package edu.ucsd.sbrg.miriam.models;

import java.util.List;
import java.util.Map;

public class Root {
  private String apiVersion;
  private String errorMessage;
  private Map<String, List<Namespace>> payload;

  public String getApiVersion() {
    return apiVersion;
  }

  public void setApiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public Map<String, List<Namespace>> getPayload() {
    return payload;
  }

  public void setPayload(Map<String, List<Namespace>> payload) {
    this.payload = payload;
  }
}
