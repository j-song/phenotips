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
package org.phenotips.hpoa.prediction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.phenotips.hpoa.annotation.AnnotationTerm;
import org.phenotips.hpoa.annotation.OmimHPOAnnotations;
import org.phenotips.hpoa.annotation.SearchResult;
import org.phenotips.hpoa.ontology.Ontology;
import org.phenotips.hpoa.ontology.OntologyTerm;
import org.xwiki.component.annotation.Component;

@Component
@Named("bn")
@Singleton
public class BNPredictor extends AbstractPredictor
{
	private final double BONUS = 0.01;
	HashMap<String, Double> distances = new HashMap<String, Double>();
	
    @Override
    public List<SearchResult> getMatches(Collection<String> phenotypes)
    {
    	boolean matchAll;
        Collection<String> specific = findMostSpecific(phenotypes);
        List<SearchResult> rawResult = new LinkedList<SearchResult>();
    	List<SearchResult> result = new LinkedList<SearchResult>();
        int limit = 20;
        double matchScore;
        
    	for (AnnotationTerm o : this.annotations.getAnnotations()) {
    		matchScore = naiveBayesScore(o, specific);
    		List<String> nbs = o.getNeighbors();
        	matchAll = match(specific, nbs);
        	if (matchAll) {
        		matchScore += BONUS;
        	}
        	if (matchScore > 0) {
        		rawResult.add(new SearchResult(o.getId(), o.getName(), 
        				-matchScore));
        	}
        }

        Collections.sort(rawResult);
        
        limit = Math.min(limit, rawResult.size());
        int count = 0;
        while (count < limit) {
        	AnnotationTerm o = this.annotations.getAnnotationNode(rawResult.get(count).getId());
        	matchScore = minTotalDistance(specific, o);
        	matchScore *= Math.log(1 / jaccard(phenotypes, o));
        	if (matchScore > 0) {
        		result.add(new SearchResult(rawResult.get(count).getId(), rawResult.get(count).getName(), matchScore / specific.size()));
        	}
        	count++;
        }
        System.out.println();
        Collections.sort(result);
        return result;
    }
    
    private double jaccard(Collection<String> phenotypes, AnnotationTerm o) {
    	Set<String> ptypes = new HashSet<String>(phenotypes);
    	Set<String> annoTypes = new HashSet<String>(o.getNeighbors());
    	int total = ptypes.size() + annoTypes.size();
    	ptypes.retainAll(annoTypes);
    	
    	return (ptypes.size() == 0 ? 1 : ptypes.size()) * 1.0 / total;
    }
    
    private boolean match(Collection<String> a, List<String> b) {
    	return b.containsAll(a);
    }
    
    private Collection<String> findMostSpecific(Collection<String> phenotypes) {
    	Collection<String> result = new ArrayList<String>(0);
    	boolean add;
    	for (String ptype : phenotypes) {
    		add = true;
    		for (String other : phenotypes) {
    			if (this.annotations.getOntology().getAncestors(other).contains(ptype) 
    					&& ! other.equals(ptype)) {
    				add =false;
    				break;
    			}
    		}
    		if (add) {
    			result.add(ptype);
    		}
    	}
    	return result;
    }
    
    private double naiveBayesScore(AnnotationTerm o, Collection<String> phenotypes) {
    	String omimId = o.getId();
    	double score = 1.0;
    	OmimHPOAnnotations oha = (OmimHPOAnnotations)this.annotations;
    	
    	for (String ptype : phenotypes) {
    		if (ptype.startsWith("NOT")) {
    			score *= 1 - oha.getConnectProb(o.getId(), ptype);
    		}
    		else {
    			score *= oha.getConnectProb(o.getId(), ptype);
    		}

    	}
    	double prev = oha.getPrev(omimId);
    	score *= prev;
    	return score;
    }
    
    private void init(OntologyTerm ont, AnnotationTerm o) {
    	OmimHPOAnnotations oha = (OmimHPOAnnotations) this.annotations;
    	ont.numAnnotations = Math.E + Math.exp(oha.getConnectProb(o.getId(), ont.getId()));
    	ont.totalAnnotations = 0.0;
    	for (String s : ont.getChildren()) {
    		init(this.annotations.getOntology().getTerm(s), o);
    	}
    }
    
