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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import choco.Choco;
import choco.kernel.model.constraints.Constraint;
import choco.kernel.model.variables.Variable;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.choco.ChocoAnalyzer;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.choco.ChocoOperation;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.choco.translators.ChocoComplianceTranslator;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.choco.translators.ChocoTranslator;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.choco.utils.Utils;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.document.AbstractDocument;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.document.AgreementElement;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.errors.AgreementError;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.errors.Explanation;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.operations.ExplainMoreRestrictiveOfferOperation;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.operations.ExplainNonComplianceOperation;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.wsag10.AgreementOffer;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.wsag10.Context;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.wsag10.GeneralConstraint;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.wsag10.GuaranteeTerm;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.wsag10.OfferItem;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.wsag10.ServiceScope;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.wsag10.Template;
import es.us.isa.ideas.adaintegration.adaadhoc.ada.wsag10.Term;

public class ChocoExplainMoreRestrictiveOfferOp extends ChocoOperation implements ExplainMoreRestrictiveOfferOperation {

	protected int level;

	protected Map<AgreementError, Explanation> explanations;
	
	/**
	 * Colecci�n que agrupa todas las restricciones de la plantilla
	 */
	private Collection<Constraint> allTemplateConstraints;
	
	/**
	 * Colecci�n que agrupa todas las restricciones de la oferta
	 */
	private Collection<Constraint> allOfferConstraints;
	
	/**
	 * Colecci�n de restricciones que pertenecen a la plantilla, excepto las que son conflictivas
	 */
	private Collection<Constraint> compliantTemplateConstraints;
	
	/**
	 * Colecci�n de restricciones que pertenecen a la oferta, excepto las que son conflictivas
	 */
	private Collection<Constraint> compliantOfferConstraints;

	@Override
	public Map<AgreementError, Explanation> explainErrors() {
		return explanations;
	}

