package edu.ucsd.sbrg.io.parsers.cobra;

import static java.text.MessageFormat.format;
import static org.sbml.jsbml.util.Pair.pairOf;

import java.util.Optional;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import edu.ucsd.sbrg.logging.BundleNames;
import edu.ucsd.sbrg.resolver.Registry;
import edu.ucsd.sbrg.resolver.identifiersorg.IdentifiersOrgURI;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.Unit;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCReactionPlugin;
import org.sbml.jsbml.util.ModelBuilder;

import de.zbit.sbml.util.SBMLtools;
import de.zbit.util.ResourceManager;
import de.zbit.util.Utils;
import edu.ucsd.sbrg.db.bigg.BiGGId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.hebi.matlab.mat.types.Array;
import us.hebi.matlab.mat.types.Cell;
import us.hebi.matlab.mat.types.Matrix;

public class ReactionParser {

  private static final Logger logger = LoggerFactory.getLogger(ReactionParser.class);

  private static final ResourceBundle MESSAGES = ResourceManager.getBundle(BundleNames.IO_MESSAGES);
  private final ModelBuilder builder;
  private final int index;
  private static MatlabFields matlabFields;
  private static final String DELIM = " ,;\t\n\r\f";

  private final Registry registry;

  public ReactionParser(ModelBuilder builder, int index, Registry registry) {
    this.builder = builder;
    this.index = index;
    matlabFields = MatlabFields.getInstance();
    this.registry = registry;
  }


  public void parse() {
    matlabFields.getCell(ModelField.rxns.name())
            .map(rxns -> COBRAUtils.asString(rxns.get(index), ModelField.rxns.name(), index + 1)).ifPresent(id -> {
              if (!id.isEmpty()) {
                Model model = builder.getModel();
                var biggId = BiGGId.createReactionId(id);
                Reaction reaction = model.createReaction(biggId.toBiGGId());
                setNameAndReversibility(reaction, index);
                setReactionBounds(builder, reaction, index);
                buildReactantsProducts(model, reaction, index);
                parseAnnotations(builder, reaction, id, index);
                if (reaction.getCVTermCount() > 0) {
                  reaction.setMetaId(reaction.getId());
                }
              }
            });
  }


  private void setNameAndReversibility(Reaction reaction, int index) {
    matlabFields.getCell(ModelField.rxnNames.name()).ifPresent(
      rxnNames -> reaction.setName(COBRAUtils.asString(rxnNames.get(index), ModelField.rxnNames.name(), index + 1)));
    matlabFields.getMatrix(ModelField.rev.name()).ifPresent(rev -> reaction.setReversible(rev.getDouble(index) != 0d));
  }


  private void setReactionBounds(ModelBuilder builder, Reaction reaction, int index) {
    FBCReactionPlugin rPlug = (FBCReactionPlugin) reaction.getPlugin(FBCConstants.shortLabel);
    matlabFields.getMatrix(ModelField.lb.name())
                .ifPresent(lb -> rPlug.setLowerFluxBound(builder.buildParameter(reaction.getId() + "_lb",
                  reaction.getId() + "_lb", lb.getDouble(index), true, (String) null)));
    matlabFields.getMatrix(ModelField.ub.name())
                .ifPresent(ub -> rPlug.setUpperFluxBound(builder.buildParameter(reaction.getId() + "_ub",
                  reaction.getId() + "_lb", ub.getDouble(index), true, (String) null)));
  }


  @SuppressWarnings("unchecked")
  private void buildReactantsProducts(Model model, Reaction reaction, int index) {
    // Take the current column of S and look for all non-zero coefficients
    matlabFields.getSparse(ModelField.S.name()).ifPresent(S -> {
      for (int i = 0; i < S.getNumRows(); i++) {
        double coeff = S.getDouble(i, index);
        if (coeff != 0d) {
          Optional<Cell> metCell = matlabFields.getCell(ModelField.mets.name());
          if (metCell.isPresent()) {
            Cell mets = metCell.get();
            String id = COBRAUtils.asString(mets.get(i), ModelField.mets.name(), i + 1);
            var metId = BiGGId.createMetaboliteId(id);
            Species species = model.getSpecies(metId.toBiGGId());
            if (coeff < 0d) { // Reactant
              ModelBuilder.buildReactants(reaction, pairOf(-coeff, species));
            } else if (coeff > 0d) { // Product
              ModelBuilder.buildProducts(reaction, pairOf(coeff, species));
            }
          }
        }
      }
    });
  }


