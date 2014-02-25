/**
 * Implements a Fluid Neural Network as described in:
 * 		Sole and Miramontes
 * 		"Information at the edge of chaos in fluid neural networks"
 * 		Physica D 80 (1995) 171-180
 * 
 * Another relevant paper:
 * 		Delgado and Sole
 * 		"Mean-field theory of fluid neural networks"
 * 		Physcial Review E 57(2) (1998) 2204-11
 */

/**
 * @author Stephen Majercik
 * 12/03/13
 *
 */
public class FluidNN {

	// an array of the neurons
	private Neuron[] neuronList;
	// the nodes/automata of an FNN live on a lattice/grid
	private Neuron[][] grid;
	// size of grid
	private int numRows;
	private int numCols;
	// number of neurons
	private int numNeurons;

	// this is the "coupling matrix" in Sole & Miramontes:
	//
	// J_ij is an arbitrary function of S_i and S_j, the activation levels
	// of neurons i and j, and is a multiplier in the activation formula:
	// in the paper, however, J is just a 2x2 matrix of constants:
	//
	//    lambda_11  lambda 12
	//    lambda_21  lambda 22
	//
	// where:
	//   J_ij = lambda_11 if both i and j are active 
	//   J_ij = lambda_12 if i is active and j is inactive 
	//   J_ij = lambda_21 if i is inactive and j is active 
	//   J_ij = lambda_22 if both i and j are inactive 
	//
	// in the paper, in all their experiments, all lambdas were 1.0
	//
	// NOTE: J is 2x2 only because we are defining only two states
	// for a neuron; in general, J will be k x k, where k is the number
	// of states a neuron can be in
	//
	// for now, we will follow the paper
	private double[][] J = { { 1.0, 1.0 }, { 1.0, 1.0 } };

	// *************************** this is what I've been using
	// *************************** it's possible that INITIAL_ACTIVATION_LOW_LEVEL should be -1.0; unclear whether it makes a difference
	// new neurons have an activation level between 0.0 and 1.0
	private static final double INITIAL_ACTIVATION_LOW_LEVEL = 0.0;
	private static final double INITIAL_ACTIVATION_HIGH_LEVEL = 1.0;
	private static final double INITIAL_ACTIVATION_RANGE = 
		INITIAL_ACTIVATION_HIGH_LEVEL- INITIAL_ACTIVATION_LOW_LEVEL;


	// NOTE: all of the values below are defaults and will usually be over-ridden
	// No idea why I made them static....
	
	
	// this is factor that dials the activation level up and down;
	// it is applied to the activation sum before the "squashing
	//  function" (tanh, in this case) is applied
	private static double gain = 0.2;

	
	// there appear to be two thresholds in the Sole & Miramontes paper:
	//   1) one that is used in the sum of neighbor activations calculation
	//		(but is set to 0.0 in the paper)
	private static double sumNeighborActivationsThreshold = 0.0;

	
	// a neuron can become active in one of two ways:
	//   1) its activation level exceeds a threshold:
	//      (in Sole & Miramontes this is 1e-16)
	private static double activationThreshold = 1e-16;
	//   2) it is inactive and becomes active spontaneously
	//		at a specified level with a specified probability
	//      In the Sole & Miramontes paper, the level is given 
	//		as 0.1; the probability is not given, but in the
	//		Delgado and Sole paper cited above, they experiment
	//		with values in the range of 0.00003 to 0.3, with
	// 		0.1 appearing to be the "default"
	private static double spontaneousActivationLevel = 0.1;
	private static double spontaneousActivationProbability = 0.1;   


	// create a FluidNN of a given size (dimensions), but with no neurons
	public FluidNN (int numRows, int numCols) {

		this.numRows = numRows;
		this.numCols = numCols;
		grid = new Neuron[numRows][numCols];
		this.numNeurons = 0;

	}

	
	// create a FluidNN of a given size with a given number of neurons (randomly placed)
	public FluidNN (int numRows, int numCols, int numNeurons) {

		this.numRows = numRows;
		this.numCols = numCols;
		neuronList = new Neuron[numNeurons];
		grid = new Neuron[numRows][numCols];
		this.numNeurons = numNeurons;
		randomPopulate(numNeurons);

	}

	
	// create a FluidNN of a given size with a given number of neurons (randomly placed)
	// and with specified values for the parameters
	public FluidNN (int numRows, int numCols, int numNeurons, double gain,
			double sumNeighborActivationsThreshold, double activationThreshold, 
			double spontaneousActivationLevel, double spontaneousActivationProbability) {

		this.numRows = numRows;
		this.numCols = numCols;
		neuronList = new Neuron[numNeurons];
		grid = new Neuron[numRows][numCols];
		this.numNeurons = numNeurons;
		randomPopulate(numNeurons);
		this.gain = gain;
		this.sumNeighborActivationsThreshold = sumNeighborActivationsThreshold;
		this.activationThreshold = activationThreshold;
		this.spontaneousActivationLevel = spontaneousActivationLevel;
		this.spontaneousActivationProbability = spontaneousActivationProbability;

	}


