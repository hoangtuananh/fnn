/**
 * The objects that are in a Fluid Neural Network
 */

/**
 * @author Stephen Majercik
 * 12/03/13
 *
 */


public class Neuron {

	
	// neuron has ID for tracking purposes
	private int ID;

	
	// a node has an activation level, but it is "active" only if the activation 
	// level exceeds a threshold  or if it becomes active "spontaneously" (see the 
	// setActivationLevel method) 
	private double activationLevel;
	private boolean active;

	
	// nodes live on a grid, so each one has a row and column
	private int row;
	private int col;


	// keep track of history of actual activation *levels*
	private double[] activationLevelHistory = new double[FNN.numIterationsDataCollection];
	// keep track of history of activation *status* (active or not)
	private int[] activeInactiveHistory = new int[FNN.numIterationsDataCollection];
	private int iteration;

	
	// need this to be able to check if the neuron has neighbors
	private FluidNN fnn;
	
	
	// basic constructor
	public Neuron() {

		iteration = 0;

		activationLevel = 0.0;
		activationLevelHistory[0] = 0.0;
		activeInactiveHistory[0] = 0;
		active = false;

		row = 0;
		col = 0;

	}


	// create at given location
	public Neuron(int ID, int row, int col) {

		this.ID = ID;
		iteration = 0;

		activationLevel = 0.0;
				activationLevelHistory[0] = 0.0;
				activeInactiveHistory[0] = 0;
		active = false;

		this.row = row;
		this.col = col;

	}


	// create at given location with given activation level;
	// mark it as active right away if it exceeds the activation
	// threshold
	public Neuron(int ID, int row, int col, double activationLevel, FluidNN fnn) {

		this.ID = ID;
		iteration = 0;

		this.activationLevel = activationLevel;
				activationLevelHistory[0] = activationLevel;
		if (activationLevel > FluidNN.getActivationThreshold()) {
			active = true;
						activeInactiveHistory[0] = 1;
		}
		else {
			active = false;
						activeInactiveHistory[0] = 0;			
		}		

		this.row = row;
		this.col = col;
		
		this.fnn = fnn;

	}


	// update the activationLevel:
	// set the new activation level
	// if it exceeds the activation level threshold, it becomes active
	//    else, it can become active spontaneously 
	//
	// All of the default values that govern this are in FluidNN.java:
	//   	private static double activationThreshold = 1e-16;
	//      private static double spontaneousActivationLevel = 0.1;
	//      private static double spontaneousActivationProbability = 0.1; 
	public void updateActivationStatus (double activationLevel, FNN.FNN_BoundaryModel currentFNNBoundaryModel) {

		// even if it doesn't become active, its level should be updated
		this.activationLevel = activationLevel;
		
		active = false;
		
		// exceeds threshold
		if (activationLevel > FluidNN.getActivationThreshold()) {
			this.activationLevel = activationLevel;
			active = true;
		}
		// else may become active spontaneously with a fixed activation level
		else if (FNN.rand.nextDouble() < FluidNN.getSpontaneousActivationProbability()) {
			this.activationLevel = FluidNN.getSpontaneousActivationLevel();
			active = true;
		}	

		// history data
		if (FNN.iteration >= FNN.firstIterationDataCollection) {
			activationLevelHistory[iteration] = this.activationLevel;

			if (active) {
				activeInactiveHistory[iteration] = 1;
			}
			else {
				activeInactiveHistory[iteration] = 0;			
			}		
			
			++iteration;
		}


	
	}

	
	public void updateActivationStatusNew (double activationLevel, FNN.FNN_BoundaryModel currentFNNBoundaryModel) {

		// even if it doesn't become active, its level should be updated
		this.activationLevel = activationLevel;
		
		active = false;
		
		// does current activation level suffice to  activate it?
		if (activationLevel > FluidNN.getActivationThreshold()) {
			this.activationLevel = activationLevel;
			active = true;
		}

		
		// if current activation level not high enough to activate it and it's isolated, can spontaneously activate
		else if (!fnn.hasNeighbors(this, currentFNNBoundaryModel)) {
			if (FNN.rand.nextDouble() < FluidNN.getSpontaneousActivationProbability()) {
				this.activationLevel = FluidNN.getSpontaneousActivationLevel();
				active = true;
			}	
		}

		
		// history data
		if (FNN.iteration >= FNN.firstIterationDataCollection) {
			activationLevelHistory[iteration] = this.activationLevel;

			if (active) {
				activeInactiveHistory[iteration] = 1;
			}
			else {
				activeInactiveHistory[iteration] = 0;			
			}		
			
			++iteration;
		}

	
	}


	// what's the probability that neuron n was in state s during the run?
	// can't remember why I made this static....  doesn't seem necessary
	public static double probabilityState(Neuron n, int state) {

		int[] activeInactiveHistory = n.getActiveInactiveHistory();

		double numItersInState = 0.0;
		for (int i = 0 ; i < activeInactiveHistory.length ; ++i) {
			if (activeInactiveHistory[i] == state)
				++numItersInState;
		}
		return numItersInState / activeInactiveHistory.length;


	}


	// what's the probability that neuron n1 was in state s1 *and*
	// what's the probability that neuron n2 was in state s2 during the run?
	// can't remember why I made this static....  doesn't seem necessary
	public static double probabilityStatesJoint(Neuron n1, int state1, Neuron n2, int state2) {

		int[] activeInactiveHistory1 = n1.getActiveInactiveHistory();
		int[] activeInactiveHistory2 = n2.getActiveInactiveHistory();

		double numItersInJointStates = 0.0;
		for (int i = 0 ; i < activeInactiveHistory1.length ; ++i) {
			if (activeInactiveHistory1[i] == state1 && activeInactiveHistory2[i] == state2)
				++numItersInJointStates;
		}
		return numItersInJointStates / activeInactiveHistory1.length;		

	}


	// getters and setters
	public int getID() {
		return ID;
	}

	public void setID(int ID) {
		this.ID = ID;
	}

	public double getActivationLevel() {
		return activationLevel;
	}

	public void setActivationLevel(double activationLevel) {
		this.activationLevel = activationLevel;
	}

	public int getRow() {
		return row;
	}

	public void setRow(int row) {
		this.row = row;
	}

	public int getCol() {
		return col;
	}

	public void setCol(int col) {
		this.col = col;
	}

	public boolean active() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public int[] getActiveInactiveHistory() {
		return activeInactiveHistory;
//		return null;
	}
	
	public double[] getActivationLevelHistory() {
		return activationLevelHistory;
	}


}
