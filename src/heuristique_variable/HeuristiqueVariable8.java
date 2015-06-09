package heuristique_variable;

import java.util.ArrayList;

import br4cp.Ordonnancement;
import br4cp.Var;

/*   (C) Copyright 2013, Schmidt Nicolas
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

public class HeuristiqueVariable8 implements HeuristiqueVariable {

	@Override
	public void reordoner(int[][] contraintes,
			Ordonnancement ord) {
		ArrayList<Var> listeTemp=new ArrayList<Var>();
		ord.constGraphAdjOriente(contraintes);
	int[] count=new int[ord.size];
	int min, suppr;
	ArrayList<Integer> reordre=new ArrayList<Integer>();
	
	//init COUNT
	for(int i=0; i<ord.size; i++)
		count[i]=0;
	
	for(int v=0; v<ord.size; v++){
		for(int vp=0; vp<ord.size; vp++){
			if(v!=vp && ord.graphAdj[v][vp]>0){		//ya un lien de v vers v'
				for(int vpp=0; vpp<ord.size; vpp++){		//on parcourt les vpps
					if(vpp!=v && vpp!=vp){							//sauf v et vp
						if(ord.graphAdj[v][vpp]>0 && ord.graphAdj[vp][vpp]==0){		// si ya un lien entre v et v" mais pas entre v' et v"
							count[v]++;
						}
					}
				}
			}
		}
	}
	//fin init
	
	//deroulement
	while(reordre.size()<ord.size){
		
		min=ord.size*2+1;
		suppr=-1;
		for(int i=0; i<ord.size; i++){		//on cherche le meilleur neud
			if(count[i]<min){
				min=count[i];
				suppr=i;
			}
		}
		reordre.add(suppr);			//on l'ajoute
		count[suppr]=ord.size*2+1;

		//System.out.println("supr="+suppr);
		//actualise count && graph adj
		for(int vp=0; vp<ord.size; vp++){
			if(ord.graphAdj[suppr][vp]>0){
				for(int vpp=0; vpp<ord.size; vpp++){
					if(ord.graphAdj[suppr][vpp]>0 && vpp!=vp){
						if(ord.graphAdj[vp][vpp]==0){
							for(int x=0; x<ord.size; x++){
								if(x!=suppr && x!=vp && x!=vpp){
									if(ord.graphAdj[x][vp]>0 && ord.graphAdj[x][vpp]>0)
										count[x]--;
									if(ord.graphAdj[x][vp]>0 && ord.graphAdj[x][vpp]==0)
										count[vp]++;
									if(ord.graphAdj[x][vp]==0 && ord.graphAdj[x][vpp]>0)
										count[vpp]++;
								}
							}
							ord.graphAdj[vp][vpp]++;
						}
					}
				}
				//on est encore dans la boucle de vp
				for(int y=0; y<ord.size; y++){
					if(y!=suppr && y!=vp && ord.graphAdj[y][vp]>0 && ord.graphAdj[y][suppr]==0)
						count[vp]--;
				}
			}
		}
		for(int i=0; i<ord.size; i++){		//on efface le suppr
			ord.graphAdj[suppr][i]=0;
			ord.graphAdj[i][suppr]=0;
		}
	}
	//fin while
	for(int i=0; i<ord.size; i++)
		listeTemp.add(ord.variables.get(reordre.get(i)));
	for(int i=0; i<listeTemp.size(); i++)
		ord.variables.set(i, listeTemp.get(i));
	}
	
}