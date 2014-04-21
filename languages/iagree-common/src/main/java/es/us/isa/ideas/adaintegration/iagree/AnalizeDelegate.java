package es.us.isa.ideas.adaintegration.iagree;

import java.util.Collection;
import java.util.Map;

import es.us.isa.ideas.adaintegration.adaadhoc.ada.document.AgreementElement;
import es.us.isa.ideas.common.AppResponse;
import es.us.isa.ideas.common.AppResponse.Status;



public class AnalizeDelegate {
	
	public static AppResponse analize(String id, String document,
			String otherDoc, boolean isOffer){
		
		// instanciados
		AdaFacade service = new AdaFacade();
		AppResponse appResponse = new AppResponse();
		// seleccion de la operacion
		if (id == "checkConsistency") {
			try {
				Boolean check = service.consistency(document);
				if (check) {
					appResponse.setStatus(Status.OK);
					appResponse.setMessage("The document is consistent");
				} else {
					appResponse.setStatus(Status.OK_PROBLEMS);
					appResponse.setMessage("The document is not consistent");
				}
			} catch (Exception e) {
				appResponse.setStatus(Status.ERROR);
				appResponse.setMessage("There has been an error: "
						+ e.getMessage());
			}
		} else if (id == "inconsistencyExplainingShort") {
			try {
				Map<AgreementElement, Collection<AgreementElement>> inconsistences = service.inconsistenciesMap(document);
				if (inconsistences.isEmpty()) {
					appResponse.setStatus(Status.OK_PROBLEMS);
					appResponse.setMessage("No inconsistences found. Please check if the document is consistent.");
				} else {
					appResponse.setStatus(Status.OK);
					appResponse.setMessage("The document is inconsistent because of these terms: " + inconsistences.toString());
				}
			} catch (Exception e) {
				appResponse.setStatus(Status.ERROR);
				appResponse.setMessage("There has been an error: " + e.getMessage());
			}
		} else if (id == "inconsistenciesExplainingLong") {
			try {
				String inconsistencies = service.inconsitencyExplaining(document);
				if (inconsistencies.contains("error")) {
					appResponse.setStatus(Status.OK_PROBLEMS);
					appResponse.setMessage(inconsistencies);
				} else {
					appResponse.setStatus(Status.OK);
					appResponse.setMessage(inconsistencies);
				}
			} catch (Exception e) {
				appResponse.setStatus(Status.ERROR);
				appResponse.setMessage("There has been an error: " + e.getMessage());
			}
		} else if (id == "getNumberOfAlternatives") {
			try {
				Integer numAlt = service.getNumberOfAlternatives(document);
				if (numAlt == 0) {
					appResponse.setStatus(Status.OK_PROBLEMS);
					appResponse.setMessage("There are no alternatives for the document");
				} else {
					appResponse.setStatus(Status.OK);
					if (numAlt == 1) {
						appResponse.setMessage("There is " + numAlt + " alternative to the document");
					} else {
						appResponse.setMessage("There are " + numAlt + " alternatives to the document");
					}
				}
			} catch (Exception e) {
				appResponse.setStatus(Status.ERROR);
				appResponse.setMessage("There has been an error: " + e.getMessage());
			}
		} else if (id == "checkCompliance" && isOffer) {
			try {
				Boolean compliance = service.isCompliant(otherDoc, document);
				if (compliance) {
					appResponse.setStatus(Status.OK);
					appResponse.setMessage("The provided offer is compliant with the provided template");
				} else {
					appResponse.setStatus(Status.OK_PROBLEMS);
					appResponse.setMessage("The provided offer is not compliant with the provided template");
				}
			} catch (Exception e) {
				appResponse.setStatus(Status.ERROR);
				appResponse.setMessage("There has been an error: " + e.getMessage());
			}
		} else if (id == "nonComplianceExplaining" && isOffer) {
			// TODO nonComplianceExplaining(String template, String offer)
			try{
				String explanation = service.nonComplianceExplaining(otherDoc, document);
				if (explanation.contains("error") || explanation.contains("errors")){
					appResponse.setStatus(Status.OK_PROBLEMS);
					appResponse.setMessage(explanation);
				} else {
					appResponse.setStatus(Status.OK);
					appResponse.setMessage(explanation);
				}
			} catch (Exception e) {
				appResponse.setStatus(Status.ERROR);
				appResponse.setMessage("There has been an error: " + e.getMessage());
			}
		} else {
			appResponse.setStatus(Status.ERROR);
			appResponse.setMessage("No such opperation");
		}

		return appResponse;
	}

}