	@Override
	public void execute(ChocoAnalyzer choco) {
		// IGUAL QUE EN QUICKXPLAIN PERO LLAMANDAO A LA OPERACI�N DE ISLESSRESTRICTIVEOFFER EN LUGAR DE AL COMPLIANCE 
		// 1� descomponer plantilla y oferta en sus
		// alternate documents

		// 2� comprobar si la oferta es compliant con la plantilla

		// 3� si no es compliant, agrupar las restricciones en conjuntos
		// de terminos por la misma variable

		// 4� por cada conjunto, eliminarlo del total,
		// 4�a si el total
		// es compliant, convertir el total en el conjunto eliminado
		// 4�b si el total no es compliant, probar con otro conjunto

		// 5� si hemos agotado todos los grupos y el total sigue sin ser
		// compliant, tomar agrupar las restricciones por conjuntos de
		// k + 1 variables

		explanations = new HashMap<AgreementError, Explanation>();
		int size = this.docs.size();
		if (size == 2) {
			// consideramos el primer documento como la plantilla, y
			// el segundo como la ofertaç

			Template t;
			AgreementOffer o;
			AbstractDocument doc1 = docs.get(0), doc2 = docs.get(1);
			if (doc1 instanceof Template && doc2 instanceof AgreementOffer) {
				// nos aseguramos que el primer documento sea una plantilla
				// y el segundo una oferta
				t = (Template) doc1;
				o = (AgreementOffer) doc2;
				Context context = o.getContext();
				String tName = t.getName(), tId = t.getId();
				String refName = context.getTemplateName(), refId = context.getTemplateId();

				if (!tName.equalsIgnoreCase(refName) || !tId.equalsIgnoreCase(refId) || !validScopes(o, t)) {

					AgreementError error = new AgreementError(Utils.single2Collection((AgreementElement) context));
					Explanation exp = new Explanation(new LinkedList<AgreementElement>());

					explanations.put(error, exp);
				}
				// 1� descomponer plantilla y oferta en sus
				// alternate documents
				Collection<AbstractDocument> altsTemplate = getAlternatives(t, choco, false);
				Collection<AbstractDocument> altsOffer = getAlternatives(o, choco, false);
				Iterator<AbstractDocument> itTemplate = altsTemplate.iterator();
				while (itTemplate.hasNext()) {

					Template template = (Template) itTemplate.next();
					Iterator<AbstractDocument> itOffer = altsOffer.iterator();

					while (itOffer.hasNext()) {
						AgreementOffer offer = (AgreementOffer) itOffer.next();

						Map<ServiceScope, AbstractDocument> offerViews = getViews(offer, choco);
						Map<ServiceScope, AbstractDocument> tempViews = getViews(template, choco);
						Map<AbstractDocument, AbstractDocument> matchedViews = matchViews(offerViews, tempViews);
						Set<Entry<AbstractDocument, AbstractDocument>> entries = matchedViews.entrySet();

						// TODO HAY QUE MODIFICAR ESTO PARA QUE COMPLIANCE RULE
						// BIEN
						for (Entry<AbstractDocument, AbstractDocument> e : entries) {
							AgreementOffer viewOffer = (AgreementOffer) e.getKey();
							Template viewTemp = (Template) e.getValue();

							ChocoLessRestrictiveOfferComplianceOp lessRestrictiveComplianceOp = new ChocoLessRestrictiveOfferComplianceOp();
							lessRestrictiveComplianceOp.addDocument(viewTemp);
							lessRestrictiveComplianceOp.addDocument(viewOffer);
							lessRestrictiveComplianceOp.execute(choco);
							boolean compliant = lessRestrictiveComplianceOp.isLessRestrictiveOffer();
							// 2� comprobar si la oferta es compliant con la
							// plantilla

							if (!compliant) {
								// 3� si no es compliant, agrupar las
								// restricciones
								// en conjuntos
								// de terminos por la misma variable
								ChocoTranslator trans1 = new ChocoTranslator(viewTemp);
								trans1.translate();

								ChocoComplianceTranslator trans2 = new ChocoComplianceTranslator(viewOffer, trans1.getChocoVars(), trans1.getServicePropsMap());
								trans2.translate();

								Collection<Variable> chocoVars = trans2.getChocoVars().values();
								Map<AgreementElement, Constraint> templateConstraints = trans1.getChocoConstraints();
								Map<AgreementElement, Constraint> offerConstraints = trans2.getChocoConstraints();
								//Map<AgreementElement, Constraint> offerConstraints = trans2.getChocoConstraintsWithoutSDT();
								Map<AgreementElement, Constraint> offerConstraintsWithoutSDT = new HashMap<AgreementElement, Constraint>();

								// TENGO QUE QUITAR LAS RESTRICCIONES DE LAS SDTs
								Set<Entry<AgreementElement, Constraint>> offerConstraintsEntries = offerConstraints.entrySet();
								for (Iterator iterator = offerConstraintsEntries.iterator(); iterator.hasNext();) {
									Entry<AgreementElement, Constraint> entry = (Entry<AgreementElement, Constraint>) iterator.next();
									AgreementElement agreementElement = entry.getKey();
									if (agreementElement instanceof GeneralConstraint || agreementElement instanceof GuaranteeTerm) {			
										offerConstraintsWithoutSDT.put(agreementElement, entry.getValue());
									}
								}
								offerConstraints = offerConstraintsWithoutSDT;
								
								
								Collection<Variable> usedVars = extractUsedVars(chocoVars, offerConstraints);
								// antes de buscar los conflictos guardamos las restricciones de plantilla y oferta
								// para despu�s poder refinar los conflictos
								allTemplateConstraints = templateConstraints.values();
								allOfferConstraints = offerConstraints.values();
								
								Map<AgreementError, Explanation> res = quickxplain(usedVars, offerConstraints, templateConstraints);
								if (level == REFINE_ALL) {
									res = refineAgreementErrors(res, offerConstraints, trans1.getChocoConstraints());
									res = refineExplanations(res, offerConstraints, trans1.getChocoConstraints());
								} else {
									if (level == REFINE_OFFER) {
										res = refineAgreementErrors(res, offerConstraints, trans1.getChocoConstraints());
									} else if (level == REFINE_TEMPLATE) {
										res = refineExplanations(res, offerConstraints, trans1.getChocoConstraints());
									}
								}
//								// Recorremos el resultado y si alg�n
//								// AgreementElement es un
//								// OfferItem, le ponemos como nombre
//								// "SDTName-OfferItemName"
//								for (AgreementError agError : res.keySet()) {
//									for (AgreementElement agElem : agError.getElements()) {
//										if (agElem instanceof OfferItem) {
//											agElem.setName(agElem.toString());
//										}
//										for (AgreementElement agElemExp : res.get(agError).getElements()) {
//											if (agElemExp instanceof OfferItem) {
//												agElemExp.setName(agElemExp.toString());
//											}
//										}
//									}
//								}
								explanations.putAll(res);
							}
						}
					}
				}
			}else{
				System.err.println("The first document must be a template and the second one must be an offer");
			}
		}
	}

