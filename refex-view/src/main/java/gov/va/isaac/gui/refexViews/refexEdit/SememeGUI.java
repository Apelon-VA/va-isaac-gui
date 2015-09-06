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

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.ToIntFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gov.va.isaac.util.AlphanumComparator;
import gov.va.isaac.util.NumberUtilities;
import gov.va.isaac.util.OchreUtility;
import gov.vha.isaac.metadata.coordinates.StampCoordinates;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronology;
import gov.vha.isaac.ochre.api.chronicle.StampedVersion;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.ComponentNidSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.LogicGraphSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.LongSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.component.sememe.version.StringSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeArrayBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeByteArrayBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeDoubleBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeFloatBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeIntegerBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeLongBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeNidBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeUUIDBI;
import gov.vha.isaac.ochre.api.relationship.RelationshipVersionAdaptor;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeLong;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeNid;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeString;

/**
 * {@link DynamicSememeGUI}
 * 
 * A Wrapper for a DynamicSememeVersionBI - because the versioned refex provides no information
 * about whether or not it is an old version, or if it is the latest version.  Add a flag for 
 * is latest.
 * 
 * Also used in cases where we are constructing a new Refex - up front, we know a NID (which is either the assemblyNid or 
 * the referenced component nid.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public class SememeGUI
{
	private static Logger logger_ = LoggerFactory.getLogger(SememeGUI.class);
	
	//These variables are used when we are working with a refex that already exists
	private SememeVersion<?> refex_;
	private boolean isCurrent_;
	private HashMap<String, AbstractMap.SimpleImmutableEntry<String, String>> stringCache_ = new HashMap<>();
	
	//These variables are used when we are creating a new refex which doesn't yet exist.
	private Integer buildFromReferenceNid_;
	private boolean referenceIsAssemblyNid_;
	
	protected SememeGUI(SememeVersion<?> refex, boolean isCurrent)
	{
		refex_ = refex;
		isCurrent_ = isCurrent;
	}
	
	protected SememeGUI(int buildFromReferenceNid, boolean referenceIsAssemblyNid)
	{
		refex_ = null;
		isCurrent_ = false;
		buildFromReferenceNid_ = buildFromReferenceNid;
		referenceIsAssemblyNid_ = referenceIsAssemblyNid;
	}

	/**
	 * Contains the refex reference when this object was constructed based on an existing refex
	 */
	public SememeVersion<?> getSememe()
	{
		return refex_;
	}

	/**
	 * If this was constructed based off of an existing refex, is this the most current refex?  Or a historical one?
	 * This is meaningless if {@link #getSememe()} return null.
	 */
	public boolean isCurrent()
	{
		return isCurrent_;
	}

	/**
	 * If this was constructed with just a nid (building a new refex from scratch) this returns it - otherwise, returns null.
	 */
	public Integer getBuildFromReferenceNid()
	{
		return buildFromReferenceNid_;
	}

	/**
	 * If this was constructed with just a nid - this returns true of the nid is pointing to an assemblage concept - false if it is
	 * pointing to a component reference.  The value is meaningless if {@link #getBuildFromReferenceNid()} returns null.
	 */
	public boolean getReferenceIsAssemblyNid()
	{
		return referenceIsAssemblyNid_;
	}
	
	
	/**
	 * For cases when it was built from an existing refex only
	 * @param attachedDataColumn - optional - ignored (can be null) except applicable to {@link SememeGUIColumnType#ATTACHED_DATA}
	 */
	public int compareTo(SememeGUIColumnType columnTypeToCompare, Integer attachedDataColumn, SememeGUI other)
	{
		switch (columnTypeToCompare)
		{
			case STATUS_CONDENSED:
			{
				//sort by uncommitted first, then current / historical, then active / inactive
				if (this.getSememe().getTime() == Long.MAX_VALUE)
				{
					return -1;
				}
				else if (other.getSememe().getTime() == Long.MAX_VALUE)
				{
					return 1;
				}
				
				if (this.isCurrent() && !other.isCurrent())
				{
					return -1;
				}
				else if (!this.isCurrent() && other.isCurrent())
				{
					return 1;
				}
				
				if (this.getSememe().getState() == State.ACTIVE && other.getSememe().getState() == State.INACTIVE)
				{
					return -1;
				}
				else if (this.getSememe().getState() == State.INACTIVE && other.getSememe().getState() == State.ACTIVE)
				{
					return 1;
				}
				return 0;
			}
			case TIME:
			{
				if (this.getSememe().getTime() < other.getSememe().getTime())
				{
					return -1;
				}
				else if (this.getSememe().getTime() > other.getSememe().getTime())
				{
					return -1;
				}
				else
				{
					return 0;
				}
			}
			case COMPONENT: case ASSEMBLAGE: case STATUS_STRING: case AUTHOR: case MODULE: case PATH:
			{
				String myString = this.getDisplayStrings(columnTypeToCompare, null).getKey();
				String otherString = other.getDisplayStrings(columnTypeToCompare, null).getKey();
				return AlphanumComparator.compare(myString, otherString, true);
			}
			case ATTACHED_DATA:
			{
				if (attachedDataColumn == null)
				{
					throw new RuntimeException("API misuse");
				}
				DynamicSememeDataBI myData = getData(this.refex_).length > attachedDataColumn ? getData(this.refex_)[attachedDataColumn] : null;
				DynamicSememeDataBI otherData = getData(other.refex_).length > attachedDataColumn ? getData(other.refex_)[attachedDataColumn] : null;
				
				if (myData == null && otherData != null)
				{
					return -1;
				}
				else if (myData != null && otherData == null)
				{
					return 1;
				}
				else if (myData == null && otherData == null)
				{
					return 0;
				}
				else if (myData instanceof DynamicSememeFloatBI && otherData instanceof DynamicSememeFloatBI)
				{
					return NumberUtilities.compare(((DynamicSememeFloatBI) myData).getDataFloat(), ((DynamicSememeFloatBI) otherData).getDataFloat());
				}
				else if (myData instanceof DynamicSememeDoubleBI && otherData instanceof DynamicSememeDoubleBI) 
				{
					return NumberUtilities.compare(((DynamicSememeDoubleBI) myData).getDataDouble(), ((DynamicSememeDoubleBI) otherData).getDataDouble());
				}
				else if (myData instanceof DynamicSememeIntegerBI && otherData instanceof DynamicSememeIntegerBI) 
				{
					return NumberUtilities.compare(((DynamicSememeIntegerBI) myData).getDataInteger(), ((DynamicSememeIntegerBI) otherData).getDataInteger());
				}
				else if (myData instanceof DynamicSememeLongBI && otherData instanceof DynamicSememeLongBI)
				{
					return NumberUtilities.compare(((DynamicSememeLongBI) myData).getDataLong(), ((DynamicSememeLongBI) otherData).getDataLong());
				}
				else
				{
					String myString = this.getDisplayStrings(columnTypeToCompare, attachedDataColumn).getKey();
					String otherString = other.getDisplayStrings(columnTypeToCompare, attachedDataColumn).getKey();
					return AlphanumComparator.compare(myString, otherString, true);
				}
			}

			default:
				throw new RuntimeException("Missing implementation: " + columnTypeToCompare);
		}
	}
	
	/**
	 * Returns the string for display, and the tooltip, if applicable.  Either / or may be null.
	 * Key is for the display, value is for the tooltip.
	 * @param attachedDataColumn should be null for most types - applicable to {@link SememeGUIColumnType#ATTACHED_DATA}
	 */
	public AbstractMap.SimpleImmutableEntry<String, String> getDisplayStrings(SememeGUIColumnType desiredColumn, Integer attachedDataColumn)
	{
		String cacheKey = desiredColumn.name() + attachedDataColumn;  //null is ok on the attachedDataColumn...
		
		AbstractMap.SimpleImmutableEntry<String, String> returnValue = stringCache_.get(cacheKey);
		if (returnValue != null)
		{
			return returnValue;
		}
		
		switch (desiredColumn)
		{
			case STATUS_CONDENSED:
			{
				//Just easier to leave the impl in StatusCell for this one.  We don't need filters on this column either.
				throw new RuntimeException("No text for this field");
			}
			case COMPONENT: case ASSEMBLAGE: case AUTHOR: case PATH: case MODULE:
			{
				String text = getComponentText(getNidFetcher(desiredColumn, attachedDataColumn));
				returnValue = new AbstractMap.SimpleImmutableEntry<String, String>(text, text);
				break;
			}
			case STATUS_STRING:
			{
				returnValue = new AbstractMap.SimpleImmutableEntry<String, String>(refex_.getState().toString(), null);
				break;
			}
			case TIME:
			{
				returnValue = new AbstractMap.SimpleImmutableEntry<String, String>((refex_.getTime() == Long.MAX_VALUE ? "-Uncommitted-" : 
					new Date(refex_.getTime()).toString()), null);
				break;
			}
			case ATTACHED_DATA:
			{
				if (attachedDataColumn == null)
				{
					throw new RuntimeException("API misuse");
				}
				DynamicSememeDataBI data = getData(this.refex_).length > attachedDataColumn ? getData(this.refex_)[attachedDataColumn] : null;
				if (data != null)
				{
					if (data instanceof DynamicSememeByteArrayBI)
					{
						returnValue = new AbstractMap.SimpleImmutableEntry<String, String>("[Binary]", null);
					}
					else if (data instanceof DynamicSememeNidBI)
					{
						String desc = getComponentText(((DynamicSememeNidBI)data).getDataNid());
						returnValue = new AbstractMap.SimpleImmutableEntry<String, String>(desc, data.getDataObject().toString());
					}
					else if (data instanceof DynamicSememeUUIDBI)
					{
						String desc;
						if (Get.identifierService().hasUuid(((DynamicSememeUUIDBI)data).getDataUUID()))
						{
							desc = getComponentText(Get.identifierService().getNidForUuids(((DynamicSememeUUIDBI)data).getDataUUID()));
						}
						else
						{
							desc = ((DynamicSememeUUIDBI)data).getDataUUID() + "";
						}
						returnValue = new AbstractMap.SimpleImmutableEntry<String, String>(desc, data.getDataObject().toString());
					}
					else if (data instanceof DynamicSememeArrayBI<?>)
					{
						DynamicSememeArrayBI<?> instanceData = (DynamicSememeArrayBI<?>)data;
						//TODO dan needs to implement - I changed some details of how array data types are handled...
//						switch (instanceData.getArrayDataType())
//						{
//							case ARRAY:
//								//Could recurse... but I can't imagine a use case at the moment.
//								returnValue = new AbstractMap.SimpleImmutableEntry<String, String>("[" + instanceData.getDataArray().length + " nested arrays]",
//										"An array of nested arrays");
//								break;
//							case STRING: case BOOLEAN: case DOUBLE: case FLOAT: case INTEGER: case LONG: case NID: case UUID:
//							{
//								//NID and UUID could be turned into strings... but, unusual use case... leave like this for now.
//								StringBuilder sb = new StringBuilder();
//								sb.append("[");
//								for (DynamicSememeDataBI d : instanceData.getDataArray())
//								{
//									sb.append(d.getDataObject().toString());
//									sb.append(", ");
//								}
//								if (sb.length() > 1)
//								{
//									sb.setLength(sb.length() - 2);
//								}
//								sb.append("]");
//								returnValue = new AbstractMap.SimpleImmutableEntry<String, String>(sb.toString(), "Array of " + instanceData.getDataArray().length + " items: " + sb.toString());
//								break;
//							}
//							
//							case BYTEARRAY:
//								returnValue = new AbstractMap.SimpleImmutableEntry<String, String>("[" + instanceData.getDataArray().length + " Binary items]",
//										"An array of binary objects");
//								break;
//							case UNKNOWN: case POLYMORPHIC:
//							{
//								//shouldn't happen - but just do the toString
//								returnValue = new AbstractMap.SimpleImmutableEntry<String, String>("[" + instanceData.getDataArray().length + " items]",
//										"An array of unknown data elements");
//								break;
//							}
//							default:
								logger_.error("Unhandled case: {}, {}", instanceData, Arrays.toString(instanceData.getDataArray()));
								returnValue = new AbstractMap.SimpleImmutableEntry<String, String>("-ERROR-", "Internal error computing value");
//								break;
//						}
					}
					else
					{
						returnValue = new AbstractMap.SimpleImmutableEntry<String, String>(data.getDataObject().toString(), data.getDataObject().toString());
					}
				}
				else
				{
					returnValue = new AbstractMap.SimpleImmutableEntry<String, String>("", null);
				}
				break;
			}

			default:
				throw new RuntimeException("Missing implementation: " + desiredColumn);
		}
		
		stringCache_.put(cacheKey, returnValue);
		return returnValue;
		
	}
	
	private String getComponentText(ToIntFunction<SememeVersion<?>> nidFetcher)
	{
		return getComponentText(nidFetcher.applyAsInt(this.refex_));
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private String getComponentText(int nid)
	{
		String text;
		
		try
		{
			//This may be a different component - like a description, or another refex... need to handle.
			Optional<? extends ObjectChronology<? extends StampedVersion>> oc = Get.identifiedObjectService().getIdentifiedObjectChronology(nid);
			if (!oc.isPresent())
			{
				text = "[NID] " + nid + " not on path";
			}
			else if (oc.get() instanceof ConceptChronology<?>)
			{
				Optional<String> conDesc = OchreUtility.getDescription(oc.get().getNid(), StampCoordinates.getDevelopmentLatest(), null);
				text = (conDesc.isPresent() ? conDesc.get() : "off path [NID]:" + oc.get().getNid());
			}
			else if (oc.get() instanceof SememeChronology<?>)
			{
				SememeChronology sc = (SememeChronology)oc.get();
				switch (sc.getSememeType()) {
					case COMPONENT_NID:
						text = "Component NID Sememe using assemblage: " + OchreUtility.getDescription(sc.getAssemblageSequence());
						break;
					case DESCRIPTION:
						Optional<LatestVersion<DescriptionSememe>> ds = sc.getLatestVersion(DescriptionSememe.class, StampCoordinates.getDevelopmentLatest());
						text = "Description Sememe: " + (ds.isPresent() ? ds.get().value().getText() : "off path [NID]: " + sc.getNid());
						break;
					case DYNAMIC:
						text = "Dynamic Sememe using assemblage: " + OchreUtility.getDescription(sc.getAssemblageSequence());
						break;
					case LOGIC_GRAPH:
						text = "Logic Graph Sememe [NID]: " + oc.get().getNid();
						break;
					case LONG:
						Optional<LatestVersion<LongSememe>> sl = sc.getLatestVersion(LongSememe.class, StampCoordinates.getDevelopmentLatest());
						text = "String Sememe: " + (sl.isPresent() ? sl.get().value().getLongValue() : "off path [NID]: " + sc.getNid());
						break;
					case MEMBER:
						text = "Member Sememe using assemblage: " + OchreUtility.getDescription(sc.getAssemblageSequence());
						break;
					case RELATIONSHIP_ADAPTOR:
						text = "Relationship Adapter Sememe [NID]: " + oc.get().getNid();
						break;
					case STRING:
						Optional<LatestVersion<StringSememe>> ss = sc.getLatestVersion(StringSememe.class, StampCoordinates.getDevelopmentLatest());
						text = "String Sememe: " + (ss.isPresent() ? ss.get().value().getString() : "off path [NID]: " + sc.getNid());
						break;
					case UNKNOWN:
					default :
						logger_.warn("The sememe type " + sc.getSememeType() + " is not handled yet!");
						//TODO should handle other types of common sememes
						text = oc.get().toUserString();
						break;
				}
			}
			else if (oc.get() instanceof DynamicSememe<?>)
			{
				DynamicSememe<?> nds = (DynamicSememe<?>) oc.get();
				text = "Nested Sememe Dynamic: using assemblage " + OchreUtility.getDescription(nds.getAssemblageSequence());
			}
			else
			{
				logger_.warn("The component type " + oc.get().getClass() + " is not handled yet!");
				//TODO should handle other types of common sememes
				text = oc.get().toUserString();
			}
		}
		catch (Exception e)
		{
			logger_.error("Unexpected error", e);
			text = "-ERROR-";
		}
		return text;
	}
	
	/**
	 * 
	 * @param attachedDataColumn null for most types - applicable to {@link SememeGUIColumnType#ATTACHED_DATA}
	 * @return
	 */
	public ToIntFunction<SememeVersion<?>> getNidFetcher(SememeGUIColumnType desiredColumn, Integer attachedDataColumn)
	{
		switch (desiredColumn)
		{
			case STATUS_CONDENSED:
			{
				throw new RuntimeException("Improper API usage");
			}
			case COMPONENT:
			{
				return new ToIntFunction<SememeVersion<?>>()
				{
					@Override
					public int applyAsInt(SememeVersion<?> value)
					{
						return refex_.getReferencedComponentNid();
					}
				};
			}
			case ASSEMBLAGE:
			{
				return new ToIntFunction<SememeVersion<?>>()
				{
					@Override
					public int applyAsInt(SememeVersion<?> value)
					{
						return Get.identifierService().getConceptNid(refex_.getAssemblageSequence());
					}
				};
			}
			case AUTHOR:
			{
				return new ToIntFunction<SememeVersion<?>>()
				{
					@Override
					public int applyAsInt(SememeVersion<?> value)
					{
						return Get.identifierService().getConceptNid(refex_.getAuthorSequence());
					}
				};
			}
			case MODULE:
			{
				return new ToIntFunction<SememeVersion<?>>()
				{
					@Override
					public int applyAsInt(SememeVersion<?> value)
					{
						return Get.identifierService().getConceptNid(refex_.getModuleSequence());
					}
				};
			}
			case PATH:
			{
				return new ToIntFunction<SememeVersion<?>>()
				{
					@Override
					public int applyAsInt(SememeVersion<?> value)
					{
						return Get.identifierService().getConceptNid(refex_.getPathSequence());
					}
				};
			}
			
			case ATTACHED_DATA:
			{
				if (attachedDataColumn == null)
				{
					throw new RuntimeException("API misuse");
				}
				return new ToIntFunction<SememeVersion<?>>()
				{
					@Override
					public int applyAsInt(SememeVersion<?> value)
					{
						DynamicSememeDataBI data = getData(refex_).length > attachedDataColumn ? getData(refex_)[attachedDataColumn] : null;
						if (data != null)
						{
							if (data instanceof DynamicSememeNidBI)
							{
								return ((DynamicSememeNidBI)data).getDataNid();
							}
							else if (data instanceof DynamicSememeUUIDBI)
							{
								if (Get.identifierService().hasUuid(((DynamicSememeUUIDBI)data).getDataUUID()))
								{
									return Get.identifierService().getNidForUuids(((DynamicSememeUUIDBI)data).getDataUUID());
								}
							}
						}
						return 0;
					}
				};
				
			}

			default:
				throw new RuntimeException("Missing implementation: " + desiredColumn);
		}
	}
	
	/**
	 * A method to read the data from a sememe of an arbitrary type, mocking up static sememes as dynamic sememems, if necessary
	 * @param sememe
	 */
	public static DynamicSememeDataBI[] getData(SememeVersion<?> sememe)
	{
		switch (sememe.getChronology().getSememeType())
		{
			case COMPONENT_NID:
				return new DynamicSememeDataBI[] {new DynamicSememeNid(((ComponentNidSememe<?>)sememe).getComponentNid())};
			case DESCRIPTION:
				return new DynamicSememeDataBI[] {new DynamicSememeString(((DescriptionSememe<?>)sememe).getText())};
			case DYNAMIC:
				return ((DynamicSememe<?>)sememe).getData();
			case LONG:
				return new DynamicSememeDataBI[] {new DynamicSememeLong(((LongSememe<?>)sememe).getLongValue())};
			case MEMBER:
				return new DynamicSememeDataBI[] {};
			case STRING:
				return new DynamicSememeDataBI[] {new DynamicSememeString(((StringSememe<?>)sememe).getString())};
			case RELATIONSHIP_ADAPTOR:
				return new DynamicSememeDataBI[] {new DynamicSememeString(((RelationshipVersionAdaptor<?>)sememe).toString())};
			case LOGIC_GRAPH:
				return new DynamicSememeDataBI[] {new DynamicSememeString(((LogicGraphSememe<?>)sememe).toString())};
			case UNKNOWN:
			default :
				throw new UnsupportedOperationException();
		}
			
	}
}
