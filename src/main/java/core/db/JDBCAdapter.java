// %4089797104:de.hattrickorganizer.database%
package core.db;

import core.util.ExceptionUtils;
import core.util.HOLogger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Provides the connection functions to the database
 */
public class JDBCAdapter {
	private Connection m_clConnection;
	private Statement m_clStatement;
	private DBInfo m_clDBInfo;

	/**
	 * Creates new JDBCApapter
	 */
	public JDBCAdapter() {
	}

	/**
	 * Closes the connection
	 */
	public final void disconnect() {
		try {
			m_clStatement.execute("SHUTDOWN");
			m_clConnection.close();
			m_clConnection = null;
		} catch (Exception e) {
			HOLogger.instance().error(getClass(), "JDBCAdapter.disconnect : " + e);
			m_clConnection = null;
		}
	}

	/**
	 * Execute a SQL Select statement
	 * 
	 * @param Sql
	 *            Sql query
	 * 
	 * @return ResultSet of the query
	 */
	public final ResultSet executeQuery(String Sql) {
		ResultSet resultat = null;

		try {
			if (m_clConnection.isClosed()) {
				return null;
			}

			// HOLogger.instance().log(getClass(), Sql );
			resultat = m_clStatement.executeQuery(Sql);

			return resultat;
		} catch (Exception e) {
			HOLogger.instance().error(
					getClass(),
					"JDBCAdapter.executeQuery : " + e + "\nStatement: " + Sql + "\n"
							+ ExceptionUtils.getStackTrace(e));
			return null;
		}
	}

	/**
	 * Executes an SQL INSERT, UPDATE or DELETE statement. In addition, SQL
	 * statements that return nothing, such as SQL DDL statements, can be
	 * executed.
	 * 
	 * @param Sql
	 *            INSERT, UPDATE or DELETE statement
	 * 
	 * @return either the row count for SQL Data Manipulation Language (DML)
	 *         statements or 0 for SQL statements that return nothing
	 * 
	 */
	public final int executeUpdate(String Sql) {
		int ret = 0;

		try {
			if (m_clConnection.isClosed()) {
				return 0;
			}
			// HOLogger.instance().log(getClass(), Sql );
			ret = m_clStatement.executeUpdate(Sql);
			return ret;
		} catch (Exception e) {
			HOLogger.instance().error(
					getClass(),
					"JDBCAdapter.executeUpdate : " + e + "\nStatement: " + Sql + "\n"
							+ ExceptionUtils.getStackTrace(e));
			return 0;
		}
	}

	public int executeUpdate_(String sql) throws SQLException {
		return m_clStatement.executeUpdate(sql);
	}

	/**
	 * Connects to the requested database
	 * 
	 * @param URL
	 *            The path to the Server
	 * @param User
	 *            User
	 * @param PWD
	 *            Password
	 * @param Driver
	 *            The driver to user
	 * 
	 */
	public final void connect(String URL, String User, String PWD, String Driver) throws Exception {
		try {
			// Initialise the Database Driver Object
			Class.forName(Driver);
			m_clConnection = DriverManager.getConnection(URL, User, PWD);
			m_clStatement = m_clConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);

		} catch (Exception e) {
			if (m_clConnection != null) {
				try {
					m_clConnection.close();
				} catch (Exception ex) {
					HOLogger.instance().error(getClass(),
							"JDBCAdapter.connect : " + ex.getMessage());
				}
			}
			HOLogger.instance().error(getClass(), "JDBCAdapter.connect : " + e.getMessage());
			throw e;
		}

	}

	/**
	 * 
	 * @return DBInfo
	 * @throws Exception
	 */
	public DBInfo getDBInfo() throws Exception {
		if (m_clDBInfo == null)
			m_clDBInfo = new DBInfo(m_clConnection.getMetaData());
		return m_clDBInfo;
	}

	public Object[] getAllTableNames() {
		try {
			return getDBInfo().getAllTablesNames();
		} catch (Exception e) {
			HOLogger.instance().error(getClass(), "JDBCAdapter.getAllTableNames : " + e);
			return new String[] { e.getMessage() };
		}
	}
}