	// put a specified number of neurons in the net at random locations; 
	// assumes that the grid is empty and that number of neurons is less 
	// than the number of cells in the grid
	public void randomPopulate(int numNeurons) {

		for (int neuronID = 0 ; neuronID < numNeurons ; ++neuronID) {
			Neuron newNeuron = addRandomNeuron(neuronID);
			neuronList[neuronID] = newNeuron;
			// null if grid is full; should not happen
			if (newNeuron == null) { 
				System.out.println("error: grid too small in randomPopulate");
				System.exit(0);
			}
		}
	}

	
	// find a random empty location in the grid (if one exists; if not, 
	// return null), put a neuron there, and return that neuron
	public Neuron addRandomNeuron(int neuronID) {

		if (gridFull()) {
			return null;
		}

		// get random row and col
		int r = FNN.rand.nextInt(numRows);
		int c = FNN.rand.nextInt(numCols);

		// kludgey way of finding an empty location
		while (grid[r][c] != null) {
			r = FNN.rand.nextInt(numRows);
			c = FNN.rand.nextInt(numCols);
		}

		// random initial activation level
		double initialActivationLevel = INITIAL_ACTIVATION_LOW_LEVEL + (FNN.rand.nextDouble() * INITIAL_ACTIVATION_RANGE);
		grid[r][c] = new Neuron(neuronID, r, c, initialActivationLevel, this);

		return grid[r][c];

	}

	
	// update the activation levels of all the neurons and move them
	public void moveAndUpdateNeurons(FNN.Topology currentTopology, FNN.SelfModel currentSelfModel, 
			FNN.FNN_BoundaryModel currentFNNBoundaryModel, FNN.FNN_ActivityModel currentFNNActivityModel) {

		// update activations
		updateActivationLevels(currentTopology, currentSelfModel, currentFNNBoundaryModel, currentFNNActivityModel);
		
		// move them
		moveAllMoore(currentFNNBoundaryModel);


	}

	
	// update activation levels according to the Sole & Miramontes paper;
	// assuming that new activation levels must be calculated entirely from 
	// current activation levels (may not be necessary)
	public void updateActivationLevels(FNN.Topology currentTopology, FNN.SelfModel currentSelfModel, 
			FNN.FNN_BoundaryModel currentFNNBoundaryModel, FNN.FNN_ActivityModel currentFNNActivityModel) {

		double[] newActivationLevels = new double[neuronList.length];

		for (int n = 0 ; n < neuronList.length ; ++n) {
			double sumAct = getSumActivations(neuronList[n], currentTopology, currentSelfModel, currentFNNBoundaryModel, currentFNNActivityModel);  
			newActivationLevels[n] = Math.tanh(gain * (sumAct - sumNeighborActivationsThreshold));
		}

		// using the raw activation levels just computed, update the neurons' activity status, which 
		// includes both its activation level and whether it is actually "active"
		for (int n = 0 ; n < neuronList.length ; ++n) {
			
			// *************************************************************************************************
			// *************************************************************************************************
			// here a neuron CAN spontaneously activate even if it has neighbors
//			neuronList[n].updateActivationStatus(newActivationLevels[n], currentFNNBoundaryModel);	
			
			// here a neuron CANNOT spontaneously activate even if it has neighbors
			neuronList[n].updateActivationStatusNew(newActivationLevels[n], currentFNNBoundaryModel);
			// *************************************************************************************************
			// *************************************************************************************************
			
		}


	}


	// sum activations according to Sole & Miramontes paper; not clear whether
	// the neuron itself is included, but the Delgado & Sole paper seems to indicate that it is, so we do
	public double getSumActivations(Neuron neuron, FNN.Topology currentTopology, FNN.SelfModel currentSelfModel, 
			FNN.FNN_BoundaryModel currentFNNBoundaryModel, FNN.FNN_ActivityModel currentFNNActivityModel) {

		// NOTE: in the Sole & Miramontes paper, it appears that the activation
		//       level of the neuron itself is also multiplied by a J factor;
		//       it will always be lambda_11 ("both" active) or lambda_22 
		//       ("both" inactive)

		// an array of the Neurons that are neighbors
		Neuron[] neighbors = getNeighborhood(neuron, currentTopology, currentSelfModel, currentFNNBoundaryModel, currentFNNActivityModel);

		// sum the activations
		double sumActivations = 0.0;
		for (int n = 0 ; n < neighbors.length ; ++n) {
			sumActivations += getJValue(neuron, neighbors[n]) * neighbors[n].getActivationLevel();
		}
		
		return sumActivations;

	}


