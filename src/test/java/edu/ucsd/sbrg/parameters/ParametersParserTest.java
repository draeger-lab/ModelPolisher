package edu.ucsd.sbrg.parameters;

import edu.ucsd.sbrg.ModelPolisherOptions;
import edu.ucsd.sbrg.db.adb.AnnotateDBOptions;
import edu.ucsd.sbrg.db.bigg.BiGGDBOptions;
import edu.ucsd.sbrg.io.IOOptions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ParametersParserTest {

    @Test
    void defaults() throws IOException {
        var parameters = new ParametersParser()
                .parse(new ByteArrayInputStream("{}".getBytes()));

        assertEquals(ModelPolisherOptions.SBML_VALIDATION.getDefaultValue(), parameters.sbmlValidation());

        assertEquals(ModelPolisherOptions.OMIT_GENERIC_TERMS.getDefaultValue(), parameters.sboTerms().omitGenericTerms());

        assertEquals(ModelPolisherOptions.CHECK_MASS_BALANCE.getDefaultValue(),
                parameters.polishing().reactionPolishingParameters().checkMassBalance());
        assertEquals(Arrays.asList(ModelPolisherOptions.FLUX_OBJECTIVES.getDefaultValue()),
                parameters.polishing().fluxObjectivesPolishingParameters().fluxObjectives());
        assertEquals(Arrays.asList(ModelPolisherOptions.FLUX_COEFFICIENTS.getDefaultValue()),
                parameters.polishing().fluxObjectivesPolishingParameters().fluxCoefficients());

        assertEquals(ModelPolisherOptions.ADD_ADB_ANNOTATIONS.getDefaultValue(),
                parameters.annotation().adbAnnotationParameters().addADBAnnotations());
        assertEquals(AnnotateDBOptions.DBNAME.getDefaultValue(),
                parameters.annotation().adbAnnotationParameters().dbParameters().dbName());
        assertEquals(AnnotateDBOptions.HOST.getDefaultValue(),
                parameters.annotation().adbAnnotationParameters().dbParameters().host());
        assertEquals(AnnotateDBOptions.PORT.getDefaultValue(),
                parameters.annotation().adbAnnotationParameters().dbParameters().port());
        assertEquals(AnnotateDBOptions.USER.getDefaultValue(),
                parameters.annotation().adbAnnotationParameters().dbParameters().user());
        assertEquals(AnnotateDBOptions.PASSWD.getDefaultValue(),
                parameters.annotation().adbAnnotationParameters().dbParameters().passwd());

        assertEquals(ModelPolisherOptions.ANNOTATE_WITH_BIGG.getDefaultValue(),
                parameters.annotation().biggAnnotationParameters().annotateWithBiGG());
        assertEquals(ModelPolisherOptions.INCLUDE_ANY_URI.getDefaultValue(),
                parameters.annotation().biggAnnotationParameters().includeAnyURI());
        assertEquals(ModelPolisherOptions.DOCUMENT_TITLE_PATTERN.getDefaultValue(),
                parameters.annotation().biggAnnotationParameters().documentTitlePattern());
        assertEquals(ModelPolisherOptions.NO_MODEL_NOTES.getDefaultValue(),
                parameters.annotation().biggAnnotationParameters().notesParameters().noModelNotes());
        assertEquals(ModelPolisherOptions.DOCUMENT_NOTES_FILE.getDefaultValue(),
                parameters.annotation().biggAnnotationParameters().notesParameters().documentNotesFile());
        assertEquals(ModelPolisherOptions.MODEL_NOTES_FILE.getDefaultValue(),
                parameters.annotation().biggAnnotationParameters().notesParameters().modelNotesFile());
        assertEquals(BiGGDBOptions.DBNAME.getDefaultValue(),
                parameters.annotation().biggAnnotationParameters().dbParameters().dbName());
        assertEquals(BiGGDBOptions.HOST.getDefaultValue(),
                parameters.annotation().biggAnnotationParameters().dbParameters().host());
        assertEquals(BiGGDBOptions.PORT.getDefaultValue(),
                parameters.annotation().biggAnnotationParameters().dbParameters().port());
        assertEquals(BiGGDBOptions.USER.getDefaultValue(),
                parameters.annotation().biggAnnotationParameters().dbParameters().user());
        assertEquals(BiGGDBOptions.PASSWD.getDefaultValue(),
                parameters.annotation().biggAnnotationParameters().dbParameters().passwd());

        assertEquals(IOOptions.OUTPUT.getDefaultValue(), parameters.output().outputFile());
        assertEquals(ModelPolisherOptions.OUTPUT_COMBINE.getDefaultValue(), parameters.output().outputCOMBINE());
        assertEquals(ModelPolisherOptions.WRITE_JSON.getDefaultValue(), parameters.output().writeJSON());
        assertEquals(ModelPolisherOptions.COMPRESSION_TYPE.getDefaultValue(), parameters.output().compression());
    }

    @Test
    void parse() throws IOException {
        var parameters = new ParametersParser()
                .parse(ParametersParserTest.class.getResourceAsStream("parameters.json"));
        assertNotNull(parameters);
        assertEquals(ModelPolisherOptions.Compression.ZIP, parameters.output().compression());
    }
}