package gov.va.isaac.gui.conceptview.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Singleton;

import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.concurrent.Task;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.stage.Window;
import gov.va.isaac.AppContext;
import gov.va.isaac.gui.util.Images;
import gov.va.isaac.interfaces.gui.ApplicationMenus;
import gov.va.isaac.interfaces.gui.MenuItemI;
import gov.va.isaac.interfaces.gui.views.IsaacViewWithMenusI;
import gov.va.isaac.interfaces.gui.views.PopupViewI;
import gov.va.isaac.interfaces.utility.DialogResponse;
import gov.va.isaac.util.Utility;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.commit.CommitRecord;


@Service
@Singleton
public class GlobalCommit implements IsaacViewWithMenusI {

	private static Logger log = LoggerFactory.getLogger(GlobalCommit.class);

	@Override
	public List<MenuItemI> getMenuBarMenus() {

		ArrayList<MenuItemI> menus = new ArrayList<>();

		// Commit
		menus.add(new MenuItemI()
		{
			@Override
			public void handleMenuSelection(Window parent, MenuItem menuItem) 
			{
				doGlobalCommit();
			}

			@Override
			public int getSortOrder()
			{
				return 40;
			}

			@Override
			public String getParentMenuId()
			{
				return ApplicationMenus.ACTIONS.getMenuId();
			}

			@Override
			public String getMenuName()
			{
				return "Global Commit";
			}

			@Override
			public String getMenuId()
			{
				return "doGlobalCommit";
			}

			@Override
			public boolean enableMnemonicParsing()
			{
				return false;
			}

			/**
			 * @see gov.va.isaac.interfaces.gui.MenuItemI#getImage()
			 */
			@Override
			public Image getImage()
			{
				return null; //Images.CLASSIFIER.getImage();
			}
		});
		
		
		// Cancel
		menus.add(new MenuItemI()
		{
			@Override
			public void handleMenuSelection(Window parent, MenuItem menuItem) 
			{
				doGlobalCancel();
			}

			@Override
			public int getSortOrder()
			{
				return 41;
			}

			@Override
			public String getParentMenuId()
			{
				return ApplicationMenus.ACTIONS.getMenuId();
			}

			@Override
			public String getMenuName()
			{
				return "Global Cancel";
			}

			@Override
			public String getMenuId()
			{
				return "doGlobalCancel";
			}

			@Override
			public boolean enableMnemonicParsing()
			{
				return false;
			}

			@Override
			public Image getImage()
			{
				return null; //Images.CLASSIFIER.getImage();
			}
		});
		
		return menus;
	}

	
	private void doGlobalCommit() {
		DialogResponse response = AppContext.getCommonDialogs().showYesNoDialog("Please Confirm", "Are you sure you want COMMIT all outstanding changes?");
		if (response == DialogResponse.YES) {
			Task<Optional<CommitRecord>> cr = Get.commitService().commit("Global commit requested from application menu");
			Utility.execute(cr);
		}
	}
	
	private void doGlobalCancel() {
		DialogResponse response = AppContext.getCommonDialogs().showYesNoDialog("Please Confirm", "Are you sure you want CANCEL all outstanding changes?");
		if (response == DialogResponse.YES) {
			Task<Void> cr = Get.commitService().cancel(); 
			Utility.execute(cr);
		}
	}


}
