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
package gov.va.isaac.gui.refexViews.refexEdit;

import java.util.Hashtable;
import java.util.List;
import java.util.UUID;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeColumnInfo;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.util.Callback;

/**
 * {@link AttachedDataCellFactory}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public class AttachedDataCellFactory implements Callback <TreeTableColumn<SememeGUI, SememeGUI>, TreeTableCell<SememeGUI,SememeGUI>>
{
	private Hashtable<UUID, List<DynamicSememeColumnInfo>> colInfo_;
	private int listPosition_;
	
	public AttachedDataCellFactory(Hashtable<UUID, List<DynamicSememeColumnInfo>> colInfo, int listPosition)
	{
		colInfo_ = colInfo;
		listPosition_ = listPosition;
	}

	/**
	 * @see javafx.util.Callback#call(java.lang.Object)
	 */
	@Override
	public TreeTableCell<SememeGUI, SememeGUI> call(TreeTableColumn<SememeGUI, SememeGUI> param)
	{
		return new AttachedDataCell(colInfo_, listPosition_);
	}
}
