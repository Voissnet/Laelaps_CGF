package net.redvoiss.sms.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import net.redvoiss.sms.service.task.ProgressableTask;

public class BackgroundExecutorService {

    private static final Logger m_logger = Logger.getLogger(BackgroundExecutorService.class.getName());

    private final ExecutorService m_pool;

    private static final int DEFAULT_POOL_SIZE = 1;
    
    private Map <ProgressableTask, Future<Object>> m_map = new <ProgressableTask, Future<Object>>HashMap(); 
    
    public BackgroundExecutorService () {
        m_pool = Executors.newFixedThreadPool( DEFAULT_POOL_SIZE );
    }
    
    public BackgroundExecutorService (int poolSize) {
        m_pool = Executors.newFixedThreadPool( poolSize );
    }
    
    public Future<Object> submit(ProgressableTask pt) {
        Future<Object> f = m_pool.submit( pt );
        m_map.put( pt, f);
        return f;
    }    

    public Object acknowledgeCompletion( ProgressableTask pt ) {
        Object ret = null;
        Future<Object> f = m_map.get( pt );
        if( f == null ) {
            m_logger.severe("");
        } else {
            if ( f.isDone() ) {
                f = m_map.remove( pt );
                try {
                    ret = f.get();
                } catch ( InterruptedException e ) {
                    m_logger.throwing("","",e);
                } catch ( ExecutionException e ) {
                    m_logger.throwing("","",e);
                }
            } else {
                m_logger.warning("");
            }    
        }
        return ret;
    }
    
    public Set<ProgressableTask> getProgressableTaskSet() {
        return m_map.keySet();
    }
}