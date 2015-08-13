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
package gov.va.isaac.associations;

import gov.va.isaac.AppContext;
import gov.va.isaac.ExtendedAppContext;
import gov.vha.isaac.ochre.api.constants.ISAAC;
import gov.vha.isaac.ochre.api.index.SearchResult;
import gov.vha.isaac.ochre.impl.sememe.DynamicSememeUsageDescription;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeString;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.lucene.queryparser.classic.ParseException;
import org.ihtsdo.otf.query.lucene.indexers.DynamicSememeIndexer;
import org.ihtsdo.otf.tcc.api.chronicle.ComponentChronicleBI;
import org.ihtsdo.otf.tcc.api.concept.ConceptChronicleBI;
import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;
import org.ihtsdo.otf.tcc.api.contradiction.ContradictionException;
import org.ihtsdo.otf.tcc.api.coordinate.ViewCoordinate;
import org.ihtsdo.otf.tcc.api.refexDynamic.DynamicSememeChronicleBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.DynamicSememeVersionBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.DynamicSememeColumnInfo;
import org.ihtsdo.otf.tcc.api.spec.ValidationException;

/**
 * {@link AssociationUtilities}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class AssociationUtilities
{
	private static int associationNid = Integer.MIN_VALUE;

	private static int getAssociationNid() throws ValidationException, IOException
	{
		if (associationNid == Integer.MIN_VALUE)
		{
			associationNid = ISAAC.ASSOCIATION_SEMEME.getNid();
		}
		return associationNid;
	}

	public static List<Association> getSourceAssociations(ComponentChronicleBI<?> component, ViewCoordinate vc) throws IOException
	{
		ArrayList<Association> result = new ArrayList<>();
		for (DynamicSememeVersionBI<?> refex : component.getRefexesDynamicActive(vc))
		{
			ConceptVersionBI refexAssemblageType = ExtendedAppContext.getDataStore().getConceptVersion(vc, refex.getAssemblageNid());

			for (DynamicSememeChronicleBI<?> refexAttachedToAssemblage : refexAssemblageType.getRefexesDynamic())
			{
				if (refexAttachedToAssemblage.getAssemblageNid() == getAssociationNid())
				{
					result.add(new Association(refex));
					break;
				}
			}

		}
		return result;
	}

	public static List<Association> getTargetAssociations(ComponentChronicleBI<?> component, ViewCoordinate vc) throws IOException, ContradictionException, ParseException, PropertyVetoException
	{
		ArrayList<Association> result = new ArrayList<>();
		
		//TODO validate the concept is annotated for association?

		DynamicSememeIndexer indexer = AppContext.getService(DynamicSememeIndexer.class);
		if (indexer == null)
		{
			throw new RuntimeException("Required index is not available");
		}
		
		for (ConceptChronicleBI associationType : getAssociationTypes())
		{
			int colIndex = findTargetColumnIndex(associationType.getNid());
			List<SearchResult> refexes = indexer.query(new DynamicSememeString(component.getNid() + " OR " + component.getPrimordialUuid()),
					associationType.getNid(), false, new Integer[] {colIndex}, Integer.MAX_VALUE, null);
			for (SearchResult sr : refexes)
			{
				DynamicSememeChronicleBI<?> rc = (DynamicSememeChronicleBI<?>) ExtendedAppContext.getDataStore().getComponent(sr.getNid());
				Optional<? extends DynamicSememeVersionBI<?>> rv = rc.getVersion(vc);
				if (rv.isPresent())
				{
					result.add(new Association(rv.get()));
				}
			}
		}
		return result;
	}

	public static List<Association> getAssociationsOfType(ConceptChronicleBI concept, ViewCoordinate vc) throws NumberFormatException, IOException, ParseException, ContradictionException
	{
		ArrayList<Association> result = new ArrayList<>();

		//TODO validate the concept is annotated for association?

		DynamicSememeIndexer indexer = AppContext.getService(DynamicSememeIndexer.class);
		if (indexer == null)
		{
			throw new RuntimeException("Required index is not available");
		}
		List<SearchResult> refexes = indexer.queryAssemblageUsage(concept.getNid(), Integer.MAX_VALUE, null);
		for (SearchResult sr : refexes)
		{
			DynamicSememeChronicleBI<?> rc = (DynamicSememeChronicleBI<?>) ExtendedAppContext.getDataStore().getComponent(sr.getNid());
			Optional<? extends DynamicSememeVersionBI<?>> rv = rc.getVersion(vc);
			if (rv.isPresent())
			{
				result.add(new Association(rv.get()));
			}
		}

		return result;
	}

	public static List<ConceptChronicleBI> getAssociationTypes() throws NumberFormatException, ValidationException, IOException, ParseException
	{
		ArrayList<ConceptChronicleBI> result = new ArrayList<>();

		DynamicSememeIndexer indexer = AppContext.getService(DynamicSememeIndexer.class);
		if (indexer == null)
		{
			throw new RuntimeException("Required index is not available");
		}
		List<SearchResult> refexes = indexer.queryAssemblageUsage(ISAAC.ASSOCIATION_SEMEME.getNid(), Integer.MAX_VALUE, null);
		for (SearchResult sr : refexes)
		{
			result.add(ExtendedAppContext.getDataStore().getConceptForNid(sr.getNid()));
		}
		return result;
	}

	/**
	 * @param assemblageNid
	 * @throws ContradictionException 
	 * @throws IOException 
	 */
	public static int findTargetColumnIndex(int assemblageNid) throws IOException, ContradictionException
	{
		DynamicSememeUsageDescription rdud = DynamicSememeUsageDescription.readDynamicSememeUsageDescription(assemblageNid);

		for (DynamicSememeColumnInfo rdci : rdud.getColumnInfo())
		{
			if (rdci.getColumnDescriptionConcept().equals(ISAAC.REFEX_COLUMN_TARGET_COMPONENT.getPrimodialUuid()))
			{
				return rdci.getColumnOrder();
			}
		}
		return Integer.MIN_VALUE;
	}
}
