
/*
 * SONEA Andreea 333CB
 * 			TEMA 2 Algoritmi Paraleli si Distribuiti
 */


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;


/**
 * Clasa ce reprezinta un thread worker.
 */
class Worker extends Thread {
	WorkPool workpool;	// workpool-ul din care scoate task-uri pentru rezolvat
	Document docs[];	// // lista documente - resursa comuna
	
	
	public Worker(WorkPool workpool, Document docs[]) {
		this.workpool = workpool;
		this.docs = docs;
	}
	
	public void run() {

		while (true) {
			Task task = workpool.getWork();
			if (task == null)
				break;	
			processTask(task);
		}
	}
	

	/*
	 * Proceseaza task-ul primit. 
	 * Daca este de tipul "map" va citi fagmentul corespunzator si il va parsa in 
	 * cuvinte care adaugate hash-ul si lista de cuvinte maximale (daca sunt cuvinte
	 * maxime ) din solutia partiala a documentului.
	 * 
	 * Daca este de tipul "reduce" va combina hash-urile si listele de 
	 * valori maximale din solutia partiala a documentului din task 
	 * intr-un singur hash si o singura lista. Este calculat rangul, 
	 * numarul de cuvinte de lungime maxima si lungimea acestora.
	 */
	void processTask(Task task) {
		String segment;
		
		// verificare daca s-a ajuns la solutie finala
		if (workpool.tasks.size() == 0) {
			workpool.ready = true;
		}
		
		// daca task-ul este de tipul map
		if (task.type == "map") {
			segment = readSegment(task);
			createPartialSolution(segment, task);
		}
		
		
		// daca task-ul este de tipul reduce
		if (task.type == "reduce") {
			HashMap<Integer, Integer> finalHashMap = new HashMap<Integer, Integer>();
			LinkedList<String> finalMaxWords = new LinkedList<String>();
			
			long nrWords;
			float rang;
			
			nrWords = combinePartialSolution(task, finalHashMap, finalMaxWords);
			rang = getRank(finalHashMap, nrWords);
			// adaugare rezultate in document, synchronized pentru ca se acceseaza 
			// o resursa comuna (lista de documente)
			addResultsToDocument(task.indexDoc, rang, 
								 finalMaxWords.get(0).length(), finalMaxWords.size());
		}

	}
	
