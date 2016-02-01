package compilateurHistorique;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import compilateur.LecteurCdXml;


/*   (C) Copyright 2016, Gimenez Pierre-François
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

/**
 * Classe pour manipuler un historique compilé
 * @author pgimenez
 *
 */

public class HistoComp implements Serializable
{
	private static final long serialVersionUID = 1L;
	private VDD arbre;
	private Instanciation instance;
	private Variable[] variables;
	private HashMap<String, Integer> mapVar; // associe au nom d'une variable sa position dans values
	
/*	public HistoComp(String[] ordre, ArrayList<String> filename, boolean entete)
	{
		mapVar = new HashMap<String, Integer>();

		for(int i = 0; i < ordre.length; i++)
			mapVar.put(ordre[i], i);
		
		VDD.setOrdreVariables(ordre.length);
		instance = new Instanciation(ordre.length);
		arbre = new VDD();
//		values = new String[ordre.length];
		deconditionneTout();
		
		compileHistorique(filename, entete);
	}*/
	
	public HistoComp(ArrayList<String> filename, boolean entete)
	{
		variables = initVariables(filename, entete);
	}
	
	public void compile(ArrayList<String> filename, boolean entete)
	{
		for(int i = 0; i < variables.length-1; i++)
		{
			int indicemax = 0;
			for(int j = 1; j < variables.length-i; j++)
			{
				if(variables[j].domain > variables[indicemax].domain)
					indicemax = j;
			}
			// On ne fait l'échange que s'il y a besoin
			if(variables[variables.length-1-i].domain != variables[indicemax].domain)
			{
				Variable tmp = variables[variables.length-1-i];
				variables[variables.length-1-i] = variables[indicemax];
				variables[indicemax] = tmp;
			}
		}

		for(int i = 0; i < variables.length; i++)
			variables[i].profondeur = i;

		mapVar = new HashMap<String, Integer>();

		for(int i = 0; i < variables.length; i++)
			mapVar.put(variables[i].name, i);

		VDD.setOrdreVariables(variables);
		arbre = new VDD();
//		values = new String[ordre.length];
		
		Instanciation.setVars(variables, mapVar);
		instance = new Instanciation();
		deconditionneTout();
		
		compileHistorique(filename, entete);
	}
	
	/**
	 * Initialise les valeurs et les domaines des variables.
	 * IL N'Y A PAS D'APPRENTISSAGE SUR LES VALEURS
	 * @param filename
	 * @param entete
	 * @return
	 */
	private Variable[] initVariables(ArrayList<String> filename, boolean entete)
	{
		// Vérification de toutes les valeurs possibles pour les variables
		Variable[] vars = null;
		for(String s : filename)
		{
			LecteurCdXml lect = new LecteurCdXml();
			lect.lectureCSV(s, entete);

			if(vars == null)
			{
				vars = new Variable[lect.nbvar];
				for(int i = 0; i < lect.nbvar; i++)
				{
					vars[i] = new Variable();
					vars[i].name = lect.var[i];
					vars[i].domain = 0;
				}
			}

			for(int i = 0; i < lect.nbligne; i++)
			{
				for(int k = 0; k < lect.nbvar; k++)
				{
					String value = lect.domall[i][k];
					if(!vars[k].values.contains(value))
					{
						vars[k].values.add(value);
						vars[k].domain++;
					}
				}
			}
		}
		return vars;
	}
	
	private void compileHistorique(ArrayList<String> filename, boolean entete)
	{
		for(String s : filename)
		{
			LecteurCdXml lect = new LecteurCdXml();
			lect.lectureCSV(s, entete);
			String[] values = new String[lect.nbvar];
//			System.out.println(lect.nbligne+" exemples");
			for(int i = 0; i < lect.nbligne; i++)
			{
				for(int k = 0; k < lect.nbvar; k++)
				{
					String var = lect.var[k];	
//					System.out.print(var+" ("+lect.domall[i][k]+"), ");
					values[mapVar.get(var)] = lect.domall[i][k];
				}
//				System.out.println();
				arbre.addInstanciation(values);
			}
		}
	}

