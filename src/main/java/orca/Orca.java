package orca;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static nl.peterbloem.kit.BitString.parse;
import static nl.peterbloem.kit.Functions.overlap;
import static nl.peterbloem.kit.Series.series;
import static org.nodes.Graphs.fromBits;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nodes.Graphs;
import org.nodes.Link;
import org.nodes.MapUTGraph;
import org.nodes.Node;
import org.nodes.UGraph;
import org.nodes.ULink;
import org.nodes.UNode;
import org.nodes.algorithms.Nauty;

import kotlin.collections.MapAccessorsKt;
import nl.peterbloem.kit.BitString;
import nl.peterbloem.kit.FrequencyModel;
import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Pair;
import nl.peterbloem.kit.Series;

/**
 * NOTE: Something goes wrong when a lightDgraph is passed. likely due to 
 * non-persistent links
 * 
 * @author Peter
 *
 * @param <?>
 */
public class Orca
{
	
	/**
	 * Largest orbit index for a size 4 graph
	 */
	private static final int  LARGEST_S4_ORBIT = 14;
	private UGraph<?> graph;
	
	private int[][] neighbors;
	private int[][] triangles;
	private int[] degree;
	
	Set<Pair> connected = new HashSet<Pair>();
	
	/**
	 * Canonical order of small graphs
	 */
	private static List<UGraph<String>> graphs;
	private static Map<UGraph<String>, Integer> graph2Index;
	private static Map<Integer, Integer> orbit2Graph;
	private static Map<Integer, Set<Integer>> graph2Orbits;
	
	private boolean count5;
	
	private long[][] orbit;
	
	/**
	 * Frequency of each orbit, summed over all nodes
	 */
	private FrequencyModel<Integer> orbitSums = new FrequencyModel<Integer>();
	
	public Orca(UGraph<?> graph, boolean count5)
	{
		this.graph = graph;
		this.count5 = count5;
		
		go();
	}
	
	public int numOrbits()
	{
		if(count5)
			return 73;
		return 15;
	}
	
	/**
	 * How often does the given subgraph occur?
	 * 
	 * @param graph
	 * @param canonical Is the given graph in canonical ordering? If not it is 
	 * 	transform first.
	 * @return
	 */
	public int count(UGraph<?> graph, boolean canonical)
	{
		if(graph.size() > 6)
			throw new IllegalArgumentException("Input is too big. We can only count graphs of size 5 or less.");
		
		if(graph.size() == 5 && ! count5)
			throw new IllegalArgumentException("Input is too big. Size 5 graphs were not counted.");

		UGraph<String> copy = copy(graph);
		if(! canonical)
			copy = Graphs.reorder(copy, Nauty.order(copy, N));
				
		int graphIndex = Orca.graph2Index.get(copy);
	//	System.out.println("graph index: " + graphIndex);
	//	System.out.println("     orbits: " + graph2Orbits.get(graphIndex));

		
		int sum = 0;
		for(int orbit : graph2Orbits.get(graphIndex))
		{
			//System.out.print((int)orbitSums.frequency(orbit) + " ");
			sum += orbitSums.frequency(orbit);
		}
		//System.out.println();
		
		return sum / graph.size();
	}
	
	private UGraph<String> copy(UGraph<?> graph)
	{
		MapUTGraph<String, String> copy = new MapUTGraph<String, String>();
		
		for(UNode<?> node : graph.nodes())
			copy.add("");
		
		for(ULink<?> link : graph.links())
			copy.get(link.first().index()).connect(copy.get(link.second().index()));
		
		return copy;
	}

	/**
	 * How often the given node participates in the given orbit
	 * 
	 * @param node
	 * @param orbit
	 * @return
	 */
	public long orbit(int node, int orbit)
	{
		return this.orbit[node][orbit];
	}
	
	private void go()
	{
		if(graph.numLinks() > Integer.MAX_VALUE)
			throw new IllegalArgumentException("Graphs with more than Integer.MAX_VALUE links are not supported.");
		
		// * init neighbor list, triangle count
		neighbors = new int[graph.size()][];
		triangles = new int[graph.size()][];

		degree = new int[graph.size()];
		
		for(int i : series(graph.size()))
		{
			degree[i] = graph.get(i).degree();

			neighbors[i] = new int[degree[i]];
			triangles[i] = new int[degree[i]];

			
			int j = 0;
			boolean sorted = true;
			
			for(Node<?> neighbor : graph.get(i).neighbors())
			{
				neighbors[i][j] = neighbor.index();
				
				if(j > 0 && neighbors[i][j] < neighbors[i][j - 1])
					sorted = false;
				
				j++;
			}
			
			if(! sorted)
				Arrays.sort(neighbors[i]);
			
		}
		
		for(int i : series(graph.size()))
			for(int jIndex : series(neighbors[i].length))
			{
				int j = neighbors[i][jIndex];
				
				triangles[i][jIndex] += overlap(neighbors[i], neighbors[j]);
			}
		
		for(ULink<?> link : graph.links())
			connected.add(new Pair(link.first().index(), link.second().index()));
		
		if(count5)
			count5();
		else
			count4();
	}
	
