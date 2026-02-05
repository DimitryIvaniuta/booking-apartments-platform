package com.github.dimitryivaniuta.booking.service;
import java.sql.SQLException;

public final class SqlStateUtil {
  private SqlStateUtil() {}

  public static boolean hasSqlState(Throwable t, String sqlState) {
    Throwable cur = t;
    while (cur != null) {
      if (cur instanceof SQLException se) {
        if (sqlState.equals(se.getSQLState())) return true;
      }
      cur = cur.getCause();
    }
    return false;
  }

  /** Postgres exclusion constraint / overlap violation */
  public static boolean isPgExclusionViolation(Throwable t) {
    return hasSqlState(t, "23P01");
  }
}
