package edu.ucsd.sbrg.io.parsers.cobra;

import de.zbit.util.ResourceManager;
import edu.ucsd.sbrg.logging.BundleNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.hebi.matlab.mat.types.Array;
import us.hebi.matlab.mat.types.Cell;
import us.hebi.matlab.mat.types.Char;
import us.hebi.matlab.mat.types.MatlabType;

import java.util.Arrays;
import java.util.ResourceBundle;

import static java.text.MessageFormat.format;

public class COBRAUtils {

  private static final Logger logger = LoggerFactory.getLogger(COBRAUtils.class);

  /**
   * Bundle for ModelPolisher logger messages
   */
  private static final ResourceBundle MESSAGES = ResourceManager.getBundle(BundleNames.IO_MESSAGES);

  /**
   * @param string
   *        the {@link String} to be tested.
   * @return {@code true} if the given {@link String} is either {@code null} or
   *         empty or equals empty square brackets.
   */
  public static boolean isEmptyString(String string) {
    return (string == null) || string.isEmpty() || string.equals("[]");
  }


  /**
   * Necessary to check for a special whitespace (code 160) at beginning of id
   * (iCHOv1.mat, possibly other models) and to remove trailing ';'
   *
   * @return trimmed id without ';' at the end
   */
  public static String checkId(String id) {
    if (id.startsWith("InChI")) {
      return id;
    }
    if (id.startsWith(Character.toString((char) 160)) || id.startsWith("/")) {
      id = id.substring(1);
    }
    if (id.endsWith(";")) {
      id = id.substring(0, id.length() - 1);
    } else if (id.contains(";")) {
      logger.debug(MESSAGES.getString("TRUNCATED_ID") + id);
      id = id.substring(0, id.indexOf(";"));
    }
    return id;
  }


  /**
   */
  public static boolean exists(Array cell, int i) {
    if (cell != null) {
      if (cell instanceof Cell) {
        return (((Cell) cell).get(i) != null);
      }
      return true;
    }
    return false;
  }


  public static String asString(Array array) {
    return asString(array, null, -1);
  }


  public static String asString(Array array, String parentName, int parentIndex) {
    StringBuilder sb = new StringBuilder();
    if (array.getType() == MatlabType.Character) {
      Char string = (Char) array;
      if (string.getDimensions()[0] > 1) {
        logger.debug(format(MESSAGES.getString("MANY_STRINGS_IN_CELL"), string.asCharSequence()));
      }
      for (int i = 0; i < string.getDimensions()[0]; i++) {
        if (i > 0) {
          sb.append('\n');
        }
        sb.append(string.getRow(i));
      }
    } else if (!Arrays.equals(array.getDimensions(), new int[] {0, 0})) {
      logger.debug(format(MESSAGES.getString("TYPE_MISMATCH_STRING"), array.getType().toString(), "parentName = %s",
        parentName, "parentIndex = %s", parentIndex));
    }
    return sb.toString();
  }
}
