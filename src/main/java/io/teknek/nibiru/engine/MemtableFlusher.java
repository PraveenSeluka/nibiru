package io.teknek.nibiru.engine;

import java.io.IOException;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;

public class MemtableFlusher implements Runnable {
  private ConcurrentSkipListSet<Memtable> memtables = new ConcurrentSkipListSet<>();
  private DefaultColumnFamily columnFamily;
  private Thread myThread;
  private AtomicLong flushes;
  private volatile boolean goOn = true;
  
  public MemtableFlusher(DefaultColumnFamily columnFamily){
    this.columnFamily = columnFamily;
    flushes = new AtomicLong(0);
  }
  
  public boolean add(Memtable memtable){
    return memtables.add(memtable);
  }

  public ConcurrentSkipListSet<Memtable> getMemtables() {
    return memtables;
  }

  public void start(){
    myThread = new Thread(this);
    myThread.start();
  }
  
  @Override
  public void run() {
    while (goOn){
      for (Memtable memtable : memtables){
        SSTableWriter ssTableWriter = new SSTableWriter();
        try {
          //TODO: a timeuuid would be better here
          String tableId = String.valueOf(System.nanoTime());
          ssTableWriter.flushToDisk(tableId, columnFamily, memtable);
          SsTable table = new SsTable(columnFamily);
          table.open(tableId, columnFamily.getKeyspace().getConfiguration());
          columnFamily.getSstable().add(table);
          memtables.remove(memtable);
          memtable.getCommitLog().delete();
          flushes.incrementAndGet();
        } catch (IOException e) {
          //TODO: catch this and terminate server?
          throw new RuntimeException(e);
        }
      }
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
  
  public long getFlushCount(){
    return flushes.get();
  }

  public boolean isGoOn() {
    return goOn;
  }

  public void setGoOn(boolean goOn) {
    this.goOn = goOn;
  }
  
}
