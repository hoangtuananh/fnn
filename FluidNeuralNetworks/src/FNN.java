/**
 * This just tests the FluidNN class
 */

/**
 * @author Stephen Majercik
 * 12/03/13
 *
 */

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class FNN {

	// for random numbers in all classes
	public static Random rand = new Random();

	public static int iteration;
	public static int numIterations;
	// a certain number of iterations are discarded before data is collected
	// to give the FNN time to settle into its behavior
	public static int numIterationsDiscarded;
	// how many iterations will we actually collect data for?
	public static int numIterationsDataCollection;
	public static int firstIterationDataCollection;


	// shape of the neighborhood
	// FNN neighborhoods must be distinguished from standard neighborhoods so we know
	// how to do updates in the Particle class
	// in Solé & Miramontes paper, it is Moore
	public static enum Topology {
		FNN_GBEST, FNN_RING, FNN_vonNEUMANN, FNN_MOORE,
	}
	public static int[] numRowsVonNeumannAndMooreList;
	public static int[] numColsVonNeumannAndMooreList;


	// include self in neighborhood?
	// same issue for any kind of neighborhood;
	// in Solé & Miramontes paper, it is to include self
	public static enum SelfModel {
		INCLUDE_SELF, NOT_INCLUDE_SELF
	}


	// is the FNN a lattice or a torus?
	// in Solé & Miramontes paper, it is a lattice
	public static enum FNN_BoundaryModel {
		LATTICE, TORUS
	}

	
	// does the neighborhood include all neurons or just active ones?
	// in Solé & Miramontes paper, it is all neurons
	public static enum FNN_ActivityModel {
		ALL_NEURONS, ONLY_ACTIVE_NEURONS
	}


	// to collect data about how many times neurons could move
	public static int numMoveOpportunities = 0;
	// how many times they were active (and could have moved)
	public static int numTimesActive = 0;
	// and how many times they actually moved
	public static int numActualMoves = 0;


	/**
	 * @param args
	 */
	public static void main(String[] args) {

		SimpleDateFormat dateformatter = new SimpleDateFormat("yyyy-MM-dd--hh-mm-ss-a");
		Calendar date = Calendar.getInstance();
		String dateString = dateformatter.format(date.getTime());
		System.out.println("RUNNING CODE ON " + dateString + "\n");			


		// set list of lattice sizes to do runs for
		int[] latticeSizes = { 7, 8, 9, 10 };

		// set list of densities to try
		double[] densities = { 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9 };

		// set list of gains to try
		double[] gains = { 0.1, 0.2, 0.3, 0.4, 0.5 };

		// set list of spontaneous activation levels to try
		double[] spontActLevels = { 0.1, 0.2, 0.3, 0.4 };

		// set list of spontaneous activation probabilities to try
		double[] spontActProbs = { 1e-6, 1e-5, 1e-4, 1e-3, 1e-2, 1e-1 };

		
		// how many runs to average over
		int numRuns = 50;


		int latticeSize = 0;
		double density = 0.0;
		int numNeurons = 0;
		double gain = 0.0;
		double spontActLevel = 0.0;
		double spontActProb = 0.0;
		double activationThreshold = 1e-16;
		double sumNeighborActivationsThreshold = 0.0;


		runExperiment2(numRuns, latticeSize, density, numNeurons, gain, spontActLevel,
					   spontActProb, activationThreshold, sumNeighborActivationsThreshold);
		
//		// headings for data ouput
//		System.out.printf("                                                                            prob        prob        prob        prob        active as    moves as  \n");	
//		System.out.printf(" lat                  spont-act    spont-act     average      average       0/0         0/1         1/0         1/1          perc of      perc of  \n");	
//		System.out.printf("size   density  gain    level         prob       entropy    info trans      pair        pair        pair        pair          opps         active  \n");
//		
//		// all lattice size values
//		for (int latticeSizeIndex = 0 ; latticeSizeIndex < latticeSizes.length ; ++latticeSizeIndex ) {
//
//			latticeSize = latticeSizes[latticeSizeIndex];
//
//
//			// all density values
//			for (int densityIndex = 0 ; densityIndex < densities.length ; ++densityIndex) {
//
//				density = densities[densityIndex];
//				numNeurons = (int) ((latticeSize * latticeSize) * density);
//
//
//				// all gain values
//				for (int gainIndex = 0 ; gainIndex < gains.length ; ++gainIndex ) {
//
//					gain = gains[gainIndex];
//
//
//					// all spontaneous activation levels
//					for (int spontActLevelIndex = 0 ; spontActLevelIndex < spontActLevels.length ; ++spontActLevelIndex ) {
//
//						spontActLevel = spontActLevels[spontActLevelIndex];
//
//
//						// all spontaneous activation probabilities
//						for (int spontActProbIndex = 0 ; spontActProbIndex < spontActProbs.length ; ++spontActProbIndex ) {
//
//							spontActProb = spontActProbs[spontActProbIndex];
//
//							runExperiment(numRuns, latticeSize, density,
//									numNeurons, gain, spontActLevel,
//									spontActProb, activationThreshold,
//									sumNeighborActivationsThreshold);	
//
//
//						}
//
//
//					}
//
//				}
//
//			}
//
//		}


		System.out.println("\n\nDONE");			

	}


	private static void runExperiment2(int numRuns, int latticeSize,
			double density, int numNeurons, double gain, double spontActLevel,
			double spontActProb, double activationThreshold,
			double sumNeighborActivationsThreshold) {
		
		// for testing purpose
		numRuns = 1;
		latticeSize = 9;
		density = 0.319;
		numNeurons = (int) ((latticeSize * latticeSize) * density);
		gain = 0.3;
		spontActLevel = 0.2;
		spontActProb = 1e-5;
		
		
		History fullActiveInactiveGlobalHistory = new History();
		History kPastActiveInactiveGlobalHistory = new History();
		History lastStateActiveInactiveGlobalHistory = new History();
		
		numIterations = 11000;
		numIterationsDiscarded = 1000;
		numIterationsDataCollection = numIterations - numIterationsDiscarded;
		firstIterationDataCollection = numIterationsDiscarded + 1;
		
		for (int run = 0; run < numRuns; ++run) {
			
			FluidNN fnn = new FluidNN(latticeSize, latticeSize, numNeurons, gain, sumNeighborActivationsThreshold,
					activationThreshold, spontActLevel, spontActProb);
			
			for (iteration = 0; iteration < numIterations; ++iteration) {
				fnn.moveAndUpdateNeurons(Topology.FNN_MOORE, SelfModel.INCLUDE_SELF, FNN_BoundaryModel.LATTICE, 
										 FNN_ActivityModel.ALL_NEURONS);
			}
			Neuron[] neurons = fnn.getNeuronList();
			for (int neuronIndex = 0; neuronIndex < neurons.length; ++neuronIndex) {
				Neuron neuron = neurons[neuronIndex];
				int[] activeInactiveHistory = Arrays.copyOfRange(neuron.getActiveInactiveHistory(), 0, neuron.getActiveInactiveHistory().length - 1);
				int[] kPastActiveInactiveHistory = Arrays.copyOfRange(activeInactiveHistory, 0, activeInactiveHistory.length - 1);
				int lastStateHistory = activeInactiveHistory[activeInactiveHistory.length - 1];
				
				HistoryKey activeInactiveLocalHistory = new HistoryKey(activeInactiveHistory);
				HistoryKey kPastActiveInactiveLocalHistory = new HistoryKey(kPastActiveInactiveHistory);
				HistoryKey lastStateLocalHistory = new HistoryKey(lastStateHistory);
				
				fullActiveInactiveGlobalHistory.put(activeInactiveLocalHistory);
				kPastActiveInactiveGlobalHistory.put(kPastActiveInactiveLocalHistory);
				lastStateActiveInactiveGlobalHistory.put(lastStateLocalHistory);
			}
		}
		
		double totalAIS = 0;
		
		for (int run = 0; run < numRuns; ++run) {
			FluidNN fnn = new FluidNN(latticeSize, latticeSize, numNeurons, gain, sumNeighborActivationsThreshold,
					activationThreshold, spontActLevel, spontActProb);
			
			for (iteration = 0; iteration < numIterations; ++iteration) {
				fnn.moveAndUpdateNeurons(Topology.FNN_MOORE, SelfModel.INCLUDE_SELF, FNN_BoundaryModel.LATTICE, 
										 FNN_ActivityModel.ALL_NEURONS);
			}
			Neuron[] neurons = fnn.getNeuronList();
			double AIS = 0;
			for (int neuronIndex = 0; neuronIndex < neurons.length; ++neuronIndex) {
				Neuron[] neuronList = fnn.getNeuronList();
				Neuron neuron = neuronList[neuronIndex];
				
				AIS += localActiveInformationStorage(neuron, fullActiveInactiveGlobalHistory, 
						kPastActiveInactiveGlobalHistory, lastStateActiveInactiveGlobalHistory);
			}
			totalAIS += AIS / numNeurons;
		}
		
		System.out.println("Average AIS = " + totalAIS/numRuns) ;
		//printMap(lastStateActiveInactiveGlobalHistory);
	}

	

	private static List<Integer> convertToIntegerList(int[] intList) {
		Integer[] IntegerList = new Integer[intList.length];
		for (int i = 0; i < intList.length; ++ i) {
			IntegerList[i] = Integer.valueOf(intList[i]);
		}
		
		return Arrays.asList(IntegerList);
	}

	
	private static void runExperiment(int numRuns, int latticeSize,
			double density, int numNeurons, double gain, double spontActLevel,
			double spontActProb, double activationThreshold,
			double sumNeighborActivationsThreshold) {
		// initializations
		double sumEntropy = 0.0;
		double sumInfoTransfer = 0.0;
		double sum00PairProb = 0.0;
		double sum01PairProb = 0.0;
		double sum10PairProb = 0.0;
		double sum11PairProb = 0.0;
		double sumTimesActivePercentOfOpportunities = 0.0;
		double sumMovesPercentOfTimesActive = 0.0;


		for(int run = 0 ; run < numRuns ; ++run) {

			numMoveOpportunities = 0;
			numTimesActive = 0;
			numActualMoves = 0;

			// this is what Sole and Miramontes did
			numIterations = 11000;
			numIterationsDiscarded = 1000;
			numIterationsDataCollection = numIterations - numIterationsDiscarded;
			firstIterationDataCollection = numIterationsDiscarded + 1;

			double[] histogramNumActive = new double[numNeurons+1];

			FluidNN fnn = new FluidNN(latticeSize, latticeSize, numNeurons, gain, sumNeighborActivationsThreshold,
					activationThreshold, spontActLevel, spontActProb);


			for (iteration = 1 ; iteration <= numIterations ; ++iteration) {

				fnn.moveAndUpdateNeurons(Topology.FNN_MOORE, SelfModel.INCLUDE_SELF, FNN_BoundaryModel.LATTICE, FNN_ActivityModel.ALL_NEURONS);
				int numActiveNeurons = fnn.numActiveNeurons();
				if (iteration >= firstIterationDataCollection)
					++histogramNumActive[numActiveNeurons];
				//				System.out.println("iteration " + iteration + "   num active neurons = " + numActiveNeurons);
				//				fnn.printActiveStatusWithIDs();
				//				activityDataFile.println(iteration + "   " + (double) numActiveNeurons/numNeurons);
			}


			double[] infoTransferInfo = calculateInfoTransferRandomPair(fnn);
			//									System.out.println("Info Transfer = " + infoTransfer);
			sumInfoTransfer += infoTransferInfo[0];
			sum00PairProb += infoTransferInfo[1];
			sum01PairProb += infoTransferInfo[2];
			sum10PairProb += infoTransferInfo[3];
			sum11PairProb += infoTransferInfo[4];

			sumTimesActivePercentOfOpportunities += numTimesActive * 100.0 / numMoveOpportunities;
			sumMovesPercentOfTimesActive += numActualMoves * 100.0 / numTimesActive;

		}


		// print out averages
		System.out.printf("%2d      %4.2f    %4.2f     %4.2f       %8.6f   %9.5f    %9.5f     %7.5f     %7.5f     %7.5f     %7.5f     %9.5f     %9.5f\n", 
				latticeSize, density, gain, spontActLevel, spontActProb,
				sumEntropy/numRuns,

				sumInfoTransfer/numRuns,
				sum00PairProb/numRuns,
				sum01PairProb/numRuns,
				sum10PairProb/numRuns,
				sum11PairProb/numRuns,

				sumTimesActivePercentOfOpportunities/numRuns,
				sumMovesPercentOfTimesActive/numRuns);
	}

	
	// Shannon-Kolmogorov entropy
	// histogramNumActive provides the number of iterations that had
	// 0, 1, ..., numNeurons neurons active for a given run
	// NOTE:  this is different from the entropy of the activity history of a neuron   *******************************
	public static double calculateSKE (double[] histogramNumActive) {

		double[] freqenciesNumActive = new double[histogramNumActive.length];

		// over how many iterations was the data collected?
		double numIterations = 0.0;
		for (int i = 0 ; i < histogramNumActive.length ; ++i) {
			numIterations += histogramNumActive[i];
		}

		for (int i = 0 ; i < histogramNumActive.length ; ++i) {
			freqenciesNumActive[i] = histogramNumActive[i] / numIterations;
		}

		double SKEsummation = 0.0;
		for (int i = 0 ; i < freqenciesNumActive.length ; ++i) {
			double frequency = freqenciesNumActive[i];
			if (frequency != 0.0)
				SKEsummation += frequency * lg(frequency);
		}

		return - SKEsummation;


	}


	// returns an array containing the information transfer for a random pair of neurons 
	// plus the probabilities of each pair of activation values
	public static double[] calculateInfoTransferRandomPair(FluidNN fnn) {

		Neuron n1 = fnn.getRandomNeuron();
		Neuron n2 = fnn.getRandomNeuron();
		while (n1 == n2) {
			n2 = fnn.getRandomNeuron();
		}

		double prob_n1state0 = Neuron.probabilityState(n1, 0);
		double prob_n1state1 = Neuron.probabilityState(n1, 1);
		double[] probs1 = { prob_n1state0, prob_n1state1};
		double entropy1 = calculateEntropy(probs1);

		double prob_n2state0 = Neuron.probabilityState(n2, 0);
		double prob_n2state1 = Neuron.probabilityState(n2, 1);
		double[] probs2 = { prob_n2state0, prob_n2state1 };
		double entropy2 = calculateEntropy(probs2);

		double prob_n1state0_n2state0 = Neuron.probabilityStatesJoint(n1, 0, n2, 0);
		double prob_n1state0_n2state1 = Neuron.probabilityStatesJoint(n1, 0, n2, 1);
		double prob_n1state1_n2state0 = Neuron.probabilityStatesJoint(n1, 1, n2, 0);
		double prob_n1state1_n2state1 = Neuron.probabilityStatesJoint(n1, 1, n2, 1);
		double[] probs = { prob_n1state0_n2state0, prob_n1state0_n2state1, prob_n1state1_n2state0, prob_n1state1_n2state1 };
		double jointEntropy = calculateEntropy(probs);

		double[] retArray = new double[5];
		retArray[0] = entropy1 + entropy2 - jointEntropy;
		retArray[1] = prob_n1state0_n2state0;
		retArray[2] = prob_n1state0_n2state1;
		retArray[3] = prob_n1state1_n2state0;
		retArray[4] = prob_n1state1_n2state1;

		return retArray;

	}


	// calc entropy for given list of probabilities
	public static double calculateEntropy (double[] probs) {

		double entropy = 0.0;
		for (int i = 0 ; i < probs.length ; ++i) {
			// 0.0 lg 0.0 defined to be 0.0 here
			if (probs[i] > 0.0)
				entropy += probs[i] * lg(probs[i]);
		}

		return -entropy;
	}

	public static double localActiveInformationStorage(Neuron neuron, History fullActiveInactiveGlobalHistory, 
													   History kPastActiveInactiveGlobalHistory, 
													   History lastStateActiveInactiveGlobalHistory) {
		int[] activeInactiveHistory = Arrays.copyOfRange(neuron.getActiveInactiveHistory(), 0, neuron.getActiveInactiveHistory().length - 1);
		int[] kPastActiveInactiveHistory = Arrays.copyOfRange(activeInactiveHistory, 0, activeInactiveHistory.length - 1);
		int lastStateActiveInactive = activeInactiveHistory[activeInactiveHistory.length - 1];
		
		HistoryKey activeInactiveLocalHistory = new HistoryKey(activeInactiveHistory);
		HistoryKey kPastActiveInactiveLocalHistory = new HistoryKey(kPastActiveInactiveHistory);
		HistoryKey lastStateLocalHistory = new HistoryKey(lastStateActiveInactive);
		
		Integer temp = fullActiveInactiveGlobalHistory.getCloset(activeInactiveLocalHistory);
		double jointKPastandPresent = temp == null? 
				0 : (double) temp / (double) fullActiveInactiveGlobalHistory.sumValues();
		
		temp = kPastActiveInactiveGlobalHistory.getCloset(kPastActiveInactiveLocalHistory);
		double probabilityKPast = temp == null? 
				0 : (double) temp / (double) kPastActiveInactiveGlobalHistory.sumValues();
		
		temp = lastStateActiveInactiveGlobalHistory.getCloset(lastStateLocalHistory);
		double probabilityCurrent = temp == null ? 
				0 : (double) temp / (double) lastStateActiveInactiveGlobalHistory.sumValues();
		
		double res = (probabilityKPast) * (probabilityCurrent) == 0 ? 
				0 : lg (jointKPastandPresent / ( (probabilityKPast) * (probabilityCurrent) ) );
		return res;
	}

	// log base 2
	public static double lg (double d) {	
		return Math.log10(d) / Math.log10(2.0);
	}



}
















