package pokerbots.states;


public class ResponseObject {
	String actionType, param;
	DecisionState next;
	
	public ResponseObject( String actionType, String param, DecisionState next ) {
		this.actionType = actionType;
		this.param = param;
		this.next = next;
	}
	
	public ResponseObject( String actionParam, DecisionState next ) {
		this.actionType = actionParam;
		this.param = "";
		this.next = next;
	}
	
	public String toString() {
		if ( param.length()>0 )
			return actionType.toUpperCase()+":"+param;
		return actionType.toUpperCase();
	}
	
	public DecisionState getNextState() {
		return next;
	}
}
