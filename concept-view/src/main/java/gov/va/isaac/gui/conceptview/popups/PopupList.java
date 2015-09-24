package gov.va.isaac.gui.conceptview.popups;

import gov.va.isaac.AppContext;
import gov.va.isaac.gui.conceptview.ConceptViewColumnType;
import gov.va.isaac.gui.conceptview.ConceptViewController;
import gov.va.isaac.gui.conceptview.data.ConceptDescription;
import gov.va.isaac.gui.conceptview.data.ConceptId;
import gov.va.isaac.gui.conceptview.data.StampedItem;
import gov.va.isaac.gui.dialog.DetachablePopOverHelper;
import gov.va.isaac.gui.util.CustomClipboard;
import gov.va.isaac.gui.util.Images;
import gov.va.isaac.interfaces.gui.views.commonFunctionality.SememeViewI;
import gov.va.isaac.util.CommonMenuBuilderI;
import gov.va.isaac.util.CommonMenus;
import gov.va.isaac.util.CommonMenusDataProvider;
import gov.va.isaac.util.CommonMenusNIdProvider;
import gov.va.isaac.util.CommonMenus.CommonMenuBuilder;
import gov.va.isaac.util.CommonMenus.CommonMenuItem;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.impl.sememe.DynamicSememeUsageDescription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.controlsfx.control.PopOver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.javafx.tk.Toolkit;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Callback;

public class PopupList {

	private String 						_title;
	private ConceptViewColumnType[] 	_columnTypes;
	private ObservableList<PopupData>	_data = FXCollections.observableArrayList();
	private Region						_popOverRegion;
	private TableView<PopupData>		_tableView;
	
	public void setTitle(String title)			{ _title = title; }
	public void setPopOverRegion(Region r)		{ _popOverRegion = r; }
	
	public void setColumnTypes(ConceptViewColumnType[] columnTypes)	{ 
		_columnTypes = columnTypes; 
	}

	private static final Logger LOG = LoggerFactory.getLogger(PopupList.class);

	public void addData(Object data) {
		PopupData pd = new PopupData(data);
		if (pd.isValid()) {
			_data.add(pd);
		}
	}
	
	@SuppressWarnings("restriction")
	protected void showPopup() {
		_tableView = new TableView<PopupData>();
		_tableView.getColumns().clear();

		// Hack to dynamically set column min widths
		Font font = new Font("System Bold", 13.0);
		
		double tableWidth = 0;
		for (int i = 0; i < _columnTypes.length; i++) {
			ConceptViewColumnType columnType = _columnTypes[i];
			TableColumn<PopupData,PopupData> column = new TableColumn<PopupData,PopupData>(columnType.toString());
			column.setUserData(columnType);
			column.setPrefWidth(TableView.USE_COMPUTED_SIZE);
			Double columnWidth = Double.max(columnType.getColumnWidth(), Toolkit.getToolkit().getFontLoader().computeStringWidth(column.getText(), font) + 30);
			column.setMinWidth(columnWidth);
			tableWidth += columnWidth;
			
			_tableView.getColumns().add(column);
			
			column.setCellValueFactory(new Callback<CellDataFeatures<PopupData, PopupData>, ObservableValue<PopupData>>() {
				@Override
				public ObservableValue<PopupData> call(CellDataFeatures<PopupData, PopupData> p) {
					return new SimpleObjectProperty<PopupData>(p.getValue());
				}
			});
			
			column.setCellFactory(new Callback<TableColumn<PopupData,PopupData>,TableCell<PopupData,PopupData>>() {
				@Override
				public TableCell<PopupData, PopupData> call(TableColumn<PopupData, PopupData> param) {
					return new TableCell<PopupData, PopupData>() {
						@Override
						public void updateItem(final PopupData popupData, boolean empty) {
							super.updateItem(popupData, empty);
							updateCell(this, popupData);
						}
					};
				}
			});
		}
		
		_tableView.setItems(_data);
		_tableView.setMinWidth(tableWidth);
		
		PopOver po = DetachablePopOverHelper.newDetachachablePopoverWithCloseButton(_title, _tableView);
		po.setMinWidth(tableWidth);
		po.setMaxWidth(tableWidth);
		
		if (_popOverRegion == null) {
			po.detach();
			po.show(AppContext.getMainApplicationWindow().getPrimaryStage());
		} else {
			DetachablePopOverHelper.showDetachachablePopOver(_popOverRegion, po);
		}
	}
	
