package edu.ucsd.sbrg.parsers.models;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;

public class Notes {
  private Map<String, Object> notes = new HashMap<>();

  @JsonAnySetter
  public void addNote(String key, Object value) {
    notes.put(key, value);
  }


  public Map<String, Object> getNotes() {
    return notes;
  }

  public void setNotes(Map<String, Object> notes) {
    this.notes = notes;
  }

}