	private void count5()
	{
		orbit = new long[graph.size()][73];

		// precompute common nodes
		// Global.log().info("stage 1 - precomputing common nodes\n");
		
		FrequencyModel<Pair> common2 = new FrequencyModel<Pair>();
		FrequencyModel<Triple> common3 = new FrequencyModel<Triple>();

		for (int x = 0; x < graph.size(); x++) 
		{
			for (int aIndex = 0; aIndex < degree[x]; aIndex++) 
			{
				int a = neighbors[x][aIndex];
				
				for (int bIndex = aIndex + 1; bIndex < degree[x]; bIndex++) 
				{
					int b = neighbors[x][bIndex];
					
					common2.add(new Pair(a, b));
					
					for (int cIndex = bIndex + 1; cIndex < degree[x]; cIndex ++) 
					{
						int c = neighbors[x][cIndex];
						
						int con = 0; 
						if(connected(a,b)) con++;
						if(connected(a,c)) con++;
						if(connected(b,c)) con++;
						
						if (con < 2) 
							continue;
						
						common3.add(new Triple(a, b, c));
					}
				}
			}
		}

		// Global.log().info("stage 2 - counting full graphlets\n");
	
		// * Stores how often the node at a given index is involved in a 
		//   complete graphlet of 5 nodes
		int[] c5 = new int[graph.size()];
		for (int i : series(graph.size())) 
			c5[i] = complete(i, 5, graph);

		// set up a system of equations relating orbit counts
		// Global.log().info("stage 3 - building systems of equations\n");
		
		for (int x = 0; x < graph.size(); x++) 
		{
			// Functions.dot(x, graph.size());
			
			int[] xCommon = new int[graph.size()];
			
			// smaller graphlets
			orbit[x][0] = degree[x];
			
			for (int aIndex = 0; aIndex < degree[x]; aIndex ++) 
			{
				int a = neighbors[x][aIndex];
				
				for (int bIndex = aIndex + 1; bIndex < degree[x]; bIndex++) 
				{
					int b = neighbors[x][bIndex];
					
					if (connected(a,b)) 
						orbit[x][3]++;
					
					else orbit[x][2]++;
				}
				
				for (int bIndex = 0; bIndex < degree[a]; bIndex++) 
				{
					int b = neighbors[a][bIndex];
					
					if (b != x && !connected(x, b)) 
					{
						orbit[x][1]++;
						
						xCommon[b]++;
					}
				}
			}

			long f_71=0, f_70=0, f_67=0, f_66=0, f_58=0, f_57=0; 							// 14
			long f_69=0, f_68=0, f_64=0, f_61=0, f_60=0, f_55=0, f_48=0, f_42=0, f_41=0;	// 13
			long f_65=0, f_63=0, f_59=0, f_54=0, f_47=0, f_46=0, f_40=0; 					// 12
			long f_62=0, f_53=0, f_51=0, f_50=0, f_49=0, f_38=0, f_37=0, f_36=0; 			// 8
			long f_44=0, f_33=0, f_30=0, f_26=0; 											// 11
			long f_52=0, f_43=0, f_32=0, f_29=0, f_25=0; 									// 10
			long f_56=0, f_45=0, f_39=0, f_31=0, f_28=0, f_24=0; 							// 9
			long f_35=0, f_34=0, f_27=0, f_18=0, f_16=0, f_15=0; 							// 4
			long f_17=0; 																	// 5
			long f_22=0, f_20=0, f_19=0; 													// 6
			long f_23=0, f_21=0; 															// 7

			for (int aIndex = 0; aIndex < degree[x]; aIndex ++) 
			{
				int a = neighbors[x][aIndex];
				
				int[] aCommon = new int[graph.size()];

				for (int bIndex = 0; bIndex< degree[a]; bIndex++) 
				{
					int b = neighbors[a][bIndex];
					
					for (int cIndex = 0; cIndex < degree[b]; cIndex++) 
					{ 
						int c = neighbors[b][cIndex];
						
						if (c==a || connected(a,c)) 
							continue;
						
						aCommon[c]++;
					}
				}

				// x = orbit-14 (tetrahedron)
				for (int bIndex = aIndex + 1; bIndex < degree[x]; bIndex ++) 
				{
					int b = neighbors[x][bIndex];
					
					if (! connected(a,b)) 
						continue;
					
					for (int cIndex = bIndex + 1; cIndex < degree[x]; cIndex ++) 
					{
						int c = neighbors[x][cIndex];
						
						if (! connected(a,c) || !connected(b,c)) 
							continue;
						
						orbit[x][14]++;
						f_70 += common3.frequency(new Triple(a,b,c)) - 1;
						f_71 += (triangles[x][aIndex] > 2 && triangles[x][bIndex] > 2) ? (common3.frequency(new Triple(x,a,b)) - 1) : 0;
						f_71 += (triangles[x][aIndex] > 2 && triangles[x][cIndex] > 2) ? (common3.frequency(new Triple(x,a,c)) - 1) : 0;
						f_71 += (triangles[x][bIndex] > 2 && triangles[x][cIndex] > 2) ? (common3.frequency(new Triple(x,b,c)) - 1) : 0;
						f_67 += triangles[x][aIndex] - 2 + triangles[x][bIndex] - 2 + triangles[x][cIndex] - 2;
						f_66 += common2.frequency(new Pair(a,b)) - 2;
						f_66 += common2.frequency(new Pair(a,c)) - 2;
						f_66 += common2.frequency(new Pair(b,c)) - 2;
						f_58 += degree[x] - 3;
						f_57 += degree[a] - 3 + degree[b] - 3 + degree[c] - 3;
					}
				}

				// x = orbit-13 (diamond)
				
				for (int bIndex = 0; bIndex < degree[x]; bIndex++) 
				{
					int b = neighbors[x][bIndex];
							
					if (! connected(a,b)) 
						continue;
					
					for (int cIndex = bIndex + 1; cIndex < degree[x]; cIndex ++) 
					{
						int c = neighbors[x][cIndex];
								
						if (!connected(a,c) || connected(b,c)) 
							continue;
						
						orbit[x][13]++;
						f_69 += (triangles[x][bIndex] > 1 && triangles[x][cIndex] > 1) ? 
									(common3.frequency(new Triple(x, b, c)) - 1) : 0;
						f_68 += common3.frequency(new Triple(a, b, c)) - 1;
						f_64 += common2.frequency(new Pair(b, c)) - 2;
						f_61 += triangles[x][bIndex] - 1 + triangles[x][cIndex] - 1;
						f_60 += common2.frequency(new Pair(a,b)) - 1;
						f_60 += common2.frequency(new Pair(a,c)) - 1;
						f_55 += triangles[x][aIndex] - 2;
						f_48 += degree[b] - 2 + degree[c] - 2;
						f_42 += degree[x] - 3;
						f_41 += degree[a] - 3;
					}
				}

				// x = orbit-12 (diamond)
				for (int bIndex = aIndex + 1; bIndex < degree[x]; bIndex++) 
				{
					int b = neighbors[x][bIndex];
					
					if (!connected(a,b)) 
						continue;
					
					for (int cIndex = 0; cIndex < degree[a]; cIndex++) 
					{
						int c = neighbors[a][cIndex];
						
						if (c==x || connected(x,c) || !connected(b,c)) 
							continue;
						
						orbit[x][12]++;
						f_65 += (triangles[a][cIndex] > 1) ? common3.frequency(new Triple(a, b, c)) : 0;
						f_63 += xCommon[c] - 2; 
						f_59 += triangles[a][cIndex] - 1 + common2.frequency(new Pair(b, c)) - 1;
						f_54 += common2.frequency(new Pair(a, b))-2;
						f_47 += degree[x] - 2;
						f_46 += degree[c] - 2;
						f_40 += degree[a] - 3 + degree[b] - 3;
					}
				}

				// x = orbit-8 (cycle)
				for (int bIndex = aIndex + 1; bIndex < degree[x]; bIndex++) 
				{
					int b = neighbors[x][bIndex];
					
					if (connected(a,b)) 
						continue;
					
					for (int cIndex = 0; cIndex < degree[a]; cIndex++) 
					{
						int c = neighbors[a][cIndex];
						
						if (c==x || connected(x,c) || !connected(b,c)) 
							continue;
						
						orbit[x][8]++;
						f_62 += (triangles[a][cIndex] > 0) ? common3.frequency(new Triple(a, b, c)) : 0;
						f_53 += triangles[x][aIndex] + triangles[x][bIndex];
						f_51 += triangles[a][cIndex] + common2.frequency(new Pair(c, b));
						f_50 += xCommon[c] - 2;
						f_49 += aCommon[b] - 2;
						f_38 += degree[x] - 2;
						f_37 += degree[a] - 2 + degree[b] - 2;
						f_36 += degree[c] - 2;
					}
				}

				// x = orbit-11 (paw)
				for (int bIndex = aIndex + 1; bIndex < degree[x]; bIndex++) 
				{
					int b = neighbors[x][bIndex];
					
					if (!connected(a,b)) 
						continue;
					
					for (int cIndex = 0; cIndex < degree[x]; cIndex++) 
					{
						int c = neighbors[x][cIndex];
						
						if (c==a || c==b || connected(a,c) || connected(b,c)) 
							continue;
						
						orbit[x][11]++;
						f_44 += triangles[x][cIndex];
						f_33 += degree[x] - 3;
						f_30 += degree[c] - 1;
						f_26 += degree[a] - 2 + degree[b] - 2;
					}
				}

				// x = orbit-10 (paw)
				for (int bIndex = 0; bIndex < degree[x]; bIndex++) 
				{
					int b= neighbors[x][bIndex];
					
					if (!connected(a,b)) 
						continue;
					
					for (int cIndex = 0; cIndex < degree[b]; cIndex++)
					{
						int c = neighbors[b][cIndex];
						
						if (c==x || c==a || connected(a,c) || connected(x,c)) 
							continue;
						
						orbit[x][10]++;
						f_52 += aCommon[c] - 1;
						f_43 += triangles[b][cIndex];
						f_32 += degree[b] - 3;
						f_29 += degree[c] - 1;
						f_25 += degree[a] - 2;
					}
				}

				// x = orbit-9 (paw)
				for (int bIndex = 0; bIndex < degree[a]; bIndex++) 
				{
					int b = neighbors[a][bIndex];
					
					if (b == x || connected(x,b)) continue;
				
					for (int cIndex = bIndex + 1; cIndex < degree[a]; cIndex ++)
					{
						int c = neighbors[a][cIndex];
						
						if (c==x || ! connected(b,c) || connected(x,c)) 
							continue;
						
						orbit[x][9]++;
						f_56 += (triangles[a][bIndex] > 1 && triangles[a][cIndex] > 1) ? 
								common3.frequency(new Triple(a,b,c)) : 0;
						f_45 += common2.frequency(new Pair(b, c)) - 1;
						f_39 += triangles[a][bIndex] - 1 + triangles[a][cIndex] - 1;
						f_31 += degree[a] - 3;
						f_28 += degree[x] - 1;
						f_24 += degree[b] - 2 + degree[c] - 2;
					}
				}

				// x = orbit-4 (path)
				for (int bIndex = 0; bIndex < degree[a]; bIndex++) 
				{
					int b = neighbors[a][bIndex];
					
					if (b==x || connected(x,b)) 
						continue;
					
					for (int cIndex = 0; cIndex < degree[b]; cIndex++) 
					{
						int c = neighbors[b][cIndex];
						
						if (c==a || connected(a,c) || connected(x,c)) 
							continue;
						
						orbit[x][4]++;
						f_35 += aCommon[c] - 1;
						f_34 += xCommon[c];
						f_27 += triangles[b][cIndex];
						f_18 += degree[b] - 2;
						f_16 += degree[x] - 1;
						f_15 += degree[c] - 1;
					}
				}

				// x = orbit-5 (path)
				for (int bIndex = 0; bIndex < degree[x]; bIndex ++)
				{
					int b = neighbors[x][bIndex];
							
					if (b == a || connected(a,b)) 
						continue;
					
					for (int cIndex = 0; cIndex < degree[b]; cIndex++) {
						int c = neighbors[b][cIndex];
						
						if (c==x || connected(a,c) || connected(x,c)) 
							continue;
						
						orbit[x][5]++;
						f_17 += degree[a] - 1;
					}
				}

				// x = orbit-6 (claw)
				for (int bIndex = 0; bIndex < degree[a]; bIndex++) 
				{
					int b = neighbors[a][bIndex];
					
					if (b==x || connected(x,b)) continue;
					for (int cIndex = bIndex + 1; cIndex < degree[a]; cIndex ++) 
					{
						int c = neighbors[a][cIndex];
						
						if (c==x || connected(x,c) || connected(b,c)) continue;
						
						orbit[x][6]++;
						f_22 += degree[a] - 3;
						f_20 += degree[x] - 1;
						f_19 += degree[b] - 1 + degree[c] - 1;
					}
				}

				// x = orbit-7 (claw)
				for (int bIndex = aIndex + 1; bIndex < degree[x]; bIndex++) 
				{
					int b = neighbors[x][bIndex];
							
					if (connected(a,b)) 
						continue;
					
					for (int cIndex = bIndex + 1; cIndex < degree[x]; cIndex++) 
					{
						int c = neighbors[x][cIndex];
								
						if (connected(a,c) || connected(b,c)) 
							continue;
						
						orbit[x][7]++;
						f_23 += degree[x] - 3;
						f_21 += degree[a] - 1 + degree[b] - 1 + degree[c] - 1;
					}
				}
			}

			// solve equations
			orbit[x][72] = c5[x];
			orbit[x][71] = (f_71-12*orbit[x][72])/2;
			orbit[x][70] = (f_70-4*orbit[x][72]);
			orbit[x][69] = (f_69-2*orbit[x][71])/4;
			orbit[x][68] = (f_68-2*orbit[x][71]);
			orbit[x][67] = (f_67-12*orbit[x][72]-4*orbit[x][71]);
			orbit[x][66] = (f_66-12*orbit[x][72]-2*orbit[x][71]-3*orbit[x][70]);
			orbit[x][65] = (f_65-3*orbit[x][70])/2;
			orbit[x][64] = (f_64-2*orbit[x][71]-4*orbit[x][69]-1*orbit[x][68]);
			orbit[x][63] = (f_63-3*orbit[x][70]-2*orbit[x][68]);
			orbit[x][62] = (f_62-1*orbit[x][68])/2;
			orbit[x][61] = (f_61-4*orbit[x][71]-8*orbit[x][69]-2*orbit[x][67])/2;
			orbit[x][60] = (f_60-4*orbit[x][71]-2*orbit[x][68]-2*orbit[x][67]);
			orbit[x][59] = (f_59-6*orbit[x][70]-2*orbit[x][68]-4*orbit[x][65]);
			orbit[x][58] = (f_58-4*orbit[x][72]-2*orbit[x][71]-1*orbit[x][67]);
			orbit[x][57] = (f_57-12*orbit[x][72]-4*orbit[x][71]-3*orbit[x][70]-1*orbit[x][67]-2*orbit[x][66]);
			orbit[x][56] = (f_56-2*orbit[x][65])/3;
			orbit[x][55] = (f_55-2*orbit[x][71]-2*orbit[x][67])/3;
			orbit[x][54] = (f_54-3*orbit[x][70]-1*orbit[x][66]-2*orbit[x][65])/2;
			orbit[x][53] = (f_53-2*orbit[x][68]-2*orbit[x][64]-2*orbit[x][63]);
			orbit[x][52] = (f_52-2*orbit[x][66]-2*orbit[x][64]-1*orbit[x][59])/2;
			orbit[x][51] = (f_51-2*orbit[x][68]-2*orbit[x][63]-4*orbit[x][62]);
			orbit[x][50] = (f_50-1*orbit[x][68]-2*orbit[x][63])/3;
			orbit[x][49] = (f_49-1*orbit[x][68]-1*orbit[x][64]-2*orbit[x][62])/2;
			orbit[x][48] = (f_48-4*orbit[x][71]-8*orbit[x][69]-2*orbit[x][68]-2*orbit[x][67]-2*orbit[x][64]-2*orbit[x][61]-1*orbit[x][60]);
			orbit[x][47] = (f_47-3*orbit[x][70]-2*orbit[x][68]-1*orbit[x][66]-1*orbit[x][63]-1*orbit[x][60]);
			orbit[x][46] = (f_46-3*orbit[x][70]-2*orbit[x][68]-2*orbit[x][65]-1*orbit[x][63]-1*orbit[x][59]);
			orbit[x][45] = (f_45-2*orbit[x][65]-2*orbit[x][62]-3*orbit[x][56]);
			orbit[x][44] = (f_44-1*orbit[x][67]-2*orbit[x][61])/4;
			orbit[x][43] = (f_43-2*orbit[x][66]-1*orbit[x][60]-1*orbit[x][59])/2;
			orbit[x][42] = (f_42-2*orbit[x][71]-4*orbit[x][69]-2*orbit[x][67]-2*orbit[x][61]-3*orbit[x][55]);
			orbit[x][41] = (f_41-2*orbit[x][71]-1*orbit[x][68]-2*orbit[x][67]-1*orbit[x][60]-3*orbit[x][55]);
			orbit[x][40] = (f_40-6*orbit[x][70]-2*orbit[x][68]-2*orbit[x][66]-4*orbit[x][65]-1*orbit[x][60]-1*orbit[x][59]-4*orbit[x][54]);
			orbit[x][39] = (f_39-4*orbit[x][65]-1*orbit[x][59]-6*orbit[x][56])/2;
			orbit[x][38] = (f_38-1*orbit[x][68]-1*orbit[x][64]-2*orbit[x][63]-1*orbit[x][53]-3*orbit[x][50]);
			orbit[x][37] = (f_37-2*orbit[x][68]-2*orbit[x][64]-2*orbit[x][63]-4*orbit[x][62]-1*orbit[x][53]-1*orbit[x][51]-4*orbit[x][49]);
			orbit[x][36] = (f_36-1*orbit[x][68]-2*orbit[x][63]-2*orbit[x][62]-1*orbit[x][51]-3*orbit[x][50]);
			orbit[x][35] = (f_35-1*orbit[x][59]-2*orbit[x][52]-2*orbit[x][45])/2;
			orbit[x][34] = (f_34-1*orbit[x][59]-2*orbit[x][52]-1*orbit[x][51])/2;
			orbit[x][33] = (f_33-1*orbit[x][67]-2*orbit[x][61]-3*orbit[x][58]-4*orbit[x][44]-2*orbit[x][42])/2;
			orbit[x][32] = (f_32-2*orbit[x][66]-1*orbit[x][60]-1*orbit[x][59]-2*orbit[x][57]-2*orbit[x][43]-2*orbit[x][41]-1*orbit[x][40])/2;
			orbit[x][31] = (f_31-2*orbit[x][65]-1*orbit[x][59]-3*orbit[x][56]-1*orbit[x][43]-2*orbit[x][39]);
			orbit[x][30] = (f_30-1*orbit[x][67]-1*orbit[x][63]-2*orbit[x][61]-1*orbit[x][53]-4*orbit[x][44]);
			orbit[x][29] = (f_29-2*orbit[x][66]-2*orbit[x][64]-1*orbit[x][60]-1*orbit[x][59]-1*orbit[x][53]-2*orbit[x][52]-2*orbit[x][43]);
			orbit[x][28] = (f_28-2*orbit[x][65]-2*orbit[x][62]-1*orbit[x][59]-1*orbit[x][51]-1*orbit[x][43]);
			orbit[x][27] = (f_27-1*orbit[x][59]-1*orbit[x][51]-2*orbit[x][45])/2;
			orbit[x][26] = (f_26-2*orbit[x][67]-2*orbit[x][63]-2*orbit[x][61]-6*orbit[x][58]-1*orbit[x][53]-2*orbit[x][47]-2*orbit[x][42]);
			orbit[x][25] = (f_25-2*orbit[x][66]-2*orbit[x][64]-1*orbit[x][59]-2*orbit[x][57]-2*orbit[x][52]-1*orbit[x][48]-1*orbit[x][40])/2;
			orbit[x][24] = (f_24-4*orbit[x][65]-4*orbit[x][62]-1*orbit[x][59]-6*orbit[x][56]-1*orbit[x][51]-2*orbit[x][45]-2*orbit[x][39]);
			orbit[x][23] = (f_23-1*orbit[x][55]-1*orbit[x][42]-2*orbit[x][33])/4;
			orbit[x][22] = (f_22-2*orbit[x][54]-1*orbit[x][40]-1*orbit[x][39]-1*orbit[x][32]-2*orbit[x][31])/3;
			orbit[x][21] = (f_21-3*orbit[x][55]-3*orbit[x][50]-2*orbit[x][42]-2*orbit[x][38]-2*orbit[x][33]);
			orbit[x][20] = (f_20-2*orbit[x][54]-2*orbit[x][49]-1*orbit[x][40]-1*orbit[x][37]-1*orbit[x][32]);
			orbit[x][19] = (f_19-4*orbit[x][54]-4*orbit[x][49]-1*orbit[x][40]-2*orbit[x][39]-1*orbit[x][37]-2*orbit[x][35]-2*orbit[x][31]);
			orbit[x][18] = (f_18-1*orbit[x][59]-1*orbit[x][51]-2*orbit[x][46]-2*orbit[x][45]-2*orbit[x][36]-2*orbit[x][27]-1*orbit[x][24])/2;
			orbit[x][17] = (f_17-1*orbit[x][60]-1*orbit[x][53]-1*orbit[x][51]-1*orbit[x][48]-1*orbit[x][37]-2*orbit[x][34]-2*orbit[x][30])/2;
			orbit[x][16] = (f_16-1*orbit[x][59]-2*orbit[x][52]-1*orbit[x][51]-2*orbit[x][46]-2*orbit[x][36]-2*orbit[x][34]-1*orbit[x][29]);
			orbit[x][15] = (f_15-1*orbit[x][59]-2*orbit[x][52]-1*orbit[x][51]-2*orbit[x][45]-2*orbit[x][35]-2*orbit[x][34]-2*orbit[x][27]);
			
	  		for(int orb : series(orbit[x].length))
    			orbitSums.add(orb, orbit[x][orb]);			
		}
		
	}

