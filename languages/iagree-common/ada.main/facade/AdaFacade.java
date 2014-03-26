package facade;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import es.us.isa.ada.ADA;
import es.us.isa.ada.document.AgreementElement;
import es.us.isa.ada.errors.AgreementError;
import es.us.isa.ada.errors.Explanation;
import es.us.isa.ada.exceptions.BadSyntaxException;
import es.us.isa.ada.loaders.ExtensionsLoader;
import es.us.isa.ada.loaders.ReflexionExtensionsLoader;
import es.us.isa.ada.service.ADAServiceImpl;
import es.us.isa.ada.service.ADAServiceV2;
import es.us.isa.ada.wsag10.Term;


//La responsabilidad de la clase debería incluir al menos:
//*Instanciado de ADA.
//*Lógica para la externalización de los mensajes.
//*Construcción de las respuestas y errores.
//*Gestión de errores internos.
public class AdaFacade {
	
	//instanciado de ADA
	private ExtensionsLoader extLoad;
	private ADA ada;
	private ADAServiceV2 service;
	
	public AdaFacade(){
		this.extLoad = new ReflexionExtensionsLoader();
		this.ada = new ADA(extLoad);
		this.service = new ADAServiceImpl(ada);
	}
	
	//Devuelve true si es consistente y false en caso contrario
	public boolean consistency(String doc){
		byte[] bdoc = doc.getBytes();
		boolean res = false;
		if (service.checkDocumentConsistency(bdoc)){	
			if (service.getDeadTerms(bdoc).size()==0){
				if (service.getLudicrousTerms(bdoc).size()==0){
					res = true;
				}
			}
		}
		return res;
	}
	
	//Devuelve el numero del alternativas del documento
	public int getNumberOfAlternatives(String doc){
		return service.getNumberOfAlternatives(doc.getBytes());
	}
	
	//Devuelve una explicación de las inconsistencias
	public Map<AgreementElement, Collection<AgreementElement>> explainInconsistencies(String doc){
		return service.explainInconsistencies(doc.getBytes());
	}
	
	//Compara si la plantilla y la oferta son igual de restrictivas
	public boolean isCompliant(String template, String offer){
		boolean isCompliant = false;
		if (service.isCompliant(template.getBytes(), offer.getBytes())){
			if (service.isLessRestrictiveOffer(template.getBytes(), offer.getBytes())){
				if (service.isMoreRestrictiveTemplateTermsThanCC(template.getBytes())){
					isCompliant = true;
				}
			}	
		}
		return isCompliant;
	}
	
	//Cargar metricas
	public void uploadMetrics(String metricsName, String metrics){
		service.addMetricFile(metrics.getBytes(), (metricsName+".xml").getBytes());
	}
	
