package edu.ucsd.sbrg.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.ucsd.sbrg.db.bigg.BiGGDBOptions;

import java.util.Objects;


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

    @Override
    public String toString() {
        return "DBParameters{" +
                "dbName='" + dbName + '\'' +
                ", host='" + host + '\'' +
                ", passwd='" + passwd + '\'' +
                ", port=" + port +
                ", user='" + user + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DBParameters that = (DBParameters) o;
        return Objects.equals(dbName, that.dbName) && Objects.equals(host, that.host) && Objects.equals(passwd, that.passwd) && Objects.equals(port, that.port) && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dbName, host, passwd, port, user);
    }
}
