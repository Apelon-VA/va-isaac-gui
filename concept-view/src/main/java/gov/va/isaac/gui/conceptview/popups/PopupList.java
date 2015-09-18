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
	private PopupDataType[] 			_columnTypes;
	private ObservableList<PopupData[]>	_data = FXCollections.observableArrayList();
	private Region						_popOverRegion;
	private TableView<PopupData[]>		_tableView;
	
	protected void setTitle(String title)					{ _title = title; }
	protected void setHeaders(PopupDataType[] columnTypes)	{ _columnTypes = columnTypes; }
	protected void addValues(PopupData[] values) 			{ _data.add(values); }
	protected void setPopOverRegion(Region r)				{ _popOverRegion = r; }
	
	protected void showPopup() {
		
		_tableView = new TableView<PopupData[]>();
		_tableView.getColumns().clear();
		for (int i = 0; i < _columnTypes.length; i++) {
			PopupDataType columnType = _columnTypes[i];
			TableColumn<PopupData[],String> column = new TableColumn<PopupData[],String>(columnType.toString());
			_tableView.getColumns().add(column);
			final int colNum = i;
			column.setCellValueFactory(new Callback<CellDataFeatures<PopupData[], String>, ObservableValue<String>>() {
				@Override
				public ObservableValue<String> call(CellDataFeatures<PopupData[], String> p) {
					return new SimpleStringProperty((p.getValue()[colNum].getValue()));
				}
			});
		}
		
		_tableView.setItems(_data);
		
		PopOver po = DetachablePopOverHelper.newDetachachablePopoverWithCloseButton(_title, _tableView);
		if (_popOverRegion == null) {
			po.detach();
			po.show(AppContext.getMainApplicationWindow().getPrimaryStage());
		} else {
			DetachablePopOverHelper.showDetachachablePopOver(_popOverRegion, po);
		}
	}
	
}