	// returns a list of neurons in the neighborhood
	// CURRENTLY NEVER USING ANYTHING BUT FNN_MOORE
	public Neuron[] getNeighborhood(Neuron neuron, FNN.Topology currentTopology, FNN.SelfModel currentSelfModel, 
			FNN.FNN_BoundaryModel currentFNNBoundaryModel, FNN.FNN_ActivityModel currentFNNActivityModel) {

		Neuron[] neighborhood = null;

		if (currentTopology == FNN.Topology.FNN_GBEST) {		
			neighborhood = getGbestNeighborhood(neuron, currentSelfModel, currentFNNBoundaryModel, currentFNNActivityModel);			
		}

		else if (currentTopology == FNN.Topology.FNN_RING) {
			neighborhood = getRingNeighborhood(neuron, currentSelfModel, currentFNNBoundaryModel, currentFNNActivityModel);				
		}

		else if (currentTopology == FNN.Topology.FNN_vonNEUMANN) {
			neighborhood = getVonNeumannNeighborhood(neuron, currentSelfModel, currentFNNBoundaryModel, currentFNNActivityModel);				
		}

		else if (currentTopology == FNN.Topology.FNN_MOORE) {
			neighborhood = getMooreNeighborhood(neuron, currentSelfModel, currentFNNBoundaryModel, currentFNNActivityModel);			
		}
		else {
			System.out.println("error:  unknown topology in FluidNN.getNeigborhood"); 
			System.exit(-1);
		}

		return neighborhood;
	}

	
	// NOTE: we don't actually need the current boundary novel, since the neighborhood is the entire grid, but include it for 
	// the sake of uniformity wrt the other getXXXNeighborhood methods;
	// returns a list of neurons in the neighborhood
	public Neuron[] getGbestNeighborhood (Neuron neuron, FNN.SelfModel currentSelfModel, FNN.FNN_BoundaryModel currentFNNBoundaryModel, 
			FNN.FNN_ActivityModel currentFNNActivityModel) {

		// create an array that can hold the maximum number of neurons in the neighborhood, including the neuron itself;
		// in the GBEST case, it could be all the neurons
		Neuron[] possibleNeighbors = new Neuron[neuronList.length];
		int numNeigh = 0;

		//		Neuron self = grid[row][col];
		for (int n = 0 ; n < neuronList.length ; ++n) {
			if (currentSelfModel == FNN.SelfModel.NOT_INCLUDE_SELF && neuronList[n] == neuron)
				continue;
			if (currentFNNActivityModel == FNN.FNN_ActivityModel.ONLY_ACTIVE_NEURONS && !neuronList[n].active())
				continue;
			possibleNeighbors[numNeigh++] = neuronList[n];
		}

		// if all the possible neurons are in the neighborhood, can just return the array 
		// they are already in
		if (numNeigh == neuronList.length) {   
			return possibleNeighbors;
		}

		// otherwise transfer the members of the neighborhood to an array that's exactly large 
		// enough to hold them
		Neuron[] actualNeighbors = new Neuron[numNeigh];
		for (int i = 0 ; i < numNeigh ; ++i) {
			actualNeighbors[i] = possibleNeighbors[i];
		}

		return actualNeighbors;

	}