	// XXX reutilizable
	protected Collection<Variable> extractUsedVars(Collection<Variable> chocoVars, Map<AgreementElement, Constraint> offerConstraints) {

		Collection<Variable> res = new HashSet<Variable>();
		Collection<Constraint> constraints = offerConstraints.values();
		for (Constraint c : constraints) {
			/*
			Iterator<Variable> it = c.getVariableIterator();
			while (it.hasNext()) {
				Variable v = it.next();
				if (chocoVars.contains(v) && !res.contains(v)) {
					res.add(v);
				}
			}*/
			Variable[] vars = c.getVariables();
			//System.out.println(vars.toString());
			for (int i = 0; i < vars.length; i++) {
				Variable variable = vars[i];
				if (chocoVars.contains(variable) && !res.contains(variable)) {
					res.add(variable);
			}				
			}
		}

		return res;
	}

	// XXX reutilizable
	protected Map<AgreementError, Explanation> quickxplain(Collection<Variable> chocoVars, Map<AgreementElement, Constraint> offerConstraints,
			Map<AgreementElement, Constraint> templateConstraints) {
		/*
		 * problema de esta solucion: se devuelve una unica solucion Si por
		 * ejemplo tenemos 2 restricciones en la oferta que, independientemente,
		 * chocan con 2 restricciones de la plantilla, con este algoritmo se nos
		 * devolvera el conjunto de las 2 de la oferta con las 2 de la
		 * plantilla. Lo ideal seria que se nos devolviera por un lado una de la
		 * oferta con la que choca de la plantilla, y por el otro lado la otra.
		 */
		Map<AgreementError, Explanation> res = new HashMap<AgreementError, Explanation>();

		int varsSize = chocoVars.size();
		boolean b = false;
		for (int i = 1; i < varsSize && !b; i++) {
			Map<Collection<Variable>, Map<AgreementElement, Constraint>> offerGroups = groupConstraints(chocoVars, offerConstraints, i);
			Map<Collection<Variable>, Map<AgreementElement, Constraint>> templateGroups = groupConstraints(chocoVars, templateConstraints, i);
			Set<Entry<Collection<Variable>, Map<AgreementElement, Constraint>>> entries = offerGroups.entrySet();
			for (Entry<Collection<Variable>, Map<AgreementElement, Constraint>> e : entries) {

				Collection<Constraint> offerConstraintsSet = e.getValue().values();
				Map<AgreementElement, Constraint> templateConstraintsMap = templateGroups.get(e.getKey());
				Collection<Constraint> templateConstraintsSet = templateConstraintsMap.values();

				// comprobamos si eliminando ambos conjuntos de oferta y
				// plantilla
				// esto es compliance
				Collection<Constraint> newTemplateSet = new LinkedList<Constraint>(templateConstraints.values());
				Collection<Constraint> newOfferSet = new LinkedList<Constraint>(offerConstraints.values());
				newTemplateSet.removeAll(templateConstraintsSet);
				newOfferSet.removeAll(offerConstraintsSet);

				Constraint[] offerArray = newOfferSet.toArray(new Constraint[0]);
				Constraint offerAnd = Choco.and(offerArray);
				Constraint[] templateArray = newTemplateSet.toArray(new Constraint[0]);
				Constraint templateAnd = Choco.and(templateArray);
				// ACABO DE CAMBIAR EL ORDEN DE LOS PAR�METROS PARA ESTUDIAR SI LOS DE
				// LA OFERTA SON M�S RESTRICTIVOS QUE LOS DE LA PLANTILLA O NO
				b = Utils.isCompliantConstraint(templateAnd, offerAnd);
				if (b) {
					// si es compliant, el problema esta en los conjuntos
					// eliminados. Guardamos los conjuntos sin restricciones
					// conflictivas para despu�s poder refinar el resultado
					compliantTemplateConstraints = newTemplateSet;
					compliantOfferConstraints = newOfferSet;
					res = quickxplain(e.getKey(), e.getValue(), templateConstraintsMap);
					// una vez encontramos un subconjunto problematico,
					// en ese conjunto habria algo salvable? o todas
					// las restricciones serian erroneas?
					break;
				}
				// 4�b si el total no es compliant, probar con otro conjunto
			}
		}

		if (!b) {			
			// si hemos encontrado un subconjunto que agrupe los errores
			// separamos por variables
			Map<Collection<Variable>, Map<AgreementElement, Constraint>> offerGroups = groupConstraints(chocoVars, offerConstraints, 1);
			Map<Collection<Variable>, Map<AgreementElement, Constraint>> templateGroups = groupConstraints(chocoVars, templateConstraints, 1);
			Set<Entry<Collection<Variable>, Map<AgreementElement, Constraint>>> entries = offerGroups.entrySet();
			for (Entry<Collection<Variable>, Map<AgreementElement, Constraint>> e : entries) {
				Map<AgreementElement, Constraint> templateConstraintsMap = templateGroups.get(e.getKey());
				// Antes de meter los conflictos, vemos si hay alguna restricci�n que no falle
				refineConflicts(e.getValue().values(), templateConstraintsMap.values());
				
//				Constraint[] offerConsts = e.getValue().values().toArray(new Constraint[0]);
//				Constraint offerConst = Choco.and(offerConsts);
//				Iterator<Constraint> it = templateConstraintsMap.values().iterator();
//				while(it.hasNext()){
//					Constraint c = it.next();
//					if(Utils.isCompliantConstraint(offerConst, c)){
//						it.remove();
//					}
//				}
				
				AgreementError error = new AgreementError(e.getValue().keySet());
				Explanation exp = new Explanation(templateConstraintsMap.keySet());
				res.put(error, exp);
			}
		}

		return res;

	}
	
