/*   (C) Copyright 2015, Gimenez Pierre-François 
 * 
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;

import compilateur.LecteurCdXml;
import compilateur.SALADD;
import preferences.completeTree.ApprentissageGloutonLexTree;
import preferences.heuristiques.HeuristiqueDuel;
import preferences.heuristiques.VieilleHeuristique;
import preferences.heuristiques.simple.HeuristiqueEntropieNormalisee;
import recommandation.*;
import recommandation.old.AlgoSaladdOubli;

/**
 * Protocole d'évaluation
 * @author Pierre-François Gimenez
 *
 */

public class Recommandation {

	/**
	 * Les fichiers .csv sont utilisés pour la gestion de la recommandation, indépendamment de l'algorithme qui recommande
	 * Les fichiers setX_exemples.csv sont aussi utilisés pour l'apprentissage bayésien avec BNlearn et la compilation d'historique avec VDD
	 * Les fichiers setX_exemples_pour_compilation.xml sont utilisés par SALADD pour apprendre des historiques
	 * Les fichiers setX_exemples.xml (et setX_scenario.xml) peuvent être utilisés par les autres algorithmes
	 * 
	 * contraintes.xml sert à connaître les contraintes, qui sont compilées dans un SLDD.
	 * On en a aussi besoin pour connaître les variables, même lorsque les contraintes en elle-même ne sont pas utilisées
	 * 
	 * CSVconverter permet de générer les .csv et setX_exemples_pour_compilation.xml à partir de setX_exemples.xml
	 * Recommandation peut servir à générer setX_exemples.xml et setX_scenario.xml à partir des .csv
	 * @throws InterruptedException 
	 */
	
	// PARAM 1 : algo
	// PARAM 2 : dataset
	// PARAM 3 : debug (optionnel) -d
	// PARAM 4 : output fichier (optionnel) -o
	
