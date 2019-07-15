package edu.ucsd.sbrg.bigg;

import java.util.TreeSet;

import static edu.ucsd.sbrg.bigg.AnnotateDBContract.Constants.*;

/**
 * @author Kaustubh Trivedi
 */
public class AnnotateDB {
    private static final String SELECT = "SELECT ";
    private static final String FROM = " FROM ";
    private static final String WHERE = " WHERE ";

    //source_namespace types:
    public static final String BIGG_METABOLITE = "bigg.metabolite";
    public static final String BIGG_REACTION = "bigg.reaction";

    public TreeSet<String>  getAnnotations(String type, String biggId){
        TreeSet<String> annotations = new TreeSet<>();

        if(!type.equals(BIGG_METABOLITE) && !type.equals(BIGG_REACTION)){
            return annotations;
        }

        String query = SELECT + COLUMN_TARGET_NAMESPACE + ", " + COLUMN_TARGET_TERM + FROM + MAPPING_VIEW + WHERE +
                COLUMN_SOURCE_NAMESPACE + " = " + type + " AND " + COLUMN_SOURCE_TERM + " = " + biggId;


    }
}