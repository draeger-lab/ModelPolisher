/**
 * 
 */
package edu.ucsd.sbrg.bigg;

import java.util.Map;


/**
 * @author Andreas Dr&auml;ger
 */
public class XHTMLBuilder {

  /**
   * 
   */
  private XHTMLBuilder() {
  }

  /**
   * @param header
   * @param data
   * @param caption
   * @param attributes
   * @param border
   * @param padding
   * @param spacing
   * @param style
   * @return
   */
  public static String table(Object header[], Object data[][], String caption, Map<String, String> attributes) {
    StringBuilder sb = new StringBuilder();
    sb.append("<table");
    if (attributes != null) {
      for (Map.Entry<String, String> entry : attributes.entrySet()) {
        sb.append(' ');
        sb.append(entry.getKey());
        sb.append("=\"");
        sb.append(entry.getValue());
        sb.append('"');
      }
    }
    sb.append(">\n");
    if (caption != null) {
      sb.append("<caption>");
      sb.append(caption);
      sb.append("</caption>\n");
    }
    if ((header != null) && (header.length > 0)) {
      sb.append("<tr>");
      for (Object head : header) {
        sb.append("<th>");
        sb.append(head.toString());
        sb.append("</th>");
      }
      sb.append("</tr>\n");
    }
    if ((data != null) && (data.length > 0)) {
      for (Object row[] : data) {
        if ((row != null) && (row.length > 0)) {
          sb.append("<tr>");
          for (Object entry : row) {
            sb.append("<td>");
            sb.append(entry.toString());
            sb.append("</td>");
          }
          sb.append("</tr>\n");
        }
      }
    }
    sb.append("</table>\n");
    return sb.toString();
  }

  /**
   * 
   * @param content
   * @return
   */
  public static String p(String content) {
    StringBuilder sb = new StringBuilder();
    sb.append("<p>\n");
    sb.append(content);
    sb.append("\n</p>\n");
    return sb.toString();
  }

}
