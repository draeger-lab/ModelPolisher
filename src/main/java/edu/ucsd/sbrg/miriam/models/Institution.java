package edu.ucsd.sbrg.miriam.models;

public class Institution {
  private long id;
  private String Name;
  private String homeUrl;
  private String description;
  private Location location;
  private String rorId;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getName() {
    return Name;
  }

  public void setName(String name) {
    Name = name;
  }

  public String getHomeUrl() {
    return homeUrl;
  }

  public void setHomeUrl(String homeUrl) {
    this.homeUrl = homeUrl;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Location getLocation() {
    return location;
  }

  public void setLocation(Location location) {
    this.location = location;
  }

  public String getRorId() {
    return rorId;
  }

  public void setRorId(String rorId) {
    this.rorId = rorId;
  }
}

