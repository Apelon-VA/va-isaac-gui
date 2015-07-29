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
package gov.va.isaac.gui.dialog;

import gov.va.isaac.util.OCHREUtility;
import gov.va.isaac.util.OTFUtility;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshotService;
import gov.vha.isaac.ochre.api.relationship.RelationshipVersionAdaptor;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.function.ToIntFunction;

import org.ihtsdo.otf.tcc.api.refexDynamic.RefexDynamicChronicleBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link RelationshipVersion}
 *
 * A wrapper for DescriptionVersionBI to add in attributes like "isLatest" and a place to implement other useful methods that 
 * you would want to ask of a Description.
 * 
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public class RelationshipVersion
{
	private static final Logger LOG = LoggerFactory.getLogger(RelationshipVersion.class);
	private boolean isLatest_;
	private RelationshipVersionAdaptor<?> rv_;
	
	private HashMap<String, AbstractMap.SimpleImmutableEntry<String, String>> stringCache_ = new HashMap<>();
	
	public RelationshipVersion(RelationshipVersionAdaptor<?> rv, boolean isLatest)
	{
		isLatest_ = isLatest;
		rv_ = rv;
	}
	
	public boolean isCurrent()
	{
		return isLatest_;
	}
	
	public RelationshipVersionAdaptor<?> getRelationshipVersion()
	{
		return rv_;
	}
	
//	public boolean hasDynamicRefex()
//	{
//		try
//		{
//			Collection<? extends RefexDynamicChronicleBI<?>> foo = rv_.getRefexDynamicAnnotations();
//			if (foo != null && foo.size() > 0)
//			{
//				return true;
//			}
//		}
//		catch (IOException e)
//		{
//			LOG.error("Unexpeted", e);
//		}
//		return false;
//	}
	
	/**
	 * Returns the string for display, and the tooltip, if applicable.  Either / or may be null.
	 * Key is for the display, value is for the tooltip.
	 */
	public AbstractMap.SimpleImmutableEntry<String, String> getDisplayStrings(RelationshipColumnType desiredColumn, ConceptSnapshotService css)
	{
		String cacheKey = desiredColumn.name();
		
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
			case AUTHOR: case PATH: case MODULE: case TYPE: case DESTINATION: case SOURCE:
			{
				String text = getConceptComponentText(getIdFetcher(desiredColumn), css);
				returnValue = new AbstractMap.SimpleImmutableEntry<String, String>(text, text);
				break;
			}
			case STATUS_STRING:
			{
				returnValue = new AbstractMap.SimpleImmutableEntry<String, String>(rv_.getState().toString(), null);
				break;
			}
			case UUID:
			{
				returnValue = new AbstractMap.SimpleImmutableEntry<String, String>(rv_.getPrimordialUuid().toString(), null);
				break;
			}
			case GROUP:
			{
				returnValue = new AbstractMap.SimpleImmutableEntry<String, String>(rv_.getGroup() + "", null);
				break;
			}
			case TIME:
			{
				returnValue = new AbstractMap.SimpleImmutableEntry<String, String>((rv_.getTime() == Long.MAX_VALUE ? "-Uncommitted-" : 
					new Date(rv_.getTime()).toString()), null);
				break;
			}
			case CHARACTERISTIC:
			{
				returnValue = new AbstractMap.SimpleImmutableEntry<String, String>(rv_.getPremiseType().name(), null);
				break;
			}
			default:
				throw new RuntimeException("Missing implementation: " + desiredColumn);
		}
		
		stringCache_.put(cacheKey, returnValue);
		return returnValue;
	}
	
	public ToIntFunction<RelationshipVersionAdaptor<?>> getIdFetcher(RelationshipColumnType desiredColumn)
	{
		switch (desiredColumn)
		{
			case STATUS_CONDENSED: case STATUS_STRING: case TIME: case UUID: case GROUP: case CHARACTERISTIC:
			{
				throw new RuntimeException("Improper API usage");
			}
			case AUTHOR:
			{
				return new ToIntFunction<RelationshipVersionAdaptor<?>>()
				{
					@Override
					public int applyAsInt(RelationshipVersionAdaptor<?> value)
					{
						return value.getAuthorSequence();
					}
				};
			}
			case MODULE:
			{
				return new ToIntFunction<RelationshipVersionAdaptor<?>>()
				{
					@Override
					public int applyAsInt(RelationshipVersionAdaptor<?> value)
					{
						return value.getModuleSequence();
					}
				};
			}
			case PATH:
			{
				return new ToIntFunction<RelationshipVersionAdaptor<?>>()
				{
					@Override
					public int applyAsInt(RelationshipVersionAdaptor<?> value)
					{
						return value.getPathSequence();
					}
				};
			}
			case TYPE:
				return new ToIntFunction<RelationshipVersionAdaptor<?>>()
				{
					@Override
					public int applyAsInt(RelationshipVersionAdaptor<?> value)
					{
						return value.getTypeSequence();
					}
				};
			case DESTINATION:
				return new ToIntFunction<RelationshipVersionAdaptor<?>>()
				{
					@Override
					public int applyAsInt(RelationshipVersionAdaptor<?> value)
					{
						return value.getDestinationSequence();
					}
				};
			case SOURCE:
				return new ToIntFunction<RelationshipVersionAdaptor<?>>()
				{
					@Override
					public int applyAsInt(RelationshipVersionAdaptor<?> value)
					{
						return value.getOriginSequence();
					}
				};
			default:
				throw new RuntimeException("Missing implementation: " + desiredColumn);
		}
	}
	
	private String getConceptComponentText(ToIntFunction<RelationshipVersionAdaptor<?>> nidFetcher, ConceptSnapshotService css)
	{
		try
		{
			return OCHREUtility.getDescription(css.getConceptSnapshot(nidFetcher.applyAsInt(rv_)).getChronology(), css.getLanguageCoordinate(), css.getStampCoordinate());
		}
		catch (Exception e)
		{
			LOG.error("Unexpected error getting text for nid {}", nidFetcher.applyAsInt(rv_));
			return "-error-";
		}
	}
}
