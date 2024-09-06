package de.uni_halle.informatik.biodata.mp.io;

import de.zbit.io.FileTools;
import de.zbit.io.filefilter.SBFileFilter;
import de.zbit.util.ResourceManager;
import de.zbit.util.Utils;
import de.uni_halle.informatik.biodata.mp.logging.BundleNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ResourceBundle;

import static java.text.MessageFormat.format;

public class SBMLFileUtils {

    private static final ResourceBundle MESSAGES = ResourceManager.getBundle(BundleNames.IO_MESSAGES);

    private static final Logger logger = LoggerFactory.getLogger(SBMLFileUtils.class);

    /**
     * Possible FileTypes of input file
     */
    public enum FileType {
        SBML_FILE,
        MAT_FILE,
        JSON_FILE,
        UNKNOWN
    }

    /**
     * Determines the type of the input file based on its extension or content.
     * This method checks if the file is an SBML, MatLab, or JSON file by utilizing the {@link SBFileFilter} class.
     *
     * @param input The file whose type needs to be determined.
     * @return FileType The type of the file, which can be SBML_FILE, MAT_FILE, JSON_FILE, or UNKNOWN if the type cannot be determined.
     */
    public static FileType getFileType(File input) {
        if (SBFileFilter.isSBMLFile(input)) {
            return FileType.SBML_FILE;
        } else if (SBFileFilter.hasFileType(input, SBFileFilter.FileType.MAT_FILES)) {
            return FileType.MAT_FILE;
        } else if (SBFileFilter.hasFileType(input, SBFileFilter.FileType.JSON_FILES)) {
            return FileType.JSON_FILE;
        } else {
            return FileType.UNKNOWN;
        }
    }

    /**
     * Fix output file name to contain xml extension
     *
     * @param file:
     *        File to get name for in input directory
     * @param output:
     *        Path to output directory
     * @return File in output directory with correct file ending for SBML
     */
    public static File getOutputFileName(File file, File output) {
        var fileType = SBMLFileUtils.getFileType(file);
        if (!fileType.equals(FileType.SBML_FILE)) {
            return new File(
                    Utils.ensureSlash(output.getAbsolutePath()) + FileTools.removeFileExtension(file.getName())
                            + "_polished.xml");
        } else {
            return new File(Utils.ensureSlash(output.getAbsolutePath())
                    + file.getName().replaceAll("\\.xml", "_polished\\.xml"));
        }
    }


    /**
     * Check if file is directory by calling {@link File#isDirectory()} on an existing file or check presence of '.' in
     * output.getName(), if this is not the case
     */
    public static boolean isDirectory(File file) {
        // file = d1/d2/d3 is taken as a file by method file.isDirectory()
        if (file.exists()) {
            return file.isDirectory();
        } else {
            return !file.getName().contains(".");
        }
    }



    /**
     * Creates output directory or output parent directory, if necessary
     *
     * @param output:
     *        File denoting output location
     */
    public static void checkCreateOutDir(File output) {
        logger.debug(format(MESSAGES.getString("OUTPUT_FILE_DESC"), isDirectory(output) ? "directory" : "file"));
        // ModelPolisher.isDirectory() checks if output location contains ., if so it is assumed to be a file
        // output is directory
        if (isDirectory(output) && !output.exists()) {
            logger.debug(format(MESSAGES.getString("CREATING_DIRECTORY"), output.getAbsolutePath()));
            if (!output.mkdirs()) {
                // TODO: grace
                throw new RuntimeException(format(MESSAGES.getString("DIRECTORY_CREATION_FAILED"), output.getAbsolutePath()));
            }
        }
        // output is a file
        else {
            // check if directory of outfile exist and create if required
            if (!output.getParentFile().exists()) {
                logger.debug(format(MESSAGES.getString("CREATING_DIRECTORY"), output.getParentFile().getAbsolutePath()));
                if (!output.getParentFile().mkdirs()) {
                    // TODO: grace
                    throw new RuntimeException(format(MESSAGES.getString("DIRECTORY_CREATION_FAILED"), output.getParentFile().getAbsolutePath()));
                }
            }
        }
    }
}
