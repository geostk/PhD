package preferences;

import java.util.ArrayList;

import compilateurHistorique.HistoComp;
import heuristiques.HeuristiqueOrdre;

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

/**
 * Apprentissage d'un arbre lexicographique incomplet
 * @author pgimenez
 *
 */

public class ApprentissageLexTree extends ApprentissageLexStructure
{
	private int profondeurMax;
	private int seuil;

	public ApprentissageLexTree(int profondeurMax, int seuil, HeuristiqueOrdre h)
	{
		this.h = h;
		this.profondeurMax = profondeurMax;
		this.seuil = seuil;
	}
	
	// inf et sup servent juste à calculer le pourcentage
	private LexicographicStructure apprendRecursif(HistoComp historique, ArrayList<String> variablesRestantes)
	{
		LexicographicTree best = null;
		double bestEntropie = 1;
	
		ArrayList<String> variablesTmp = new ArrayList<String>();
		variablesTmp.addAll(variablesRestantes);
	
		for(String var : variablesTmp)
		{
			LexicographicTree tmp = new LexicographicTree(var, historique.nbModalites(var), h);
			tmp.setNbExemples(historique.getNbInstancesToutesModalitees(var, null, true));
			double entropie = tmp.getHeuristique();
			if(best == null || entropie < bestEntropie)
			{
				best = tmp;
				bestEntropie = entropie;
			}
		}

		// Si c'était la dernière variable, alors c'est une feuille
		if(variablesTmp.size() == 1)
			return best;
		
		variablesTmp.remove(best.getVar());
		int nbMod = best.getNbMod();
		for(int i = 0; i < nbMod; i++)
		{
			historique.conditionne(best.getVar(), best.getPref(i));
			// On conditionne par une certaine valeur
			if(variablesTmp.size() >= variables.size() - profondeurMax && historique.getNbInstances() > seuil)
				best.setEnfant(i, apprendRecursif(historique, variablesTmp));
			else
				best.setEnfant(i, apprendOrdre(historique, variablesTmp));
			historique.deconditionne(best.getVar());
		}
		// A la fin, le VDD est conditionné de la même manière qu'à l'appel
		return best;
	}
	
	public void apprendDonnees(ArrayList<String> filename, boolean entete)
	{
		super.apprendDonnees(filename, entete);
		ArrayList<String> variablesTmp = new ArrayList<String>();
		variablesTmp.addAll(variables);
		struct = apprendRecursif(historique, variables);
		struct.updateBase(base);
	}
	
	@Override
	public String toString()
	{
		return super.toString()+"-"+profondeurMax;
	}

}