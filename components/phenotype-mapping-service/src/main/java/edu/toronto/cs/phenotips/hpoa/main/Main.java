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
package edu.toronto.cs.phenotips.hpoa.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import edu.toronto.cs.phenotips.hpoa.PhenotypeMappingScriptService;
import edu.toronto.cs.phenotips.hpoa.annotation.OmimHPOAnnotations;
import edu.toronto.cs.phenotips.hpoa.annotation.PrettyPrint;
import edu.toronto.cs.phenotips.hpoa.annotation.SearchResult;
import edu.toronto.cs.phenotips.hpoa.ontology.HPO;
import edu.toronto.cs.phenotips.hpoa.ontology.Ontology;
import edu.toronto.cs.phenotips.hpoa.prediction.BNPredictor;

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
<<<<<<< HEAD
        		cl.getResource("phenotype_annotation.tab").getPath(), false));
        
        ann.loadOMIMHPO(cl.getResourceAsStream("freq.txt"));
        ann.loadPrev(cl.getResourceAsStream("rescalePrevParse.txt"));
=======
        		"/home/jsong/Document/phenotype_annotation.tab", false));
        
        ann.loadOMIMHPO(new File("/home/jsong/Document/freq.txt"));
        ann.loadPrev(new File("/home/jsong/Document/rescale_prev_parse.txt"));
>>>>>>> 46cdcbbae832dca1517d405680628e647e90de81

        Set<String> phenotypes = new HashSet<String>();
        BNPredictor predictor = new BNPredictor();
        //ICPredictor predictor = new ICPredictor();
        predictor.setAnnotation(ann);
        
        Scanner sc = new Scanner(new FileReader
<<<<<<< HEAD
        		(new File("/home/jsong/Document/patient.txt")));
        String line;
        
        PrintStream ps = new PrintStream(
        		new File("/home/jsong/Document/output.txt"));
=======
        		(new File("/home/jsong/Document/fhs_patients_inclu_neg.txt")));
        String line;
        
        PrintStream ps = new PrintStream(
        		new File("/home/jsong/Document/output-neg-all-rescale.txt"));
>>>>>>> 46cdcbbae832dca1517d405680628e647e90de81
        int ctr = 1;
        while (sc.hasNextLine()) {
        	phenotypes.clear();
        	line = sc.nextLine().trim();
        	String[] split = line.split(" ");
        	for (String s : split) {
        		phenotypes.add(s);
        	}
        	List<SearchResult> presults = predictor.getMatches(phenotypes);
        	ps.printf("Patient #%s:\n", ctr);
        	PrettyPrint.printList(presults, 20, ps);
        	ps.print("\n");
        	ctr += 1;
        }
        sc.close();
        ps.close();
    }
}
