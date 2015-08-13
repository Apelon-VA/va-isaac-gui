package gov.va.isaac.gui.enhancedsearchview.filters;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import org.ihtsdo.otf.tcc.api.concept.ConceptChronicleBI;
import org.ihtsdo.otf.tcc.api.concept.ConceptFetcherBI;
import org.ihtsdo.otf.tcc.api.concept.ProcessUnfetchedConceptDataBI;
import org.ihtsdo.otf.tcc.api.nid.NativeIdSetBI;
import org.ihtsdo.otf.tcc.api.store.TerminologySnapshotDI;
import org.ihtsdo.otf.tcc.api.store.Ts;
import gov.va.isaac.util.OTFUtility;
import gov.vha.isaac.ochre.collections.NidSet;
import javafx.application.Platform;
import javafx.scene.control.ProgressIndicator;

public class NoSearchTermConcurrentSearcher implements ProcessUnfetchedConceptDataBI
	{
		private volatile boolean continue_ = true;
		private Set<Integer> isKindOfResults_ = new ConcurrentSkipListSet<>();
		private int totalToProcess_;
		private AtomicInteger totalProcessed_ = new AtomicInteger();
		private ProgressIndicator progressIndicator_;
		private TerminologySnapshotDI snapshot;
		private int parentNid;

		public NoSearchTermConcurrentSearcher(NativeIdSetBI initialNids, int parentNid) throws IOException
		{
			this.parentNid = parentNid;
			totalToProcess_ = initialNids.getSetValues().length;
			snapshot = Ts.get().getSnapshot(OTFUtility.getViewCoordinate());
		}
		
		/**
		 * Call this to request the background processing threads cease
		 */
		public void requestStop()
		{
			continue_ = false;
		}

		@Override
		public boolean continueWork()
		{
			return continue_;
		}

		@Override
		public boolean allowCancel()
		{
			return true;
		}

		@Override
		public void processUnfetchedConceptData(int cNid, ConceptFetcherBI fetcher) throws Exception
		{
			if (progressIndicator_ != null)
			{
				int count = totalProcessed_.incrementAndGet();
				if (count % 1000 == 0)
				{
					Platform.runLater(() -> {progressIndicator_.setProgress((double)count / (double)totalToProcess_);});
				}
			}
			ConceptChronicleBI cc = fetcher.fetch();
			if (snapshot.isKindOf(cc.getNid(), parentNid)) {
				isKindOfResults_.add(cc.getNid());
			}
		}
		

		@Override
		public NidSet getNidSet() 
		{
			return null;
		}

		@Override
		public String getTitle()
		{
			return "Find concepts with sememe annotation as specified";
		}

		/**
		 * Return the results.  Will not return null
		 * @return
		 */
		public Set<Integer> getResults()
		{
			return isKindOfResults_;
		}


}
