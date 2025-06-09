package net.redvoiss.sms.service.task;

public class BackgroundTask implements ProgressableTask {
    
    private long m_progress = 0;
    
    @Override
    public boolean equals( Object o ) {
        return true;
    }
    
    @Override
    public int hashCode() {
        return 0;
    }
    
    @Override
    public long getProgress() {
        return m_progress;
    }
    
    @Override
    public Object call() throws Exception {
        for ( int i = 0; i < 100; i++) {
            m_progress += 1;
            Thread.sleep( 2 * 10 );
        }                    
        return null;
    }
}