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
package edu.toronto.cs.phenotips.hpoa.prediction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import edu.toronto.cs.phenotips.hpoa.annotation.AnnotationTerm;
import edu.toronto.cs.phenotips.hpoa.annotation.OmimHPOAnnotations;
import edu.toronto.cs.phenotips.hpoa.annotation.SearchResult;

@Component
@Named("bn")
@Singleton
public class BNPredictor extends AbstractPredictor
{
	private double allScores = 0.0;
	private final double BONUS = 0.01;
    @Override
    public List<SearchResult> getMatches(Collection<String> phenotypes)
    {
    	boolean matchAll;
    	allScores = 0.0;
        Collection<String> specific = findMostSpecific(phenotypes);
    	List<SearchResult> rawResult = new LinkedList<SearchResult>();
    	List<SearchResult> result = new LinkedList<SearchResult>();
        
    	for (AnnotationTerm o : this.annotations.getAnnotations()) {
    		double matchScore = naiveBayesScore(o, specific);
        	List<String> nbs = o.getNeighbors();
        	matchAll = match(specific, nbs);
        	if (matchAll) {
        		matchScore += BONUS;
        		allScores += BONUS;
        	}
        	if (matchScore > 0) {
        		rawResult.add(new SearchResult(o.getId(), o.getName(), 
        				matchScore));
        	}
        }
        
        for (SearchResult sr : rawResult) {
        	result.add(new SearchResult(sr.getId(), sr.getName(), 
        			sr.getScore() / allScores));
        }
        
        Collections.sort(result);
        return result;
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
<<<<<<< HEAD
    		if (ptype.startsWith("NOT")) {
    			score *= 1 - oha.getConnectProb(omimId, ptype);
    		}
    		else {
    			score *= oha.getConnectProb(omimId, ptype);
    		}
=======
    		if (ptype.startsWith("NOT"))
    			score *= 1 - oha.getConnectProb(omimId, ptype);
    		else score *= oha.getConnectProb(omimId, ptype);
>>>>>>> 46cdcbbae832dca1517d405680628e647e90de81
    	}
    	double prev = oha.getPrev(omimId);
    	score *= prev;
    	allScores += score;
    	return score;
    }
}
