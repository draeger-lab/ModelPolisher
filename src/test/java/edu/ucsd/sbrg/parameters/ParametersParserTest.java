package edu.ucsd.sbrg.parameters;

import edu.ucsd.sbrg.annotation.AnnotationOptions;
import edu.ucsd.sbrg.db.adb.AnnotateDBOptions;
import edu.ucsd.sbrg.db.bigg.BiGGDBOptions;
import edu.ucsd.sbrg.fixing.FixingOptions;
import edu.ucsd.sbrg.io.IOOptions;
import edu.ucsd.sbrg.polishing.PolishingOptions;
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

        assertEquals(GeneralOptions.SBML_VALIDATION.getDefaultValue(), parameters.sbmlValidation());
        assertEquals(GeneralOptions.ADD_GENERIC_TERMS.getDefaultValue(), parameters.sboParameters().addGenericTerms());

        assertEquals(Arrays.asList(FixingOptions.FLUX_OBJECTIVES.getDefaultValue()),
                parameters.fixing().fluxObjectivesPolishingParameters().fluxObjectives());
        assertEquals(Arrays.asList(FixingOptions.FLUX_COEFFICIENTS.getDefaultValue()),
                parameters.fixing().fluxObjectivesPolishingParameters().fluxCoefficients());

        assertEquals(AnnotationOptions.ADD_ADB_ANNOTATIONS.getDefaultValue(),
                parameters.annotation().adbAnnotationParameters().annotateWithAdb());
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

        assertEquals(AnnotationOptions.ANNOTATE_WITH_BIGG.getDefaultValue(),
                parameters.annotation().biggAnnotationParameters().annotateWithBiGG());
        assertEquals(AnnotationOptions.INCLUDE_ANY_URI.getDefaultValue(),
                parameters.annotation().biggAnnotationParameters().includeAnyURI());
        assertEquals(AnnotationOptions.DOCUMENT_TITLE_PATTERN.getDefaultValue(),
                parameters.annotation().biggAnnotationParameters().documentTitlePattern());
        assertEquals(AnnotationOptions.NO_MODEL_NOTES.getDefaultValue(),
                parameters.annotation().biggAnnotationParameters().notesParameters().noModelNotes());
        assertEquals(AnnotationOptions.DOCUMENT_NOTES_FILE.getDefaultValue(),
                parameters.annotation().biggAnnotationParameters().notesParameters().documentNotesFile());
        assertEquals(AnnotationOptions.MODEL_NOTES_FILE.getDefaultValue(),
                parameters.annotation().biggAnnotationParameters().notesParameters().modelNotesFile());

        assertNull(parameters.annotation().biggAnnotationParameters().dbParameters().dbName());
        assertNull(parameters.annotation().biggAnnotationParameters().dbParameters().host());
        assertNull(parameters.annotation().biggAnnotationParameters().dbParameters().port());
        assertNull(parameters.annotation().biggAnnotationParameters().dbParameters().user());
        assertNull(parameters.annotation().biggAnnotationParameters().dbParameters().passwd());

        assertEquals(IOOptions.OUTPUT_TYPE.getDefaultValue(), parameters.outputType());
    }

    @Test
    void parse() throws IOException {
        var parameters = new ParametersParser()
                .parse(ParametersParserTest.class.getResourceAsStream("parameters.json"));
        assertNotNull(parameters);
        assertEquals(IOOptions.OutputType.COMBINE, parameters.outputType());
    }
}