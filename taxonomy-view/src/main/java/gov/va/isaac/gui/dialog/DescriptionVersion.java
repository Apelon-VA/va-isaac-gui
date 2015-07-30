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
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;

import java.util.AbstractMap;
import java.util.Date;
import java.util.HashMap;
import java.util.function.ToIntFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link OCHREDescriptionVersion}
 *
 * A wrapper for DescriptionVersionBI to add in attributes like "isLatest" and a place to implement other useful methods that 
 * you would want to ask of a Description.
 * 
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public class DescriptionVersion
{
	private static final Logger LOG = LoggerFactory.getLogger(DescriptionVersion.class);
	private boolean isLatest_;
	private DescriptionSememe<?> dv_;
	
	private HashMap<String, AbstractMap.SimpleImmutableEntry<String, String>> stringCache_ = new HashMap<>();
	
	public DescriptionVersion(DescriptionSememe<?> dv, boolean isLatest)
	{
		isLatest_ = isLatest;
		dv_ = dv;
	}
	
	public boolean isCurrent()
	{
		return isLatest_;
	}
	
	public DescriptionSememe<?> getDescriptionVersion()
	{
		return dv_;
	}
	
//	public boolean hasDynamicRefex()
//	{
//		try
//		{
//			Collection<? extends RefexDynamicChronicleBI<?>> foo = dv_.getRefexDynamicAnnotations();
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
	public AbstractMap.SimpleImmutableEntry<String, String> getDisplayStrings(DescriptionColumnType desiredColumn)
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
			case AUTHOR: case PATH: case MODULE: case TYPE:
			{
				String text = getConceptComponentText(getNidFetcher(desiredColumn));
				returnValue = new AbstractMap.SimpleImmutableEntry<String, String>(text, text);
				break;
			}
			case STATUS_STRING:
			{
				returnValue = new AbstractMap.SimpleImmutableEntry<String, String>(dv_.getState().toString(), null);
				break;
			}
			case UUID:
			{
				returnValue = new AbstractMap.SimpleImmutableEntry<String, String>(dv_.getPrimordialUuid().toString(), null);
				break;
			}
			case TIME:
			{
				returnValue = new AbstractMap.SimpleImmutableEntry<String, String>((dv_.getTime() == Long.MAX_VALUE ? "-Uncommitted-" : 
					new Date(dv_.getTime()).toString()), null);
				break;
			}
			case DESCRIPTION:
				returnValue = new AbstractMap.SimpleImmutableEntry<String, String>(dv_.getText(), null);
				break;
			case LANGUAGE:
				String text = "";
				try
				{
					// TODO implement handling of LANGUAGE column
					//Not bothering with historical here - doesn't really fit the display paradigm.
					text = OCHREUtility.conceptDescriptionText(dv_.getLanguageConceptSequence());
//					for (RefexChronicleBI<?> rc :  dv_.getAnnotationsActive(OTFUtility.getViewCoordinate()))
//					{
//						for (RefexVersionBI<?> rv : rc.getVersions())
//						{
//							if (rv instanceof RefexNidAnalogBI<?> && 
//									ExtendedAppContext.getDataStore().isKindOf(rv.getAssemblageNid(), OTFUtility.getLangTypeNid(), 
//											OTFUtility.getViewCoordinate()))
//							{
//								if (text.length() > 0)
//								{
//									text += ", ";
//								}
//								text += OTFUtility.getDescription(((RefexNidAnalogBI<?>)rv).getNid1()) + " - " + 
//										OTFUtility.getDescription(rv.getAssemblageNid());
//							}
//						}
//					}
					
					
				}
				catch (Exception e)
				{
					LOG.error("Error setting up language view column");
					text = "-error-";
				}
				returnValue = new AbstractMap.SimpleImmutableEntry<String, String>(text, null);
				break;
			default:
				throw new RuntimeException("Missing implementation: " + desiredColumn);
		}
		
		stringCache_.put(cacheKey, returnValue);
		return returnValue;
	}
	
	public ToIntFunction<DescriptionSememe<?>> getNidFetcher(DescriptionColumnType desiredColumn)
	{
		switch (desiredColumn)
		{
			case STATUS_CONDENSED: case LANGUAGE: case STATUS_STRING: case TIME: case UUID: case DESCRIPTION:
			{
				throw new RuntimeException("Improper API usage");
			}
			case AUTHOR:
			{
				return new ToIntFunction<DescriptionSememe<?>>()
				{
					@Override
					public int applyAsInt(DescriptionSememe<?> value)
					{
						return value.getAuthorSequence();
					}
				};
			}
			case MODULE:
			{
				return new ToIntFunction<DescriptionSememe<?>>()
				{
					@Override
					public int applyAsInt(DescriptionSememe<?> value)
					{
						return value.getModuleSequence();
					}
				};
			}
			case PATH:
			{
				return new ToIntFunction<DescriptionSememe<?>>()
				{
					@Override
					public int applyAsInt(DescriptionSememe<?> value)
					{
						return value.getPathSequence();
					}
				};
			}
			case TYPE:
				return new ToIntFunction<DescriptionSememe<?>>()
				{
					@Override
					public int applyAsInt(DescriptionSememe<?> value)
					{
						return value.getDescriptionTypeConceptSequence();
					}
				};

			default:
				throw new RuntimeException("Missing implementation: " + desiredColumn);
		}
	}
	
	private String getConceptComponentText(ToIntFunction<DescriptionSememe<?>> nidFetcher)
	{
		try
		{
			return OCHREUtility.conceptDescriptionText(nidFetcher.applyAsInt(dv_));
		}
		catch (Exception e)
		{
			LOG.error("Unexpected error getting text for nid {}", nidFetcher.applyAsInt(dv_));
			return "-error-";
		}
	}
}