	/**
	 * Elimina restricciones que se consideren conflictivas err�neamente por estar en el
	 * mismo conjunto que otra restricci�n conflictiva. 
	 * @param offerConstraints Colecci�n de restricciones conflictivas en la oferta
	 * @param templateConstraints Colecci�n de restricciones conflictivas en la plantilla
	 */
	private void refineConflicts(Collection<Constraint> offerConstraints, Collection<Constraint> templateConstraints){
		if(offerConstraints.size() > 1){
			// si hay menos de dos restricciones no hace falta que comprobemos
			// porque al meter la �nica restricci�n conflictiva en la oferta
			// tendr�amos la oferta original y que ya ha sido comprobada su
			// conformidad con la plantilla
			Iterator<Constraint> itOfferConstraints = offerConstraints.iterator();
			while(itOfferConstraints.hasNext()){
				Constraint offConst = itOfferConstraints.next();
				// a�adimos la restricci�n a las restricciones no conflictivas de la oferta
				compliantOfferConstraints.add(offConst);
				// creamos una �nica restricci�n que une las no conflictivas de la oferta junto a la que hemos a�adido
				Constraint[] arrayOfferConsts = compliantOfferConstraints.toArray(new Constraint[0]);
				Constraint offerConsts = Choco.and(arrayOfferConsts);
				// cremos a una �nica restricci�n que une todas las restricciones de la plantilla
				Constraint[] arrayTemplateConsts = allTemplateConstraints.toArray(new Constraint[0]);
				Constraint tempConsts = Choco.and(arrayTemplateConsts);
				
				if(Utils.isCompliantConstraint(offerConsts, tempConsts)){
					// si son compliant quiere decir que la constraint de la oferta
					// que hemos a�adido no es conflictiva y podemos borrarla del
					// resultado
					itOfferConstraints.remove();
				}
				// una vez terminado dejamos las restricciones no conflictivas de la plantilla
				// en su estado inicial
				compliantOfferConstraints.remove(offConst);
			}
		}
		
		// refinamos la plantilla
		if(templateConstraints.size() > 1){
			// si hay menos de dos restricciones no hace falta que comprobemos
			// porque al meter la �nica restricci�n conflictiva en la plantilla
			// tendr�amos la plantilla original y que ya ha sido comprobada su
			// conformidad con la oferta
			Iterator<Constraint> itTemplateConstraints = templateConstraints.iterator();
			while(itTemplateConstraints.hasNext()){
				Constraint tempConst = itTemplateConstraints.next();
				// a�adimos la restricci�n a las restricciones no conflictivas de la plantilla
				compliantTemplateConstraints.add(tempConst);
				// creamos una �nica restricci�n que une las no conflictivas de la plantilla junto a la que hemos a�adido
				Constraint[] arrayTemplateConsts = compliantTemplateConstraints.toArray(new Constraint[0]);
				Constraint templateConsts = Choco.and(arrayTemplateConsts);
				// creamos una �nica restricci�n que une todas las restricciones de la oferta
				Constraint[] arrayOfferConsts = allOfferConstraints.toArray(new Constraint[0]);
				Constraint offerConsts = Choco.and(arrayOfferConsts);
				
				if(Utils.isCompliantConstraint(offerConsts, templateConsts)){
					// si son compliant borramos la restricci�n de los conflictos de la plantilla
					itTemplateConstraints.remove();
				}
				// una vez terminado dejamos las restricciones no conflictivas de la plantilla
				// en su estado inicial
				compliantTemplateConstraints.remove(tempConst);
			}
		}
	}

