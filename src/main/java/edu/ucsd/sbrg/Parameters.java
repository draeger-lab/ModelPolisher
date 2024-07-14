package edu.ucsd.sbrg;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Helper class to store all parameters for running ModelPolisher in batch
 * mode.
 *
 * @author Andreas Dr&auml;ger
 */
public class Parameters {

  /**
   * @see ModelPolisherOptions#ADD_ADB_ANNOTATIONS
   */
  protected boolean addADBAnnotations = ModelPolisherOptions.ADD_ADB_ANNOTATIONS.getDefaultValue();
  /**
   * @see ModelPolisherOptions#ANNOTATE_WITH_BIGG
   */
  protected boolean annotateWithBiGG = ModelPolisherOptions.ANNOTATE_WITH_BIGG.getDefaultValue();
  /**
   * @see ModelPolisherOptions#CHECK_MASS_BALANCE
   */
  protected boolean checkMassBalance = ModelPolisherOptions.CHECK_MASS_BALANCE.getDefaultValue();
  /**
   * @see ModelPolisherOptions#COMPRESSION_TYPE
   */
  protected ModelPolisherOptions.Compression compression = ModelPolisherOptions.Compression.NONE;
  /**
   * @see ModelPolisherOptions#DOCUMENT_TITLE_PATTERN
   */
  protected String documentTitlePattern = ModelPolisherOptions.DOCUMENT_TITLE_PATTERN.getDefaultValue();
  /**
   * @see ModelPolisherOptions#FLUX_COEFFICIENTS
   */
  protected List<Double> fluxCoefficients = List.of((double) 0);
  /**
   * @see ModelPolisherOptions#FLUX_OBJECTIVES
   */
  protected List<String> fluxObjectives = List.of(ModelPolisherOptions.FLUX_OBJECTIVES.getDefaultValue());
  /**
   * @see ModelPolisherOptions#INCLUDE_ANY_URI
   */
  protected boolean includeAnyURI = ModelPolisherOptions.INCLUDE_ANY_URI.getDefaultValue();
  /**
   * @see ModelPolisherOptions#NO_MODEL_NOTES
   */
  protected boolean noModelNotes = ModelPolisherOptions.NO_MODEL_NOTES.getDefaultValue();
  /**
   * @see ModelPolisherOptions#OMIT_GENERIC_TERMS
   */
  protected boolean omitGenericTerms = ModelPolisherOptions.OMIT_GENERIC_TERMS.getDefaultValue();
  /**
   * @see ModelPolisherOptions#OUTPUT_COMBINE
   */
  protected boolean outputCOMBINE = ModelPolisherOptions.OUTPUT_COMBINE.getDefaultValue();
  /**
   * @see ModelPolisherOptions#SBML_VALIDATION
   */
  protected boolean sbmlValidation = ModelPolisherOptions.SBML_VALIDATION.getDefaultValue();
  /**
   * @see ModelPolisherOptions#WRITE_JSON
   */
  protected boolean writeJSON = ModelPolisherOptions.WRITE_JSON.getDefaultValue();

  /**
   * @see ModelPolisherOptions#MODEL_NOTES_FILE
   */
  protected File modelNotesFile = null;

  /**
   * @see ModelPolisherOptions#DOCUMENT_NOTES_FILE
   */
  protected File documentNotesFile = null;