	public static void main(String[] args)
	{
		args = new String[2];
		// Algo
		args[0] = "lextree-new";
		// Dataset
		args[1] = "renault_small_header";
		
//		args[2] = "-o";
//		args[3] = "./";
		
		if(args.length < 2)
		{
			System.out.println("Usage : algo dataset [-v] [-d] [-o]");
			System.out.println("-v : verbose");
			System.out.println("-d : debug");
			System.out.println("-o : sauvegarde les résultats dans \"algo\"_\"dataset\".data");
			System.out.println("Valeurs pour algo: rc, drc, naif, jointree, oracle, v-majority, v-popular, v-naif, lextree");
			return;
		}
		
		int ouChercher = 2;
		
		final boolean verbose = args.length > ouChercher && args[ouChercher].toLowerCase().contains("-v");
		if(verbose)
			ouChercher++;

		final boolean debug = args.length > ouChercher && args[ouChercher].toLowerCase().contains("-d");
		if(debug)
			ouChercher++;

		boolean oracle = args[0].toLowerCase().contains("oracle");
//		final boolean testRapide = false;
		final boolean sleep = debug;
		String outputFolder = null;
		final boolean outputFichier = args.length > ouChercher && args[ouChercher].toLowerCase().contains("-o");
		if(outputFichier)
		{
			ouChercher++;
			outputFolder = args[ouChercher];
			ouChercher++;
		}
				
//		final boolean half = args.length > ouChercher && args[ouChercher].toLowerCase().contains("-h");
//		if(half)
//			ouChercher++;

		
		final String dataset = args[1].trim();
		final boolean half = dataset.contains("appl");

		boolean entete = dataset.contains("header");
		final String prefixData = "datasets/"+dataset+"/";

		int nbPli = half ? 1 : 2;

		if(!new File(prefixData).exists())		
		{
			System.out.println("Dataset inconnu : "+dataset);
			return;
		}
		
		final boolean contraintesPresentes =  dataset.contains("contraintes") ;
		long lastAff;
		
		Random randomgenerator = new Random(0);
		AlgoReco recommandeur;
		
//		recommandeur = new AlgoRandom();				// Algorithme de choix aléatoire
//		recommandeur = new AlgoRBJayes(prefixData);		// Réseaux bayésiens
//		recommandeur = new AlgoLexTree(new ApprentissageLexOrder(new HeuristiqueEntropieNormalisee()), prefixData);
//		recommandeur = new AlgoOubli(30);
		
		if(oracle)
			recommandeur = new AlgoOubliRien();
		else
		{
			if(args[0].toLowerCase().contains("drc"))
				recommandeur = new AlgoDRC(10, 1);
			else if(args[0].toLowerCase().contains("rc"))
				recommandeur = new AlgoDRC(-1, 1);
			else if(args[0].toLowerCase().contains("jointree"))
				recommandeur = new AlgoRBJayes(prefixData);
			else if(args[0].toLowerCase().contains("v-maj"))
				recommandeur = new AlgoVoisinsMajorityVoter(199);
			else if(args[0].toLowerCase().contains("v-pop"))
				recommandeur = new AlgoVoisinsMostPopular(20);
			else if(args[0].toLowerCase().contains("v-nai"))
				recommandeur = new AlgoVoisinsNaive(20);
			else if(args[0].toLowerCase().contains("lextree-old"))
				recommandeur = new AlgoLexTree(new ApprentissageGloutonLexTree(300, 20, new VieilleHeuristique(new HeuristiqueEntropieNormalisee())), prefixData);
			else if(args[0].toLowerCase().contains("lextree-new"))
				recommandeur = new AlgoLexTree(new ApprentissageGloutonLexTree(300, 20, new HeuristiqueDuel()), prefixData);
			else if(args[0].toLowerCase().contains("nai"))
				recommandeur = new AlgoRBNaif();
			else
			{
				System.out.println("Algo inconnu : "+args[0]);
				return;
			}
		}
		
//		recommandeur = new AlgoRC(1);
//		recommandeur = new AlgoRBJayes(prefixData);
//		recommandeur = new AlgoARC(100000000, 1);
//		recommandeur = new AlgoVoisins(20);
//		recommandeur = new AlgoRBNaif();
//		recommandeur = new AlgoOubliRien();
		
		// Pas des algorithmes de recommandation mais de conversion vers XML. Utilisé pour la génération de données
//		recommandeur = new XMLconverter(prefixData);
//		recommandeur = new XMLconverter2(prefixData);
		
		// Legacy
		if(args.length == 1)
		{
			String o = "";
			if(oracle)
				o = "-O";
			String c = "";
			if(contraintesPresentes)
				c = "-C";
			System.out.println(dataset+o+c+"-"+recommandeur);
			return;
		}

		long toutDebut = System.currentTimeMillis();
		
		System.out.println("Début du test de "+recommandeur);
		System.out.println("Dataset = "+dataset);
		System.out.println("Oracle = "+oracle);
		System.out.println("Entete = "+entete);
		System.out.println("Output fichier = "+outputFichier);
//		System.out.println("Test rapide = "+testRapide);
		System.out.println("Contraintes = "+contraintesPresentes);
		

		int echec = 0, succes = 0, trivial = 0;

		String fichierContraintes = prefixData+"contraintes.xml";
		
		SALADD contraintes, contraintes2;
		contraintes = null;
		contraintes2 = null;
		

		if(new File(fichierContraintes).exists())			
		{
			System.err.print("Compilation des contraintes...");
			contraintes = new SALADD();
			contraintes.compilation(fichierContraintes, true, 4, 0, 0, true);
			contraintes.propagation();
			contraintes2 = new SALADD();
			contraintes2.compilation(fichierContraintes, true, 4, 0, 0, true);
			contraintes2.propagation();
			System.err.println(" finie");
		}
		else if(contraintesPresentes)
		{
			System.out.println("Pas de fichier de contraintes!");
			System.out.println("Veuillez relancez avec \"contraintesPresentes = false\"");
			return;
		}
	
		LecteurCdXml lect=new LecteurCdXml();
		// On lit le premier fichier afin de récupére le nombre de variables
		if(half)
			lect.lectureCSV(prefixData+"testing", entete);
		else
			lect.lectureCSV(prefixData+"set0_exemples", entete);
		
//		ArrayList<String> variables_tmp = new ArrayList<String>();
//		variables_tmp.addAll(contraintes.getFreeVariables());
//		recommandeur.initialisation(variables_tmp);

		ArrayList<String> variables=new ArrayList<String>();
		ArrayList<String> solutions=new ArrayList<String>();
		ArrayList<String> ordre=new ArrayList<String>();
		
//		HashMap<String,Integer[][]> matricesConfusion = new HashMap<String,Integer[][]>();
		
/*		for(String v: contraintes.getFreeVariables())
		{
			int domain = contraintes.getVar(v).domain;
			Integer[][] mat = new Integer[domain][domain];
			for(int i = 0; i < domain; i++)
				for(int j = 0; j < domain; j++)
					mat[i][j] = 0;
//			matricesConfusion.put(v, mat);
		}*/
		
		double[][] sauvTemps = new double[lect.nbvar][nbPli*lect.nbligne];
				
		long[] instancesRestantes = new long[lect.nbvar];
		int[] oubliparpos = new int[lect.nbvar];
		int[] parpos = new int[lect.nbvar];
		int[] parposTrivial = new int[lect.nbvar];
		int[] parposnb = new int[lect.nbvar];
		int[] parModalite = new int[1000];
		int[] parModaliteNb = new int[1000];
		int[] parOubli = new int[lect.nbvar];
		int[] parOubliNb = new int[lect.nbvar];
		int[] parTauxOubli = new int[11];
		int[] parTauxOubliNb = new int[11];
		long[] dureePos = new long[lect.nbvar];
		for(int i=0; i<parpos.length; i++){
			oubliparpos[i] = 0;
			parpos[i]=0;
			parposTrivial[i]=0;
			parposnb[i]=0;
			parOubli[i] = 0;
			parOubliNb[i] = 0;
			instancesRestantes[i] = 0;
			dureePos[i] = 0;
		}
		for(int i = 0; i < 11; i++)
		{
			parTauxOubli[i] = 0;
			parTauxOubliNb[i] = 0;
		}
		for(int i = 0; i < 1000; i++)
		{
			parModalite[i] = 0;
			parModaliteNb[i] = 0;
		}
		
//		if(contraintesPresentes)
		recommandeur.apprendContraintes(contraintes2);

		ArrayList<String> learning_set = new ArrayList<String>();

		if(half)
		{
			learning_set.add(prefixData+"training");
			learning_set.add(prefixData+"testing");
		}
		else
			for(int i = 0; i < 2; i++)
				learning_set.add(prefixData+"set"+i+"_exemples");

		recommandeur.initHistorique(learning_set, entete);
		
		long duree = 0;
		long avant;
		
		for(int i = 0; i < nbPli; i++)
//		for(int i = 9; i < 10; i++)
		{
			learning_set.clear();
			// Si le fichier de test n'existe pas, on passe au suivant
			if(!half && !new File(prefixData+"set"+i+"_exemples.csv").exists())
			{
				System.err.println("Non trouvé : "+prefixData+"set"+i+"_exemples.csv");
				continue;
			}
			
			// Si le fichier d'ordre des variables n'existe pas, l'ordre sera improvisé
			boolean randomOrder = !new File(prefixData+"set"+i+"_scenario.csv").exists();
				
				
			if(oracle)
			{
				if(half)
					learning_set.add(prefixData+"testing");
				else
					learning_set.add(prefixData+"set"+i+"_exemples");
			}
			else
			{
	//			int i = 0;
	//			learning_set.add("datasets/set1");
				if(half)
					learning_set.add(prefixData+"training");
				else
					for(int j = 0; j < 2; j++)
					{
						if(j != i)
						{
							String fichier = prefixData+"set"+j+"_exemples";
							if(new File(fichier+".csv").exists())
								learning_set.add(fichier);
						}
					}
			}
//			learning_set.add("datasets/set"+i);
			if(half)
				lect.lectureCSV(prefixData+"testing", entete);
			else
				lect.lectureCSV(prefixData+"set"+i+"_exemples", entete);

			if(!randomOrder)
				lect.lectureCSVordre(prefixData+"set"+i+"_scenario");
			
			if(contraintesPresentes)
			{
				contraintes.reinitialisation();
				contraintes.propagation();
			}

			recommandeur.apprendDonnees(learning_set, i, entete);

//			System.out.println("Apprentissage : "+(System.currentTimeMillis() - avant));
			lastAff = System.currentTimeMillis();
//			for(int test=0; test<1; test++)
			for(int test=0; test<lect.nbligne; test++)
			{
//				System.out.println(test);
//				avant = System.currentTimeMillis();
				variables.clear();
				solutions.clear();
				ordre.clear();
		
				for(int k=0; k<lect.nbvar; k++){
//					System.out.println("CSV : "+lect.var[k].trim()+" "+lect.domall[test][k].trim());
					variables.add(lect.var[k]);
					solutions.add(lect.domall[test][k]);
				}
				
				if(randomOrder) // on génère un ordre
				{
					boolean[] dejaTire = new boolean[lect.nbvar];
					for(int k = 0; k < lect.nbvar; k++)
						dejaTire[k] = false;

					int n;
					for(int k = 0; k < lect.nbvar; k++)
					{
						n = randomgenerator.nextInt(lect.nbvar);
						do {
							n = randomgenerator.nextInt(lect.nbvar);
						} while(dejaTire[n]);
						ordre.add(lect.var[n]);
						dejaTire[n] = true;
					}
				}
				else // on lit l'ordre du fichier si on peut
					for(int k=0; k<lect.nbvar; k++)
						ordre.add(lect.ordre[test][k].trim());
				
				recommandeur.oublieSession();
				//System.out.println("intro : "+(System.currentTimeMillis() - avant));
				for(int occu=0; occu<ordre.size(); occu++)
				{
					if(sleep)
						try {
							Thread.sleep(300);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					

//					avant = System.currentTimeMillis();
					int k = variables.indexOf(ordre.get(occu));
//					System.out.println("Recherche de "+ordre.get(occu)+" : "+k);
					String v = variables.get(k);
					String solution = solutions.get(k);
					Set<String> values = null;
					int nbModalites = 0;
					
					if(contraintes != null)
					{
						values = contraintes.getCurrentDomainOf(v);						
						nbModalites = values.size();
						if(nbModalites == 0)
						{
							System.out.println("Aucune valeur possible !");
							int z = 0;
							z = 1/z;
						}
					}
					
					if(recommandeur instanceof AlgoSaladdOubli)
						instancesRestantes[i] += ((AlgoSaladdOubli) recommandeur).count();
					
					if(contraintes != null && nbModalites == 1)
					{
						if(debug)
						{
							ArrayList<String> values_array = new ArrayList<String>();
							values_array.addAll(values);
							System.out.println(occu+" variables connues. Possible: "+values.iterator().next()+". Scénario pour "+v+": "+solution+" (trivial)");
						}
						parposTrivial[occu]++;
						trivial++;
						recommandeur.setSolution(v, solution);
						if(contraintesPresentes)
							contraintes.assignAndPropagate(v, solution);
						//System.out.println("début trivial : "+(System.currentTimeMillis() - avant));
						continue;
					}
					
					parModaliteNb[nbModalites]++;
					ArrayList<String> values_array = null;
					
					if(contraintes != null)
					{
						values_array = new ArrayList<String>();
						values_array.addAll(values);
					}

					//System.out.println("début : "+(System.currentTimeMillis() - avant));
					avant = System.nanoTime();

					String r = recommandeur.recommande(v, values_array);
					long delta = (System.nanoTime() - avant);

					sauvTemps[occu][test+i*lect.nbligne] = delta;
					dureePos[occu] += delta;
					duree += delta;
					
					//System.out.println("reco : "+(System.currentTimeMillis() - avant));
					if(contraintes != null && debug)
						System.out.print(occu+" variables connues. "+values_array.size()+" possibles. ");
					if(debug)
						System.out.print("Recommandation ("+occu+" var. connues) pour "+v+": "+r);
//					avant = System.currentTimeMillis();
					recommandeur.setSolution(v, solution);
					
					if(contraintesPresentes)
						contraintes.assignAndPropagate(v, solution);
					
//					matricesConfusion.get(v)
//						[contraintes.getVar(v).conv(solution)]
//						[contraintes.getVar(v).conv(r)]++;

					if(r != null && solution.compareTo(r)==0)
					{
						if(debug)
							System.out.println(" (succès)");
						succes++;
						if(recommandeur instanceof AlgoSaladdOubli)
						{
							int nbOubli = ((AlgoSaladdOubli)recommandeur).getNbOublis();
							oubliparpos[occu] += nbOubli;
							parOubli[nbOubli]++;
							parOubliNb[nbOubli]++;
							if(occu != 0)
							{
								int taux = (int)(10.*nbOubli/occu);
								parTauxOubli[taux]++;
								parTauxOubliNb[taux]++;
							}
						}
						parModalite[nbModalites]++;
						parpos[occu]++;
					}
					else
					{
						if(debug)
							System.out.println(" (échec, vraie valeur: "+solution+")");
						echec++;
						if(recommandeur instanceof AlgoSaladdOubli)
						{
							int nbOubli = ((AlgoSaladdOubli)recommandeur).getNbOublis();
							oubliparpos[occu] += nbOubli;
							parOubliNb[nbOubli]++;
							if(occu != 0)
							{
								int taux = (int)(10.*nbOubli/occu);
								parTauxOubliNb[taux]++;
							}
						}
					}
					parposnb[occu]++;
					
					if(System.currentTimeMillis() - lastAff >= 1000)
					{
						// Sauvegarde des résultats

						double[] intervalleSucces = new double[lect.nbvar];

						for(int j = 0; j < lect.nbvar; j++)
						{
							double tmp = ((double)parpos[j])/parposnb[j];
							tmp = tmp*(1-tmp); // variance d'une variable de Bernoulli

							intervalleSucces[j] = 1.96*Math.sqrt(tmp / (i*lect.nbligne+test));			
						}

						
						double[] intervalleTemps = new double[lect.nbvar];

						for(int l = 0; l < lect.nbvar; l++)
						{
							double tmp = 0;
							for(int j = 0; j < i*lect.nbligne+test; j++)
							{
								tmp += (sauvTemps[l][j]/1000000. - dureePos[l]/(1000000.*parposnb[l])) * (sauvTemps[l][j]/1000000. - dureePos[l]/(1000000.*parposnb[l]));				
							}
							tmp = tmp / (i*lect.nbligne+test-1); // estimateur de la variance non biaisé
//							System.out.println("Variance : "+tmp);
//							intervalleTemps[i] = Math.sqrt(tmp / 0.05);
							intervalleTemps[l] = 1.96*Math.sqrt(tmp / (i*lect.nbligne+test));			
						}
						

						if(outputFichier)
						{
							PrintWriter writer;
							try {
								String fichier = outputFolder+"/"+recommandeur+"_"+dataset+".data.tmp";
								writer = new PrintWriter(fichier, "UTF-8");
								for(int l=0; l<ordre.size(); l++)
								{
									writer.print(((double)parpos[l])/parposnb[l]);
									if(l < ordre.size()-1)
									writer.print(",");
								}
								writer.println();

								for(int l=0; l<ordre.size(); l++)
								{
									writer.print(intervalleSucces[l]);
									if(l < ordre.size()-1)
									writer.print(",");
								}
								writer.println();

								for(int l=0; l<ordre.size(); l++)
								{
									writer.print(((double)dureePos[l])/(1000000.*parposnb[l]));
									if(l < ordre.size()-1)
									writer.print(",");
								}
								writer.println();

								for(int l=0; l<ordre.size(); l++)
								{
									writer.print(intervalleTemps[l]);
									if(l < ordre.size()-1)
									writer.print(",");
								}
								writer.println();

								String out = "Résultat partiel : pli ";
								if(!half)
									out += i+" ";
								out += "à "+Math.round(test*10000./lect.nbligne)/100.+"%";
								writer.println(out);
								writer.close();

							} catch (FileNotFoundException e) {
								e.printStackTrace();
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
						}
						
						lastAff = System.currentTimeMillis();
						String efface = "\r                ";
						String out = "\rPli ";
						if(!half)
							out += i+" ";
						out += "à "+Math.round(test*10000./lect.nbligne)/100.+"%";
						try {
							System.out.write(efface.getBytes());
							System.out.write(out.getBytes());
						} catch (IOException e) {
							e.printStackTrace();
						}
						if(verbose)
						{
							System.out.println();
							System.out.println("Taux succès: "+100.*succes/(echec+succes));
							if(contraintesPresentes)
								System.out.println("Taux trivial: "+100.*trivial/(echec+succes+trivial));
							System.out.println("Durée: "+(duree));
							System.out.println("Durée moyenne d'une recommandation en ms: "+((double)duree)/(1000000.*(echec+succes)));
							System.out.println("Succès par position: ");
							for(int l=0; l<ordre.size(); l++)
								System.out.print(((double)parpos[l])/parposnb[l]+", ");
							System.out.println();
							System.out.println("Durée par position: ");
							for(int l=0; l<ordre.size(); l++)
							{
								System.out.print(((double)dureePos[l])/(1000000.*parposnb[l]));
								if(l < ordre.size()-1)
									System.out.print(", ");
							}
							System.out.println();
	
							if(recommandeur instanceof AlgoSaladdOubli)
							{
								System.out.println("Oublis par position: ");
								for(int l=0; l<ordre.size(); l++)
									System.out.print(((double)oubliparpos[l])/parposnb[l]+", ");
								System.out.println();
							}
						}
					}
					//System.out.println("après : "+(System.currentTimeMillis() - avant));
				}
//				avant = System.currentTimeMillis();
				if(contraintesPresentes)
				{
					contraintes.reinitialisation();
					contraintes.propagation();
				}
				//System.out.println("prog : "+(System.currentTimeMillis() - avant));
			}
		}
		System.out.println();
		recommandeur.termine();
/*
		for(String v: contraintes.getFreeVariables())
		{
			int domain = contraintes.getVar(v).domain;
			Integer[][] mat = matricesConfusion.get(v);
			int bon = 0, total = 0;
			for(int i = 0; i < domain; i++)
				for(int j = 0; j < domain; j++)
				{
					total += mat[i][j];
					if(i == j)
						bon += mat[i][j];
					System.out.println(v+" "+i+" "+j+" "+mat[i][j]);

				}
			System.out.println(v+" "+((double)bon)/total);
		}
		*/
		System.out.println("*** FIN DU TEST DE "+recommandeur+" SUR "+dataset);
		System.out.println();
		System.out.println();

/*		System.out.println("Exemples par position: ");
		for(int l=0; l<ordre.size(); l++)
			System.out.print(((double)instancesRestantes[l])/parposnb[0]+", ");
		System.out.println();*/
		
		if(contraintesPresentes)
		{
			System.out.println("Succès par position avec trivial: ");
			for(int l=0; l<ordre.size(); l++)
			{
				System.out.print(((double)parpos[l] + parposTrivial[l])/parposnb[0]);
				if(l < ordre.size()-1)
					System.out.print(", ");
			}
			System.out.println();
		}
		
		System.out.println("Durée par position (ms): ");
		for(int l=0; l<ordre.size(); l++)
		{
			System.out.print(((double)dureePos[l])/(1000000.*parposnb[l]));
			if(l < ordre.size()-1)
				System.out.print(", ");
		}
		System.out.println();
		
		System.out.println("Succès par position: ");
		for(int l=0; l<ordre.size(); l++)
		{
			System.out.print(((double)parpos[l])/parposnb[l]);
			if(l < ordre.size()-1)
			System.out.print(", ");
		}
		System.out.println();
	
		System.out.println("Taux succès: "+100.*succes/(echec+succes));
		if(contraintesPresentes)
			System.out.println("Taux trivial: "+100.*trivial/(echec+succes+trivial));

		// Inégalité de Hoeffding pour l'intervalle de confiance de la précision (qui est bornée) (inutilisé)
//		double intervalleSucces = Math.sqrt(Math.log(2./0.05)/(2*2*lect.nbligne));
//		System.out.println("Intervalle de confiance à 95% du succès : "+intervalleSucces);

		double[] intervalleSucces = new double[lect.nbvar];

		for(int i = 0; i < lect.nbvar; i++)
		{
			double tmp = ((double)parpos[i])/parposnb[i];
			tmp = tmp*(1-tmp); // variance d'une variable de Bernoulli

			intervalleSucces[i] = 1.96*Math.sqrt(tmp / (nbPli*lect.nbligne));			
		}
		
		System.out.println("Intervalle de confiance à 95% du succès: ");
		for(int l=0; l<ordre.size(); l++)
		{
			System.out.print(intervalleSucces[l]);
			if(l < ordre.size()-1)
			System.out.print(", ");
		}
		System.out.println();
		
		double[] intervalleTemps = new double[lect.nbvar];

		// Estimation de la variance
		// On suppose n (le nombre d'exemples) assez grand (>= 30) pour que la distribution t(n-1) soit approchée par la loi normale
		if(nbPli*lect.nbligne < 30)
			System.out.println("L'intervalle de confiance temporel n'est pas fiable ! (pas assez d'exemples)");
		
		for(int i = 0; i < lect.nbvar; i++)
		{
			double tmp = 0;
			for(int j = 0; j < nbPli*lect.nbligne; j++)
			{
				tmp += (sauvTemps[i][j]/1000000. - dureePos[i]/(1000000.*parposnb[i])) * (sauvTemps[i][j]/1000000. - dureePos[i]/(1000000.*parposnb[i]));				
			}
			tmp = tmp / (nbPli*lect.nbligne-1); // estimateur de la variance non biaisé
//			System.out.println("Variance : "+tmp);
//			intervalleTemps[i] = Math.sqrt(tmp / 0.05);
			intervalleTemps[i] = 1.96*Math.sqrt(tmp / (nbPli*lect.nbligne));			
		}
		
		System.out.println("Intervalle de confiance à 95% du temps: ");
		for(int l=0; l<ordre.size(); l++)
		{
			System.out.print(intervalleTemps[l]);
			if(l < ordre.size()-1)
			System.out.print(", ");
		}
		System.out.println();

		if(outputFichier)
		{
			PrintWriter writer;
			try {
				String fichier = outputFolder+"/"+recommandeur+"_"+dataset+".data";
				System.out.println("Sauvegardes des résultats dans "+fichier);
				writer = new PrintWriter(fichier, "UTF-8");
				for(int l=0; l<ordre.size(); l++)
				{
					writer.print(((double)parpos[l])/parposnb[l]);
					if(l < ordre.size()-1)
					writer.print(",");
				}
				writer.println();

				for(int l=0; l<ordre.size(); l++)
				{
					writer.print(intervalleSucces[l]);
					if(l < ordre.size()-1)
					writer.print(",");
				}
				writer.println();

				for(int l=0; l<ordre.size(); l++)
				{
					writer.print(((double)dureePos[l])/(1000000.*parposnb[l]));
					if(l < ordre.size()-1)
					writer.print(",");
				}
				writer.println();

				for(int l=0; l<ordre.size(); l++)
				{
					writer.print(intervalleTemps[l]);
					if(l < ordre.size()-1)
					writer.print(",");
				}
				writer.println();

				writer.close();
				(new File(outputFolder+"/"+recommandeur+"_"+dataset+".data.tmp")).delete();

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		
		System.out.println("Durée totale (ms): "+(System.currentTimeMillis() - toutDebut));
		System.out.println("Durée de la recommandation: "+duree);
		System.out.println("Nombre de recommandations: "+(echec+succes));
		System.out.println("Durée moyenne d'une recommandation (ns): "+((double)duree)/(echec+succes));
	}

}

