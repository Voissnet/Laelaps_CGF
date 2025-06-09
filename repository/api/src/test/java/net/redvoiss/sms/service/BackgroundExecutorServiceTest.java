
package net.redvoiss.sms.service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import net.redvoiss.sms.service.task.BackgroundTask;
import net.redvoiss.sms.service.task.ProgressableTask;

public class BackgroundExecutorServiceTest {
    @Test
    public void test() throws Exception {
        BackgroundExecutorService aBackgroundExecutorService = new BackgroundExecutorService();
        ProgressableTask aProgressableTask = new BackgroundTask();
        Future aFuture = aBackgroundExecutorService.submit( aProgressableTask );
        while ( ! aFuture.isDone() ) {
            System.out.println( String.format("%d", aProgressableTask.getProgress() ) );
            Thread.sleep( 1 * 1000 );
        }
        Object o = aBackgroundExecutorService.acknowledgeCompletion(aProgressableTask);
        assertNull( o );             
        Set<ProgressableTask> aSet = aBackgroundExecutorService.getProgressableTaskSet();
        assertTrue( aSet.isEmpty() );
    }
    
}