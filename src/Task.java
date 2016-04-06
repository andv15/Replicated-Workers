
/*
 * SONEA Andreea 333CB
 * 			TEMA 2 Algoritmi Paraleli si Distribuiti
 */


/**
 * Clasa ce reprezinta un task pentru problema de rezolvat. Aceste 
 * task-uri sunt introduse in workpool, urmand a fi scoase de workeri 
 * pentru a fi rezolvate.
 */

class Task {
	String type;		// map sau reduce
	
	String fileName;	// numele fisierului 
	int indexDoc;		// indexul documentului
	long offset;		// offset-ul pentru citire
	long D;				// lungimea de citit
	

	PartialSolution ps;	// solutie partiala pentru task-ul de reduce
	
	
	/*
	 * Constructor pentru task-ul de map
	 */
	Task(String type, String fileName, int indexDoc, long offset, long D) {
		this.type = type;
		this.fileName = fileName;
		this.indexDoc = indexDoc;
		this.offset = offset;
		this.D = D;
	}
	
	/*
	 * Constructor pentru task-ul de reduce
	 */	
	Task(String type, String fileName, int indexDoc, PartialSolution ps) {
		this.type = type;
		this.fileName = fileName;
		this.indexDoc = indexDoc;
		this.ps = ps;		
	}
	
}