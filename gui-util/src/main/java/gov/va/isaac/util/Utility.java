/**
 * Copyright Notice
 * 
 * This is a work of the U.S. Government and is not subject to copyright
 * protection in the United States. Foreign copyrights may apply.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.va.isaac.util;

import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.util.WorkExecutors;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 
 * {@link Utility}
 *
 * @author ocarlsen
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class Utility {

    private static final ScheduledExecutorService scheduledExecutor_ = Executors.newScheduledThreadPool(1, new BackgroundThreadFactory());

    public static void execute(Runnable command) {
        LookupService.getService(WorkExecutors.class).getExecutor().execute(command);
    }
    
    public static <T> Future<T> submit(Callable<T> task) {
        return LookupService.getService(WorkExecutors.class).getExecutor().submit(task);
    }
    
    public static Future<?> submit(Runnable task) {
        return LookupService.getService(WorkExecutors.class).getExecutor().submit(task);
    }
    
    public static <T> Future<?> submit(Runnable task, T result) {
        return LookupService.getService(WorkExecutors.class).getExecutor().submit(task, result);
    }
    
    public static ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return scheduledExecutor_.schedule(command, delay, unit);
    }
    
    public static <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return scheduledExecutor_.schedule(callable, delay, unit);
    }

    public static UUID getUUID(String string) {
        if (string == null)
        {
            return null;
        }
        if (string.length() != 36) {
            return null;
        }
        try {
            return UUID.fromString(string);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    public static boolean isUUID(String string) {
        return (getUUID(string) != null);
    }

    public static boolean isLong(String string) {
        if (string == null)
        {
            return false;
        }
        try {
            Long.parseLong(string);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static Integer getNID(String string) {
    	Integer possibleInt = getInt(string);
    	
    	return possibleInt != null && possibleInt.intValue() < 0 ? possibleInt : null;
    }
    
    public static boolean isInt(String string) {
        return (getInt(string) != null);
    }
    
    public static Integer getInt(String string) {
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    public static void shutdownThreadPools()
    {
        scheduledExecutor_.shutdownNow();
    }
}
