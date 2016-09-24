package orca;

import static java.util.Arrays.asList;
import static nl.peterbloem.kit.Functions.tic;
import static nl.peterbloem.kit.Functions.toc;
import static nl.peterbloem.kit.Series.series;
import static orca.Orca.complete;
import static org.junit.Assert.*;
import static org.nodes.random.RandomGraphs.randomFast;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.nodes.Graph;
import org.nodes.Graphs;
import org.nodes.MapUTGraph;
import org.nodes.Subgraph;
import org.nodes.UGraph;
import org.nodes.UNode;
import org.nodes.algorithms.Nauty;
import org.nodes.motifs.AllSubgraphs;
import org.nodes.random.RandomGraphs;
import org.omg.Messaging.SyncScopeHelper;

import nl.peterbloem.kit.BitString;
import nl.peterbloem.kit.FrequencyModel;
import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Series;

public class OrcaTest
{

	@Test
	public void testComplete()
	{
		for(int n : series(2, 10))
		{
			UGraph<?> graph = Graphs.k(n, "");
    		
    		for(int i : Series.series(n))
    		{
    			assertEquals(1, Orca.complete(i, n, graph));
    		}
		}
	}
	
	@Test
	public void testCount4Tri()
	{
		UGraph<String> graph = new MapUTGraph<String, String>();
		
		UNode<String> a = graph.add("");
		UNode<String> b = graph.add("");
		UNode<String> c = graph.add("");
		UNode<String> d = graph.add("");
		UNode<String> e = graph.add("");
		UNode<String> f = graph.add("");
		
		a.connect(b);
		b.connect(c);
		c.connect(a);
		
		d.connect(e);
		e.connect(f);
		f.connect(d);
		
		b.connect(d);
		
		System.out.println(graph);
		
		Orca orca = new Orca(graph, false);
		
		assertEquals(2, orca.orbit(0, 0));
		assertEquals(3, orca.orbit(1, 0));
		assertEquals(2, orca.orbit(2, 0));
		assertEquals(3, orca.orbit(3, 0));
		assertEquals(2, orca.orbit(4, 0));
		assertEquals(2, orca.orbit(5, 0));

		assertEquals(1, orca.orbit(0, 1));
		assertEquals(2, orca.orbit(1, 1));
		assertEquals(1, orca.orbit(2, 1));
		assertEquals(2, orca.orbit(3, 1));
		assertEquals(1, orca.orbit(4, 1));
		assertEquals(1, orca.orbit(5, 1));
		
		assertEquals(0, orca.orbit(0, 2));
		assertEquals(2, orca.orbit(1, 2));
		assertEquals(0, orca.orbit(2, 2));
		assertEquals(2, orca.orbit(3, 2));
		assertEquals(0, orca.orbit(4, 2));
		assertEquals(0, orca.orbit(5, 2));
		
		assertEquals(1, orca.orbit(0, 3));
		assertEquals(1, orca.orbit(1, 3));
		assertEquals(1, orca.orbit(2, 3));
		assertEquals(1, orca.orbit(3, 3));
		assertEquals(1, orca.orbit(4, 3));
		assertEquals(1, orca.orbit(5, 3));
		
		assertEquals(2, orca.orbit(0, 4));
		assertEquals(0, orca.orbit(1, 4));
		assertEquals(2, orca.orbit(2, 4));
		assertEquals(0, orca.orbit(3, 4));
		assertEquals(2, orca.orbit(4, 4));
		assertEquals(2, orca.orbit(5, 4));
		
		assertEquals(0, orca.orbit(0, 5));
		assertEquals(4, orca.orbit(1, 5));
		assertEquals(0, orca.orbit(2, 5));
		assertEquals(4, orca.orbit(3, 5));
		assertEquals(0, orca.orbit(4, 5));
		assertEquals(0, orca.orbit(5, 5));
		
		assertEquals(0, orca.orbit(0, 6));
		assertEquals(0, orca.orbit(1, 6));
		assertEquals(0, orca.orbit(2, 6));
		assertEquals(0, orca.orbit(3, 6));
		assertEquals(0, orca.orbit(4, 6));
		assertEquals(0, orca.orbit(5, 6));
		
		assertEquals(0, orca.orbit(0, 7));
		assertEquals(0, orca.orbit(1, 7));
		assertEquals(0, orca.orbit(2, 7));
		assertEquals(0, orca.orbit(3, 7));
		assertEquals(0, orca.orbit(4, 7));
		assertEquals(0, orca.orbit(5, 7));
		
		assertEquals(0, orca.orbit(0, 8));
		assertEquals(0, orca.orbit(1, 8));
		assertEquals(0, orca.orbit(2, 8));
		assertEquals(0, orca.orbit(3, 8));
		assertEquals(0, orca.orbit(4, 8));
		assertEquals(0, orca.orbit(5, 8));
	
		assertEquals(0, orca.orbit(0, 9));
		assertEquals(1, orca.orbit(1, 9));
		assertEquals(0, orca.orbit(2, 9));
		assertEquals(1, orca.orbit(3, 9));
		assertEquals(0, orca.orbit(4, 9));
		assertEquals(0, orca.orbit(5, 9));
		
		assertEquals(1, orca.orbit(0, 10));
		assertEquals(0, orca.orbit(1, 10));
		assertEquals(1, orca.orbit(2, 10));
		assertEquals(0, orca.orbit(3, 10));
		assertEquals(1, orca.orbit(4, 10));
		assertEquals(1, orca.orbit(5, 10));
		
		assertEquals(0, orca.orbit(0, 11));
		assertEquals(1, orca.orbit(1, 11));
		assertEquals(0, orca.orbit(2, 11));
		assertEquals(1, orca.orbit(3, 11));
		assertEquals(0, orca.orbit(4, 11));
		assertEquals(0, orca.orbit(5, 11));
		
		assertEquals(0, orca.orbit(0, 12));
		assertEquals(0, orca.orbit(1, 12));
		assertEquals(0, orca.orbit(2, 12));
		assertEquals(0, orca.orbit(3, 12));
		assertEquals(0, orca.orbit(4, 12));
		assertEquals(0, orca.orbit(5, 12));
		
		assertEquals(0, orca.orbit(0, 13));
		assertEquals(0, orca.orbit(1, 13));
		assertEquals(0, orca.orbit(2, 13));
		assertEquals(0, orca.orbit(3, 13));
		assertEquals(0, orca.orbit(4, 13));
		assertEquals(0, orca.orbit(5, 13));
		
		assertEquals(0, orca.orbit(0, 14));
		assertEquals(0, orca.orbit(1, 14));
		assertEquals(0, orca.orbit(2, 14));
		assertEquals(0, orca.orbit(3, 14));
		assertEquals(0, orca.orbit(4, 14));
		assertEquals(0, orca.orbit(5, 14));
	}
	
