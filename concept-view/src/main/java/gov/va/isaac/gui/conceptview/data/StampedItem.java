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
package gov.va.isaac.gui.conceptview.data;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Comparator;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.ProgressBar;
import gov.va.isaac.util.Utility;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.chronicle.StampedVersion;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;

/**
 * {@link StampedItem}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public abstract class StampedItem<T extends StampedVersion>
{
	private T _stampedVersion;
	
	//private UUID authorUUID;
	//private UUID moduleUUID;
	//private UUID pathUUID;
	
	private final SimpleStringProperty authorSSP = new SimpleStringProperty("-");
	private final SimpleStringProperty moduleSSP = new SimpleStringProperty("-");
	private final SimpleStringProperty pathSSP   = new SimpleStringProperty("-");
	private final SimpleStringProperty stateSSP  = new SimpleStringProperty("-");
	private final SimpleStringProperty timeSSP   = new SimpleStringProperty("-");
	
	public T getStampedVersion() { return _stampedVersion; }
	
	protected void readStampDetails(T stampedVersion) 
	{
		_stampedVersion = stampedVersion;

		// Not using "U" any longer
		//stateSSP.set(Get.commitService().isUncommitted(stampedVersion.getStampSequence()) ? "U" : isActive() ? "A" : "I");
		stateSSP.set(_stampedVersion.getState().getAbbreviation());
		timeSSP.set(Get.commitService().isUncommitted(stampedVersion.getStampSequence()) ? 
				"Uncommitted" : 
				new SimpleDateFormat("MM/dd/yy HH:mm:ss").format(getCreationDate())
		);
		
		Utility.execute(() ->
		{
			String authorName = Get.conceptDescriptionText(getAuthorSequence());
			String moduleName = Get.conceptDescriptionText(getModuleSequence());
			String pathName =   Get.conceptDescriptionText(getPathSequence());
			Platform.runLater(() -> {
				authorSSP.set(authorName);
				moduleSSP.set(moduleName);
				pathSSP.set(pathName);
			});
		});
	}
	
	/**
	 * @return the creationDate
	 */
	public long getCreationDate()
	{
		return _stampedVersion.getTime();
	}

	/**
	 * @return the isActive
	 */
	public boolean isActive()
	{
		return _stampedVersion.getState() == State.ACTIVE;
	}
	
	public SimpleStringProperty getAuthorProperty() { return authorSSP; }
	public SimpleStringProperty getModuleProperty() { return moduleSSP; }
	public SimpleStringProperty getPathProperty()   { return pathSSP; }
	public SimpleStringProperty getStateProperty()  { return stateSSP; }
	public SimpleStringProperty getTimeProperty()   { return timeSSP; }
	
	public int getAuthorSequence() { return _stampedVersion.getAuthorSequence(); }
	public int getModuleSequence() { return _stampedVersion.getModuleSequence(); }
	public int getPathSequence()   { return _stampedVersion.getPathSequence(); }
	
	public String toggledStateName() {
		return (_stampedVersion.getState().inverse().toString());
	}
	
	public static final Comparator<StampedItem<?>> statusComparator = new Comparator<StampedItem<?>>() {
		@Override
		public int compare(StampedItem<?> o1, StampedItem<?> o2) {
			// o1 and o2 intentionally reversed in this call, to make Active come before Inactive
			return Boolean.compare(o2.isActive(), o1.isActive());
		}
	};
	
	public static final Comparator<StampedItem<?>> timeComparator = new Comparator<StampedItem<?>>() {
		@Override
		public int compare(StampedItem<?> o1, StampedItem<?> o2) {
			return Long.compare(o1.getCreationDate(), o2.getCreationDate());
		}
	};
	
	public static final Comparator<StampedItem<?>> authorComparator = new Comparator<StampedItem<?>>() {
		@Override
		public int compare(StampedItem<?> o1, StampedItem<?> o2) {
			return Utility.compareStringsIgnoreCase(o1.getAuthorProperty().get(), o2.getAuthorProperty().get());
		}
	};
	
	public static final Comparator<StampedItem<?>> moduleComparator = new Comparator<StampedItem<?>>() {
		@Override
		public int compare(StampedItem<?> o1, StampedItem<?> o2) {
			return Utility.compareStringsIgnoreCase(o1.getModuleProperty().get(), o2.getModuleProperty().get());
		}
	};
	
	public static final Comparator<StampedItem<?>> pathComparator = new Comparator<StampedItem<?>>() {
		@Override
		public int compare(StampedItem<?> o1, StampedItem<?> o2) {
			return Utility.compareStringsIgnoreCase(o1.getPathProperty().get(), o2.getPathProperty().get());
		}
	};
}