	// XXX reutilizable
	protected Map<AgreementError, Explanation> refineAgreementErrors(Map<AgreementError, Explanation> exps, Map<AgreementElement, Constraint> offerConstraints,
			Map<AgreementElement, Constraint> templateConstraints) {

		Set<Entry<AgreementError, Explanation>> entries = exps.entrySet();
		for (Entry<AgreementError, Explanation> entry : entries) {
			AgreementError ae = entry.getKey();
			if (ae.getElements().size() > 1) {
				// XXX atencion a esta linea porque puede petar
				// probar en una clase dummy un casting de este tipo
				Collection<AgreementElement> elems = ae.getElements();

				Collection<AgreementElement> expElems = entry.getValue().getElements();
				Constraint[] templateArray = new Constraint[expElems.size()];
				int i = 0;
				for (AgreementElement e : expElems) {
					templateArray[i] = templateConstraints.get(e);
					i++;
				}
				Constraint templateAnd = Choco.and(templateArray);

				Collection<Collection<AgreementElement>> col = combineElements(elems, elems.size());
				for (Collection<AgreementElement> subcol : col) {
					// por cada combinacion
					// eliminamos sus restricciones del problema
					// y si es compliant lo que queda
					// ya hemos encontrado el subconjunto minimo
					Collection<Constraint> constraints = new LinkedList<Constraint>();
					for (AgreementElement elem : elems) {
						// tomamos todas las restricciones que no estan en la
						// combinacion
						if (!subcol.contains(elem)) {
							Constraint aux = offerConstraints.get(elem);
							constraints.add(aux);
						}
					}
					// y si lo que queda de oferta es compliant con el trozo de
					// plantilla
					// el subconjunto que hemos eliminao es el conjunto minimo
					Constraint[] offerArray = constraints.toArray(new Constraint[0]);
					Constraint offerAnd = Choco.and(offerArray);
					boolean compliant = Utils.isCompliantConstraint(offerAnd, templateAnd);
					if (compliant) {
						ae.setElements(subcol);
						break;
					}
				}
			}
		}
		return exps;

	}