	//Explicacion de la inconsistencia
	public String inconsitencyExplaining(String wsag) {
		
		Object[] result = new Object[3];
		//1. isConsistent
		//2. hasWarnings
		//3. inconsistencias si las hay, warnings si no hay inconsistencias
		List<String> warningsNames = new ArrayList<String>();
		List<String> errorsNames = new ArrayList<String>();
		Integer conflictsShowedInConsole = 1;
		Integer deadTermsShowedInConsole = 1;
		Integer ludicrousTermsShowedInConsole = 1;
		
		String returnMsg = "";
		
		Collection<Collection<String>> explaining = new LinkedList<Collection<String>>();
		try{
			boolean isConsistent;
			boolean hasWarnings = false;
			
			//Checking Inconsistent Terms
			isConsistent = service.checkDocumentConsistency(wsag.getBytes());
			
			if(isConsistent){
				returnMsg += "The document doesn't contain Inconsistent Terms.";
				
				//Checking Dead Terms
				Collection<Term> deadTerms = service.getDeadTerms(wsag.getBytes());

				if(deadTerms.size() > 0){
					returnMsg += "The document contains the following Dead Terms:";
					
					//Explaining Dead Terms
					Map<Term,Collection<AgreementElement>> deadTermsExplanation = service.explainDeadTerms(wsag.getBytes(), deadTerms);
					for(Map.Entry<Term,Collection<AgreementElement>> e: deadTermsExplanation.entrySet()){
							
						Collection<String> set = new LinkedList<String>();
						Collection<AgreementElement> termValues = e.getValue();
						String warn = e.getKey().getName();
						
						returnMsg += "Dead term "+deadTermsShowedInConsole+": "+warn;
						
						deadTermsShowedInConsole++;
						
						if(!warningsNames.contains(warn)){
							//metemos el warning
							set.add(warn);
							warningsNames.add(warn);
						}
						
						for (AgreementElement cause:termValues){
							//causas del warning
							returnMsg += "Cause: "+cause.getName();
							if (!warningsNames.contains(cause)){
								//y metemos sus causas
								set.add(cause.getName());
								warningsNames.add(cause.getName());
							}
						}
						if (!set.isEmpty()){
							explaining.add(set);
						}
					}
				}else{
					returnMsg += "The document doesn't contain Dead Terms.";
				}
				
				//Checking Ludicrous Terms
				Collection<Term> ludicrousTerms = service.getLudicrousTerms(wsag.getBytes());
				
				if(ludicrousTerms.size() > 0){
					returnMsg += "The document contains the following Conditionally Inconsistent Terms:";
					
					//Explaining Ludicrous Terms
					Map<Term,Collection<AgreementElement>> ludicrousTermsExplanation = service.explainLudicrousTerms(wsag.getBytes(), ludicrousTerms);
					for(Map.Entry<Term,Collection<AgreementElement>> e: ludicrousTermsExplanation.entrySet()){
						Collection<String> set = new LinkedList<String>();
						Collection<AgreementElement> termValues = e.getValue();
						
						String warn = e.getKey().getName();
						
						returnMsg += "Conditionally Inconsistent Term "+ludicrousTermsShowedInConsole+": "+warn;
						
						ludicrousTermsShowedInConsole++;
						if(!warningsNames.contains(warn)){
							//metemos el warning
							set.add(warn);
							warningsNames.add(warn);
						}
						
						for (AgreementElement cause:termValues){
							returnMsg += "Cause: "+cause.getName();
							//causas del warning
							if (!warningsNames.contains(cause)){
								//y metemos sus causas
								set.add(cause.getName());
								warningsNames.add(cause.getName());
							}
						}
						
						if (!set.isEmpty()){
							explaining.add(set);
						}
					}
					
				}else{
					returnMsg += "The document doesn't contain Conditionally Inconsistent Terms.";
				}
				hasWarnings = !(deadTerms.size() == 0 && ludicrousTerms.size() == 0);
				
			}else{
				returnMsg += "The document contains the following Inconsistent Terms:";
				
				//Explaining inconsistent terms
				Map<AgreementElement,Collection<AgreementElement>> errorsExplanation = service.explainInconsistencies(wsag.getBytes());
				for(Map.Entry<AgreementElement,Collection<AgreementElement>> e: errorsExplanation.entrySet()){

					Collection<String> set = new LinkedList<String>();
					
					String keyName = e.getKey().getName();
					returnMsg += "Conflict "+conflictsShowedInConsole+": "+keyName;
					conflictsShowedInConsole++;
					if (!errorsNames.contains(keyName)){
						set.add(keyName);
						errorsNames.add(keyName);
					}
					
					Collection<AgreementElement> termValues = e.getValue();
					for(AgreementElement ae: termValues){
						returnMsg += "Cause: "+ae.getName();
						if(!errorsNames.contains(ae.getName())){
							set.add(ae.getName());
							errorsNames.add(ae.getName());
						}
					}
					
					if (!set.isEmpty()){
						explaining.add(set);
					}
				}			
			}
			result[0] = isConsistent;
			result[1] = hasWarnings;
			result[2] = explaining;
		}catch(BadSyntaxException e){
			returnMsg += "There was an error: It may be due to a syntax error, please check the document syntax.";
			result = null;
		}
		
		return returnMsg;
	}
	
