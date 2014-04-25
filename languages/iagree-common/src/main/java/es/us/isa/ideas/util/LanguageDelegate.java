package es.us.isa.ideas.util;

import java.util.ArrayList;
import java.util.List;

import es.us.isa.ideas.common.AppAnnotations;
import es.us.isa.ideas.common.AppResponse;
import es.us.isa.ideas.common.AppResponse.Status;

public class LanguageDelegate {

	@SuppressWarnings("unchecked")
	public static AppResponse checkLanguage(String format, String content, String fileUri, boolean isOffer) {
		
		AppResponse appResponse = new AppResponse();
		appResponse.setFileUri(fileUri);
		List<AppAnnotations> annotations = new ArrayList<AppAnnotations>();
		
		if (format.equals("iagree")) {
			
			// TODO: Refactorizar junto con lo que hay en Convert.
			// En convert no se está gestionando bien la concurrencia. Los errores no se deben
			// "preguntar", sino que se deben gestionar dentro de la misma llamada.
			
			annotations.addAll((List<AppAnnotations>) Convert.getWsagFromIAgree(content).get("annotations"));
			
			if (!annotations.isEmpty()) {
				appResponse.setStatus(Status.OK_PROBLEMS);
				appResponse.setAnnotations(annotations.toArray(new AppAnnotations[annotations.size()]));
			} else {
				appResponse.setStatus(Status.OK);
			}
			
		} else
			System.out.println("No language checker is implemented for this format: " + format);
		
		return appResponse;
	}
	
}
