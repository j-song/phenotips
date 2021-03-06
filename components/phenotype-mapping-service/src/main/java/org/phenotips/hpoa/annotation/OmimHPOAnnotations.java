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
package org.phenotips.hpoa.annotation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.phenotips.hpoa.ontology.Ontology;
import org.phenotips.hpoa.ontology.OntologyTerm;
import org.phenotips.hpoa.utils.graph.BGraph;
import org.xwiki.component.annotation.Component;

@Component
@Named("omim-hpo")
@Singleton
public class OmimHPOAnnotations extends AbstractHPOAnnotation
{
    public static final Side OMIM = BGraph.Side.L;

    private static final String OMIM_ANNOTATION_MARKER = "OMIM";

    private static final String SEPARATOR = "\t";

    private static final int MIN_EXPECTED_FIELDS = 8;

    private Hashtable<String, Double> prevalence;
    
    private Set<String> originalAnnos = new HashSet<String>();
    
    public OmimHPOAnnotations(Ontology hpo)
    {
        super(hpo);
        this.prevalence = new Hashtable<String, Double>();
    }

    public int loadPrev(InputStream source) {
    	if (source == null) {
    		return -1;
    	}
    	
    	String omimId;
    	double prob;
    	
    	try {
    		Scanner sc = new Scanner(source);
    		while (sc.hasNext()) {
    			omimId = sc.next().trim();
    			omimId = "OMIM:" + omimId;
    			prob = sc.nextDouble();
    			prevalence.put(omimId, prob);
    		}
    		sc.close();
    		return 0;
    	} catch (Exception ioe) {
    		System.err.println("Exception: " + ioe.getMessage());
    		return -1;
    	}
    }
    
    public int loadOMIMHPO(InputStream source) {
    	if (source == null) {
    		return -1;
    	}
    	
    	String omimId, hpoId;
    	double prob;
    	
    	try {
    		Scanner sc = new Scanner(source);
    		while (sc.hasNext()) {
    			omimId = sc.next();
    			hpoId = sc.next();
    			prob = sc.nextDouble();
    			String key = omimId + " " + hpoId;
    			if (connectProb.containsKey(key)) {
    				connectProb.put(key, Math.max(prob, connectProb.get(key)));
    			}
    			else {
    				connectProb.put(key, prob);
    			}
    			originalAnnos.add(omimId + " " + hpoId);
    		}
    		sc.close();		
    		propagate();   		
    		return 0;
    	} catch (Exception ioe) {
    		System.err.println("Exception: " + ioe.getMessage());
    		return -1;
    	} 	
    }

    public double getPrev(String omimId) {
    	if (prevalence.containsKey(omimId)) {
    		return prevalence.get(omimId);
    	} else {
    		return 0.0;
    	}
    }
    
    /* For each disease, fill in frequency data for phenotypes which are not 
     * present in the available file.
     */
    private void propagate() {
    	String omimId;
    	Iterator<String> iter;
    	Hashtable<String, Double> filled = new Hashtable<String, Double>();
    	for (AnnotationTerm o : this.getAnnotations()) {
    		omimId = o.getId();
    		filled = propagateParents(omimId);
    		iter = filled.keySet().iterator();
    		String key;
    		while (iter.hasNext()) {
    			key = iter.next();
    			this.connectProb.put(key, filled.get(key));
    		}
    		propagateChildren(omimId);
    	}
    }
    
    private void propagateChildren(String omimId) {
    	String hpoId;
    	List<String> nbs = new LinkedList<String>(this.getNeighborIds(omimId));
    	while (nbs.size() > 0) {
    		hpoId = nbs.remove(0);
    		OntologyTerm ont = hpo.getTerm(hpoId);
    		List<String> children = ont.getChildren();
    		String newKey;
    		int count = 0;
    		double prob = this.connectProb.get(omimId + " " + hpoId);
    		for (String childId : children) {
    			newKey = omimId + " " + childId;
    			if (!this.originalAnnos.contains(newKey)) {
    				count += 1;
    			}
    			else {
    				prob -= this.connectProb.get(newKey);
    			}
    		}
    	
    		if (prob > 0) {
    			double partial = prob / count;
    			for (String childId : children) {
    				newKey = omimId + " " + childId;
    				if (! this.originalAnnos.contains(newKey)) {
    					if (this.connectProb.containsKey(newKey)) {
    						this.connectProb.put(newKey, partial + 
    							this.getConnectProb(omimId, childId));
    					}
    					else {
    						this.connectProb.put(newKey, partial);
    						nbs.add(childId);
    					}
    				}
    			}
    		}
    	}
    }
    
    private Hashtable<String, Double> propagateParents(String omimId) {
    	String hpoId;
		List<String> nbs;
		Hashtable<String, Double> newProb = new Hashtable<String, Double>();
		nbs = new LinkedList<String>(this.getNeighborIds(omimId));
		while (nbs.size() > 0) {
			hpoId = nbs.remove(0);
			OntologyTerm ont = hpo.getTerm(hpoId);
			List<String> parents = ont.getParents();
			String newKey;
			double prob;
			try {
				prob = this.connectProb.get(omimId + " " + hpoId);
			} catch (NullPointerException npe) {
				prob = newProb.get(omimId + " " + hpoId);
			}
			for (String parentId : parents) {
				newKey = omimId + " " + parentId;
				if (! this.originalAnnos.contains(newKey)) {
					if (newProb.containsKey(newKey)) {
						newProb.put(newKey, 
								0.5 * (1 - newProb.get(newKey) * 2 * (1 - prob)));
					}
					else {
						newProb.put(newKey, 0.5 * (1 - prob));
						nbs.add(parentId);
					}
				}
			}
		}
		return newProb;
    }
    
    
    @Override
    public int load(File source)
    {
        // Make sure we can read the data
        if (source == null) {
            return -1;
        }
        clear();
        // Load data
        try {
            BufferedReader in = new BufferedReader(new FileReader(source));
            String line;
            Map<Side, AnnotationTerm> connection = new HashMap<Side, AnnotationTerm>();
            while ((line = in.readLine()) != null) {
                if (!line.startsWith(OMIM_ANNOTATION_MARKER)) {
                    continue;
                }
                String pieces[] = line.split(SEPARATOR, MIN_EXPECTED_FIELDS);
                if (pieces.length != MIN_EXPECTED_FIELDS) {
                    continue;
                }
                final String omimId = OMIM_ANNOTATION_MARKER + ":" + pieces[1], omimName = pieces[2], hpoId =
                    this.hpo.getRealId(pieces[4]), rel = pieces[3];
                if (!"NOT".equals(rel)) {
                    connection.clear();
                    connection.put(OMIM, new AnnotationTerm(omimId, omimName));
                    connection.put(HPO, new AnnotationTerm(hpoId));
                    this.addConnection(connection);
                }
            }
            in.close();
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            System.err.println("File does not exist");
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            System.err.println("Could not locate source file: " + source.getAbsolutePath());
        } catch (IOException ex) {
            // TODO Auto-generated catch blocken_product1
            ex.printStackTrace();
        }
        return size();
    }

    public Set<String> getOMIMNodesIds()
    {
        return this.getNodesIds(OMIM);
    }

    public Collection<AnnotationTerm> getOMIMNodes()
    {
        return this.getNodes(OMIM);
    }

    public AnnotationTerm getOMIMNode(String omimId)
    {
        return this.getNode(omimId, OMIM);
    }
}