	private void count4() 
	{		
		orbit = new long[graph.size()][15];
		
		// Global.log().info("stage 1 - precomputing common nodes");
		
		// Global.log().info("stage 2 - counting complete, size 4 graphlets");
		
		// * Stores how often the node at a given index is involved in a 
		//   complete graphlet of 4 nodes
		int[] c4 = new int[graph.size()];
		for (int i : series(graph.size())) 
			c4[i] = complete(i, 4, graph);
			
		// Global.log().info("stage 3 - building systems of equations\n");	
		
		for(int x = 0; x < graph.size(); x++)
		{			
    		long f_12_14 = 0, 	
    		     f_10_13 = 0,
    		     f_13_14 = 0, 	
    		     f_11_13 = 0,
    		     f_7_11 = 0, 	
    		     f_5_8 = 0,
    		     f_6_9 = 0, 	
    		     f_9_12 = 0, 	
    		     f_4_8 = 0, 	
    		     f_8_12 = 0,
    		     f_14 = c4[x];
    		
    		int[] common = new int[graph.size()];
    		    		
    		// * Size 2 graphlets
    		orbit[x][0] = degree[x];
    		
    		// * Size 4 graphlets
    		// * Loop over all connected triples where x is the middle node
    		for (int yIndex = 0; yIndex < degree[x]; yIndex++) // loop over neighbors of x
    		{
    			int y = neighbors[x][yIndex];
    			    
    			for (int zIndex = 0; zIndex < degree[y]; zIndex++)
    			{
    				int z = neighbors[y][zIndex];
    				    
    				if (connected(x, z)) // triangle
    				{ 
    					if (z < y) 
    					{
    						f_12_14 += triangles[y][zIndex] - 1;
    						f_10_13 += (degree[y] - 1 - triangles[y][zIndex]) 
    							     + (degree[z] - 1 - triangles[y][zIndex]);
    					}
    				} else {
    					common[z] ++;
    				}
    			}
    			
    			for (int zIndex = yIndex + 1; zIndex < degree[x]; zIndex ++) 
    			{
    				int z = neighbors[x][zIndex];
    			
    				if (connected(y, z)) // triangle
    				{ 
    					orbit[x][3]++;
    					
    					f_13_14 += (triangles[x][yIndex] - 1) + (triangles[x][zIndex] - 1);
    					f_11_13 += (degree[x] - 1 - triangles[x][yIndex]) + 
    							   (degree[x] - 1 - triangles[x][zIndex]);
    				} else { // path
    					orbit[x][2]++;
    					
    					f_7_11 += (degree[x] - 1 - triangles[x][yIndex] - 1) + (degree[x] - 1 - triangles[x][zIndex] - 1);
    					f_5_8 += (degree[y] - 1 - triangles[x][yIndex]) + (degree[z] - 1 - triangles[x][zIndex]);
    				}
    			}
    		}
    		
    		// * Loop over all connected triples where x is the first node
    		for (int yIndex = 0; yIndex < degree[x]; yIndex ++) 
    		{
    			int y = neighbors[x][yIndex];
    			
    			for (int zIndex = 0; zIndex < degree[y]; zIndex++) 
    			{
    				int z = neighbors[y][zIndex];
    				
    				if (x == z) 
    					continue;
    					
    				if (! connected(x, z)) 
    				{ 	// path
    					orbit[x][1] ++;
    					
    					f_6_9  += degree[y] - 1 - triangles[x][yIndex] - 1;
    					f_9_12 += triangles[y][zIndex];
    					f_4_8  += degree[z] - 1 - triangles[y][zIndex];
    					f_8_12 += common[z] - 1;
       				}
    			}
    		}
    		
    		// * Solve system of equations
    		orbit[x][14] = f_14;
    		orbit[x][13] = (f_13_14 - 6 * f_14) / 2;
    		orbit[x][12] = f_12_14 - 3 * f_14;
    		orbit[x][11] = (f_11_13 - f_13_14 + 6 * f_14) / 2;
    		orbit[x][10] = f_10_13 - f_13_14 + 6 * f_14;
    		orbit[x][9] = (f_9_12-2 * f_12_14 + 6 * f_14) / 2;
    		orbit[x][8] = (f_8_12-2 * f_12_14 + 6 * f_14) / 2;
    		orbit[x][7] = (f_13_14 + f_7_11 - f_11_13 - 6 * f_14) / 6;
    		orbit[x][6] = (2 * f_12_14 + f_6_9 - f_9_12 - 6 * f_14) / 2;
    		orbit[x][5] = 2 * f_12_14 + f_5_8 - f_8_12 - 6 * f_14;
    		orbit[x][4] = 2 * f_12_14 + f_4_8 - f_8_12 - 6 * f_14;
    		
    		for(int orb : series(orbit[x].length))
    			orbitSums.add(orb, orbit[x][orb]);
		}
	}

