
/*
 * SONEA Andreea 333CB
 * 			TEMA 2 Algoritmi Paraleli si Distribuiti
 */

/**
 * Clasa pentru Document ce retine : numele fisierului, index-ul in lista de documente,
 * solutia partiala rezultata in urma "map", si solutia finala reprezentata prin: 
 * rang, lungimea maxima si numarul de cuvinte de lungime maxima.
 * 
 * @author andreea
 *
 */
public class Document {
	String fileName;	// numele fisierului
	int index;			// index-ul documentului in lista de documente
	float rang;			// rang-ul
	String rangFormat;	// rang-ul formatat	
	long nrWords;		// numarul total de cuvinte din document
	
	int nrMax;			// numarul de cuvinte de lungime maxima
	int lengthMax;		// lungimea maxima din document
	
	PartialSolution ps = new PartialSolution();	// solutia partiala pentru document
	
	public Document(String fileName, int index) {
		super();
		this.fileName = fileName;
		this.index = index;
		this.nrWords = 0;
	}
		
}