	//Explicacion de la inconsistencia
	public String nonComplianceExplaining(String template, String offer) {
		Object[] result = new Object[5];
		List<String> templateErrorsNames = new ArrayList<String>();
		List<String> offerErrorsNames = new ArrayList<String>();
		String returnMsg = "";
		
		Collection<Collection<String>> tempErrors = new LinkedList<Collection<String>>();
		Collection<Collection<String>> offerErrors = new LinkedList<Collection<String>>();
		try{
			boolean isTempConsistent;
			boolean isOfferConsistent;
			boolean isCompliant = false;
			boolean isLessRestrictiveOffer = false;
			boolean isMoreRestrictiveTemplateTerms = false;
			
			
			//Checking Template Inconsistent Terms
			
			isTempConsistent = service.checkDocumentConsistency(template.getBytes());
			if(isTempConsistent){
				returnMsg += "The template is valid.";
			}else{
				returnMsg += "The is NOT valid because it contains semantic errors.";
			}
			
			
			isOfferConsistent = service.checkDocumentConsistency(offer.getBytes());
			if(isOfferConsistent){
				returnMsg += "The offer is valid.";
			}else{
				returnMsg += "The offer is NOT valid because contains semantic errors.";
			}
			if(isTempConsistent && isOfferConsistent){
				
				isCompliant = service.isCompliant(template.getBytes(), offer.getBytes());
				isLessRestrictiveOffer = service.isLessRestrictiveOffer(template.getBytes(), offer.getBytes());
				isMoreRestrictiveTemplateTerms = service.isMoreRestrictiveTemplateTermsThanCC(template.getBytes());
				
				if(!isCompliant){
					returnMsg += "Conflicts between agreement offer terms and the template:";
					
					//Explaining Non-Compliance conflicts
					Map<AgreementError,Explanation> errorsArrayMapEntry = service.explainNonCompliance(template.getBytes(), offer.getBytes());
			        for(Map.Entry<AgreementError,Explanation> e: errorsArrayMapEntry.entrySet()){
			        	Collection<AgreementElement> agreementErrorElements = e.getKey().getElements();
			        	Collection<String> offerElems = new LinkedList<String>();
			        	for(AgreementElement agErrorElem: agreementErrorElements){
			        		String offerElemName = agErrorElem.getName();
			        		
			        		returnMsg += "Offer conflict: "+offerElemName;
			        		
			        		if (!offerErrorsNames.contains(offerElemName)){
				        		offerElems.add(offerElemName);
				        		offerErrorsNames.add(offerElemName);
				        	}
			        	}
			        	offerErrors.add(offerElems);
			        	
			        	Collection<AgreementElement> explanationElements = e.getValue().getElements();
			        	Collection<String> tempElems = new LinkedList<String>();
			        	for(AgreementElement explanationElem: explanationElements){
			        		String templateElemName = explanationElem.getName();
			        		
			        		returnMsg += "Template cause: "+templateElemName;
			        		
			        		if (!templateErrorsNames.contains(templateElemName)){
			        			tempElems.add(templateElemName);
			        			templateErrorsNames.add(templateElemName);
			        		}
			        	}
			        	tempErrors.add(tempElems);
			        }
				}else{
					returnMsg += "The agreement offer terms are compliant with the template terms and creation constraints.";
				}
				
				if((isCompliant)&&(!isLessRestrictiveOffer)){
					
					returnMsg += "Offer terms that are More Restrictive than the template terms:";
					
					//Explaining Non-Compliance for More Restrictive offer conflicts
					Map<AgreementError,Explanation> errorsArrayMapEntry = service.explainMoreRestrictiveOffer(template.getBytes(), offer.getBytes());
					for(Map.Entry<AgreementError,Explanation> e: errorsArrayMapEntry.entrySet()){
			        	Collection<AgreementElement> agreementErrorElements = e.getKey().getElements();
			        	Collection<String> offerElems = new LinkedList<String>();
			        	for(AgreementElement agErrorElem: agreementErrorElements){
			        		String offerElemName = agErrorElem.getName();
			        		
			        		returnMsg += "Offer term: "+offerElemName;
			        		
			        		if (!offerErrorsNames.contains(offerElemName)){
				        		offerElems.add(offerElemName);
				        		offerErrorsNames.add(offerElemName);
				        	}
			        	}
			        	offerErrors.add(offerElems);
			        	
			        	Collection<AgreementElement> explanationElements = e.getValue().getElements();
			        	Collection<String> tempElems = new LinkedList<String>();
			        	for(AgreementElement explanationElem: explanationElements){
			        		String templateElemName = explanationElem.getName();
			        		
			        		returnMsg += "Template element: "+templateElemName;
			        		
			        		if (!templateErrorsNames.contains(templateElemName)){
			        			tempElems.add(templateElemName);
			        			templateErrorsNames.add(templateElemName);
			        		}
			        	}
			        	tempErrors.add(tempElems);
					}
				}else{
					if(!isCompliant){
						returnMsg += "As the documents are not compliant, the explaining for more restrictive offer is not performed.";
					}else returnMsg += "The agreement offer is Less Restrictive, or equal, than the template.";
				}
				if(!isMoreRestrictiveTemplateTerms){
					returnMsg += "Template terms are less restrictive than the template creation constraints:";
					
					//Explaining Non-Compliance for Less Restrictive template terms conflicts
					Map<AgreementError,Explanation> errorsArrayMapEntry = service.explainLessRestrictiveTemplateTerms(template.getBytes());
					for(Map.Entry<AgreementError,Explanation> e: errorsArrayMapEntry.entrySet()){
			        	Collection<AgreementElement> guaranteeTermErrorElements = e.getKey().getElements(); //agreementErrorElements
			        	Collection<String> gtElems = new LinkedList<String>();  //agErrorElem
			        	for(AgreementElement gtErrorElem: guaranteeTermErrorElements){
			        		String gtElemName = gtErrorElem.getName();
			        		
			        		returnMsg += "Conflicting Template Term: "+gtElemName;
			        		
			        		if (!offerErrorsNames.contains(gtElemName)){
			        			gtElems.add(gtElemName);
				        		offerErrorsNames.add(gtElemName);
				        	}

			        	}
			        	offerErrors.add(gtElems);
			        	
			        	Collection<AgreementElement> explanationElements = e.getValue().getElements();
			        	Collection<String> ccElems = new LinkedList<String>();
			        	for(AgreementElement ccElem: explanationElements){
			        		String ccElemName = ccElem.getName();
			        		
			        		returnMsg += "Template Creation Constraint Cause: "+ccElemName;
			        		
			        		if (!templateErrorsNames.contains(ccElemName)){
			        			ccElems.add(ccElemName);
			        			templateErrorsNames.add(ccElemName);
			        		}
			        	}
			        	tempErrors.add(ccElems);
					}					
					
				}else{
					returnMsg += "The template terms are more restrictive, or equal, than the template creation constraints.";
				}
		
			}
	
			result[0] = isTempConsistent;
			result[1] = isOfferConsistent;
			result[2] = isCompliant;
			result[3] = tempErrors;
			result[4] = offerErrors;
		}catch(BadSyntaxException e){
			returnMsg += "There was an error: It may be due to a syntax error, please check the document syntax.";
		}
		
		return returnMsg;
	}
	
}