	@Test
	public void testCount4()
	{
		UGraph<String> graph = Graphs.ladder(3, "");
		
		System.out.println(graph);
		
		Orca orca = new Orca(graph, false);
		
		for(int node : series(graph.size()))
		{
    		for(int orbit : series(orca.numOrbits()))
    		{
    			System.out.println("node " + node + ", orbit " + orbit + ":\t" + orca.orbit(node, orbit));
    		}
    		
    		System.out.println();
		}
	}
	
	/**
	 * Mapping from binary strings to canonical graphs
	 */
//	@Test
	public void print()
	{
		for(int n : series(2, 6))
		{
    		
    		final int m = (n*n-n)/2;
    		Set<Graph<String>> seen = new HashSet<Graph<String>>();
    
    		for(BitString bits : BitString.all(m))
    		{
    			UGraph<String> g = Graphs.fromBits(bits, "");
    			
    			if(Graphs.connected(g) && ! seen.contains(Nauty.canonize(g)))
    			{
    				System.out.println(bits + "\t" + g);
    				
    				seen.add(Nauty.canonize(g));
    			}
    		}
		}
	}
	
	@Test
	public void testComplete2()
	{
		UGraph<String> graph = new MapUTGraph<String, String>();
		
		UNode<String> x = graph.add("x");
		
		UNode<String> a = graph.add("a");
		UNode<String> b = graph.add("b");
		UNode<String> c = graph.add("c");
		UNode<String> d = graph.add("d");
		connectAll(asList(x, a, b, c, d));

		UNode<String> e = graph.add("e");
		UNode<String> f = graph.add("f");
		UNode<String> g = graph.add("g");
		UNode<String> h = graph.add("h");
		connectAll(asList(x, e, f, g, h));

		UNode<String> i = graph.add("i");
		UNode<String> j = graph.add("j");
		UNode<String> k = graph.add("k");
		UNode<String> l = graph.add("l");
		connectAll(asList(x, i, j, k, l));

		
		assertEquals(3, complete(0, 5, graph));
		
		for(int t : Series.series(1, graph.size()))
		{
			assertEquals(1, complete(t, 5, graph));		
			}
	}
	
