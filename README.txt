
	--------------------------------SONEA Andreea 333CB---------------------------------------
	>>>>>>>>>>>>>>>>	TEMA 2 Algoritmi Paraleli si Distribuiti	<<<<<<<<<<<<<<<<<<




	Indexarea Documentelor folosind paradigma Map-Reduce.
	Pentru rezolvarea temei am folosit scheletul din Laboratorul 5: Replicated Workers.

	Am folosit 2 workpool-uri: unul pentru task-urile de map si unul pentru cele de reduce.
	
	ETAPE:
	- citire date si adaugare task-uri map pentru fiecare fragment din fiecare document
	- rezolvare task-uri map
	- asteptare ca toti workeri sa termine de rezolvat task-urile de map
	- adaugare task-uri reduce pentru fiecare document specificand in  fiecare task solutia partiala corespunzatoare
documentului din task
	- rezolvare task-uri reduce
	- asteptare ca toti workeri sa termine de rezolvat task-urile de reduce
	- afisare date



	CITIRE DATE
 	- citire fisier intrare: numar thread-uri, numar documente, numele documentelor. 
	Pentru fiecare document este calculata dimensiunea, respectiv numarul de task-uri pentru a acoperi toate
segmentele. Pentru fiecare segment este introdus un task in workpool-ul de map.

	MAP
	- sunt porniti workeri pentru map
	- fiecare worker va lua (sincronizat) cate un task din workpool-ul de map
	Un task de map va contine numele documentului, dimensiunea fragmentului, offset-ul fragmentului.

	In processPartialSolution sunt rezolvate task-urile.
	Pentru task-ul de tipul map :
		- este citit segmentul corespunzator. Se va citi inceputul si sfarsitul fragmentului din fisier. Daca 
fragmentul incepe in mijlocul unui cuvant, "se deplaseaza" inceputul si sfarsitul fragmentului pana la primul delimitator. 
Citirea fagmentulului se face intre cei doi delimitatori.
		- parsare fragment in cuvinte
		- fiecare cuvant rezultat este adaugat in hashMap-ul si in lista de cuvinte maximale, daca e cazul, rezultate 
in urma rezolvarii task-ului
		- adaugare hashmap si lista de cuvinte maximale rezultate in urma task-ului in solutia partiala a documentului

	Un document contine nume, index in lista de documente, solutie partiala.
	O solutie partiala contine lista de hashmap-uri si lista de liste de cuvinte maximale.
	In urma rezolvarii fiecarui task de map sunt adaugate in cele doua liste ale documentului indicat de task, hashmap-ul si lista 
de cuvinte maximale rezultata.



	REDUCE
	- sunt porniti workeri pentru reduce
	- fiecare worker va lua (sincronizat) cate un task din workpool-ul de reduce
	Un task de reduce va contine numele documentului si solutia partiala a documentului.

	Operatia de combinare pentru un document:
	- sunt combinte hashmap-urile si listele de cuvinte maximale a solutiei partiale a task-ului in unul singur : finalHashMap 
si finalMaxWords. Sunt parcurse toate hashmap-urile si din fiecare, pentru fiecare cheie continuta sunt adaugate valorile in hashmap-ul 
final. Sunt parcurse toate listele de cuvinte maximale din solutia partiala. Atunci cand se gaseste acceasi lungime maxima sunt adaugate 
cuvintele care nu exista deja. Atunci cand se gaseste o lungime mai mare este curata lista finala si sunt adaugate cuvintele din lista 
curenta.
	- este calculat numarul total de cuvinte din document

	Operatia de reducere pentru un document:
	- sunt parcurse toate cheile din finalHashMap rezultat in urma operatiei de combinare. Pentru fiecare cheie se calculeaza valoarea
fibonacci de (cheie + 1) si se inmulteste cu valoarea din hashmap. Toate rezultatele sunt adunate si impartite la numarul de cuvinte din 
document rezultand rangul.
	- sunt adaugate in document in mod sincronizat rangul, lungimea cuvantului maxim si numarul de cuvinte de lungime maxima


	AFISARE DATE
	- sunt sortate descrescator documentele in functie de rang
	- sunt afisate in fisierul de iesire : nume_document;rang;[lungime_maxima,numar_cuvinte_lungime_maxima] pe cate un rand fiecare















