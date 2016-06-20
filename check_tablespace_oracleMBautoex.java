// ----------------------------------------------------------------------------
// check_tablespace_oracle.java 20100820 frank4dd version 1.0
//								20110725 rfults   version 1.1 -coverted to MB
//								20130805 rfults	  version 1.2 -added autoextend and status
// ----------------------------------------------------------------------------
// e-mail: support[at]frank4dd.com
// web: http://www.frank4dd.com/howto/nagios/db-monitoring.htm
//
// This nagios plugin queries the Oracle dba_free_space and dba_data_files
// system tables. Supported are Oracle versions 10g and up.
//
// Pre-requisites: Oracle JDBC driver installed and a valid DB user.
// ----------------------------------------------------------------------------
// Example Output:
// > java check_tablespace_oracle 127.0.0.1 1521 XE system test -d
// DB connect: jdbc:oracle:thin:system/test@127.0.0.1:1521:XE
// DB query: select  df.TABLESPACE_NAME, df.FILE_ID, (((df.BYTES+fs.BYTES)/1024)/1024) MBytes_max,
// ((df.BYTES/1024)/1024) mb_used, round(((df.BYTES - fs.BYTES) / df.BYTES) * 100) usage_pct
// from ( select  TABLESPACE_NAME, sum(BYTES) BYTES, count(distinct FILE_ID) FILE_ID from
// dba_data_files group by TABLESPACE_NAME ) df, ( select TABLESPACE_NAME, sum(BYTES) BYTES
// from dba_free_space group by TABLESPACE_NAME) fs where df.TABLESPACE_NAME=fs.TABLESPACE_NAME
// order by df.TABLESPACE_NAME asc
//Name:               SYSAUX Files:  1 Space total:     374912 MB Space used:     317440 MB Space % used:  82 %
//Name:               SYSTEM Files:  1 Space total:     350208 MB Space used:     348160 MB Space % used:  99 %
//Name:                 UNDO Files:  1 Space total:     384384 MB Space used:     215040 MB Space % used:  21 %
//Name:                USERS Files:  1 Space total:     203136 MB Space used:     102400 MB Space % used:   2 %
// ----------------------------------------------------------------------------
import java.sql.*;

class check_tablespace_oracle_MB {

  static int mb_warn = 0;  // the commandline argument for warning threshold of MB used as PCT
  static int mb_crit = 0;  // the commandline argument for critical threshold of MB used as PCT
  static int dfiles_total= 0;  // the returned number of tablespace files
  static int mb_used = 0;  // the returned tablespace value of used MB
  static int mb_total= 0;  // the returned tablespace value of total MB available
  static int percent_used= 0;  // the returned tablespace value, current space used in percent
  static int return_code = 0;  // 'OK'=>0,'WARNING'=>1,'CRITICAL'=>2,'UNKNOWN'=>3,'DEPENDENT'=>4
  static int debug       = 0;  // 'normal'=>0,'verbose'=>1 when -d parameter is given
  static String output   = ""; // the plugin output string
  static String perfdata = ""; // the plugin perfdata output, returning the MB values
  static String tbspname = ""; // the tablespace to check
  static String autoex = ""; // the tablespace to check
  static String onstatus = ""; // the tablespace to check
  static String dbUrl    = ""; // the access URL for the database to query
  static String query    = ""; // the SQL query to execute
  static String newline = System.getProperty("line.separator");