	// XXX reutilizable
	protected Map<AgreementError, Explanation> refineExplanations(Map<AgreementError, Explanation> exps, Map<AgreementElement, Constraint> offerConstraints,
			Map<AgreementElement, Constraint> templateConstraints) {

		Set<Entry<AgreementError, Explanation>> entries = exps.entrySet();
		for (Entry<AgreementError, Explanation> entry : entries) {
			Explanation ex = entry.getValue();
			if (ex.getElements().size() > 1) {
				Collection<AgreementElement> elems = ex.getElements();

				Collection<AgreementElement> errorsElems = entry.getKey().getElements();
				Constraint[] offerArray = new Constraint[errorsElems.size()];
				int i = 0;
				for (AgreementElement e : errorsElems) {
					offerArray[i] = offerConstraints.get(e);
					i++;
				}
				Constraint offerAnd = Choco.and(offerArray);

				Collection<Collection<AgreementElement>> col = combineElements(elems, elems.size());
				for (Collection<AgreementElement> subcol : col) {
					// por cada combinacion
					// eliminamos sus restricciones del problema
					// y si es compliant lo que queda
					// ya hemos encontrado el subconjunto minimo
					Collection<Constraint> constraints = new LinkedList<Constraint>();
					for (AgreementElement elem : elems) {
						// tomamos todas las restricciones que no estan en la
						// combinacion
						if (!subcol.contains(elem)) {
							Constraint aux = offerConstraints.get(elem);
							constraints.add(aux);
						}
					}
					// y si lo que queda de oferta es compliant con el trozo de
					// plantilla
					// el subconjunto que hemos eliminao es el conjunto minimo
					Constraint[] templateArray = constraints.toArray(new Constraint[0]);
					Constraint templateAnd = Choco.and(templateArray);
					boolean compliant = Utils.isCompliantConstraint(offerAnd, templateAnd);
					if (compliant) {
						ex.setElements(subcol);
						break;
					}
				}
			}
		}
		return exps;

	}

	// XXX reutilizable
	protected Map<Collection<Variable>, Map<AgreementElement, Constraint>> groupConstraints(Collection<Variable> vars,
			Map<AgreementElement, Constraint> constraints, int elems) {

		Map<Collection<Variable>, Map<AgreementElement, Constraint>> res = new HashMap<Collection<Variable>, Map<AgreementElement, Constraint>>();

		Collection<Collection<Variable>> combinations = combineElements(vars, elems);

		for (Collection<Variable> comb : combinations) {
			Set<Entry<AgreementElement, Constraint>> entries = constraints.entrySet();
			Map<AgreementElement, Constraint> aux = new HashMap<AgreementElement, Constraint>();
			for (Entry<AgreementElement, Constraint> entry : entries) {
				for (Variable v : comb) {
					if (constraintContainsVariable(entry.getValue(), v)) {
						// meter en el resultado, y salir del primer bucle
						aux.put(entry.getKey(), entry.getValue());
						break;
					}
				}
			}
			res.put(comb, aux);
		}

		return res;
	}

