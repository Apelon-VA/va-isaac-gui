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
package gov.va.isaac.gui.interfaces;

import javafx.stage.Window;

/**
 * MenuItemI
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public abstract class MenuItemI implements Comparable<MenuItemI>
{
	/**
	 * The FxMenuId of the MenuItem this menu should be added to.  Leave null for this to be treated as a new top-level
	 * menu in the applications menu bar.
	 */
	public abstract String getParentMenuId();
	
	/**
	 * The FxMenuId of this menu item, should uniquely identify the menu across the application
	 */
	public abstract String getMenuId();
	
	
	/**
	 * The text to use for the Menu itself
	 */
	public abstract String getMenuName();
	
	/**
	 * Should the MenuName be parsed for mnemonics
	 */
	public abstract boolean enableMnemonicParsing();
	
	/**
	 * Desired sort order for this menu item, relative to other menu items in the same parent menu.
	 */
	public abstract int getSortOrder();
	
	/**
	 * Called when the user selects the menu
	 */
	public abstract void handleMenuSelection(Window parent);

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(MenuItemI o)
	{
		return getSortOrder() - o.getSortOrder();
	}
}