  private void parseAnnotations(ModelBuilder builder, Reaction reaction, String rId, int index) {
    matlabFields.getCell(ModelField.ecNumbers.name()).ifPresent(ecNumbers -> {
      if (ecNumbers.get(index) != null) {
        parseECcodes(COBRAUtils.asString(ecNumbers.get(index), ModelField.ecNumbers.name(), index + 1), reaction);
      }
    });
    matlabFields.getCell(ModelField.rxnKeggID.name()).ifPresent(rxnKeggID -> {
      if (rxnKeggID.get(index) != null) {
        parseRxnKEGGids(COBRAUtils.asString(rxnKeggID.get(index), ModelField.rxnKeggID.name(), index + 1), reaction);
      }
    });
    matlabFields.getCell(ModelField.rxnKeggOrthology.name()).ifPresent(rxnKeggOrthology -> {
      if (rxnKeggOrthology.get(index) != null) {
        parseRxnKEGGOrthology(
          COBRAUtils.asString(rxnKeggOrthology.get(index), ModelField.rxnKeggOrthology.name(), index + 1), reaction);
      }
    });
    matlabFields.getCell(ModelField.comments.name()).ifPresent(comments -> {
      if (comments.get(index) != null) {
        String comment = COBRAUtils.asString(comments.get(index), ModelField.comments.name(), index + 1);
        appendComment(comment, reaction);
      }
    });
    matlabFields.getCell(ModelField.confidenceScores.name()).ifPresent(confidenceScores -> {
      if (confidenceScores.get(index) != null) {
        Array cell = confidenceScores.get(index);
        if (cell instanceof Matrix) {
          if (cell.getNumElements() == 0) {
            logger.debug(MESSAGES.getString("CONF_CELL_WRONG_DIMS"));
            return;
          }
          double score = ((Matrix) cell).getDouble(0);
          logger.debug(format(MESSAGES.getString("DISPLAY_CONF_SCORE"), score, reaction.getId()));
          builder.buildParameter("P_confidenceScore_of_" + org.sbml.jsbml.util.SBMLtools.toSId(rId), // id
            format("Confidence score of reaction {0}", reaction.isSetName() ? reaction.getName() : reaction.getId()), // name
            score, // value
            true, // constant
            Unit.Kind.DIMENSIONLESS // unit
          ).setSBOTerm(613);
          // TODO: there should be a specific term for confidence scores.
          // Use "613 - reaction parameter" for now.
        } else {
          logger.debug(format(MESSAGES.getString("TYPE_MISMATCH_MLDOUBLE"), cell.getClass().getSimpleName()));
        }
      }
    });
    matlabFields.getCell(ModelField.citations.name()).ifPresent(citations -> {
      if (citations.get(index) != null) {
        parseCitation(COBRAUtils.asString(citations.get(index), ModelField.citations.name(), index + 1), reaction);
      }
    });
  }


  private void appendComment(String comment, SBase sbase) {
    try {
      if (!COBRAUtils.isEmptyString(comment)) {
        sbase.appendNotes(SBMLtools.toNotesString("<p>" + comment + "</p>"));
      }
    } catch (XMLStreamException exc) {
      throw new RuntimeException(exc);
    }
  }


  private void parseECcodes(String ec, Reaction reaction) {
    if (COBRAUtils.isEmptyString(ec)) {
      return;
    }
    CVTerm term = findOrCreateCVTerm(reaction, CVTerm.Qualifier.BQB_HAS_PROPERTY);
    StringTokenizer st = new StringTokenizer(ec, DELIM);
    boolean match = false;
    while (st.hasMoreElements()) {
      String ecCode = st.nextElement().toString().trim();
      if (!ecCode.isEmpty() && validId("ec-code", ecCode)) {
        String resource = new IdentifiersOrgURI("ec-code", ecCode).getURI();
        if (!term.getResources().contains(resource)) {
          match = term.addResource(resource);
        }
      }
    }
    if (!match) {
      logger.debug(format(MESSAGES.getString("EC_CODES_UNKNOWN"), ec));
    }
    if ((term.getResourceCount() > 0) && (term.getParent() == null)) {
      reaction.addCVTerm(term);
    }
  }


  private void parseRxnKEGGids(String keggId, Reaction reaction) {
    if (COBRAUtils.isEmptyString(keggId)) {
      return;
    }
    String prefix = "kegg.reaction";
    String pattern = registry.getPatternByNamespaceName(registry.getNamespaceForPrefix(prefix));
    CVTerm term = findOrCreateCVTerm(reaction, CVTerm.Qualifier.BQB_IS);
    StringTokenizer st = new StringTokenizer(keggId, DELIM);
    while (st.hasMoreElements()) {
      String kId = st.nextElement().toString().trim();
      if (!kId.isEmpty() && Pattern.compile(pattern).matcher(kId).matches()) {
        term.addResource(new IdentifiersOrgURI(prefix, kId).getURI());
      }
    }
    if (term.getResourceCount() == 0) {
      // This is actually bad.. should only be KEGG ids, not EC-Codes
      parseECcodes(keggId, reaction);
    }
    if ((term.getResourceCount() > 0) && (term.getParent() == null)) {
      reaction.addCVTerm(term);
    }
  }