	// this isn't really RING; it's treating each row of the grid/lattice as a ring, which is not quite right,
	// but it's reasonable (although not an exact analogue to standard RING);
	// in any case, RING in the context of FNN neighborhoods does not make a whole lot of sense;
	// returns a list of neurons in the neighborhood
	public Neuron[] getRingNeighborhood (Neuron neuron, FNN.SelfModel currentSelfModel, FNN.FNN_BoundaryModel currentFNNBoundaryModel, 
			FNN.FNN_ActivityModel currentFNNActivityModel) {

		// create an array that can hold the maximum number of neurons in the neighborhood, including the neuron itself
		Neuron[] possibleNeighbors = new Neuron[3];
		int numNeigh = 0;

		// center, i.e. the particle itself
		// NOTE: we could potentially check here if the currentSelfModel is INCLUDE_SELF, but we would also have to 
		// check the current activity model; isNeighbor does both, so call isNeighbor 
		int rDelta = 0;
		int cDelta = 0;
		Neuron possibleNeighbor = isNeighbor(neuron, rDelta, cDelta, currentSelfModel, currentFNNBoundaryModel, currentFNNActivityModel);
		if (possibleNeighbor != null)
			possibleNeighbors[numNeigh++] = possibleNeighbor;

		// right
		rDelta = 0;
		cDelta = 1;
		possibleNeighbor = isNeighbor(neuron, rDelta, cDelta, currentSelfModel, currentFNNBoundaryModel, currentFNNActivityModel);
		if (possibleNeighbor != null)
			possibleNeighbors[numNeigh++] = possibleNeighbor;

		// left
		rDelta = 0;
		cDelta = -1;
		possibleNeighbor = isNeighbor(neuron, rDelta, cDelta, currentSelfModel, currentFNNBoundaryModel, currentFNNActivityModel);
		if (possibleNeighbor != null)
			possibleNeighbors[numNeigh++] = possibleNeighbor;

		// if all the possible neurons are in the neighborhood, can just return the array 
		// they are already in
		if (numNeigh == 3) {   // 3 includes the neuron whose neighbors we are determining
			return possibleNeighbors;
		}

		// otherwise transfer the members of the neighborhood to an array that's exactly large 
		// enough to hold them
		Neuron[] actualNeighbors = new Neuron[numNeigh];
		for (int i = 0 ; i < numNeigh ; ++i) {
			actualNeighbors[i] = possibleNeighbors[i];
		}

		return actualNeighbors;

	}


	// returns a list of neurons in the von Neumann neighborhood
	public Neuron[] getVonNeumannNeighborhood (Neuron neuron, FNN.SelfModel currentSelfModel, FNN.FNN_BoundaryModel currentFNNBoundaryModel, 
			FNN.FNN_ActivityModel currentFNNActivityModel) {


		// create an array that can hold the maximum number of neurons in the neighborhood, including the neuron itself
		Neuron[] possibleNeighbors = new Neuron[5];
		int numNeigh = 0;

		// center, i.e. the particle itself
		// NOTE: we could potentially check here if the currentSelfModel is INCLUDE_SELF, but we would also have to 
		// check the current activity model; isNeighbor does both, so call isNeighbor 
		int rDelta = 0;
		int cDelta = 0;
		Neuron possibleNeighbor = isNeighbor(neuron, rDelta, cDelta, currentSelfModel, currentFNNBoundaryModel, currentFNNActivityModel);
		if (possibleNeighbor != null)
			possibleNeighbors[numNeigh++] = possibleNeighbor;

		// north
		rDelta = -1;
		cDelta = 0;
		possibleNeighbor = isNeighbor(neuron, rDelta, cDelta, currentSelfModel, currentFNNBoundaryModel, currentFNNActivityModel);
		if (possibleNeighbor != null)
			possibleNeighbors[numNeigh++] = possibleNeighbor;

		// east
		rDelta = 0;
		cDelta = 1;
		possibleNeighbor = isNeighbor(neuron, rDelta, cDelta, currentSelfModel, currentFNNBoundaryModel, currentFNNActivityModel);
		if (possibleNeighbor != null)
			possibleNeighbors[numNeigh++] = possibleNeighbor;

		// south
		rDelta = 1;
		cDelta = 0;
		possibleNeighbor = isNeighbor(neuron, rDelta, cDelta, currentSelfModel, currentFNNBoundaryModel, currentFNNActivityModel);
		if (possibleNeighbor != null)
			possibleNeighbors[numNeigh++] = possibleNeighbor;

		// west
		rDelta = 0;
		cDelta = -1;
		possibleNeighbor = isNeighbor(neuron, rDelta, cDelta, currentSelfModel, currentFNNBoundaryModel, currentFNNActivityModel);
		if (possibleNeighbor != null)
			possibleNeighbors[numNeigh++] = possibleNeighbor;

		// if all the possible neurons are in the neighborhood, can just return the array 
		// they are already in
		if (numNeigh == 5) {   // 5 includes the neuron whose neighbors we are determining
			return possibleNeighbors;
		}

		// otherwise transfer the members of the neighborhood to an array that's exactly large 
		// enough to hold them
		Neuron[] actualNeighbors = new Neuron[numNeigh];
		for (int i = 0 ; i < numNeigh ; ++i) {
			actualNeighbors[i] = possibleNeighbors[i];
		}

		return actualNeighbors;


	}


