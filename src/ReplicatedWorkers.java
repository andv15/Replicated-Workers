
/*
 * SONEA Andreea 333CB
 * 			TEMA 2 Algoritmi Paraleli si Distribuiti
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.LinkedList;


public class ReplicatedWorkers {
	int NT;	// numar thread-uri
	int ND;	// numar documente
	int D;	// dimensiune fragment

	WorkPool wp_map;	// workpool "map"
	WorkPool wp_reduce;	// workpool "reduce"
	
	// lista workerilor
	LinkedList<Worker> workers = new LinkedList<Worker>();
	
	// lista documente
	Document[] docs;	

	
	public static void main(String args[]) {
		
		String file_in = args[1];
		String file_out = args[2];
		
		ReplicatedWorkers replicatedWorkers = new ReplicatedWorkers();
		
		replicatedWorkers.readData(file_in, args);
		replicatedWorkers.doMap();
		replicatedWorkers.waitToFinishAll();
		replicatedWorkers.addReduceTask();
		replicatedWorkers.doReduce();
		replicatedWorkers.waitToFinishAll();
		replicatedWorkers.writeData(file_out);
		
	}
	
	/*
	 * Metoda asteapta sa termine toti workerii task-urile.
	 */
	public void waitToFinishAll() {

		for (int i = 0; i < NT; i++) {
			try {
				workers.get(i).join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}		
	}
	
	/*
	 * Metoda creaza wokeri pentru "map" vand ca informatii 
	 * workpool-ul pentru "map" si lista de documente.
	 * Este creat cate un task de "reduce" pentru fiecare document si 
	 * este adaugat in workpool-ul de "reduce".
	 */
	public void doMap() {
		
		for (int i = 0; i < NT; i++) {
			workers.add(new Worker(wp_map, docs));
			workers.get(i).start();
		}
		
	}
	
	/*
	 * Adauga un task de reduce pentru fiecare document in workpool-ul pentru
	 * reduce
	 */
	public void addReduceTask() {
		
		for (int i = 0; i < ND; i++) {
			PartialSolution ps = docs[i].ps;
			Task reduceTask = new Task("reduce", docs[i].fileName, i, ps);
			wp_reduce.putWork(reduceTask);
		}		
	}
	
	/*
	 * Metoda sterge workeri adaugati pentru "map" si ii creaza pe cei pentru 
	 * "reduce". Fiecare worker primeste workpool-ul pentru reduce, lista 
	 * documentelor ce contin solutiile partiale in urma "map-ului" si 
	 * lungimea maxima a cuvintelor.
	 */
	public void doReduce() {
		
		workers.clear();
		for (int i = 0; i < NT; i++) {
			workers.add(new Worker(wp_reduce, docs));
			workers.get(i).start();
		}
	}
	
	/*
	 * Metoda afiseaza datele in fisierul de iesire. Sunt sortate 
	 * descrescator documentele in functie de rang.
	 */
	public void writeData(String file_out) {
		Document aux;
	
		try {
			BufferedWriter bw = new BufferedWriter(
								new OutputStreamWriter(
								new FileOutputStream(
								new File(file_out))));
		
			for (int i = 0; i < ND - 1; i++) {
				for (int j = i + 1; j < ND; j++) {
					if (docs[i].rang < docs[j].rang) {
						aux = docs[i];
						docs[i] = docs[j];
						docs[j] = aux;
					}
				}
			}
			
			for (int i = 0; i < ND; i++) {
				bw.write(docs[i].fileName + ";" + 
						 docs[i].rangFormat + ";[" + 
						 docs[i].lengthMax + "," + 
						 docs[i].nrMax +"]\n");
			}
			bw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Metoda citeste datele din fisierul de intrare: numar thread-uri, 
	 * numar documente, nume documente. Pentru fiecare document este calculata 
	 * dimensiune si numarul de fragmente. Pentru fiecare fragment este introdus 
	 * un task de tipul "map". 
	 */
	public void readData(String file_in, String args[]) {
		// citire fisier intrare
		NT = Integer.valueOf(args[0]);
		wp_map = new WorkPool(NT);
		wp_reduce = new WorkPool(NT);
		
		BufferedReader br;
		try {
			File file = new File(file_in);
			br = new BufferedReader(
					new InputStreamReader(new FileInputStream(file)));

			// dimensiunea D (in octeti) a unui fragment
			String line  = br.readLine();
			D = Integer.parseInt(line);
			
			// numarul ND de documente de tip text de procesat
			line  = br.readLine();
			ND = Integer.parseInt(line);
			
			docs = new Document[ND];
			// pe urmatoarele ND linii: numele celor ND documente
			for (int i = 0; i < ND; i++) {
				
				// citire nume document
				line  = br.readLine();
				docs[i] = new Document(line, i);
				
				File doc = new File(line);
				// calculare lungime 
				long fileDim = doc.length();
				long nrTasks = fileDim / D;
				
				// adaugare task-uri in workpool pentru fiecare fragment din doc
				for (int index = 0; index < nrTasks; index++) {
					Task task = new Task("map", line, i, D*index, D);
					wp_map.putWork(task);
				}
				// cazul in care ultimul fragment are dimensiune < D
				if (fileDim - nrTasks * D > 0) {
					Task task = new Task("map", line, i, D*nrTasks, fileDim - nrTasks * D);
					wp_map.putWork(task);					
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
}
