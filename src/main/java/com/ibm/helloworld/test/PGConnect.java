package com.ibm.helloworld.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.*;
import java.lang.System;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.json.*;



public class PGConnect {

    private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    public Connection connect() {

        String vcap = System.getenv("CE_SERVICES");
        //System.out.println(vcap);
        JSONObject vcap_json = new JSONObject(vcap);
        JSONObject postgres_db = vcap_json.getJSONArray("databases-for-postgresql").getJSONObject(0);
        JSONObject postgres = postgres_db.getJSONObject("credentials").getJSONObject("connection").getJSONObject("postgres");
        //System.out.println(postgres.toString());

        Properties props = new Properties();
        props.setProperty("user", postgres.getJSONObject("authentication").getString("username"));
        props.setProperty("password",  postgres.getJSONObject("authentication").getString("password"));
        //props.setProperty("ssl", "true");
        //props.setProperty("sslmode", "verify-full");
        //props.setProperty("sslrootcert",postgres.getJSONObject("certificate").getString("certificate_base64"));
        final String url = "jdbc:postgresql://" +
                postgres.getJSONArray("hosts").getJSONObject(0).getString("hostname") + ":"
                + postgres.getJSONArray("hosts").getJSONObject(0).getInt("port") + "/ibmclouddb";

        Connection conn = null;
        while (conn == null) {
            try {
                conn = DriverManager.getConnection(url, props);
                System.out.println("Connected to PG");
            } catch (SQLException e) {
                System.out.printf("%s\n", e);
                LOGGER.info("Not connected, retying ...");
            }
        }

        return conn;

    }
}