    private double sumAnnotations(OntologyTerm term) {
        double sum = term.numAnnotations;
        Ontology hpo = this.annotations.getOntology();
        for (String ch : term.getChildren()) {
                OntologyTerm child = hpo.getTerm(ch);
                if (child.totalAnnotations > 0) {
                        sum += child.totalAnnotations;
                }
                else {
                        child.totalAnnotations = sumAnnotations(child);
                        sum += child.totalAnnotations;
                }
        }
        term.totalAnnotations = sum;
        return sum;
    }
    
    private List<String> getRealPhenotypes(Collection<String> phenotypes) {
    	OmimHPOAnnotations oha = (OmimHPOAnnotations) this.annotations;
    	List<String> result = new ArrayList<String>();
    	for (String p : phenotypes) {
    		if (oha.getOntology().getTerm(p) != null) {
    			result.add(p);
    		}
    	}
    	return result;
    }
    
    private double[][] process(double[][] pairDistance) {
    	if (pairDistance.length < pairDistance[0].length) {
    		return pairDistance;
    	} else {
    		double[][] result = new double[pairDistance.length][pairDistance.length];
    		for (int i=0; i<result.length; i++) {
    			for (int j=0; j<result[0].length; j++) {
    				result[i][j] = j < pairDistance[0].length ? pairDistance[i][j] : 100;
    			} 
    		}
    		return result;
    	}
    }
    
    private double minTotalDistance(Collection<String> phenotypes, AnnotationTerm o) {
    	OmimHPOAnnotations oha = (OmimHPOAnnotations) this.annotations;
    	init((OntologyTerm) oha.getOntology().getRoot(), o);
    	sumAnnotations((OntologyTerm) oha.getOntology().getRoot());
    	List<String> realPhenotypes = getRealPhenotypes(phenotypes);
    	int realPhenotypeSize = realPhenotypes.size();
    	double[][] pairDistance = new double[realPhenotypes.size()][o.getNeighborsCount()];
    	Iterator<String> ptypes = realPhenotypes.iterator();
    	
    	for (int i=0; i<realPhenotypeSize; i++) {
    		Arrays.fill(pairDistance[i], 100);
    	}
    	
    	int i = 0;
    	
    	HashMap<Integer, String> maps = new HashMap<Integer, String>();
    	
    	while (ptypes.hasNext()) {
    		String ptype = ptypes.next();
    		for (int j=0; j<o.getNeighborsCount(); j++) {
    			maps.put(i, ptype);
    			if (ptype.startsWith("NOT")) {
    				String key = ptype.substring(4)
    						.compareTo(o.getNeighbors().get(j)) < 0 ? 
    								ptype.substring(4) + o.getNeighbors().get(j) + o.getId() 
    								: o.getNeighbors().get(j) + ptype.substring(4) + o.getId();
    				pairDistance[i][j] = distances.containsKey(key) ? 
    						distances.get(key) 
    						:  (distance(ptype.substring(4), o.getNeighbors().get(j), o)) * 5;
    				distances.put(key, pairDistance[i][j]);
    			}
    			else {
    				String key = 
    						ptype.compareTo(o.getNeighbors().get(j)) < 0 ? 
    								ptype + o.getNeighbors().get(j) + o.getId() 
    								: o.getNeighbors().get(j) + ptype + o.getId();
    				pairDistance[i][j] = distances.containsKey(key) ? 
    						distances.get(key) 
    						: (distance(ptype, o.getNeighbors().get(j), o));
    				distances.put(key, pairDistance[i][j]);
    			}	
    		}
    		i++;
    	}
    	
    	Hungarian hun = new Hungarian();
    	
    	pairDistance = process(pairDistance);
    	
    	int[][] assignment = hun.computeAssignment(pairDistance);
    	double result = 0;
    	ArrayList<Double> minDistances = new ArrayList<Double>();
    	for (i=0; i<assignment.length; i++) {
    		if (pairDistance.length <= pairDistance[0].length) {
    			minDistances.add(pairDistance[assignment[i][0]][assignment[i][1]]);
    		} else {
    			try {
    				minDistances.add(pairDistance[assignment[i][1]][assignment[i][0]]);
    			} catch (Exception e) {
    				e.printStackTrace();
    				System.out.println(assignment[i][1]);
    				System.out.println(assignment[i][0]);
    				System.out.println(pairDistance.length);
    				System.out.println(pairDistance[0].length);
    				System.exit(0);
    			}
    		}
    	}
    	Collections.sort(minDistances);
    	for (i=1; i<minDistances.size()-1; i++) {
    		result += minDistances.get(i);
    	}
    	return result / (realPhenotypeSize <= 2 ? 1 : realPhenotypeSize - 2);
    }
    