	/*
	 * Metoda va citi inceputul si sfarsitul fragmentului din fisier. Daca fragmentul 
	 * incepe in mijlocul unui cuvant, "se deplaseaza" inceputul si sfarsitul fragmentului
	 * pana la primul delimitator. Se citeste fagmentul intre cei doi delimitatori.
	 */
	public String readSegment(Task task) {
		String delimiters = ";:/?~.,><~`[]{}()!@#$%^&-_+'=*| " 
							+ "\\" + '"' + '\t' + '\n' + '\r';
		byte []bytes;
		String segment = null;
		long start = 0, end = 0;
		char c;

		File file = new File(task.fileName);
		
		try {
			// deschidere fisier
			RandomAccessFile RAF = new RandomAccessFile(file, "r");
		
			// verificare daca segmentul incepe in mijlocul unui cuvant
			// exceptie primul fragment din document
			if (task.offset != 0) {
				
				// ultimul caracter din segmentul anterior
				RAF.seek(task.offset - 1);
				c = (char) RAF.readByte();
				
				// daca ultimul caracter nu este separator
				if (!delimiters.contains(c + "")) {
					// sarim peste caractere pana gasim un separator
					// pentru a nu procesa un cuvant deja procesat
					c = (char) RAF.readByte();
					while (!delimiters.contains(c + "") && 
							task.offset + start < RAF.length() - 1) {
						c = (char) RAF.readByte();
						start++;
					}
					if (!delimiters.contains(c + "")) {
						start++;
					}
				}
			}
			
			// verificare daca segmentul se termina in mijlocul unui cuvant
			// verificare sa nu citim EOF
			if (task.offset + task.D > RAF.length() - 1) {
				task.D = RAF.length() - 1 - task.offset;
			}
			
			RAF.seek(task.offset + task.D - 1);
			c = (char) RAF.readByte();
			// sarim peste caractere pana gasim un separator
			// cuvantul va trebui procesat
			// se incepe cu ultimul caracter din segment
			while (!delimiters.contains(c + "") && 
					task.offset + task.D + end < RAF.length() - 1) {
				c = (char) RAF.readByte();
				end++;
			}
			if (!delimiters.contains(c + "")) {
				end++;
			}
			
			// citire segment intre delimitatori
			int length = (int) (task.D + end - start);
			bytes = new byte[length];
			RAF.seek(task.offset + start);
			RAF.read(bytes, 0, length);
			segment = new String(bytes, "UTF-8");
			RAF.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return segment;

	}
	
	/*
	 * Metoda imparte segmentul primit in cuvinte. Fiecare cuvant rezultat 
	 * este adaugat in hashMap-ul si in lista de cuvinte maximale( daca e 
	 * cuvant maxim) rezultate in urma rezolvarii task-ului. Sunt adaugate hash-ul 
	 * si lista de cuvinte maximale in solutia partiala a documentului in mod 
	 * sincronizat pentru ca se acceseaza o resursa comuna.
	 */
	public void createPartialSolution(String segment, Task task) {
		String delimiters = ";:/?~.,><~`[]{}()!@#$%^&-_+'=*| " 
				+ "\\" + '"' + '\t' + '\n' + '\r';
		
		// hashmap-ul si lista de cuvinte maximale rezultate in urma rezolvarii 
		// fiecarui task de map
		HashMap<Integer, Integer> hashMap = new HashMap<Integer, Integer>();
		LinkedList<String> maxWords = new LinkedList<String>();
		
		// prelucrare cuvinte din segment
		StringTokenizer stringTokenizer = new StringTokenizer(segment, delimiters);
		if (stringTokenizer.countTokens() != 0) {
			while (stringTokenizer.hasMoreTokens()) {
				String token = stringTokenizer.nextToken();
				if (token != "") {
					// adaugare in hash-ul si in lista de cuvinte maximale 
					// a task-ului cuvantul curent
					addWord(token, hashMap, maxWords);
				}
			}
			// synchronized
			// adaugare hash si lista de cuvinte maximale in solutia partiala
			// a documentului
			addPartialSolution(task, hashMap, maxWords);
		}
	}
	
	
	
	/*
	 * Metoda adauga un cuvant in hashmap si in lista de cuvinte maximale calculate
	 * de task-ul de "map". (Dupa adaugarea tuturor cuvintelor din segmentul
	 * de care se ocupa task-ul, hashmap-ul si lista de valori maximale sunt 
	 * adaugate in solutia partiala a documentului).
	 */
	void addWord(String word, HashMap<Integer, Integer> hashMap, 
							  LinkedList<String> maxWords) {
		// adaugare in map
		if (!hashMap.containsKey(word.length())) {
			hashMap.put(word.length(), 1);
		}
		else {
				int value = hashMap.get(word.length());
				hashMap.put(word.length(), value + 1);
		}
		
		// adaugare in lista de cuvinte maximale daca e cazul
		if (maxWords.size() == 0) {
			maxWords.add(word);
		}
		else {
				if (word.length() == maxWords.get(0).length()) {
					// il adaugam fara a modifica lungimea maxima daca nu exista
					if (!maxWords.contains(word)) {
						maxWords.add(word);
					}
				}
				else	if (word.length() > maxWords.get(0).length()) {
							// am gasit o noua lungime maxima
							// stergere cuvinte vechi
							maxWords.clear();
							maxWords.add(word);
						}
		}
	}	
	
	/*
	 * Metoda combina hashmap-urile si listele de cuvinte maximale a solutiei partiale
	 * a task-ului in unul singur : finalHashMap si finalMaxWords. Este calculat numarul 
	 * de cuvinte total din document. 
	 * Sunt parcurse toate hashmap-urile si din fiecare, pentru fiecare cheie continuta 
	 * sunt adaugate valorile in hashmap-ul final.
	 * Sunt parcurse toate listele de cuvinte maximale din solutia partiala. Atunci cand 
	 * se gaseste acceasi lungime maxima sunt adaugate cuvintele care nu exista deja. 
	 * Atunci cand se gaseste o lungime mai mare este curata lista finala si sunt adaugate
	 * cuvintele din lista curenta.
	 * 
	 */
	public long combinePartialSolution(Task task, HashMap<Integer, Integer> finalHashMap, 
												  LinkedList<String> finalMaxWords) {
		HashMap<Integer, Integer> hashTask = null;
		LinkedList<String> maxWordsTask = null;
		
		long nrWords = 0;
		Iterator<Integer> keys = null;

		for (int i = 0; i < task.ps.hashList.size(); i++) {
			// operatia de combinare a hashmap-urilor
			hashTask = task.ps.hashList.get(i);
			
			// adaugare in hashMap-ul final a fiecarui hash din solutia partiala
			keys = hashTask.keySet().iterator();
			while (keys.hasNext()) {
				// fiecare cheie din hashmap
				Integer key = keys.next();
				
				// calcul numar cuvinte document
				nrWords += hashTask.get(key);
				
				// adaugare valori in hashmap-ul final
				if (!finalHashMap.containsKey(key)) {
					finalHashMap.put(key, hashTask.get(key));
				}
				else 	{
							Integer value = finalHashMap.get(key);
							finalHashMap.put(key, value + hashTask.get(key));
				}
			}
			
			// combinarea listelor maximale
			maxWordsTask = task.ps.maxWordsList.get(i);
			
			if (finalMaxWords.size() == 0) {
				finalMaxWords.addAll(0, maxWordsTask);
			}
			else
				if (maxWordsTask.get(0).length() == finalMaxWords.get(0).length()) {
					// adaugam doar cuvintele care nu exista deja
					// pentru cazul in care lungimea maxima e egala cu cea a 
					// listei maximale curente
					for (int j = 0; j < maxWordsTask.size(); j++) {
						if (!finalMaxWords.contains(maxWordsTask.get(j))) {
							finalMaxWords.add(maxWordsTask.get(j));
						}
					}
				}
				else {
						// am gasit o dimensiune mai mare
						// sunt eliminate cele anterioare din lista maximala finala
						// sunt adaugate cuvintele din lista maximala curenta
						if (maxWordsTask.get(0).length() > finalMaxWords.get(0).length()) {
							finalMaxWords.clear();
							for (int j = 0; j < maxWordsTask.size(); j++) {
								finalMaxWords.add(maxWordsTask.get(j));
							}
						}
				}
		}
		return nrWords;
	}
	
	
	/*
	 * Metoda calculeaza rangul unui document. Se calculeaza suma dintre fiecare produs
	 * valoarea fibonacii(cheie+1) * valoare(cheie) si se imparte la numarul total de 
	 * cuvinte din document.
	 */
	public float getRank(HashMap<Integer, Integer> finalHashMap, long nrWords) {
		Iterator<Integer> keys = null;

		// operatia de reducere pentru un document
		keys = finalHashMap.keySet().iterator();
		long sum = 0;
		while (keys.hasNext()) {
			Integer key = keys.next();
			Integer value = finalHashMap.get(key);
			sum += (F(key + 1) * value);
		}
		return (float) sum / nrWords;
	}

	
	/*
	 * Metoda adauga in solutia partiala a documentului specificat in task 
	 * hashmap-ul si lista de cuvinte maximale calculate de task.
	 */
	public synchronized void addPartialSolution(Task mapTask, HashMap<Integer, Integer> hashMap, 
															  LinkedList<String> maxWords) {
		
		docs[mapTask.indexDoc].ps.addHash(hashMap);
		docs[mapTask.indexDoc].ps.addMaxWords(maxWords);
	}
	
	/*
	 * Metoda adauga rezultatele calculate in document.
	 */
	public synchronized void addResultsToDocument(int index, float rang, int length, int nr) {
		
		docs[index].rangFormat = formatRang(rang);
		docs[index].rang = Float.valueOf(formatRang(rang));
		docs[index].lengthMax = length;
		docs[index].nrMax = nr;
	}
	
	
	/*
	 * Metoda calcululeaza cel de-al n-lea numar Fibonacci.
	 */
	public long F(int n) {
		long first = 0, second = 1, next = 0;

		if (n == 0) {
			next = 0;
		}
		else {
				if ( n == 1) {
					next = 1;
				}
				else {
						for (int i = 2 ; i <= n; i++) {
					         next = first + second;
					         first = second;
					         second = next;
						}
				}
		}
		return next;
		
	}
	
	/*
	 * Metoda returneaza un String ce reprezinta numarul float cu 
	 * 2 zecimale prin trunchiere.
	 */
	String formatRang(float rang) {
		String r = String.valueOf(rang);
		int pos = r.indexOf('.');
		String rr = r.substring(0, pos + 3); // pentru 2 zecimale dupa .
		return rr;
		
	}

	
}


