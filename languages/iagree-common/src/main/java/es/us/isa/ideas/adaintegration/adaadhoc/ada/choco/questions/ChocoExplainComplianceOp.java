/**
 * 	This file is part of ADA.
 *
 *     ADA is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     ADA is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with ADA.  If not, see <http://www.gnu.org/licenses/>.
 */

package es.us.isa.ideas.adaintegration.adaadhoc.ada.choco.questions;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import es.us.isa.ideas.adaintegration.adaadhoc.ada.choco.ChocoAnalyzer;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.choco.ChocoOperation;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.document.AbstractDocument;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.operations.AlternativeDocumentsOperation;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.operations.ExplainComplianceOperation;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.wsag10.AbstractAgreementDocument;

public class ChocoExplainComplianceOp extends ChocoOperation implements
		ExplainComplianceOperation {

	private Collection<AbstractDocument> explanations;
	
	@Override
	public void execute(ChocoAnalyzer an) {
		explanations = new LinkedList<AbstractDocument>();
		if (docs.size() == 2) {
			//se asume que el primer documento es la plantilla
			AbstractAgreementDocument d1 = (AbstractAgreementDocument) docs.get(0);
			AbstractAgreementDocument d2 = (AbstractAgreementDocument) docs.get(1);
			
			AlternativeDocumentsOperation ado = new ChocoAlternativeDocumentsOp();
			ado.addDocument(d2);
			an.analyze(ado);
			Collection<AbstractDocument> alternatives2 = ado.getAlternativeDocuments();
			
			Iterator<AbstractDocument> it = alternatives2.iterator();
			while (it.hasNext()){
				AbstractAgreementDocument doc = (AbstractAgreementDocument) it.next();
				ChocoComplianceOp cco = new ChocoComplianceOp();
				cco.addDocument(d1);
				cco.addDocument(doc);
				an.analyze(cco);
				if (cco.isCompliant()){
					explanations.add(doc);
				}
			}
		}
	}

	@Override
	public Collection<AbstractDocument> explain() {
		return explanations;
	}

}
