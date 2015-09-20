package gov.va.isaac.gui.conceptCreation.wizardPages;

public enum RoleType {
	All_Role, 
	Some_Role;

	@Override
	public String toString() {
		if(this.equals(All_Role)) {
			return "All Role";
		} else if(this.equals(Some_Role)) {
			return "Some Role";
		}
		return super.toString();
	}
	

	
}
