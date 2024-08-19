/*
 * 
 */
package edu.ucsd.sbrg.cli;

import de.zbit.io.filefilter.MultipleFileFilter;
import de.zbit.io.filefilter.SBFileFilter;
import de.zbit.util.ResourceManager;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.Range;
import edu.ucsd.sbrg.io.IOOptions;
import edu.ucsd.sbrg.logging.BundleNames;

import java.io.File;
import java.util.ResourceBundle;

/**
 * @author Andreas Dr&auml;ger
 */
public interface CommandLineIOOptions extends IOOptions {

  /**
   * Bundle for ModelPolisher logger messages
   */
  ResourceBundle MESSAGES = ResourceManager.getBundle(BundleNames.CLI_MESSAGES);

  /**
   * Specifies the SBML or MAT input file. If a directory is given, the
   * conversion
   * will be recursively performed. MAT files will be converted to SBML prior to
   * polishing.
   */
  Option<File> INPUT = new Option<>("INPUT", File.class, "Input SBML file",
          new Range<>(File.class,
                  new MultipleFileFilter(MESSAGES.getString("INPUT_FILE_DESCRIPTION"),
                          SBFileFilter.createSBMLFileFilter(),
                          SBFileFilter.createMATFileFilter(),
                          SBFileFilter.createJSONFileFilter(),
                          SBFileFilter.createDirectoryFilter())));

  /**
   * The path to the file into which the output should be written. If the
   * input is a directory, this must also be a directory in order to perform a
   * recursive conversion.
   */
  Option<File> OUTPUT = new Option<>("OUTPUT", File.class, MESSAGES.getString("OUTPUT_FILE_DESCRIPTION"),
          new Range<>(File.class,
                  new MultipleFileFilter("SBML",
                          SBFileFilter.createSBMLFileFilter(),
                          SBFileFilter.createDirectoryFilter())));


}
