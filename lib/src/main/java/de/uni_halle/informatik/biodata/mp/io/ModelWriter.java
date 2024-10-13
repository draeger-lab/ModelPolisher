package de.uni_halle.informatik.biodata.mp.io;

import de.unirostock.sems.cbarchive.CombineArchiveException;
import de.uni_halle.informatik.biodata.mp.io.parsers.json.JSONConverter;
import org.jdom2.JDOMException;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.TidySBMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.text.ParseException;

import static java.text.MessageFormat.format;

public class ModelWriter implements IWriteModelsToFile, IWriteModels{
    private static final Logger logger = LoggerFactory.getLogger(ModelWriter.class);
    private final IOOptions.OutputType outputType;

    public ModelWriter(IOOptions.OutputType outputType) {
        this.outputType = outputType;
    }

    public InputStream write(SBMLDocument doc) throws ModelWriterException {
        logger.debug("Write document to InputStream.");
        try {
            return switch (outputType) {
                case SBML -> writeSBML(doc);
                case JSON -> writeJSON(doc);
                case COMBINE -> writeCOMBINE(doc);
            };
        } catch (Exception e) {
            throw new ModelWriterException("Error while writing output document.", e, doc, null);
        }
    }

    public File write(SBMLDocument doc, File output) throws ModelWriterException {
        logger.debug(format("Write document to File: {0}", output.toString()));
        try {
            return switch (outputType) {
                case SBML -> writeSBML(doc, output);
                case JSON -> writeJSON(doc, output);
                case COMBINE -> writeCOMBINE(doc, output);
            };
        } catch (Exception e) {
            throw new ModelWriterException("Error while writing output document.", e, doc, output);
        }
    }

    private InputStream writeSBML(SBMLDocument doc) throws IOException {
        PipedInputStream result = new PipedInputStream();
        PipedOutputStream pout = new PipedOutputStream(result);

        // Create a new thread to handle the writing to the piped output stream
        new Thread(() -> {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                TidySBMLWriter.write(
                        doc,
                        baos,
                        "ModelPolisher",
                        getClass().getPackage().getImplementationVersion(),
                        ' ',
                        (short) 2);
                baos.writeTo(pout);
            } catch (IOException | XMLStreamException e) {
                // Handle exceptions appropriately
                logger.error("Error in writing.", e);
            } finally {
                // Ensure the PipedOutputStream is closed properly
                try {
                    pout.close();
                } catch (IOException e) {
                    logger.error("Error in writing.", e);
                }
            }
        }).start();

        return result;
    }

    private InputStream writeJSON(SBMLDocument doc) throws ModelWriterException {
        PipedOutputStream out = new PipedOutputStream();
        try (var writer = new BufferedWriter(new OutputStreamWriter(out))) {
            writer.write(JSONConverter.getJSONDocument(doc));
            return new PipedInputStream(out);
        } catch (IOException | XMLStreamException e) {
            throw new ModelWriterException("Error while converting to JSON.", e, doc, null);
        }
    }

    private InputStream writeCOMBINE(SBMLDocument doc) throws IOException, XMLStreamException, ParseException, URISyntaxException, JDOMException, CombineArchiveException, TransformerException {
        File f = Files.createTempFile("", ".xml").toFile();
        TidySBMLWriter.write(
                doc,
                f,
                "ModelPolisher",
                getClass().getPackage().getImplementationVersion(),
                ' ',
                (short) 2);
        CombineArchive combineArchive = new CombineArchive(doc, f);
        File combineArchiveFile = combineArchive.write();
        return new DeleteOnCloseFileInputStream(combineArchiveFile);
    }


    private File writeCOMBINE(SBMLDocument doc, File output) throws IOException, XMLStreamException, ParseException, URISyntaxException, JDOMException, CombineArchiveException, TransformerException, ModelWriterException {
        File f = Files.createTempFile("", ".xml").toFile();
        TidySBMLWriter.write(
                doc,
                f,
                "ModelPolisher",
                getClass().getPackage().getImplementationVersion(),
                ' ',
                (short) 2);
        CombineArchive combineArchive = new CombineArchive(doc, output);
        File combineArchiveFile = combineArchive.write();
        if (!output.delete()) {
            throw new ModelWriterException("Could not delete file after COMBINE archive creation.",
                    doc, output, combineArchiveFile);
        }
        return combineArchiveFile;
    }

    private static File writeJSON(SBMLDocument doc, File output) throws IOException, XMLStreamException {
        String out = output.getAbsolutePath().replaceAll("\\.xml", ".json");
        try (var writer = new BufferedWriter(new FileWriter(out))) {
            writer.write(JSONConverter.getJSONDocument(doc));
        }
        return new File(out);
    }

    private File writeSBML(SBMLDocument doc, File output) throws XMLStreamException, IOException {
        TidySBMLWriter.write(
                doc,
                output,
                "ModelPolisher",
                getClass().getPackage().getImplementationVersion(),
                ' ',
                (short) 2);
        return output;
    }

//    private File writeZIP(File output) throws IOException {
//        String fileExtension = parameters.compression().getFileExtension();
//        String archive = output.getAbsolutePath() + "." + fileExtension;
//        logger.info(format(MESSAGES.getString("ARCHIVE"), archive));
//        switch (parameters.compression()) {
//            case ZIP:
//                ZIPUtils.ZIPcompress(new String[] {output.getAbsolutePath()}, archive, "SBML Archive", true);
//                break;
//            case GZIP:
//                ZIPUtils.GZip(output.getAbsolutePath(), archive);
//                break;
//            default:
//                break;
//        }
//        // Delete the original output file if outputType is successful
//
//        return new File(archive);
//    }

}
