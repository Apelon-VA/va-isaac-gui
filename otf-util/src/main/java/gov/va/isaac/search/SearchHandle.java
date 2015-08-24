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
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.va.isaac.search;

import java.util.Collection;
import java.util.List;

/**
 * Handle object to get search results.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 * @author ocarlsen
 */
public class SearchHandle {

    private final long searchStartTime = System.currentTimeMillis();
    private Integer searchID_;

    private List<CompositeSearchResult> result_;
    private volatile boolean cancelled = false;
    private Exception error = null;
    
    SearchHandle(Integer searchID)
    {
        searchID_ = searchID;
    }

    /**
     * Blocks until the results are available....
     *
     * @return
     * @throws Exception
     */
    public Collection<CompositeSearchResult> getResults() throws Exception {
        if (result_ == null) {
            synchronized (SearchHandle.this) {
                while (result_ == null && error == null && !cancelled) {
                    try {
                        SearchHandle.this.wait();
                    } catch (InterruptedException e) {
                        // noop
                    }
                }
            }
        }
        if (error != null) {
            throw error;
        }
        return result_;
    }
    
    /**
     * This is not the same as the size of the result collection, as results may be merged.
     * @return
     * @throws Exception 
     */
    public int getHitCount() throws Exception
    {
        int result = 0;
        for (CompositeSearchResult csr : getResults())
        {
            result += csr.getMatchingComponents().size();
        }
        return result;
    }

    protected void setResults(List<CompositeSearchResult> results) {
        synchronized (SearchHandle.this) {
            result_ = results;
            SearchHandle.this.notifyAll();
        }
    }

    protected void setError(Exception e) {
        synchronized (SearchHandle.this) {
            this.error = e;
            SearchHandle.this.notifyAll();
        }
    }

    public long getSearchStartTime() {
        return searchStartTime;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel() {
        this.cancelled = true;
    }
    
    /**
     * Returns the identifier provided (if any) by the caller when the search was started
     */
    public Integer getTaskId()
    {
        return searchID_;
    }
}
