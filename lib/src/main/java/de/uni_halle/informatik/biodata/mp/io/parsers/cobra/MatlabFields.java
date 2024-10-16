package de.uni_halle.informatik.biodata.mp.io.parsers.cobra;

import static java.text.MessageFormat.format;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import de.uni_halle.informatik.biodata.mp.logging.BundleNames;
import org.sbml.jsbml.Model;

import de.zbit.sbml.util.SBMLtools;
import de.zbit.util.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.hebi.matlab.mat.types.Array;
import us.hebi.matlab.mat.types.Cell;
import us.hebi.matlab.mat.types.Char;
import us.hebi.matlab.mat.types.MatlabType;
import us.hebi.matlab.mat.types.Matrix;
import us.hebi.matlab.mat.types.Sparse;
import us.hebi.matlab.mat.types.Struct;

class MatlabFields {

  private static final Logger logger = LoggerFactory.getLogger(MatlabFields.class);
  private static final ResourceBundle MESSAGES = ResourceManager.getBundle(BundleNames.IO_MESSAGES);

  private final Map<String, Array> fields;
  private static MatlabFields instance;

  /**
   */
  private MatlabFields(Struct struct) {
    fields = new HashMap<>();
    initializeFields(struct);
  }


  /**
   */
  public static void init(Struct struct) {
    instance = new MatlabFields(struct);
  }


  /**
   *
   */
  private void initializeFields(Struct struct) {
    List<String> knownFields = Arrays.stream(ModelField.values()).map(Enum::name).toList();
    List<String> fieldsFound = struct.getFieldNames();
    logger.debug(format(MESSAGES.getString("KNOWN_FIELDS_MISSING"),
      knownFields.parallelStream().filter(Predicate.not(fieldsFound::contains)).collect(Collectors.toSet())));
    logger.debug(format(MESSAGES.getString("ADDITIONAL_FIELDS_PRESENT"),
      fieldsFound.parallelStream().filter(Predicate.not(knownFields::contains)).collect(Collectors.toSet())));
    for (String field : fieldsFound) {
      fields.put(field, struct.get(field));
    }
  }


  /**
   */
  public void setDescriptionFields(Model model) {
    if (!fields.containsKey(ModelField.description.name())) {
      logger.debug(format(MESSAGES.getString("FIELD_MISSING"), ModelField.description));
      return;
    }
    Array description = fields.get(ModelField.description.name());
    if (description.getType() == MatlabType.Character) {
      Char desc = (Char) description;
      if (desc.getDimensions()[0] == 1) {
        model.setName(desc.getRow(0));
      } else {
        logger.debug(format(MESSAGES.getString("MANY_IDS_IN_DESC"), desc.asCharSequence()));
      }
    } else if (description.getType() == MatlabType.Structure) {
      setDescriptionFromStruct(model, description);
    }
  }


  /**
   */
  private void setDescriptionFromStruct(Model model, Array description) {
    Struct desc = (Struct) description;
    List<String> descriptionFields = desc.getFieldNames();
    if (descriptionFields.contains(ModelField.name.name())) {
      Array tmp = desc.getObject(ModelField.name.name());
      model.setName(COBRAUtils.asString(tmp));
    }
    if (descriptionFields.contains(ModelField.notes.name())) {
      Array tmp = desc.getObject(ModelField.notes.name());
      if (tmp.getType() == MatlabType.Character) {
        try {
          model.appendNotes(SBMLtools.toNotesString("<p>" + COBRAUtils.asString(tmp) + "</p>"));
        } catch (XMLStreamException exc) {
          throw new RuntimeException(exc);
        }
      }
    }
  }


  /**
   */
  public static MatlabFields getInstance() {
    if (instance == null) {
      throw new IllegalStateException("MatlabFields is not initialized!");
    }
    return instance;
  }


  /**
   */
  public Optional<Cell> getCell(String name) {
    if (fields.containsKey(name)) {
      Array array = fields.get(name);
      if (array.getType() == MatlabType.Cell) {
        return Optional.of((Cell) array);
      }
    }
    return Optional.empty();
  }


  /**
   */
  public Optional<Char> getChar(String name) {
    if (fields.containsKey(name)) {
      Array array = fields.get(name);
      if (array.getType() == MatlabType.Character) {
        return Optional.of((Char) array);
      }
    }
    return Optional.empty();
  }


  /**
   */
  public Optional<Matrix> getMatrix(String name) {
    if (fields.containsKey(name)) {
      Array array = fields.get(name);
      if (array instanceof Matrix) {
        return Optional.of((Matrix) array);
      }
    }
    return Optional.empty();
  }


  /**
   */
  public Optional<Sparse> getSparse(String name) {
    if (fields.containsKey(name)) {
      Array array = fields.get(name);
      if (array.getType() == MatlabType.Sparse) {
        return Optional.of((Sparse) array);
      }
    }
    return Optional.empty();
  }
}
