/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.hpoa.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.phenotips.hpoa.PhenotypeMappingScriptService;
import org.phenotips.hpoa.annotation.OmimHPOAnnotations;
import org.phenotips.hpoa.annotation.PrettyPrint;
import org.phenotips.hpoa.annotation.SearchResult;
import org.phenotips.hpoa.ontology.HPO;
import org.phenotips.hpoa.ontology.Ontology;
import org.phenotips.hpoa.prediction.BNPredictor;

public class Main
{
    private static PhenotypeMappingScriptService hpoa = new PhenotypeMappingScriptService();

    public static void main(String[] args) throws FileNotFoundException
    {	
		HPO hpo = new HPO();
		hpo.initialize();
		Ontology ont = HPO.getInstance();

		OmimHPOAnnotations ann = new OmimHPOAnnotations(ont);
		ClassLoader cl = ann.getClass().getClassLoader();
		ann.load(hpoa.getInputFileHandler(

				cl.getResource("phenotype_annotation.tab").getPath(), false));

		ann.loadOMIMHPO(cl.getResourceAsStream("freq.txt"));
		ann.loadPrev(cl.getResourceAsStream("rescalePrevParse.txt"));
		         
		Set<String> phenotypes = new HashSet<String>();
		BNPredictor predictor = new BNPredictor();
		//ICPredictor predictor = new ICPredictor();
		predictor.setAnnotation(ann);
		Scanner sc = new Scanner(new FileReader
				(new File("/home/jsong/Document/randwalkpatients.txt")));
		String line;

		PrintStream ps = new PrintStream(new File("/home/jsong/Document/randwalkpatients_output.txt"));

		int ctr = 0;
		int correct = 0;
		int topThree = 0;
		int topFive = 0;
		int top20 = 0;
		int top100 = 0;
		
		while (sc.hasNextLine()) {
			ctr++;
			System.out.println(ctr);
			System.out.println("Total: " + ctr);
			System.out.println("Correct: " + correct);
			System.out.println("Top Three: " + topThree);
			System.out.println("Top Five: " + topFive);
			System.out.println("Top 20: " + top20);
			System.out.println("Top 100: " + top100);
			phenotypes.clear();
			line = sc.nextLine().trim();
			String[] split = line.split("\t");
			String disease = split[0];
			split = split[1].split(" ");
			
			for (int i=0; i<split.length; i++) {
				phenotypes.add(split[i]);
			}
			
			List<SearchResult> results = predictor.getMatches(phenotypes);
			for (int i=0; i<results.size(); i++) {
				if (results.get(i).getName().equals(disease)) {
					if (i<=99) {
						top100++;
					}
					
					if (i <= 19) {
						top20++;
					}
					if (i <= 4) {
						topFive++;
					}
					
					if (i <= 2) {
						topThree++;
					}
					
					if (i == 0) {
						correct++;
					}
				}
			}
			
			PrettyPrint.printList(results, 20, ps);
			ps.println();
		}
		
		ps.println("Total: " + ctr);
		ps.println("Correct: " + correct);
		ps.println("Top Three: " + topThree);
		ps.println("Top Five: " + topFive);
		ps.println("Top 20: " + top20);
		ps.println("Top 100: " + top100);
		sc.close();
		ps.close();
	}
}
