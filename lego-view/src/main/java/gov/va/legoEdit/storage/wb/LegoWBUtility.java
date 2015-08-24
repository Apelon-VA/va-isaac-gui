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
package gov.va.legoEdit.storage.wb;

import gov.va.isaac.util.OchreUtility;
import gov.va.legoEdit.model.schemaModel.Assertion;
import gov.va.legoEdit.model.schemaModel.AssertionComponent;
import gov.va.legoEdit.model.schemaModel.Concept;
import gov.va.legoEdit.model.schemaModel.Expression;
import gov.va.legoEdit.model.schemaModel.Lego;
import gov.va.legoEdit.model.schemaModel.LegoList;
import gov.va.legoEdit.model.schemaModel.Measurement;
import gov.va.legoEdit.model.schemaModel.Relation;
import gov.va.legoEdit.model.schemaModel.RelationGroup;
import gov.va.legoEdit.model.schemaModel.Type;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.impl.utility.Frills;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * OTFUtility
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 * Copyright 2013
 */
public class LegoWBUtility
{
	private static Logger logger = LoggerFactory.getLogger(LegoWBUtility.class);

	public static Concept convertConcept(ConceptSnapshot concept)
	{
		Concept c = null;
		if (concept != null)
		{
			c = new Concept();
			c.setDesc(concept.getConceptDescriptionText());
			c.setUuid(concept.getPrimordialUuid().toString());
			try
			{
				Optional<Long> sctId = Frills.getSctId(concept.getNid(), null);
				if (sctId.isPresent())
				{
					c.setSctid(sctId.get());
				}
			}
			catch (Exception e)
			{
				logger.warn("Oops", e);
			}
			if (c.getSctid() == null)
			{
				logger.debug("Couldn't find SCTID for concept " + c.getDesc() + " " + c.getUuid());
			}
		}
		return c;
	}

	/**
	 * Updates (in place) all of the concepts within the supplied LegoList with
	 * the results from a WB lookup. Concepts which fail lookup are returned in
	 * the result list.
	 */
	public static List<Concept> lookupAllConcepts(LegoList ll)
	{
		ArrayList<Concept> failures = new ArrayList<>();

		// walk through the legolist, and to a lookup on each concept, flagging
		// errors on the ones that failed lookup.
		for (Lego l : ll.getLego())
		{
			for (Assertion a : l.getAssertion())
			{
				failures.addAll(lookupAll(a.getDiscernible().getExpression()));
				failures.addAll(lookupAll(a.getQualifier().getExpression()));
				failures.addAll(lookupAll(a.getValue().getExpression()));
				if (a.getValue() != null && a.getValue().getMeasurement() != null)
				{
					failures.addAll(lookupAll(a.getValue().getMeasurement()));
				}
				for (AssertionComponent ac : a.getAssertionComponent())
				{
					failures.addAll(lookupAll(ac.getType()));
				}
			}
		}
		return failures;
	}

	private static List<Concept> lookupAll(Expression e)
	{
		ArrayList<Concept> failures = new ArrayList<>();
		if (e == null)
		{
			return failures;
		}
		if (e.getConcept() != null)
		{
			Optional<? extends ConceptChronology<? extends ConceptVersion<?>>> cc = OchreUtility.getConceptForUnknownIdentifier(e.getConcept().getUuid());
			if (!cc.isPresent())
			{
				cc = OchreUtility.getConceptForUnknownIdentifier(e.getConcept().getSctid() + "");
			}
			if (cc.isPresent())
			{
				Optional<ConceptSnapshot> cs = OchreUtility.getConceptSnapshot(cc.get().getNid(), null, null);
				if (cs.isPresent())
				{
					e.setConcept(convertConcept(cs.get()));
				}
				else
				{
					failures.add(e.getConcept());
				}
			}
			else
			{
				failures.add(e.getConcept());
			}
		}
		for (Expression e1 : e.getExpression())
		{
			failures.addAll(lookupAll(e1));
		}
		for (Relation r : e.getRelation())
		{
			failures.addAll(lookupAll(r));
		}
		for (RelationGroup rg : e.getRelationGroup())
		{
			for (Relation r : rg.getRelation())
			{
				failures.addAll(lookupAll(r));
			}
		}
		return failures;
	}

	private static List<Concept> lookupAll(Relation r)
	{
		ArrayList<Concept> failures = new ArrayList<>();
		if (r.getType() != null && r.getType().getConcept() != null)
		{
			failures.addAll(lookupAll(r.getType()));
		}
		if (r.getDestination() != null)
		{
			failures.addAll(lookupAll(r.getDestination().getExpression()));
			failures.addAll(lookupAll(r.getDestination().getMeasurement()));
		}
		return failures;
	}

	private static List<Concept> lookupAll(Type t)
	{
		ArrayList<Concept> failures = new ArrayList<Concept>();
		if (t == null || t.getConcept() == null)
		{
			return failures;
		}
		Optional<? extends ConceptChronology<? extends ConceptVersion<?>>> cc = OchreUtility.getConceptForUnknownIdentifier(t.getConcept().getUuid());
		if (!cc.isPresent())
		{
			cc = OchreUtility.getConceptForUnknownIdentifier(t.getConcept().getSctid() + "");
		}
		if (cc.isPresent())
		{
			Optional<ConceptSnapshot> cs = OchreUtility.getConceptSnapshot(cc.get().getNid(), null, null);
			if (cs.isPresent())
			{
				t.setConcept(convertConcept(cs.get()));
			}
			else
			{
				failures.add(t.getConcept());
			}
		}
		else
		{
			failures.add(t.getConcept());
		}
		
		return failures;
	}

	private static List<Concept> lookupAll(Measurement m)
	{
		ArrayList<Concept> failures = new ArrayList<>();
		if (m == null || m.getUnits() == null || m.getUnits().getConcept() == null)
		{
			return failures;
		}
		
		Optional<? extends ConceptChronology<? extends ConceptVersion<?>>> cc = OchreUtility.getConceptForUnknownIdentifier(m.getUnits().getConcept().getUuid());
		if (!cc.isPresent())
		{
			cc = OchreUtility.getConceptForUnknownIdentifier(m.getUnits().getConcept().getSctid() + "");
		}
		if (cc.isPresent())
		{
			Optional<ConceptSnapshot> cs = OchreUtility.getConceptSnapshot(cc.get().getNid(), null, null);
			if (cs.isPresent())
			{
				m.getUnits().setConcept(convertConcept(cs.get()));
			}
			else
			{
				failures.add(m.getUnits().getConcept());
			}
		}
		else
		{
			failures.add(m.getUnits().getConcept());
		}
		
		return failures;
	}
}