	/**
	 * Sauvegarde par sérialisation
	 */
	public void save(String s)
	{
		File fichier =  new File(s+".sav") ;
		ObjectOutputStream oos;
		try {
			oos = new ObjectOutputStream(new FileOutputStream(fichier));
			oos.writeObject(this);
			oos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Chargement d'un historique compilé sauvegardé
	 * @return
	 */
	public static HistoComp load(String s)
	{
		HistoComp out;
		File fichier =  new File(s+".sav") ;
		ObjectInputStream ois;
		try {
			ois = new ObjectInputStream(new FileInputStream(fichier));
			out = (HistoComp)ois.readObject() ;
			ois.close();
			return out;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void conditionne(String v, String value)
	{
		conditionne(mapVar.get(v), value);
	}
	
	public void conditionne(String v, int value)
	{
		int var = mapVar.get(v);
		conditionne(var, variables[var].values.get(value));
	}

	private void conditionne(int v, String value)
	{
		if(instance.values[v] != value)
		{
			if(instance.values[v] == null)
				instance.nbVarInstanciees++;
			instance.values[v] = value;
		}
	}
	
	public void deconditionne(ArrayList<String> l)
	{
		for(String s : l)
			deconditionne(s);
	}

	public void deconditionne(String v)
	{
		deconditionne(mapVar.get(v));
	}
	
/*	public void deconditionne(Var v)
	{
		deconditionne(v.name);
	}*/

	private void deconditionne(int v)
	{
		if(instance.values[v] != null)
		{
			instance.nbVarInstanciees--;
			instance.values[v] = null;
		}
	}
	
	public void deconditionneTout()
	{
		for(int i = 0; i < instance.values.length; i++)
			instance.values[i] = null;
		instance.nbVarInstanciees = 0;
	}
	
	public Instanciation getCurrentState()
	{
		// On ne donne pas l'objet mais une copie (sinon il pourrait être modifié!)
		return instance.clone();
	}
	
	public void loadSavedState(Instanciation instance)
	{
		this.instance = instance;
	}
	
	public HashMap<String, Double> getProbaToutesModaliteesWithZero(String variable)
	{
		return getProbaToutesModalitees(variable, null, true);
	}

	public HashMap<String, Double> getProbaToutesModalitees(String variable)
	{
		return getProbaToutesModalitees(variable, null, false);
	}
	/**
	 * Retourne des proba (entre 0 et 1 donc)
	 * @param variable
	 * @param possibles
	 * @return
	 */
	public HashMap<String, Double> getProbaToutesModalitees(String variable, ArrayList<String> possibles, boolean withZero)
	{
		HashMap<String, Double> out = new HashMap<String, Double>();
		HashMap<String, Integer> exemples = getNbInstancesToutesModalitees(variable, possibles, withZero);
		
		double somme = 0.;
		for(Integer i : exemples.values())
			somme += i;
		
		for(String s : exemples.keySet())
			out.put(s, exemples.get(s) / somme);
		
		return out;
	}

	public HashMap<String, Integer> getNbInstancesToutesModalitees(String variable)
	{
		return getNbInstancesToutesModalitees(variable, null, false);
	}

	/**
	 * Retourne le nombre d'exemples pour chaque modalité
	 * @param variable
	 * @param possibles
	 * @return
	 */
	public HashMap<String, Integer> getNbInstancesToutesModalitees(String variable, ArrayList<String> possibles, boolean withZero)
	{
		int var = mapVar.get(variable);
		if(instance.values[var] != null)
		{
			System.out.println("Attention, variable déjà instanciée");
			deconditionne(var);
		}
		
		HashMap<String, Integer> out = new HashMap<String, Integer>();;

		if(withZero)
			for(String s : variables[var].values)
				out.put(s, 0);
		
//		System.out.println("Nb exemples : " + arbre.getNbInstances(values, nbVarInstanciees));
		

/*		if(possibles != null) // commenté car pas du tout efficace en temps d'exécution
		{
			out = new HashMap<String, Integer>();
			for(String p : possibles)
			{
				values[var] = p;
				out.put(p, arbre.getNbInstances(values, nbVarInstanciees + 1));
			}
			values[var] = null;
		}
		else
		{*/
			arbre.getNbInstancesToutesModalitees(out, var, instance.values, possibles, instance.nbVarInstanciees);
/*			int somme = 0;
			for(Integer i : out.values())
				somme += i;
			if(somme != arbre.getNbInstances(values, nbVarInstanciees))
				System.out.println("Erreur de calcul du nombre d'instances!");*/
//		}
		
//		conditionne(var, sauv);
		return out;
	}
	
	public int getNbInstances()
	{
		return getNbInstances(instance);
	}
	
	public int nbModalites(String v)
	{
		return variables[mapVar.get(v)].domain;
	}
	
	public int getNbInstances(Instanciation instance)
	{
		return arbre.getNbInstances(instance.values, instance.nbVarInstanciees);
	}
	
	public int getNbNoeuds()
	{
		return arbre.getNbNoeuds();
	}
	
	public ArrayList<String> getVarConnues()
	{
		return getVarConnues(instance);
	}	
	
	public ArrayList<String> getVarConnues(Instanciation instance)
	{
		ArrayList<String> varConnues = new ArrayList<String>();
		for(String v : mapVar.keySet())
			if(instance.values[mapVar.get(v)] != null)
				varConnues.add(v);
		return varConnues;
	}
	
	public IteratorInstances getIterator(String var)
	{
		ArrayList<String> cutset = new ArrayList<String>();
		cutset.add(var);
		return new IteratorInstances(instance, variables, mapVar, cutset);
	}

	public IteratorInstances getIterator(Instanciation instance, ArrayList<String> cutset)
	{
		return new IteratorInstances(instance, variables, mapVar, cutset);
	}
	
	/**
	 * Retourne le nombre total d'exemples
	 * @return
	 */
	public int getNbInstancesTotal()
	{
		return arbre.getNbInstances(null, 0);
	}

	public ArrayList<String> getValues(String variable)
	{
		return variables[mapVar.get(variable)].values;
	}
	
	@Override
	public String toString()
	{
		return instance.toString();
	}
}