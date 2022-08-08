package org.openmrs.module.ohrireports.reports.datasetevaluator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.annotation.Handler;
import org.openmrs.api.ConceptService;
import org.openmrs.module.ohrireports.reports.datasetdefinition.TXCurrDataSetDefinition;
import org.openmrs.module.reporting.dataset.DataSet;
import org.openmrs.module.reporting.dataset.DataSetColumn;
import org.openmrs.module.reporting.dataset.DataSetRow;
import org.openmrs.module.reporting.dataset.SimpleDataSet;
import org.openmrs.module.reporting.dataset.definition.DataSetDefinition;
import org.openmrs.module.reporting.dataset.definition.evaluator.DataSetEvaluator;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.EvaluationException;
import org.openmrs.module.reporting.evaluation.querybuilder.HqlQueryBuilder;
import org.openmrs.module.reporting.evaluation.service.EvaluationService;
import org.springframework.beans.factory.annotation.Autowired;
import static org.openmrs.module.ohrireports.OHRIReportsConstants.*;

@Handler(supports = { TXCurrDataSetDefinition.class })
public class TXCurrDataSetDefinitionEvaluator implements DataSetEvaluator {
	
	@Autowired
	EvaluationService evaluationService;
	
	@Autowired
	ConceptService conceptService;
	
	@Override
	public DataSet evaluate(DataSetDefinition dataSetDefinition, EvaluationContext evalContext) throws EvaluationException {
		
		TXCurrDataSetDefinition hdsd = (TXCurrDataSetDefinition) dataSetDefinition;
		
		SimpleDataSet data = new SimpleDataSet(dataSetDefinition, evalContext);
		
		List<Person> patients = getTxCurrPatients(hdsd, evalContext);
		
		DataSetRow row = null;
     
        List<Person> managPerson = new ArrayList<>();
		for (Person person : patients) {

            if(!managPerson.contains(person)){
                row = new DataSetRow();
                row.addColumnValue(new DataSetColumn("PersonID", "#", Integer.class), person.getPersonId());
                row.addColumnValue(new DataSetColumn("Name", "Name", String.class), person.getNames());
                row.addColumnValue(new DataSetColumn("Age", "Age", Integer.class), person.getAge());
                row.addColumnValue(new DataSetColumn("Gender", "Gender", Integer.class), person.getGender());
				managPerson.add(person);
                  
                data.addRow(row);
            }
		
		}
		return data;
	}
	
	private List<Integer> getListOfALiveORRestartPatientObservertions(EvaluationContext context,TXCurrDataSetDefinition hdsd) {
      
        List<Integer> uniqiObs =new ArrayList<>();
        HqlQueryBuilder queryBuilder = new HqlQueryBuilder();
      
        queryBuilder.select("obv.personId")
        .from(Obs.class,"obv")
		.whereEqual("obv.encounter.encounterType", hdsd.getEncounterType())
		.and()
      	.whereEqual("obv.concept", conceptService.getConceptByUuid(PATIENT_STATUS))
		.and()
		.whereIn("obv.valueCoded", Arrays.
		asList(conceptService.getConceptByUuid(ALIVE),
		conceptService.getConceptByUuid(RESTART)))
		.and().whereLess("obv.obsDatetime", hdsd.getEndDate());
		queryBuilder.orderDesc("obv.personId,obv.obsDatetime");

       

        for (Integer row : evaluationService.evaluateToList(queryBuilder,Integer.class, context)) {
				if(!uniqiObs.contains(row))
		 	        uniqiObs.add(row);
		}

        return uniqiObs;
    }
	
	private List<Person> getTxCurrPatients(TXCurrDataSetDefinition hdsd, EvaluationContext context) {
		
		List<Integer> patientsId = getListOfALiveORRestartPatientObservertions(context, hdsd);
		
		List<Person> patients = new ArrayList<>();
		
		if (patientsId ==null || patientsId.size()==0) 
		return patients;
		HqlQueryBuilder queryBuilder = new HqlQueryBuilder();
		queryBuilder.select("obv.person");
		queryBuilder.from(Obs.class,"obv")
		.whereEqual("obv.encounter.encounterType", hdsd.getEncounterType())
	  	.and()
		.whereEqual("obv.concept",conceptService.getConceptByUuid(TREATMENT_END_DATE))
		.and()
		.whereGreater("obv.valueDatetime", hdsd.getEndDate())
		.and()
		.whereLess("obv.obsDatetime", hdsd.getEndDate())
		.whereIdIn("obv.personId", patientsId)
        .orderDesc("obv.personId,obv.obsDatetime") ;

		for (Person patient : evaluationService.evaluateToList(queryBuilder, Person.class, context)) {
			if(!patients.contains(patient))
			patients.add(patient);
		}
		return patients;
	}
}
