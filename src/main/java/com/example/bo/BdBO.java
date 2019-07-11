package com.example.bo;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.example.client.UdpIntegrationClient;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Component
public class BdBO {

	private final static Logger LOGGER = LoggerFactory.getLogger(UdpIntegrationClient.class);

	@Value("${spring.datasource.url}")
	private String dbUrl;

	@Value("${udp.port}")
	private String udpPort;

	@Autowired
	private DataSource dataSource;
	
	@Autowired
	public BdBO() {
		
	}

	@Bean
	public DataSource dataSource() throws SQLException {
		if (dbUrl == null || dbUrl.isEmpty()) {
			return new HikariDataSource();
		} else {
			HikariConfig config = new HikariConfig();
			config.setJdbcUrl(dbUrl);
			return new HikariDataSource(config);
		}
	}
	
	public String testaBd(Map<String, Object> model) {
		try (Connection connection = dataSource.getConnection()) {
			Statement stmt = connection.createStatement();
			stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ticks (tick timestamp)");
			stmt.executeUpdate("INSERT INTO ticks VALUES (now())");
			ResultSet rs = stmt.executeQuery("SELECT tick FROM ticks");

			ArrayList<String> output = new ArrayList<String>();
			while (rs.next()) {
				output.add("Read from DB: " + rs.getTimestamp("tick"));
			}

			model.put("records", output);

			connection.close();

			return "db";
		} catch (Exception e) {
			model.put("message", e.getMessage());
			return "error";
		}
	}

	@PostConstruct 
	public String inicializa(){
		try (Connection connection = dataSource.getConnection()) {
			PreparedStatement stmt = connection.prepareStatement(getSql("init"));
			
			stmt.setString(1, InetAddress.getLocalHost().getHostAddress());
			stmt.setInt(2, Integer.valueOf(udpPort));
			
			stmt.executeUpdate();
			
			ResultSet rs = stmt.executeQuery("SELECT * FROM dyno");

			String retorno =  "Dynos online: \n"+resultSetPrettyPrint(rs);
			
			connection.close();
			
			return retorno;
		} catch (Exception e) {
			LOGGER.error("Erro ao abrir conexão.", e);
			return "error";
		}
	}

	/**
	 * Recupera o arquivo SQL dos resources pelo nome e retorna a string do sql
	 * contido nele
	 * 
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	private String getSql(String filename) throws IOException {
		ClassLoader classLoader = this.getClass().getClassLoader();

		return new String(classLoader.getResourceAsStream("sql/".concat(filename).concat(".sql")).readAllBytes());
	}

	private String resultSetPrettyPrint(ResultSet rs) throws SQLException {
		ResultSetMetaData rsmd = rs.getMetaData();
		
		String result = new String();

		int columnsNumber = rsmd.getColumnCount();
		while (rs.next()) {
			for (int i = 1; i <= columnsNumber; i++) {
				if (i > 1)
					result.concat("\n,  ");
				String columnValue = rs.getString(i);
				result.concat("\n"+columnValue + " " + rsmd.getColumnName(i));
			}
			result.concat("\n");
		}
		
		return result;
	}
}