    private double distance(String s1, String s2, AnnotationTerm o) {
    	OmimHPOAnnotations oha = (OmimHPOAnnotations) this.annotations;
    	OntologyTerm term1, term2;
    	HashSet<String> visited1 = new HashSet<String>();
    	HashSet<String> visited2 = new HashSet<String>();
    	LinkedList<String> toEx1 = new LinkedList<String>();
    	LinkedList<String> toEx2 = new LinkedList<String>();
    	toEx1.add(s1);
    	toEx2.add(s2);
    	HashMap<String, Integer> term1Dis = new HashMap<String, Integer>();
    	HashMap<String, Integer> term2Dis = new HashMap<String, Integer>();
    	HashMap<String, Double> term1Ancestors = new HashMap<String, Double>();
    	HashMap<String, Double> term2Ancestors = new HashMap<String, Double>();
    	term1Ancestors.put(s1, - Math.log(oha.getConnectProb(o.getId(), s1)));
    	term2Ancestors.put(s2, - Math.log(oha.getConnectProb(o.getId(), s2)));
    	term1Dis.put(s1, 1);
    	term2Dis.put(s2, 1);
    	while (true) {
    		String now1 = null;
    		String now2 = null;
    		List<String> parents = null;
    		
    		Set<String> common = new HashSet<String>(term1Ancestors.keySet());
    		common.retainAll(term2Ancestors.keySet());
    		if (! common.isEmpty()) {
    			Iterator<String> keys = common.iterator();
    			double minDis = Double.MAX_VALUE;
    			while (keys.hasNext()) {
    				String key = keys.next();
    				minDis = Math.min(minDis, term1Ancestors.get(key) + term2Ancestors.get(key));
    			}
    			return Double.isNaN(minDis) ? 100 : minDis;
    		}
    		
    		if (! toEx1.isEmpty()) {
    			now1 = toEx1.remove();
    			visited1.add(now1);
        		term1 = this.annotations.getOntology().getTerm(now1);
        		
        		if (term1 != null) {
        			parents = term1.getParents();
        			for (String p : parents) {
        				if (! visited1.contains(p)) {
        					toEx1.add(p);
        					visited1.add(p);
        					term1Dis.put(p, term1Dis.get(now1) + 1);
        					term1Ancestors.put(p, term1Ancestors.get(now1) - Math.log(oha.getOntology().getTerm(now1).totalAnnotations / oha.getOntology().getTerm(p).totalAnnotations));
        				}
        			}
        		}
    		}
    		
    		if (! toEx2.isEmpty()) {
    			now2 = toEx2.remove();        		
        		visited2.add(now2);
        		term2 = this.annotations.getOntology().getTerm(now2);        		
        		if (term2 != null) {
        			parents = term2.getParents();
        			for (String p : parents) {
        				if (! visited2.contains(p)) {
        					toEx2.add(p);
        					visited2.add(p);
        					term2Dis.put(p,  term2Dis.get(now2) + 1);
        					term2Ancestors.put(p, term2Ancestors.get(now2) - Math.log(oha.getOntology().getTerm(now2).totalAnnotations / oha.getOntology().getTerm(p).totalAnnotations));
        				}
        			}
        		}
    		}
    	}
    }
}
