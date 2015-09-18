package gov.va.isaac.gui.conceptview.popups;

import gov.va.isaac.AppContext;
import gov.va.isaac.gui.dialog.DetachablePopOverHelper;

import java.util.ArrayList;
import java.util.List;

import org.controlsfx.control.PopOver;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.layout.Region;
import javafx.util.Callback;

public class PopupList {

	private String 						_title;
	private String[] 					_headers;
	private ObservableList<String[]>	_values = FXCollections.observableArrayList();
	private Region						_popOverRegion;
	private TableView<String[]>			_tableView;
	
	protected void setTitle(String title)		{ _title = title; }
	protected void setHeaders(String[] headers)	{ _headers = headers; }
	protected void addValues(String[] values) 	{ _values.add(values); }
	protected void setPopOverRegion(Region r)	{ _popOverRegion = r; }
	
	protected void showPopup() {
		
		_tableView = new TableView<String[]>();
		_tableView.getColumns().clear();
		for (int i = 0; i < _headers.length; i++) {
			String header = _headers[i];
			TableColumn<String[],String> column = new TableColumn<String[],String>(header);
			_tableView.getColumns().add(column);
			final int colNum = i;
			column.setCellValueFactory(new Callback<CellDataFeatures<String[], String>, ObservableValue<String>>() {
				@Override
				public ObservableValue<String> call(CellDataFeatures<String[], String> p) {
					return new SimpleStringProperty((p.getValue()[colNum]));
				}
			});
		}
		
		_tableView.setItems(_values);
		
		PopOver po = DetachablePopOverHelper.newDetachachablePopoverWithCloseButton(_title, _tableView);
		if (_popOverRegion == null) {
			po.detach();
			po.show(AppContext.getMainApplicationWindow().getPrimaryStage());
		} else {
			DetachablePopOverHelper.showDetachachablePopOver(_popOverRegion, po);
		}
	}
	
}