	// returns a list of neurons in the Moore neighborhood
	public Neuron[] getMooreNeighborhood (Neuron neuron, FNN.SelfModel currentSelfModel, FNN.FNN_BoundaryModel currentFNNBoundaryModel, 
			FNN.FNN_ActivityModel currentFNNActivityModel) {

		// create an array that can hold the maximum number of neurons in the neighborhood, including the neuron itself
		Neuron[] possibleNeighbors = new Neuron[9];
		int numNeigh = 0;

		// this will also consider the neuron itself;
		// NOTE: we could potentially check here if the currentSelfModel is INCLUDE_SELF, but we would also have to 
		// check the current activity model; isNeighbor does both, so call isNeighbor 
		for (int rDelta = -1 ; rDelta <= 1 ; ++rDelta) {
			for (int cDelta = -1 ; cDelta <= 1 ; ++cDelta) {
				Neuron possibleNeighbor = isNeighbor(neuron, rDelta, cDelta, currentSelfModel, currentFNNBoundaryModel, currentFNNActivityModel);
				if (possibleNeighbor != null)
					possibleNeighbors[numNeigh++] = possibleNeighbor;
			}
		}

		// if all the possible neurons are in the neighborhood, can just return the array 
		// they are already in
		if (numNeigh == 9) {   // 9 includes the neuron whose neighbors we are determining
			return possibleNeighbors;
		}

		// otherwise transfer the members of the neighborhood to an array that's exactly large 
		// enough to hold them
		Neuron[] actualNeighbors = new Neuron[numNeigh];
		for (int i = 0 ; i < numNeigh ; ++i) {
			actualNeighbors[i] = possibleNeighbors[i];
		}

		return actualNeighbors;

	}


	// Currently, most of this is superfluous, since we are only looking at:
	//   Moore neighborhood
	//   on a lattice
	//   including all neurons (including self)
	//   
	public Neuron isNeighbor (Neuron neuron, int rDelta, int cDelta, FNN.SelfModel currentSelfModel, 
			FNN.FNN_BoundaryModel currentFNNBoundaryModel, FNN.FNN_ActivityModel currentFNNActivityModel) {

		int row = neuron.getRow();
		int col = neuron.getCol();

		Neuron neighbor = null;

		if (currentSelfModel == FNN.SelfModel.INCLUDE_SELF) {

			if (currentFNNBoundaryModel == FNN.FNN_BoundaryModel.TORUS) {

				int neighRow = rowWrap(row + rDelta);
				int neighCol = colWrap(col + cDelta);

				if (currentFNNActivityModel == FNN.FNN_ActivityModel.ONLY_ACTIVE_NEURONS) {
					if ((grid[neighRow][neighCol] != null) && grid[neighRow][neighCol].active())
						neighbor = grid[neighRow][neighCol];
				}

				else if (currentFNNActivityModel == FNN.FNN_ActivityModel.ALL_NEURONS) {				
					if ((grid[neighRow][neighCol] != null))
						neighbor = grid[neighRow][neighCol];
				}

			}				

			else if (currentFNNBoundaryModel == FNN.FNN_BoundaryModel.LATTICE) {

				int neighRow = row + rDelta;
				int neighCol = col + cDelta;

				if (currentFNNActivityModel == FNN.FNN_ActivityModel.ONLY_ACTIVE_NEURONS) {
					if (legalCell(neighRow, neighCol) && (grid[neighRow][neighCol] != null) && grid[neighRow][neighCol].active())
						neighbor = grid[neighRow][neighCol];
				}

				else if (currentFNNActivityModel == FNN.FNN_ActivityModel.ALL_NEURONS) {				
					if (legalCell(neighRow, neighCol) && (grid[neighRow][neighCol] != null))
						neighbor = grid[neighRow][neighCol];
				}

			}

		}

		else if (currentSelfModel == FNN.SelfModel.NOT_INCLUDE_SELF) {

			if (currentFNNBoundaryModel == FNN.FNN_BoundaryModel.TORUS) {

				int neighRow = rowWrap(row + rDelta);
				int neighCol = colWrap(col + cDelta);

				if (currentFNNActivityModel == FNN.FNN_ActivityModel.ONLY_ACTIVE_NEURONS) {
					if ((neighRow != row || neighCol != col) && (grid[neighRow][neighCol] != null) && grid[neighRow][neighCol].active())
						neighbor = grid[neighRow][neighCol];
				}

				else if (currentFNNActivityModel == FNN.FNN_ActivityModel.ALL_NEURONS) {				
					if ((neighRow != row || neighCol != col) && (grid[neighRow][neighCol] != null))
						neighbor = grid[neighRow][neighCol];
				}

			}
			else if (currentFNNBoundaryModel == FNN.FNN_BoundaryModel.LATTICE) {

				int neighRow = row + rDelta;
				int neighCol = col + cDelta;

				if (currentFNNActivityModel == FNN.FNN_ActivityModel.ONLY_ACTIVE_NEURONS) {
					if ((neighRow != row || neighCol != col) && legalCell(neighRow, neighCol) && (grid[neighRow][neighCol] != null) && grid[neighRow][neighCol].active())
						neighbor = grid[neighRow][neighCol];
				}

				else if (currentFNNActivityModel == FNN.FNN_ActivityModel.ALL_NEURONS) {				
					if ((neighRow != row || neighCol != col) && legalCell(neighRow, neighCol) && (grid[neighRow][neighCol] != null))
						neighbor = grid[neighRow][neighCol];
				}

			}

		}

		// could be null
		return neighbor;

	}


