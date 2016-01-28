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
   * dimension as {@link #mets}.
   * <p>
   * The mat files where the b vector isn't zero include constraints that couple
   * the flux through the model's reactions to the BOF. These are mat files
   * joining reconstructions of two or more species. These constraints are
   * represented in the b and A fields.
   * <p>
   * If this cannot be expressed in SBML, then an error message would be
   * appropriate. The SBML file would probably not result in a functional joined
   * model though.
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
   * Confidence scores must have the same dimension as the reactions. These are
   * an optional input, but it provides additional information on the reaction
   * content. Adding them as notes would be a good idea.
   */
  confidenceScores,
  /**
   * The csense field expresses equality constraints in the model. If this field
   * is not defined in the reconstruction, equality constraints are assumed when
   * performing the optimization. Its value is a {@link String} whose length
   * must equal the number of metabolites. An 'E' at index <i>i</i> means that
   * in this dimension <i>S &sdot; v = b</i> (equality), 'G' means &ge; greater
   * than or equal to and an 'L' denotes &le;.
   * 
   * @see #osense
   */
  csense,
  /**
   * Human-redable information about the model
   */
  description,
  /**
   * The "disabled" field comes from the reconstruction tool rBioNet.
   * The tool offers the option to disable certain reactions.
   * If no reactions are disabled, then the "disabled" field in empty.
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
   * Objective sense, i.e., to minimize or maximize the objective. This field
   * can either be defined in the mat file itself or when performing an
   * optimization function (e.g., optimizeCbModel).
   */
  osense,
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
