package io.teknek.nibiru.engine;

import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class CompactionManager {
  private ColumnFamily columnFamily;
  
  public CompactionManager(ColumnFamily columnFamily){
    this.columnFamily = columnFamily;
  }
  
  public void compact(SsTable [] ssTables) throws IOException {
    SsTableStreamReader[] r = new SsTableStreamReader[ssTables.length];
    SsTableStreamWriter w = new SsTableStreamWriter(String.valueOf(System.nanoTime()), 
            this.columnFamily.getKeyspace().getConfiguration());
    Token[] t = new Token[ssTables.length];
    for (int i = 0; i < ssTables.length; i++) {
      r[i] = ssTables[i].getStreamReader();
    }
    for (int i = 0; i < t.length; i++) {
      t[i] = r[i].getNextToken();
    }
    while (!allNull(t)){
      String lowestToken = lowestToken(t);
      SortedMap<String,Val> allColumns = new TreeMap<>();
      for (int i = 0; i < t.length; i++) {
        if (t[i].getToken().equals(lowestToken)) {
          SortedMap<String, Val> columns = r[i].readColumns();
          merge(allColumns, columns);
        }
      }
      
      advance(lowestToken, r, t);
    }
  }
  
  private void advance(String lowestToken, SsTableStreamReader[] r, Token[] t) throws IOException{
    for (int i = 0; i < t.length; i++) {
      if (t[i] != null && t[i].getToken().equals(lowestToken)){
        t[i] = r[i].getNextToken();
      }
    }
  }
  
  private void merge(SortedMap<String,Val> allColumns, SortedMap<String,Val> otherColumns){
    for (Map.Entry<String,Val> x: otherColumns.entrySet()){
      Val existing = allColumns.get(x.getKey());
      if (existing==null){
        allColumns.put(x.getKey(), x.getValue());
      } else if (existing.getTime() < x.getValue().getTime()){
        allColumns.put(x.getKey(), x.getValue());
      }  // we should handle the equal/tombstone case here
    }
    
  }
  
  private String lowestToken(Token [] t){
    String lowestToken = null;
    for (Token j: t){
      if (lowestToken == null){
        lowestToken = j.getToken();
      } else {
        if (j.getToken().compareTo(lowestToken)==-1){
          lowestToken = j.getToken();
        }
      }
    }
    return lowestToken;
  }
  
  private boolean allNull(Token[] t){
    for (Token j : t){
      if (j != null){
        return false;
      }
    }
    return true;
  }
}