  public Parameters(Map<String, Object> params) {
    documentTitlePattern = (String) params.getOrDefault("documentTitlePattern",
            ModelPolisherOptions.DOCUMENT_TITLE_PATTERN.getDefaultValue());
    fluxCoefficients = (List<Double>) params.getOrDefault("fluxCoefficients",
            List.of(ModelPolisherOptions.FLUX_COEFFICIENTS.getDefaultValue()));
    fluxObjectives = (List<String>) params.getOrDefault("fluxObjectives",
            List.of(ModelPolisherOptions.FLUX_OBJECTIVES.getDefaultValue()));
    annotateWithBiGG = (boolean) params.getOrDefault("annotateWithBiGG",
            ModelPolisherOptions.ANNOTATE_WITH_BIGG.getDefaultValue());
    outputCOMBINE = (boolean) params.getOrDefault("outputCOMBINE",
            ModelPolisherOptions.OUTPUT_COMBINE.getDefaultValue());
    addADBAnnotations = (boolean) params.getOrDefault("addADBAnnotations",
            ModelPolisherOptions.ADD_ADB_ANNOTATIONS.getDefaultValue());
    checkMassBalance = (boolean) params.getOrDefault("checkMassBalance",
            ModelPolisherOptions.CHECK_MASS_BALANCE.getDefaultValue());
    noModelNotes = (boolean) params.getOrDefault("noModelNotes",
            ModelPolisherOptions.NO_MODEL_NOTES.getDefaultValue());
    compression = ModelPolisherOptions.Compression.valueOf(
            (String) params.getOrDefault("compressionType",
                    ModelPolisherOptions.COMPRESSION_TYPE.getDefaultValue().toString()));
    includeAnyURI = (boolean) params.getOrDefault("includeAnyURI",
            ModelPolisherOptions.INCLUDE_ANY_URI.getDefaultValue());
    omitGenericTerms = (boolean) params.getOrDefault("omitGenericTerms",
            ModelPolisherOptions.OMIT_GENERIC_TERMS.getDefaultValue());
    sbmlValidation = (boolean) params.getOrDefault("sbmlValidation",
            ModelPolisherOptions.SBML_VALIDATION.getDefaultValue());
    writeJSON = (boolean) params.getOrDefault("writeJSON",
            ModelPolisherOptions.WRITE_JSON.getDefaultValue());
  }

  public Parameters() {
  }


  public boolean includeAnyURI() {
    return includeAnyURI;
  }


  public boolean annotateWithBiGG() {
    return annotateWithBiGG;
  }


  public boolean outputCOMBINE() {
    return outputCOMBINE;
  }


  public boolean addADBAnnotations() {
    return addADBAnnotations;
  }


  public boolean checkMassBalance() {
    return checkMassBalance;
  }


  public boolean noModelNotes() {
    return noModelNotes;
  }


  public ModelPolisherOptions.Compression compression() {
    return compression;
  }


  public String documentTitlePattern() {
    return documentTitlePattern;
  }


  public List<Double> fluxCoefficients() {
    return fluxCoefficients;
  }


  public List<String> fluxObjectives() {
    return fluxObjectives;
  }


  public boolean omitGenericTerms() {
    return omitGenericTerms;
  }


  public boolean SBMLValidation() {
    return sbmlValidation;
  }


  public boolean writeJSON() {
    return writeJSON;
  }

  public File documentNotesFile() {
    return documentNotesFile;
  }


  public File modelNotesFile() {
    return modelNotesFile;
  }

  @Override
  public String toString() {
    return "Parameters{" +
            "addADBAnnotations=" + addADBAnnotations +
            ", annotateWithBiGG=" + annotateWithBiGG +
            ", checkMassBalance=" + checkMassBalance +
            ", compression=" + compression +
            ", documentTitlePattern='" + documentTitlePattern + '\'' +
            ", fluxCoefficients=" + fluxCoefficients +
            ", fluxObjectives=" + fluxObjectives +
            ", includeAnyURI=" + includeAnyURI +
            ", noModelNotes=" + noModelNotes +
            ", omitGenericTerms=" + omitGenericTerms +
            ", outputCOMBINE=" + outputCOMBINE +
            ", sbmlValidation=" + sbmlValidation +
            ", writeJSON=" + writeJSON +
            ", modelNotesFile=" + modelNotesFile +
            ", documentNotesFile=" + documentNotesFile +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Parameters that = (Parameters) o;
    return addADBAnnotations == that.addADBAnnotations && annotateWithBiGG == that.annotateWithBiGG && checkMassBalance == that.checkMassBalance && includeAnyURI == that.includeAnyURI && noModelNotes == that.noModelNotes && omitGenericTerms == that.omitGenericTerms && outputCOMBINE == that.outputCOMBINE && sbmlValidation == that.sbmlValidation && writeJSON == that.writeJSON && compression == that.compression && Objects.equals(documentTitlePattern, that.documentTitlePattern) && Objects.equals(fluxCoefficients, that.fluxCoefficients) && Objects.equals(fluxObjectives, that.fluxObjectives) && Objects.equals(modelNotesFile, that.modelNotesFile) && Objects.equals(documentNotesFile, that.documentNotesFile);
  }

  @Override
  public int hashCode() {
    return Objects.hash(addADBAnnotations, annotateWithBiGG, checkMassBalance, compression, documentTitlePattern, fluxCoefficients, fluxObjectives, includeAnyURI, noModelNotes, omitGenericTerms, outputCOMBINE, sbmlValidation, writeJSON, modelNotesFile, documentNotesFile);
  }
}
