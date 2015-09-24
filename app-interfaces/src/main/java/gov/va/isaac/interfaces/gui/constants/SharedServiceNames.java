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
package gov.va.isaac.interfaces.gui.constants;

/**
 * {@link SharedServiceNames}
 * 
 * These values are used as the @Named annotation on some service implementations - they are useful while distinguishing
 * when there are multiple implementations of a particular service - for example - a docked view, and a stand-along popup
 * view.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class SharedServiceNames
{
	public static final String DOCKED = "Docked";
	public static final String EMBEDDED = "Embedded";
	public static final String LEGACY_STYLE = "LegacyStyle";
	public static final String DIAGRAM_STYLE = "DiagramStyle";
	public static final String USCRS = "USCRS";
	public static final String LOINC = "LOINC";
	public static final String GIT = "GIT";
	public static final String SVN = "SVN";
	public static final String SEMEME_VIEW = "SememeView";

	public static final String VIEW_COORDINATE_PREFERENCES_PLUGIN = "View Coordinate";
	public static final String EDIT_COORDINATE_PREFERENCES_PLUGIN = "Edit Coordinate";
	public static final String DROOLS_PREFERENCES_PLUGIN = "Drools";
	public static final String EXPORT_PREFERENCES_PLUGIN = "Export";
	public static final String DISPLAY_OPTIONS_PREFERENCES_PLUGIN = "Display Options";
	public static final String SYNC_PREFERENCES_PLUGIN = "Sync";
	public static final String WORKFLOW_PREFERENCES_PLUGIN = "Workflow";

}
