import java.util.HashMap;
import java.util.LinkedList;

/*
 * SONEA Andreea 333CB
 * 			TEMA 2 Algoritmi Paraleli si Distribuiti
 */

/**
 * Clasa ce retine solutia partiala pentru un document. 
 * Contine o lista de hashMap-uri si o lista de liste de cuvinte maximale 
 * rezultate in urma prelucrarii fiecarui fragment de catre task-urile de map.
 * @author andreea
 *
 */
public class PartialSolution {
	LinkedList<HashMap<Integer, Integer>> hashList;
	LinkedList<LinkedList<String>> maxWordsList;
	
	
	public PartialSolution() {
		this.hashList = new LinkedList<HashMap<Integer, Integer>>();
		this.maxWordsList = new LinkedList<LinkedList<String>>();		
	}
	
	
	public PartialSolution(LinkedList<HashMap<Integer, Integer>> hashList,
			LinkedList<LinkedList<String>> maxWordsList) {
		this.hashList = hashList;
		this.maxWordsList = maxWordsList;
	}
	
	/*
	 * Adaugare hashMap in lista  mod sincronizat pentru ca se scrie intr-o
	 * resursa comuna.
	 */
	synchronized void addHash(HashMap<Integer, Integer> hashMap) {
		hashList.add(hashMap);
	}
	
	/*
	 * Adaugare lista de cuvinte maximale in lista in mod sincronizat pentru ca 
	 * se scrie intr-o resursa comuna.
	 */
	synchronized void addMaxWords(LinkedList<String> maxWords) {
		maxWordsList.add(maxWords);
	}

}
