package org.openmrs.module.rwandasphstudyreports;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class VLTreatmentFailureAction {
	public VLTreatmentFailureAction(Actions rep) {
        this.name = rep.name();
        this.displayName = rep.displayName();
    }
    private String name;
    private String displayName;

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }
    
    public enum Actions {
    	counseling("Counseling"),
    	checkDosage("Check dosage"),
    	homeVisit("Home visit"),
    	orderVL("Order VL"),
    	secondLineART("Shift patient to second line ART"),
    	thirdLineART("Shift patient to third line ART");

		private String displayName;

		Actions(String displayName) {
			this.displayName = displayName;
		}

		public String displayName() {
			return displayName;
		}
    }
    
    public static List<VLTreatmentFailureAction> getAllVLTreatmentFailureActions() {
		List<VLTreatmentFailureAction> reps = new ArrayList<VLTreatmentFailureAction>();

		for(Actions r : new ArrayList<Actions>(EnumSet.allOf(Actions.class))) {
			reps.add(new VLTreatmentFailureAction(r));
		}

		return reps;
	}
}