	// returns correct J value given activation status
	public double getJValue(Neuron n1, Neuron n2) {

		double JValue = 0.0;

		boolean isActive1 = n1.active();
		boolean isActive2 = n2.active();

		if (isActive1 && isActive2) {
			JValue = J[0][0];
		}

		else if (isActive1 && !isActive2) {
			JValue = J[0][1];
		}

		else if (!isActive1 && isActive2) {
			JValue = J[1][0];
		}

		else if (!isActive1 && !isActive2) {
			JValue = J[1][1];
		}

		return JValue;

	}


	// move the neurons:
	// not in parallel, instead sweep through the grid, moving them sequentially;
	// this means that a neuron that could have moved before the sweep started,
	// might not be able to move when its turn comes
	// NOTE: movement will always be in a Moore neighborhood at this point;
	//   movement in a vonNeumann neighborhood is also a possibility, but what
	//   I'm doing depends strongly on the dynamics/behavior of the FNN model
	//   in the Solé & Miramontes paper, which uses a Moore neighborhood
	//
	// also collect some data about activity and movement
	public void moveAllMoore(FNN.FNN_BoundaryModel currentFNNBoundaryModel) {

		
		
		for (int i = 0 ; i < neuronList.length ; ++i) {
			++FNN.numMoveOpportunities;
			Neuron n = neuronList[i];
			if (n.active()) {
				++FNN.numTimesActive;
				boolean moved = moveMoore(n, currentFNNBoundaryModel);
				if (moved) {
					++FNN.numActualMoves;
				}
			}
		}

	}


	// move the neuron to a random location in the Moore neighborborhood;
	// if there is an empty neighbor cell, a move is *always* made;
	// this is in contrast to picking a neighbor cell randomly and then moving there
	// if it is not occupied (and if it is occupied, no movement takes place)
	// returns true if move was made
	public boolean moveMoore(Neuron neuron, FNN.FNN_BoundaryModel currentFNNBoundaryModel) {

		int row = neuron.getRow();
		int col = neuron.getCol();

		if (noMovePossible(neuron, currentFNNBoundaryModel)) {
			return false;
		}

		// get random changes in row and column (-1, 0, or +1 in both cases)
		int newRow = 0;
		int newCol = 0;


		// at this point, always using this
		if (currentFNNBoundaryModel == FNN.FNN_BoundaryModel.LATTICE) {
			int rowChange = FNN.rand.nextInt(3) - 1;
			int colChange = FNN.rand.nextInt(3) - 1;
			newRow = row + rowChange;
			newCol = col + colChange;
			// must:
			//   1) not be the same location it is in already
			//   2) not an illegal location (no wrap-around)
			//   3) not already occupied
			// we know a move is possible, since we checked that at the beginning
			while ((rowChange == 0 && colChange == 0) || 
					!legalCell(newRow, newCol) || 
					(legalCell(newRow, newCol) && grid[newRow][newCol] != null)) {
				rowChange = FNN.rand.nextInt(3) - 1;
				colChange = FNN.rand.nextInt(3) - 1;
				newRow = row + rowChange;
				newCol = col + colChange;
			}
		}

		// never used yet
		else if (currentFNNBoundaryModel == FNN.FNN_BoundaryModel.TORUS) {
			int rowChange = FNN.rand.nextInt(3) - 1;
			int colChange = FNN.rand.nextInt(3) - 1;
			newRow = rowWrap(row + rowChange);
			newCol = colWrap(col + colChange);
			// must:
			//   1) not be the same location it is in already
			//   2) not already occupied
			// we know a move is possible, since we checked that at the beginning
			while ((rowChange == 0 && colChange == 0) || grid[newRow][newCol] != null) {
				rowChange = FNN.rand.nextInt(3) - 1;
				colChange = FNN.rand.nextInt(3) - 1;
				newRow = rowWrap(row + rowChange);
				newCol = colWrap(col + colChange);

			}

		}

		// In an earlier paper (Miramontes, Solé, and Goodwin, "Collective behavior of random-activated mobile cellular automata," 1993)
		//  a neuron is allowed 6 attempts to move, i.e. pick a neighboring space and move to it if unoccupied; the latter paper (1995) I am using
		//  as my implementation guide, does not seem to have this limit, but it is unclear how it works
		//						boolean foundSpace = false;
		//						int newRow = 0;
		//						int newCol = 0;
		//						for (int i = 1 ; i <= 6 && !foundSpace; ++i) {
		//							int rowChange = FNN.rand.nextInt(3) - 1;
		//							int colChange = FNN.rand.nextInt(3) - 1;
		//							newRow = row + rowChange;
		//							newCol = col + colChange;
		//							// must:
		//							//   1) not be the same location it is already in
		//							//   2) not an illegal location (no wrap-around)
		//							//   3) not already occupied
		//							while ((rowChange == 0 && colChange == 0) || 
		//									!legalCell(newRow, newCol)) {
		//								rowChange = FNN.rand.nextInt(3) - 1;
		//								colChange = FNN.rand.nextInt(3) - 1;
		//								newRow = row + rowChange;
		//								newCol = col + colChange;
		//							}
		//				
		//							if (grid[newRow][newCol] == null) {
		//								foundSpace = true;
		//							}
		//						}
		//				
		//						if (!foundSpace)
		//							return false;

		
		// reset the neuron's row and column
		neuron.setRow(newRow);
		neuron.setCol(newCol);

		// move the neuron to its new location
		grid[newRow][newCol] = neuron;
		// be sure to set the old location to null
		grid[row][col] = null;

		return true;

	}

		
	// check whether given values for row and column are legal
	public boolean legalCell(int row, int col) {

		return row >= 0 && row < numRows && col >= 0 && col < numCols;

	}