  /**
   * Searches for the first {@link CVTerm} within the given {@link SBase} that
   * has the given {@link CVTerm.Qualifier}.
   *
   * @return the found {@link CVTerm} or a new {@link CVTerm} if non exists.
   *         To distinguish between both cases, test if the parent is
   *         {@code null}.
   */
  private CVTerm findOrCreateCVTerm(SBase sbase, CVTerm.Qualifier qualifier) {
    if (sbase.getCVTermCount() > 0) {
      for (CVTerm term : sbase.getCVTerms()) {
        if (term.getQualifier().equals(qualifier)) {
          return term;
        }
      }
    }
    return new CVTerm(CVTerm.Type.BIOLOGICAL_QUALIFIER, qualifier);
  }


  private void parseRxnKEGGOrthology(String keggId, Reaction reaction) {
    if (COBRAUtils.isEmptyString(keggId)) {
      return;
    }
    String catalog = "kegg.orthology";
    String pattern = registry.getPatternByNamespaceName(registry.getNamespaceForPrefix(catalog));
    CVTerm term = findOrCreateCVTerm(reaction, CVTerm.Qualifier.BQB_IS);
    StringTokenizer st = new StringTokenizer(keggId, DELIM);
    while (st.hasMoreElements()) {
      String kId = st.nextElement().toString().trim();
      if (!kId.isEmpty() && Pattern.compile(pattern).matcher(kId).matches()) {
        term.addResource(new IdentifiersOrgURI(catalog, kId).getURI());
      }
    }
    if ((term.getResourceCount() > 0) && (term.getParent() == null)) {
      reaction.addCVTerm(term);
    }
  }


  private void parseCitation(String citation, Reaction reaction) {
    StringBuilder otherCitation = new StringBuilder();
    if (COBRAUtils.isEmptyString(citation)) {
      return;
    }
    CVTerm term = new CVTerm(CVTerm.Type.BIOLOGICAL_QUALIFIER, CVTerm.Qualifier.BQB_IS_DESCRIBED_BY);
    StringTokenizer st = new StringTokenizer(citation, ",");
    while (st.hasMoreElements()) {
      String ref = st.nextElement().toString().trim();
      if (!addResource(ref, term, "pubmed")) {
        if (!addResource(ref, term, "doi")) {
          if (!otherCitation.isEmpty()) {
            otherCitation.append(", ");
          }
          otherCitation.append(ref);
        }
      }
    }
    if (!otherCitation.isEmpty()) {
      try {
        if (reaction.isSetNotes()) {
          reaction.appendNotes("\n\nReference: " + otherCitation);
        } else {
          reaction.appendNotes(SBMLtools.toNotesString("<p>Reference: " + otherCitation + "</p>"));
        }
      } catch (XMLStreamException exc) {
        throw new RuntimeException(exc);
      }
    }
    if ((term.getResourceCount() > 0) && (term.getParent() == null)) {
      reaction.addCVTerm(term);
    }
  }


  /**
   * Tries to update a resource according to pre-defined rules. If the resource
   * starts with the MIRIAM name followed by a colon, its value is added to the
   * given term. This method assumes that there is a colon between catalog id
   * and resource id. If this is not the case, {@code false} will be returned.
   *
   * @return {@code true} if successful, {@code false} otherwise.
   */
  public boolean addResource(String resource, CVTerm term, String prefix) {
    StringTokenizer st = new StringTokenizer(resource, " ");
    while (st.hasMoreElements()) {
      String r = st.nextElement().toString().trim();
      if (r.contains(":")) {
        r = r.substring(r.indexOf(':') + 1).trim();
      } else {
        continue;
      }
      if (r.endsWith("'") || r.endsWith(".")) {
        r = r.substring(0, r.length() - 1);
      }
      r = COBRAUtils.checkId(r);
      if (validId(prefix, r)) {
        if (!resource.isEmpty()) {
          if (st.countTokens() > 1) {
            logger.debug(format(MESSAGES.getString("SKIP_COMMENT"), resource, r, prefix));
          }
          resource = new IdentifiersOrgURI(prefix, r).getURI();
          logger.debug(format(MESSAGES.getString("ADDED_URI"), resource));
          return term.addResource(resource);
        }
      }
    }
    return false;
  }


  /**
   * Checks if id belongs to a given collection by matching it with the
   * respective regexp
   *
   * @param prefix:
   *        Miriam collection
   * @param id:
   *        id to test for membership
   * @return {@code true}, if it matches, else {@code false}
   */
  private boolean validId(String prefix, String id) {
    if (id.isEmpty()) {
      return false;
    }
      String pattern = registry.getPatternByNamespaceName(registry.getNamespaceForPrefix(prefix));
    boolean validId = false;
    if (!pattern.isEmpty()) {
      validId = Pattern.compile(pattern).matcher(id).matches();
      if (!validId) {
        logger.debug(format(MESSAGES.getString("PATTERN_MISMATCH"), id, pattern));
      }
    } else {
      logger.debug(format(MESSAGES.getString("COLLECTION_UNKNOWN"), prefix));
    }
    return validId;
  }
}