	private void updateCell(TableCell<?, ?> cell, PopupData popupData) {
		if (!cell.isEmpty() && popupData.isValid()) {
			ContextMenu cm = new ContextMenu();
			cell.setContextMenu(cm);
			StringProperty textProperty = null;
			int conceptSequence = 0;
			int conceptNid = 0;
			ConceptViewColumnType columnType = (ConceptViewColumnType) cell.getTableColumn().getUserData();

			cell.setText(null);
			cell.setGraphic(null);
			cell.setTooltip(null);
		
			if (popupData.isConceptId()) {
				ConceptId conceptId = popupData.getConceptId();
				switch (columnType) {
				case ID_TYPE:
					textProperty = conceptId.getTypeProperty();
					break;
				case ID_VALUE:
					textProperty = conceptId.getValueProperty();
					break;
				case TIMESTAMP:
					textProperty = conceptId.getTimestampProperty();
					break;
				default:
					break;
				}
			} 
			
			if (popupData.isConceptDescription()) {
				ConceptDescription conceptDescription = popupData.getConceptDescription();
				switch (columnType) {
				case TERM:
					textProperty = conceptDescription.getValueProperty();
					//conceptSequence = conceptDescription.getSequence();
					//conceptNid = Get.identifierService().getConceptNid(conceptSequence);
					break;
					
				case TYPE:
					textProperty = conceptDescription.getTypeProperty();
					conceptSequence = conceptDescription.getTypeSequence();
					conceptNid = Get.identifierService().getConceptNid(conceptSequence);
					break;
					
				case LANGUAGE:
					textProperty = conceptDescription.getLanguageProperty();
					conceptSequence = conceptDescription.getLanguageSequence();
					conceptNid = Get.identifierService().getConceptNid(conceptSequence);
					break;
					
				case ACCEPTABILITY:
					textProperty = conceptDescription.getAcceptabilityProperty();
					//conceptSequence = conceptDescription.getAcceptabilitySequence();
					break;
				case SIGNIFICANCE:
					textProperty = conceptDescription.getSignificanceProperty();
					conceptSequence = conceptDescription.getSignificanceSequence();
					conceptNid = Get.identifierService().getConceptNid(conceptSequence);
					break;
				}
			}
			if (popupData.isStampedItem()) {
				StampedItem<?> stampedItem = popupData.getStampedItem();
				switch (columnType) {
				case STAMP_STATE:
					textProperty = stampedItem.getStateProperty();
					break;
				case STAMP_TIME:
					textProperty = stampedItem.getTimeProperty();
					break;
				case STAMP_AUTHOR:
					textProperty = stampedItem.getAuthorProperty();
					conceptSequence = stampedItem.getAuthorSequence();
					conceptNid = Get.identifierService().getConceptNid(conceptSequence);
					break;
				case STAMP_MODULE:
					textProperty = stampedItem.getModuleProperty();
					conceptSequence = stampedItem.getModuleSequence();
					conceptNid = Get.identifierService().getConceptNid(conceptSequence);
					break;
				case STAMP_PATH:
					textProperty = stampedItem.getPathProperty();
					conceptSequence = stampedItem.getPathSequence();
					conceptNid = Get.identifierService().getConceptNid(conceptSequence);
					break;
				default:
					// Nothing
				}
				
			}
			
			if (textProperty != null) {
				// TODO Make text overrun work on text property
				Text text = new Text();
				text.textProperty().bind(textProperty);
				//text.wrappingWidthProperty().bind(cell.getTableColumn().widthProperty());
				cell.setGraphic(text);
				
				Tooltip tooltip = new Tooltip();
				tooltip.textProperty().bind(textProperty);
				cell.setTooltip(tooltip);
	
				MenuItem mi = new MenuItem("Copy Value");
				mi.setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent arg0) {
						CustomClipboard.set(((Text)cell.getGraphic()).getText());
					}
				});
				mi.setGraphic(Images.COPY.createImageView());
				cm.getItems().add(mi);

				MenuItem miWrap = new MenuItem("Wrap Text");
				miWrap.setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent arg0) {
						Text text = (Text)cell.getGraphic();
						if (text.wrappingWidthProperty().isBound()) {
							miWrap.setText("Wrap Text");
							text.wrappingWidthProperty().unbind();
							text.setWrappingWidth(0.0);
							
						} else {
							miWrap.setText("Truncate Text");
							text.wrappingWidthProperty().bind(cell.getTableColumn().widthProperty());
						}
					}
				});
				cm.getItems().add(miWrap);
			}
			
			final String textValue = (textProperty != null)? textProperty.get() : null;
			if (conceptNid != 0) {
				final int finalConceptNid = conceptNid;
				final int finalConceptSequence = conceptSequence;
				CommonMenuBuilderI builder = CommonMenuBuilder.newInstance();
				builder.setMenuItemsToExclude(
						CommonMenuItem.COPY,
						CommonMenuItem.COPY_CONTENT,
						CommonMenuItem.COPY_NID,
						CommonMenuItem.COPY_SCTID,
						CommonMenuItem.COPY_UUID,
						CommonMenuItem.LOINC_REQUEST_VIEW,
						CommonMenuItem.USCRS_REQUEST_VIEW);
				CommonMenus.addCommonMenus(cm,
						builder,
						new CommonMenusDataProvider() {
					@Override
					public String[] getStrings() {
						return textValue == null ? new String[0] : new String[] { textValue };
					}
				}, new CommonMenusNIdProvider() {
					@Override
					public Collection<Integer> getNIds() {
						try {
							boolean isDynamicSememe = DynamicSememeUsageDescription.isDynamicSememe(finalConceptNid);
							LOG.debug("Creating common menus for sequence={}, nid={}, desc={}, which {} a dynamic sememe", finalConceptSequence, finalConceptNid, Get.conceptDescriptionText(finalConceptNid), isDynamicSememe ? "is" : "is not");
						} catch (Exception e) {
							//
						}
						return Arrays.asList(new Integer[] { finalConceptNid });
					}
				});
			}
			
		} else {
			//cell.setText(null);
			cell.setGraphic(null);
			cell.setTooltip(null);
		}
	}

}