	// if one of the 8 Moore neighbor cells is empty, return true; otherwise false.
	// assume that automata cannot wrap around when moving
	public boolean noMovePossible(Neuron neuron, FNN.FNN_BoundaryModel boundaryCondition) {
		
		int row = neuron.getRow();
		int col = neuron.getCol();
		
		int newRow;
		int newCol;

		// at this point, always using this
		if (boundaryCondition == FNN.FNN_BoundaryModel.LATTICE) {
			for (int rDelta = -1 ; rDelta <= 1 ; ++rDelta) {
				for (int cDelta = -1 ; cDelta <= 1 ; ++cDelta) {
					newRow = row + rDelta;
					newCol = col + cDelta;
					// ignore the neuron at [row][col], since that's the neuron trying to move
					if ((newRow != row || newCol != col) && legalCell(newRow, newCol) && grid[newRow][newCol] == null)
						return false;
				}
			}
		}

		// never used yet
		else if (boundaryCondition == FNN.FNN_BoundaryModel.TORUS) {
			for (int rDelta = -1 ; rDelta <= 1 ; ++rDelta) {
				for (int cDelta = -1 ; cDelta <= 1 ; ++cDelta) {
					newRow = rowWrap(row + rDelta);
					newCol = colWrap(col + cDelta);
					// ignore the neuron at [row][col], since that's the neuron trying to move
					// since we're using a torus, the new row and col will always be a legal cell
					if ((newRow != row || newCol != col) && grid[newRow][newCol] == null)
						return false;
				}
			}
		}

		return true;

	}


	// find out if the neuron has neighbors; used to determine whether a neuron can spontaneously activate
	public boolean hasNeighbors(Neuron neuron, FNN.FNN_BoundaryModel boundaryCondition) {
		
		int row = neuron.getRow();
		int col = neuron.getCol();
		
		int newRow;
		int newCol;

		// at this point, always using this
		if (boundaryCondition == FNN.FNN_BoundaryModel.LATTICE) {
			for (int rDelta = -1 ; rDelta <= 1 ; ++rDelta) {
				for (int cDelta = -1 ; cDelta <= 1 ; ++cDelta) {
					newRow = row + rDelta;
					newCol = col + cDelta;
					// ignore the neuron at [row][col]
					if ((newRow != row || newCol != col) && legalCell(newRow, newCol) && grid[newRow][newCol] != null)
						return true;
				}
			}
		}

		// never used yet
		else if (boundaryCondition == FNN.FNN_BoundaryModel.TORUS) {
			for (int rDelta = -1 ; rDelta <= 1 ; ++rDelta) {
				for (int cDelta = -1 ; cDelta <= 1 ; ++cDelta) {
					newRow = rowWrap(row + rDelta);
					newCol = colWrap(col + cDelta);
					// ignore the neuron at [row][col], since that's the neuron trying to move
					// since we're using a torus, the new row and col will always be a legal cell
					if ((newRow != row || newCol != col) && grid[newRow][newCol] != null)
						return true;
				}
			}
		}

		return false;

	}


