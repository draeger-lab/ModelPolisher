package edu.ucsd.sbrg.bigg;

import de.zbit.util.Utils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TreeSet;

import static edu.ucsd.sbrg.bigg.AnnotateDBContract.Constants.*;
import static edu.ucsd.sbrg.bigg.BiGGAnnotation.logger;

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

    private edu.ucsd.sbrg.bigg.SQLConnector connector;

    /**
     * Initialize a SQL connection
     *
     * @param connector
     * @throws SQLException
     */
    AnnotateDB(SQLConnector connector) throws SQLException {
        this.connector = connector;
        if (!connector.isConnected()) {
            connector.connect();
        }
    }

    /**
     *
     */
    void closeConnection() {
        if (connector.isConnected()) {
            try {
                connector.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public TreeSet<String>  getAnnotations(String type, String biggId){
        TreeSet<String> annotations = new TreeSet<>();

        if(!type.equals(BIGG_METABOLITE) && !type.equals(BIGG_REACTION)){
            return annotations;
        }

        if(type.equals(BIGG_METABOLITE) && biggId.substring(0,2).equals("M_")) biggId = biggId.substring(2);
        if(type.equals(BIGG_METABOLITE) && biggId.substring(0,2).equals("R_")) biggId = biggId.substring(2);
        if(biggId.substring(biggId.length()-2,biggId.length()-1).equals("_")) biggId = biggId.substring(0,biggId.length()-2);

        String query = SELECT + "m." + COLUMN_TARGET_TERM + ", ac." + COLUMN_URLPATTERN + FROM + MAPPING_VIEW + " m, " + ADB_COLLECTION + " ac" + WHERE +
                "m." + COLUMN_SOURCE_NAMESPACE + " = '" + type + "' AND " + "m." + COLUMN_SOURCE_TERM + " = '" + biggId + "' AND ac." + COLUMN_NAMESPACE + " = m." + COLUMN_TARGET_NAMESPACE;


        try {
            ResultSet rst = connector.query(query);
            while (rst.next()) {
                String uri = rst.getString(COLUMN_URLPATTERN);
                String id = rst.getString(COLUMN_TARGET_TERM);
                uri = uri.replace("{$id}", id);
                annotations.add(uri);
            }
        } catch (SQLException exc) {
            logger.warning(Utils.getMessage(exc));
        }

        return annotations;
    }
}