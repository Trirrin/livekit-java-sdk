package io.livekit.sdk.rtc;

import java.util.ArrayList;
import java.util.List;

/** Configuration for an ICE server. */
public class IceServerConfig {
  private final List<String> urls;
  private String username;
  private String credential;

  public IceServerConfig() {
    this.urls = new ArrayList<>();
  }

  public IceServerConfig(String url) {
    this();
    this.urls.add(url);
  }

  public IceServerConfig(List<String> urls) {
    this.urls = new ArrayList<>(urls);
  }

  public List<String> getUrls() {
    return urls;
  }

  public void addUrl(String url) {
    urls.add(url);
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getCredential() {
    return credential;
  }

  public void setCredential(String credential) {
    this.credential = credential;
  }
}