	@Test
	public void printOrbits()
	{
		UGraph<String> graph = Graphs.ladder(3, "");
		
		System.out.println(graph);
		
		Orca orca = new Orca(graph, true);

		for(int orbit : series(orca.numOrbits()))
			for(int node : series(graph.size()))
				System.out.println("o" + orbit + ", n"+ node + ": " + orca.orbit(node, orbit));
	}
	
	@Test
	public void testCompare()
	{
		for(int n : series(2, 20))
		{
    		UGraph<String> graph = Graphs.ladder(n, "");
    		
    		//System.out.println(graph);
    		
    		Orca orca = new Orca(graph, true);
    		System.out.println("Orca finished");
    		
    		FrequencyModel<Graph<String>> fanmod = new FrequencyModel<Graph<String>>();		
    		for(Set<Integer> ind : new AllSubgraphs(graph, 5))
    		{
    			fanmod.add(c(Subgraph.uSubgraphIndices(graph, ind)));
    		}
    		System.out.println("Fanmod finished");
    		
    		for(UGraph<String> sub : Graphs.allIsoConnected(5, ""))
    		{
    			sub = c(sub);
    			
    			// System.out.println((int)fanmod.frequency(sub) + " " + orca.count(sub, true) + " " + sub);

    			assertEquals((int)fanmod.frequency(sub), orca.count(sub, true));
    		}  

		}
		
	}
	private static Comparator<String> n = Functions.natural();

	@Test
	public void testCompare2()
	{
		
		UGraph<String> graph = RandomGraphs.randomFast(20, 190);
				
		tic();
		Orca orca = new Orca(graph, true);
		System.out.println("Orca finished, " + toc() + " seconds.");
		
		tic();
		FrequencyModel<Graph<String>> fanmod = new FrequencyModel<Graph<String>>();		
		for(Set<Integer> ind : new AllSubgraphs(graph, 5))
			fanmod.add(c(Subgraph.uSubgraphIndices(graph, ind)));
		System.out.println("Fanmod finished, " + toc() + " seconds.");
		
		for(UGraph<String> sub : Graphs.allIsoConnected(5, "x"))
		{
			sub = c(sub);
			assertEquals((int)fanmod.frequency(sub), orca.count(sub, true));
		}  
	}
	
	@Test
	public void testTime()
	{
		Orca orca;
		UGraph<?> graph;
		int n = 20;
		
		double avg = 0.0;
		
		for(int i : series(n))
		{
    		// * Ideally, this should finish in under 0.1 seconds with four nodes
			//   and under 6.6 with 5 nodes.
    		graph = randomFast(5097, 22282);
    		    		
    		tic();
    		orca = new Orca(graph, true);
    		avg += toc();
		}
		
		System.out.println("average time: " + (avg/n));
		
//		// * this should finish in under 10 seconds
//		graph = randomFast(5000, 100000);
//		
//		System.out.println("Graph generated.");
//		
//		tic();
//		orca = new Orca(graph, false);
//		System.out.println("Finished in " + toc() + " seconds.");
//	
	}
	
	private UGraph<String> c(UGraph<String> graph)
	{
		return Graphs.reorder(graph, Nauty.order(graph, n));

	}

	private void connectAll(List<UNode<String>> nodes)
	{
		for(int i : series(nodes.size()))
			for(int j : series(i+1, nodes.size()))
			{
				nodes.get(i).connect(nodes.get(j));
			}	
	}
	
	@Test
	public void testOverlap()
	{
		assertEquals(1, Orca.overlap(new int[]{10}, new int[]{1, 5, 10}));
		assertEquals(2, Orca.overlap(new int[]{1, 4, 5, 8, 10, 11, 12}, new int[]{0, 8, 11}));
		assertEquals(2, Orca.overlap(new int[]{0, 8, 11}, new int[]{1, 4, 5, 8, 10, 11, 12}));
		assertEquals(3, Orca.overlap(new int[]{0, 8, 11}, new int[]{0, 8, 11}));
		assertEquals(2, Orca.overlap(new int[]{0, -1, 11}, new int[]{0, 8, 11}));
	}
	
	@Test
	public void testOverlapTiming()
	{
		double sum = 0.0;
		int size = 1000;
		for(int i : series(1000))
		{
			int[] a = new int[size];
			int[] b = new int[size];
			
			for(int j : series(size))
			{
				a[j] = Global.random().nextInt();
				b[j] = Global.random().nextInt();
				
				tic();
				Orca.overlap(a, b);
				sum += toc();
			}
		}
		
		System.out.println("time: " + sum + " seconds.");
	}
}
