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
   * the vector of metabolite concentration change rates (usually but not always
   * all zero, i.e., steady-state):  <i>S &sdot; v = b</i>. Must have same
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
  genes,
  /**
   * 
   */
  grRules,
  /**
   *  Lower reaction flux bounds
   */
  lb,
  /**
   * Chemical formulas for metabolites, must have same dimension as {@link #mets}.
   */
  metFormulas,
  /**
   * Descriptive metabolite names, must have same dimension as {@link #mets}
   */
  metNames,
  /**
   * Metabolite BiGG ids (incl. compartment code)
   */
  mets,
  /**
   * 
   */
  rev,
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
   * 
   */
  subSystems,
  /**
   * Upper reaction flux bounds
   */
  ub;
}