	private boolean connected(int a, int b)
	{
		return connected.contains(new Pair(a, b));
	}

	/**
	 * Count the the number of complete graphlets (of a given size) of which the
	 * node with index 'i' is part.
	 * @param i
	 * @param graph
	 * @return
	 */
	public static int complete(int i, int size, UGraph<?> graph)
	{
		UNode<?> node = graph.get(i);
		
		List<Integer> nodes = new ArrayList<Integer>();
		nodes.add(i);
		
		Set<Integer> candidates = new LinkedHashSet<Integer>(); 
		for(UNode<?> neigh : node.neighbors())
			candidates.add(neigh.index());
				
		return completeInner(nodes, candidates, size, graph);
	}

	/**
	 * Counts how many ways the list nodes can be extended with increasing
	 * integers, in such a way that the nodes it represents induce a complete 
	 * graph.
	 * 
	 * @param nodes
	 * @param candidates
	 * @param size
	 * @param graph
	 * @return
	 */
	private static int completeInner(
			List<Integer> nodes, 
			Set<Integer> candidates, 
			int size, UGraph<?> graph)
	{
		if(nodes.size() == size)
			return 1;
		
		int sum = 0;
		for(int c : candidates)
		{
			nodes.add(c);
			
			Set<Integer> newCandidates = new LinkedHashSet<Integer>();
			for(UNode<?> neigh : graph.get(c).neighbors())
				if(neigh.index() > c && candidates.contains(neigh.index()))
					newCandidates.add(neigh.index());
			
			sum += completeInner(nodes, newCandidates, size, graph);
			nodes.remove(nodes.size() - 1);
		}
		
		return sum;
	}
	
