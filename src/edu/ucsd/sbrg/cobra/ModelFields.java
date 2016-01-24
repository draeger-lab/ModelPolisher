/**
 * 
 */
package edu.ucsd.sbrg.cobra;

/**
 * @author Andreas Dr&auml;ger
 */
public enum ModelFields {
  /**
   * Matrix of constraints, form <i>&mu; &sdot; A &sdot; v + B &sdot; v =
   * 0</i> or g(<i>&mu;</i>) &sdot; <i>A &sdot; v + B &sdot; v = 0</i> with
   * g(<i>&mu;</i>) being some continuous nonlinear function of <i>&mu</i>;
   * @see #B
   */
  A,
  /**
   * Can be part of the {@link #description}.
   */
  author,
  /**
   * The bound vector of metabolite concentration change rates (usually but not
   * always all zero, i.e., steady-state): <i>S &sdot; v = b</i>. Must have same
   * dimension as {@link #mets}
   */
  b,
  /**
   * Matrix of constraints, form <i>&mu; &sdot; A &sdot; v + B &sdot; v =
   * 0</i> or g(<i>&mu;</i>) &sdot; <i>A &sdot; v + B &sdot; v = 0</i> with
   * g(<i>&mu;</i>) being some continuous nonlinear function of <i>&mu</i>;
   * @see #A
   */
  B,
  /**
   * The objective function vector for max(<i>c' &sdot; v</i>)
   */
  c,
  /**
   * Literature references for the reactions.
   */
  citations,
  /**
   * These are human-readable notes for reactions.
   */
  comments,
  /**
   * 
   */
  confidenceScores,
  /**
   * Objective sense, i.e., to minimize or maximize the objective
   */
  csense,
  /**
   * Human-redable information about the model
   */
  description,
  /**
   * 
   */
  disabled,
  /**
   * The Enzyme Commission codes for the reactions.
   */
  ecNumbers,
  /**
   * Can be part of the {@link #description}.
   */
  genedate,
  /**
   * Can be part of the {@link #description}.
   */
  geneindex,
  /**
   * The gene products.
   */
  genes,
  /**
   * Can be part of the {@link #description}.
   */
  genesource,
  /**
   * 
   */
  grRules,
  /**
   *  Lower reaction flux bounds
   */
  lb,
  /**
   * Must have same dimension as {@link #mets}.
   */
  metCharge,
  /**
   * Optional: if present, it must have same dimension as {@link #mets}.
   */
  metCHEBIID,
  /**
   * Chemical formulas for metabolites, must have same dimension as {@link #mets}.
   */
  metFormulas,
  /**
   * Optional: if present, it must have same dimension as {@link #mets}.
   */
  metHMDB,
  /**
   * Optional: if present, it must have same dimension as {@link #mets}.
   */
  metInchiString,
  /**
   * Optional: if present, it must have same dimension as {@link #mets}.
   */
  metKeggID,
  /**
   * Descriptive metabolite names, must have same dimension as {@link #mets}
   */
  metNames,
  /**
   * Optional: if present, it must have same dimension as {@link #mets}.
   */
  metPubChemID,
  /**
   * Metabolite BiGG ids (incl. compartment code)
   */
  mets,
  /**
   * Optional: if present, it must have same dimension as {@link #mets}.
   */
  metSmile,
  /**
   * Can be part of the {@link #description}.
   */
  name,
  /**
   * Can be part of the {@link #description}.
   */
  notes,
  /**
   * Can be part of the {@link #description}.
   */
  organism,
  /**
   * A vector consisting of zeros and ones that translate to binary and determine
   * if the corresponding reaction of that index is reversible (1) or not (0).
   */
  rev,
  /**
   * 
   */
  rules,
  /**
   * 
   */
  rxnGeneMat,
  /**
   * Reaction identifiers in KEGG, but sometimes also contains {@link #ecNumbers}.
   */
  rxnKeggID,
  /**
   * Descriptive reaction names
   */
  rxnNames,
  /**
   * Reaction BiGG ids
   */
  rxns,
  /**
   * Stoichiometric matrix
   */
  S,
  /**
   * This defines groups of reactions that belong to a common reaction subsystem.
   * Their number must hence be identical to the reaction count. Subsystems are
   * listed repeatedly in the source file.
   */
  subSystems,
  /**
   * Upper reaction flux bounds
   */
  ub;
}