  public static void main (String args[]) {
    if (args.length < 6) {
      System.err.println("Error: Missing Arguments.");
      System.err.println("Syntax: java check_tablespace_oracle <db-ip> <db-port> <db-instance> <db-user> <db-pwd> <tablespace-name> <PCT-warn> <PCT-crit>");
      System.err.println("        java check_tablespace_oracle <db-ip> <db-port> <db-instance> <db-user> <db-pwd> -r <tablespace-name>");
      System.err.println("        java check_tablespace_oracle <db-ip> <db-port> <db-instance> <db-user> <db-pwd> -d");
      System.exit(-1);
    }
    // Check if we got a particular tablespace to check for
    if (args.length == 6 && args[5].equals("-d")) { debug = 1;}

    dbUrl = "jdbc:oracle:thin:" + args[3] + "/" + args[4] + "@" + args[0] +":" + args[1] +":" + args[2];
    if (debug == 1) { System.out.println("DB connect: " + dbUrl); }

    // Check if we just return the data without any values to compare to
    if (args.length == 7 && args[5].equals("-r")) {
      tbspname = args[6];
    }

    // Check if we got warn and crit values to check against
    if (args.length == 8) { 
      tbspname = args[5];
      mb_warn = Integer.parseInt(args[6]);
      mb_crit = Integer.parseInt(args[7]);
    }


    try {
      // use the Oracle JDBC driver
      Class.forName("oracle.jdbc.driver.OracleDriver");
    } catch (ClassNotFoundException e) {
      System.err.println("Error: JDBC Driver Problem.");
      System.err.println (e);
      System.exit (3);
    }
   try {
      // open connection to database "jdbc:oracle:thin:@destinationhost:port:dbname", "dbuser", "dbpassword"
      Connection connection = DriverManager.getConnection(dbUrl);

      // build query

      // table dba_data_files: TABLESPACE_NAME,  FILE_NAME, BYTES, MAXBYTES, AUTOEXTENSIBLE
      // dba_free_space: TABLESPACE_NAME,  FILE_ID, BYTES
      // Show free tablespace: Select tablespace_name, Sum((bytes/(1024))/1024) "Total Free (MB) " From dba_free_space Group By tablespace_name; 
      // Show used tablespace: Select tablespace_name, Sum((bytes/(1024))/1024) "Total Used (MB) " From dba_data_files Group By tablespace_name; 
      if (tbspname == "") {
        query = "select  df.TABLESPACE_NAME, df.FILE_ID, round(((df.BYTES)/1024)/1024) mb_max, round(((df.BYTES-fs.BYTES)/1024)/1024) mb_used, round(((df.BYTES - fs.BYTES) / df.BYTES) * 100) usage_pct, df.AUTOEXTENSIBLE, df.ONLINE_STATUS from ( select  TABLESPACE_NAME, sum(BYTES) BYTES, count(distinct FILE_ID) FILE_ID, AUTOEXTENSIBLE, ONLINE_STATUS from dba_data_files group by TABLESPACE_NAME,AUTOEXTENSIBLE,ONLINE_STATUS ) df, ( select TABLESPACE_NAME, sum(BYTES) BYTES from dba_free_space group by TABLESPACE_NAME) fs where df.TABLESPACE_NAME=fs.TABLESPACE_NAME order by df.TABLESPACE_NAME asc";
      } else {
        query = "select  df.TABLESPACE_NAME, df.FILE_ID, round(((df.BYTES)/1024)/1024) mb_max, round(((df.BYTES-fs.BYTES)/1024)/1024) mb_used, round(((df.BYTES - fs.BYTES) / df.BYTES) * 100) usage_pct, df.AUTOEXTENSIBLE, df.ONLINE_STATUS from ( select  TABLESPACE_NAME, sum(BYTES) BYTES, count(distinct FILE_ID) FILE_ID, AUTOEXTENSIBLE, ONLINE_STATUS from dba_data_files where TABLESPACE_NAME = '" + tbspname + "' group by TABLESPACE_NAME,AUTOEXTENSIBLE,ONLINE_STATUS) df, ( select TABLESPACE_NAME, sum(BYTES) BYTES from dba_free_space group by TABLESPACE_NAME) fs where df.TABLESPACE_NAME=fs.TABLESPACE_NAME order by df.TABLESPACE_NAME asc";
      }
      if (debug == 1) { System.out.println ("DB query: " + query); }

      // execute query
      Statement statement = connection.createStatement ();
      ResultSet rs = statement.executeQuery (query);

      while ( rs.next () ) {
        // get content from column "1 -4"
        if (debug == 1) {
          System.out.format ("Name: %20s ", rs.getString (1));      // TBSP_NAME, VARCHAR(128)
          System.out.format ("Files: %2d ", rs.getInt(2));// TBSP_TOTAL_SIZE_MB, BIGINT
          System.out.format ("Space total: %10d MB ", rs.getInt(3));// TBSP_TOTAL_SIZE_MB, BIGINT
          System.out.format ("Space used: %10d MB ", rs.getInt(4)); // TBSP_USED_SIZE_MB, BIGINT
          System.out.format ("Space %% used: %3d ", rs.getInt(5)); // TBSP_UTILIZATION_PERCENT, BIGINT
		  System.out.format ("Autoextensible: %8s ", rs.getString (6));      // AUTOEXTENSIBLE, VARCHAR(128)
		  System.out.format ("Online Status: %8s  %%\n", rs.getString (7));      // ONLINE_STATUS, VARCHAR(128)
        }
        tbspname=rs.getString (1);
        dfiles_total=rs.getInt(2);
        mb_total=rs.getInt(3);
        mb_used=rs.getInt(4);
        percent_used=rs.getInt(5);
		autoex=rs.getString (6);
		onstatus=rs.getString (7);
      }

      rs.close () ;
      statement.close () ;
      connection.close () ;

    } catch (java.sql.SQLException e) {
      System.err.println (e) ;
      System.exit (3) ; // Unknown
    }

    perfdata = tbspname + ": " +dfiles_total + " datafiles, " + mb_used + " MB used/ " + mb_total + " MB total " + newline + "Autoextend " + autoex + newline + " Status " + onstatus;
    output = tbspname + " " + percent_used + "% used" + "|" + perfdata;

    if ( (mb_warn != 0) && (mb_crit != 0) ) {
      if ( percent_used < mb_warn ) {
        System.out.println("Tablespace OK: " + output);
        System.exit (0); // OK
      }
      if ( (percent_used >= mb_warn) && (percent_used < mb_crit) ) {
        System.out.println("Tablespace WARN: "  + output);
        System.exit (1); // WARN
      }
      if ( percent_used >= mb_crit ) {
        System.out.println("Tablespace CRIT: "  + output);
        System.exit (2); // CRIT
      }
    }
    if (args.length == 7 && args[5].equals("-r")) {
      System.out.println("Tablespace OK: " + output);
      System.exit (0); // OK
    }
  }
}