	private static final Comparator<String> N = Functions.natural();

	
	static {		
		graphs = new ArrayList<UGraph<String>>(30);
		
		for(int i : series(30))
			graphs.add(null);
		
		graphs.set(0, fromBits(parse("1"), ""));
		
		graphs.set(1, fromBits(parse("110"), ""));
		graphs.set(2, fromBits(parse("111"), ""));
		
		graphs.set(3, fromBits(parse("101100"), ""));
		graphs.set(4, fromBits(parse("110100"), ""));
		graphs.set(5, fromBits(parse("011110"), ""));
		graphs.set(6, fromBits(parse("111100"), ""));
		graphs.set(7, fromBits(parse("111110"), ""));
		graphs.set(8, fromBits(parse("111111"), ""));
		
		graphs.set( 9, fromBits(parse("0110101000"), ""));
		graphs.set(10, fromBits(parse("1011001000"), ""));
		graphs.set(11, fromBits(parse("1101001000"), ""));
		graphs.set(12, fromBits(parse("1110101000"), ""));
		graphs.set(13, fromBits(parse("1010111000"), ""));
		graphs.set(14, fromBits(parse("1111001000"), ""));
		graphs.set(15, fromBits(parse("0011011100"), ""));
		graphs.set(16, fromBits(parse("0111101000"), ""));
		graphs.set(17, fromBits(parse("1111101000"), ""));
		graphs.set(18, fromBits(parse("1101011100"), ""));
		graphs.set(19, fromBits(parse("1110111000"), ""));
		graphs.set(20, fromBits(parse("0111101100"), ""));
		graphs.set(21, fromBits(parse("1011011100"), ""));
		graphs.set(22, fromBits(parse("1111101100"), ""));
		graphs.set(23, fromBits(parse("1111111000"), ""));
		graphs.set(24, fromBits(parse("1111011100"), ""));
		graphs.set(25, fromBits(parse("0111111100"), ""));
		graphs.set(26, fromBits(parse("1111111100"), ""));
		graphs.set(27, fromBits(parse("1101111110"), ""));
		graphs.set(28, fromBits(parse("1111111110"), ""));
		graphs.set(29, fromBits(parse("1111111111"), ""));

		/**
		 * Canonize the graphs
		 */
		for(int i : series(graphs.size()))
		{
			UGraph<String> g = graphs.get(i);
			g = Graphs.reorder(g, Nauty.order(g, N));
			
			graphs.set(i, g);
		}
				
		graph2Index = new HashMap<UGraph<String>, Integer>();
		for(int i : series(graphs.size()))
			graph2Index.put(graphs.get(i), i);
		
		orbit2Graph = new HashMap<Integer, Integer>();
		orbit2Graph.put(0, 0);
		orbit2Graph.put(1, 1);
		orbit2Graph.put(2, 1);
		orbit2Graph.put(3, 2);
		orbit2Graph.put(4, 3);
		orbit2Graph.put(5, 3);
		orbit2Graph.put(6, 4);
		orbit2Graph.put(7, 4);
		orbit2Graph.put(8, 5);
		orbit2Graph.put(9, 6);
		orbit2Graph.put(10, 6);
		orbit2Graph.put(11, 6);
		orbit2Graph.put(12, 7);
		orbit2Graph.put(13, 7);
		orbit2Graph.put(14, 8);
		orbit2Graph.put(15, 9);
		orbit2Graph.put(16, 9);
		orbit2Graph.put(17, 9);
		orbit2Graph.put(18, 10);
		orbit2Graph.put(19, 10);
		orbit2Graph.put(20, 10);
		orbit2Graph.put(21, 10);
		orbit2Graph.put(22, 11);
		orbit2Graph.put(23, 11);
		orbit2Graph.put(24, 12);
		orbit2Graph.put(25, 12);
		orbit2Graph.put(26, 12);
		orbit2Graph.put(27, 13);
		orbit2Graph.put(28, 13);
		orbit2Graph.put(29, 13);
		orbit2Graph.put(30, 13);
		orbit2Graph.put(31, 14);
		orbit2Graph.put(32, 14);
		orbit2Graph.put(33, 14);
		orbit2Graph.put(34, 15);
		orbit2Graph.put(35, 16);
		orbit2Graph.put(36, 16);
		orbit2Graph.put(37, 16);
		orbit2Graph.put(38, 16);
		orbit2Graph.put(39, 17);
		orbit2Graph.put(40, 17);
		orbit2Graph.put(41, 17);
		orbit2Graph.put(42, 17);
		orbit2Graph.put(43, 18);
		orbit2Graph.put(44, 18);
		orbit2Graph.put(45, 19);
		orbit2Graph.put(46, 19);
		orbit2Graph.put(47, 19);
		orbit2Graph.put(48, 19);
		orbit2Graph.put(49, 20);
		orbit2Graph.put(50, 20);
		orbit2Graph.put(51, 21);
		orbit2Graph.put(52, 21);
		orbit2Graph.put(53, 21);
		orbit2Graph.put(54, 22);
		orbit2Graph.put(55, 22);
		orbit2Graph.put(56, 23);
		orbit2Graph.put(57, 23);
		orbit2Graph.put(58, 23);
		orbit2Graph.put(59, 24);
		orbit2Graph.put(60, 24);
		orbit2Graph.put(61, 24);
		orbit2Graph.put(62, 25);
		orbit2Graph.put(63, 25);
		orbit2Graph.put(64, 25);
		orbit2Graph.put(65, 26);
		orbit2Graph.put(66, 26);
		orbit2Graph.put(67, 26);
		orbit2Graph.put(68, 27);
		orbit2Graph.put(69, 27);
		orbit2Graph.put(70, 28);
		orbit2Graph.put(71, 28);
		orbit2Graph.put(72, 29);
		
		graph2Orbits = new HashMap<Integer, Set<Integer>>();
		for(int orbit : orbit2Graph.keySet())
		{
			int graph = orbit2Graph.get(orbit);
			
			if(! graph2Orbits.containsKey(graph))
				graph2Orbits.put(graph, new LinkedHashSet<Integer>());
			
			graph2Orbits.get(graph).add(orbit);
		}
	}
	

