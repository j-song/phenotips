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
    @Override
    public List<SearchResult> getMatches(Collection<String> phenotypes)
    {
        List<SearchResult> result = new LinkedList<SearchResult>();
        for (AnnotationTerm o: this.annotations.getAnnotations()) {
        	double matchScore = naiveBayesScore(o, phenotypes);
        	if (matchScore > 0) {
        		result.add(new SearchResult(o.getId(), o.getName(), matchScore));
        	}
        }
        Collections.sort(result);
        return result;
    }
    
    private double naiveBayesScore(AnnotationTerm o, Collection<String> phenotypes) {
    	String omimId = o.getId();
    	double score = 1.0;
    	OmimHPOAnnotations oha = (OmimHPOAnnotations)this.annotations;
    	
    	for (String ptype : phenotypes) {
    		score *= oha.getConnetProb(omimId, ptype);
    	}
    	double prev = oha.getPrev(omimId);
    	score *= prev;
    	return score;
    }
}
