package edu.ucsd.sbrg.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.ucsd.sbrg.db.bigg.BiGGDBOptions;


public class DBParameters {

    @JsonProperty("db-name")
    private String dbName = BiGGDBOptions.DBNAME.getDefaultValue();
    @JsonProperty("host")
    private String host = BiGGDBOptions.HOST.getDefaultValue();
    @JsonProperty("password")
    private String passwd = BiGGDBOptions.PASSWD.getDefaultValue();
    @JsonProperty("port")
    private Integer port = BiGGDBOptions.PORT.getDefaultValue();
    @JsonProperty("user")
    private String user = BiGGDBOptions.USER.getDefaultValue();

    public DBParameters() {
    }

    public DBParameters (String dbname, String host, String passwd, Integer port, String user) {
        this.dbName = dbname;
        this.host = host;
        this.passwd = passwd;
        this.port = port;
        this.user = user;
    }

    public String dbName() {
        return dbName;
    }

    public String host() {
        return host;
    }

    public String passwd() {
        return passwd;
    }

    public Integer port() {
        return port;
    }

    public String user() {
        return user;
    }
}
