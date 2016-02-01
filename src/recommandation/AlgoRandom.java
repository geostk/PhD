package recommandation;

import java.util.ArrayList;
import java.util.Random;

import compilateur.SALADD;

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
 * Recommandation aléatoire
 * @author pgimenez
 *
 */

public class AlgoRandom implements AlgoReco {

	private Random r = new Random();
	
	@Override
	public void apprendContraintes(SALADD contraintes)
	{}
	
	@Override
	public void apprendDonnees(ArrayList<String> filename, int nbIter, boolean entete)
	{}

	@Override
	public String recommande(String variable, ArrayList<String> possibles)
	{
		return possibles.get(r.nextInt(possibles.size()));
	}

	@Override
	public void setSolution(String variable, String solution)
	{}

	@Override
	public void oublieSession()
	{}

	@Override
	public void termine()
	{}

	public String toString()
	{
		return getClass().getSimpleName();
	}
}