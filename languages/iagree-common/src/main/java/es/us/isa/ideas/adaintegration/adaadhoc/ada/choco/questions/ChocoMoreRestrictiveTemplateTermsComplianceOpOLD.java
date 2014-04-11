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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import choco.Choco;
import choco.kernel.model.constraints.Constraint;
import choco.kernel.model.variables.Variable;
//import es.us.isa.ada.wsag10.Context;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.choco.ChocoAnalyzer;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.choco.ChocoOperation;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.choco.translators.ChocoComplianceTranslator;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.choco.translators.ChocoTranslator;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.choco.utils.Utils;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.document.AbstractDocument;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.document.AgreementElement;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.operations.ComplianceOperation;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.operations.ConsistencyOperation;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.operations.MoreRestrictiveTemplateTermsComplianceOperation;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.wsag10.AbstractAgreementDocument;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.wsag10.AgreementOffer;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.wsag10.GuaranteeTerm;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.wsag10.ServiceScope;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.wsag10.Template;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.wsag10.Term;

/*
 * Importante para esta operacion: primero debe hacerse el add de la plantilla,
 * y luego de la oferta (la plantilla ocupara el lugar 0, y la oferta el 1)
 */
public class ChocoMoreRestrictiveTemplateTermsComplianceOpOLD extends ChocoOperation implements
MoreRestrictiveTemplateTermsComplianceOperation {

	private boolean moreRestrictiveTemplateTerms;

	public ChocoMoreRestrictiveTemplateTermsComplianceOpOLD() {
		moreRestrictiveTemplateTerms = false;
	}

	@Override
	public boolean isMoreRestrictiveTemplateTermsThanCC() {
		return moreRestrictiveTemplateTerms;
	}
	
	public boolean checkConsistency(AbstractDocument d, ChocoAnalyzer an){
		ConsistencyOperation op = new ChocoConsistencyOp();
		op.addDocument(d);
		an.analyze(op);
		boolean result = op.isConsistent(); 
		return result;
	}
	
	/*public boolean checkCompliance(AbstractDocument t, AbstractDocument o, ChocoAnalyzer an){
		ComplianceOperation op = new ChocoComplianceOp();
		op.addDocument(t);
		op.addDocument(o);
		an.analyze(op);
		boolean result = op.isCompliant();
		return result;
	}*/

	@Override
	public void execute(ChocoAnalyzer an) {
		
		moreRestrictiveTemplateTerms = false;
		// debe haber un documento que es la plantilla a comprobar

		if (!docs.isEmpty()) {
			Template t;
			AbstractDocument doc = docs.get(0);
			if (doc instanceof Template) {
				// nos aseguramos que el documento sea una plantilla
				t = (Template) doc;
				if (checkConsistency(t,an)){
					//System.out.println("La plantilla es consistente");
				// para esta operaci�n debemos asegurarnos de que la plantilla sea consistente
						ChocoAlternativeDocumentsOp adop = new ChocoAlternativeDocumentsOp();
						adop.addDocument(doc);
						an.analyze(adop);
						Collection<AbstractDocument> alts = adop
								.getAlternativeDocuments();

						Iterator<AbstractDocument> it = alts.iterator();
						
						boolean b = false; 
						// revisar la condici�n de !b... pq..
						// �con que haya una more restrictive ya devolvemos TRUE?
						// es decir, creo que TODAS las vistas deben devolver true... (REVISAR)
						while (it.hasNext() && !b) {
							// por cada alternative document de la plantilla
							AbstractDocument d1 = it.next();
							//Map<ServiceScope,AbstractDocument> templateviews = super.getViews(d1,an);
							
							Collection<AbstractDocument> templateviews = super.getViews(d1, an).values();
							Iterator<AbstractDocument> itViews = templateviews.iterator();
							
							while (itViews.hasNext() && !b){
								AbstractAgreementDocument aux = (AbstractAgreementDocument) itViews.next();
								ChocoComplianceTranslator trans = new ChocoComplianceTranslator(aux);
								//trans.translate();
								//ChocoTranslator trans = new ChocoTranslator(aux);
								
								/*
								// necesito todas las constraints de la plantilla
								Map<AgreementElement, Constraint> tempCons = trans
										.getChocoConstraints();
								Map<String, Variable> tempVars = trans.getChocoVars();
								// para evitar el nullPointerException
								Collection<Constraint> auxToArray = new LinkedList<Constraint>(
										tempCons.values());
								auxToArray.add(Choco.TRUE);
								// tempCons.put("_TrueAuxConstraint", Choco.TRUE);
								Constraint[] templateAux = auxToArray
										.toArray(new Constraint[0]);
								Constraint template = Choco.and(templateAux);
								*/
								
								// XXX no considerar los sdt's de forma distinta
								//Collection<Constraint> sdtConstraints = trans.getSdtConstraints();
								Collection<Constraint> gtConstraints = trans
										.getGtConstraints();
								
								//int termsArraySize = gtConstraints.size() + sdtConstraints.size();
								int termsArraySize = gtConstraints.size();
								Constraint[] termsConstraintsarray; 
								if (termsArraySize == 0){
									//para que al final tengamos algun tipo de restricion si no hay nada
									termsConstraintsarray = new Constraint[1];
									termsConstraintsarray[0] = Choco.TRUE;
								}
								else{
									termsConstraintsarray = new Constraint[termsArraySize];
									Iterator<Constraint> it1 = gtConstraints
											.iterator();
									int i = 0;
									while (it1.hasNext()) {
										Constraint c = it1.next();
										termsConstraintsarray[i] = c;
										i++;
									}
									/*
									it1 = sdtConstraints.iterator();
									while (it1.hasNext()) {
										Constraint c = it1.next();
										termsConstraintsarray[i] = c;
										i++;
									}*/
								}
								
								// todas las constraints de SDTs y GTs
								Constraint templateTermsConstraint = Choco.and(termsConstraintsarray);
								
								
								Collection<Constraint> ccConstraints = trans
										.getCcConstraints();
								
								Constraint[] ccConstraintsarray; 
								if (ccConstraints.size() == 0){
									//para que al final tengamos algun tipo de restricion si no hay nada
									ccConstraintsarray = new Constraint[1];
									ccConstraintsarray[0] = Choco.TRUE;
								}
								else{
									ccConstraintsarray = new Constraint[ccConstraints.size()];
									Iterator<Constraint> it2 = ccConstraints
											.iterator();
									int i = 0;
									while (it2.hasNext()) {
										Constraint c = it2.next();
										ccConstraintsarray[i] = c;
										i++;
									};
								}
								
								// todas las constraints de las CCs
								Constraint templateCcConstraint = Choco.and(ccConstraintsarray);
								
								// XXX el primer par�metro debe ser el equivalente a la template en el compliance 
								// y el segundo debe ser el equivalente a la oferta en el compliance
								// para reutilizar la operaci�n isCompliantConstraint de cara a
								// comprobar si los t�rminos de la plantilla son m�s restrictivos
								// o no que las CCs de la plantilla
								b = Utils.isCompliantConstraint(templateTermsConstraint, templateCcConstraint);
							}
							moreRestrictiveTemplateTerms = b;			
						}
				} 
				else {
					moreRestrictiveTemplateTerms = false;
				}
			}
		}
	}
								
								

	public boolean validScopes(AgreementOffer o, Template t) {
		// XXX recorre la oferta para ver si todos los scopes
		// tienen su correspondencia en la plantilla
		Collection<Term> offerTerms = o.getAllTerms();
		Collection<ServiceScope> offerScopes = new HashSet<ServiceScope>();
		for (Term term:offerTerms){
			if (term instanceof GuaranteeTerm){
				GuaranteeTerm gt = (GuaranteeTerm) term;
				Set<ServiceScope> aux = gt.getScopes();
				for (ServiceScope ss:aux){
					offerScopes.add(ss);
				}
			}
		}
		
		Collection<Term> tempTerms = o.getAllTerms();
		Collection<ServiceScope> tempScopes = new HashSet<ServiceScope>();
		for (Term term:tempTerms){
			if (term instanceof GuaranteeTerm){
				GuaranteeTerm gt = (GuaranteeTerm) term;
				Set<ServiceScope> aux = gt.getScopes();
				for (ServiceScope ss:aux){
					tempScopes.add(ss);
				}
			}
		}
		
		return tempScopes.containsAll(offerScopes);
	}

	public Map<AbstractDocument,AbstractDocument> matchViews(Map<ServiceScope,AbstractDocument> offerViews,
						Map<ServiceScope,AbstractDocument> templateViews){
		
		Map<AbstractDocument,AbstractDocument> res = new HashMap<AbstractDocument, AbstractDocument>();
		Set<Entry<ServiceScope, AbstractDocument>> set1 = offerViews.entrySet();
		for (Entry<ServiceScope, AbstractDocument> e1:set1){
			AbstractDocument aux = templateViews.get(e1.getKey());
			if (aux != null){
				res.put(e1.getValue(), aux);
			}
			else{
				//scope declarado en la oferta que no existe en la plantilla!!
				//error!!!!
				//de todos modos, esto ya se habria detectado mediante
				//una comprobacion previa
			}
		}
		return res;
		
	}
	
}