	// grid full?
	public boolean gridFull() {

		boolean isFull = true;

		for (int r = 0 ; r < grid.length ; ++r) {
			for (int c = 0 ; c < grid[r].length ; ++c) {
				if (grid[r][c] == null)
					return false;
			}
		}

		return true;

	}

	
	// get the number of active neurons; note that we are not checking whether
	// the activation level is > 0.0, because a neuron could have a non-zero
	// activation level, but still be inactive (I think -- this is a question
	// I have for Solé)
	public int numActiveNeurons() {

		int numActiveNeurons = 0;
		for (int i = 0 ; i < neuronList.length ; ++i) {
			if (neuronList[i].active())
				++numActiveNeurons;
			}

		return numActiveNeurons;
		
	}

	
	public Neuron getRandomNeuron () {

		return neuronList[FNN.rand.nextInt(neuronList.length)];

	}

	
	// row wrap-around
	public int rowWrap (int row) {
		if (row < 0)
			return numRows - 1;

		if (row == numRows) {
			return 0;
		}

		return row;
	}

	
	// column wrap-around
	public int colWrap (int col) {
		if (col < 0)
			return numCols - 1;

		if (col == numCols) {
			return 0;
		}

		return col;
	}


	// print activation levels
	public void printActivationLevels() {

		for (int r = 0 ; r < grid.length ; ++r) {
			for (int c = 0 ; c < grid[r].length ; ++c) {
				if (grid[r][c] != null) {
					System.out.printf("%8.4f   ", grid[r][c].getActivationLevel());
				}
				else {
					System.out.printf(" _________ ");					
				}
			}
			System.out.println();
		}
		System.out.println();
		System.out.println();
		System.out.println();

	}


	// print active status
	public void printActiveStatus() {

		for (int r = 0 ; r < grid.length ; ++r) {
			for (int c = 0 ; c < grid[r].length ; ++c) {
				if (grid[r][c] != null) {
					if (grid[r][c].active()) {
						System.out.printf("1  ");
					}
					else {
						System.out.printf("0  ");
					}
				}
				else {
					System.out.printf("-  ");					
				}
			}
			System.out.println();
		}
		System.out.println();
		System.out.println();
		System.out.println();

	}


	// print active status
	public void printActiveStatusWithIDs() {

		for (int r = 0 ; r < grid.length ; ++r) {
			for (int c = 0 ; c < grid[r].length ; ++c) {
				Neuron n = grid[r][c];
				if (n != null) {
					if (n.active()) {
						System.out.printf(" %2d ", n.getID());
					}
					else {
						System.out.printf(" %2d ", 0);
					}
				}
				else {
					System.out.printf(" -- ");
				}
			}
			System.out.println();
		}
		System.out.println();
		System.out.println();
		System.out.println();

	}


	// getters and setters

	public Neuron[] getNeuronList() {
		return neuronList;
	}
	
	public static double getGain() {
		return gain;
	}

	public static void setGain(double gain) {
		FluidNN.gain = gain;
	}

	public static double getSumNeighborActivationsThreshold() {
		return sumNeighborActivationsThreshold;
	}

	public static void setSumNeighborActivationsThreshold(double sumNeighborActivationsThreshold) {
		FluidNN.sumNeighborActivationsThreshold = sumNeighborActivationsThreshold;
	}

	public static double getActivationThreshold() {
		return activationThreshold;
	}

	public static void setActivationThreshold(double activationThreshold) {
		FluidNN.activationThreshold = activationThreshold;
	}

	public static double getSpontaneousActivationLevel() {
		return spontaneousActivationLevel;
	}

	public static void setSpontaneousActivationLevel(double spontaneousActivationLevel) {
		FluidNN.spontaneousActivationLevel = spontaneousActivationLevel;
	}

	public static double getSpontaneousActivationProbability() {
		return spontaneousActivationProbability;
	}

	public static void setSpontaneousActivationProbability(double spontaneousActivationProbability) {
		FluidNN.spontaneousActivationProbability = spontaneousActivationProbability;
	}


}



