	protected boolean constraintContainsVariable(Constraint c, Variable v) {
		boolean found = false;
		/*Iterator<Variable> it = c.getVariableIterator();
		while (it.hasNext() && !found) {
			if (v.equals(it.next())) {
				found = true;
			}
		}*/
		Variable[] vars = c.getVariables();
		//System.out.println(vars.toString());
		for (int i = 0; i < vars.length; i++) {
			Variable variable = vars[i];
			if (v.equals(variable)) {
				found = true;
			}
			}
		return found;
	}

	protected <T> Collection<Collection<T>> combineElements(Collection<T> vars, int elems) {

		Collection<Collection<T>> res = new LinkedList<Collection<T>>();

		int[] indexes = new int[vars.size()];
		for (int i = 0; i < indexes.length; i++) {
			indexes[i] = i;
		}
		List<T> l = new ArrayList<T>();
		l.addAll(vars);

		Collection<int[]> combs = comb(elems, indexes);
		for (int[] aux : combs) {
			Collection<T> col = new LinkedList<T>();
			for (int i = 0; i < aux.length; i++) {
				T v = l.get(aux[i]);
				col.add(v);
			}
			res.add(col);
		}

		return res;
	}

	protected Collection<int[]> comb(int elems, int... items) {
		return kcomb(items, 0, elems, new int[elems]);
	}

	protected Collection<int[]> kcomb(int[] items, int n, int k, int[] arr) {
		Collection<int[]> res = new LinkedList<int[]>();
		if (k == 0) {
			int[] aux = Arrays.copyOf(arr, arr.length);
			res.add(aux);
		} else {
			for (int i = n; i <= items.length - k; i++) {
				arr[arr.length - k] = items[i];
				Collection<int[]> aux = kcomb(items, i + 1, k - 1, arr);
				res.addAll(aux);
			}
		}
		return res;
	}

	@Override
	public void setExplanationLevel(int level) {
		this.level = level;
	}

	private Map<AbstractDocument, AbstractDocument> matchViews(Map<ServiceScope, AbstractDocument> offerViews, 
			Map<ServiceScope, AbstractDocument> templateViews) {

		Map<AbstractDocument, AbstractDocument> res = new HashMap<AbstractDocument, AbstractDocument>();
		Set<Entry<ServiceScope, AbstractDocument>> set1 = offerViews.entrySet();
		for (Entry<ServiceScope, AbstractDocument> e1 : set1) {
			AbstractDocument aux = templateViews.get(e1.getKey());
			if (aux != null) {
				res.put(e1.getValue(), aux);
			} else {
				// scope declarado en la oferta que no existe en la plantilla!!
				// error!!!!
				// de todos modos, esto ya se habria detectado mediante
				// una comprobacion previa
			}
		}
		return res;

	}

	private boolean validScopes(AgreementOffer o, Template t) {
		// XXX recorre la oferta para ver si todos los scopes
		// tienen su correspondencia en la plantilla
		Collection<Term> offerTerms = o.getAllTerms();
		Collection<ServiceScope> offerScopes = new HashSet<ServiceScope>();
		for (Term term : offerTerms) {
			if (term instanceof GuaranteeTerm) {
				GuaranteeTerm gt = (GuaranteeTerm) term;
				Set<ServiceScope> aux = gt.getScopes();
				for (ServiceScope ss : aux) {
					offerScopes.add(ss);
				}
			}
		}

		Collection<Term> tempTerms = o.getAllTerms();
		Collection<ServiceScope> tempScopes = new HashSet<ServiceScope>();
		for (Term term : tempTerms) {
			if (term instanceof GuaranteeTerm) {
				GuaranteeTerm gt = (GuaranteeTerm) term;
				Set<ServiceScope> aux = gt.getScopes();
				for (ServiceScope ss : aux) {
					tempScopes.add(ss);
				}
			}
		}

		return tempScopes.containsAll(offerScopes);
	}

}