	/**
	 * Computes the overlap (number of shared elements) between two sorted arrays
	 * 
	 * @param is
	 * @param is2
	 * @return
	 */
	public static int overlap(int[] a, int[] b)
	{
		int i = 0, j = 0;
		
		int res = 0;

		while(i < a.length && j < b.length)
		{
			if(a[i] > b[j])
				j ++;
			else if(a[i] < b[j])
				i ++;
			else if(a[i] == b[j]) // superfluous, but faster, idk
			{
				res ++;
				i ++;
				j ++;
			}
		}
		
		return res;
	}
	
	/**
	 * Triple of sorted integers
	 * @author Peter
	 *
	 */
	private static class Triple
	{
		int a, b, c;

		public Triple(int x, int y, int z)
		{
			if(x < y && x < z)
			{
				a = x;
				if(y < z)
				{
					b = y;
					c = z;
				} else
				{
					b = z;
					c = y;
				}
				
			} else if(y < z)
			{
				a = y;
				if(x < z)
				{
					b = x;
					c = z;
				} else
				{
					b = z;
					c = x;
				}
				
			} else
			{
				a = z;
				if(x < y)
				{
					b = x;
					c = y;
				} else
				{
					b = y;
					c = x;
				}
			}
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + a;
			result = prime * result + b;
			result = prime * result + c;
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Triple other = (Triple) obj;
			if (a != other.a)
				return false;
			if (b != other.b)
				return false;
			if (c != other.c)
				return false;
			return true;
		}
	}
	
	private static class Pair
	{
		int a, b;

		public Pair(int x, int y)
		{
			if(x < y)
			{
    			this.a = x;
    			this.b = y;
			} else
			{
				this.a = y;
				this.b = x;
			}
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + a;
			result = prime * result + b;
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Pair other = (Pair) obj;
			if (a != other.a)
				return false;
			if (b != other.b)
				return false;
			return true;
		}
		
		
	}

}
