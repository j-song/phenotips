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

public class Hungarian {
	static int step = 1;
	// mask[r, c] = 1 represents that (r, c) is marked with a star
	// mask[r, c] = 2 represents that (r, c) is marked with a prime
	// mask[r, c] = 0 otherwise
	static int[][] mask;
	static boolean[] rowCover;
	static boolean[] colCover;
	static int k;
	static double[][] cost;
	static double EPSILON = 0.001;
	
	public int[][] computeAssignment(double[][] distances) {
		step = 1;
		int row = distances.length;
		int col = distances[0].length;
		
		// the start position of an augmenting path
		int[] pathStart = null;
		cost = new double[Math.min(row, col)][Math.max(row, col)];
		k = Math.min(row, col);
		mask = new int[cost.length][cost[0].length];
		rowCover = new boolean[cost.length];
		colCover = new boolean[cost[0].length];
		
		Arrays.fill(rowCover, false);
		Arrays.fill(colCover, false);
		
		for (int i=0; i<mask.length; i++) {
			Arrays.fill(mask[i], 0);
		}
		
		if (row <= col) {
			for (int i=0; i<row; i++) {
				for (int j=0; j<col; j++) {
					cost[i][j] = distances[i][j];
				}
			}
		} else {
			for (int i=0; i<col; i++) {
				for (int j=0; j<row; j++) {
					cost[i][j] = distances[j][i];
				}
			}
		}
		
		boolean finished = false;
	
		while (! finished) {
			switch (step) {
				case 1: {
					reduceRow();
					step = 2;
					break;
				}
				
				case 2: {
					starZeros();
					step = 3;
					break;
				}
					
				case 3: {
					coverStaredZeros();
					break;
				}
				
				case 4: {
					pathStart = coverOtherZeros();
					break;
				}
				
				case 5: {
					augment(pathStart);
					break;
				}
				
				case 6: {
					subtractSmallest();
					break;
				}
				
				case 7: {
					return getResult();
				}
			}
		}
		return null;
	}
	
	private int[][] getResult() {
		int[][] result = new int[k][2];
		int ctr = 0;
		
		for (int r=0; r<cost.length; r++) {
			for (int c=0; c<cost[0].length; c++) {
				if (mask[r][c] == 1) {
					result[ctr][0] = r;
					result[ctr][1] = c;
					ctr++;
					if (ctr == k) {
						return result;
					}
				}
			}
		}
		return null;
	}
	
	private void subtractSmallest() {
		double smallest = findSmallest();
		for (int r=0; r<cost.length; r++) {
			for (int c=0; c<cost[0].length; c++) {
				if (rowCover[r]) {
					cost[r][c] += smallest;
				}
				
				if (! colCover[c]) {
					cost[r][c] -= smallest;
				}
			}
		}
		step = 4;
	}
	
	private double findSmallest() {
		double result = Double.MAX_VALUE;
		for (int r=0; r<cost.length; r++) {
			for (int c=0; c<cost[0].length; c++) {
				if (! rowCover[r] && ! colCover[c]) {
					result = Math.min(result, cost[r][c]);
				}
			}
		}
		return result;
	}
	
	private void augment(int[] pathStart) {
		ArrayList<Integer> pathRows = new ArrayList<Integer>(0);
		ArrayList<Integer> pathCols = new ArrayList<Integer>(0);
		pathRows.add(pathStart[0]);
		pathCols.add(pathStart[1]);
		
		while (true) {
			int row = colStar(pathCols.get(pathCols.size()-1));
			if (row == -1) {
				break;
			} else {
				pathRows.add(row);
				pathCols.add(pathCols.get(pathCols.size()-1));
			}
			
			int col = rowPrime(pathRows.get(pathCols.size()-1));
			pathRows.add(pathRows.get(pathRows.size()-1));
			pathCols.add(col);
		}
		
		for (int i=0; i<pathRows.size(); i++) {
			// it is a primed zero, star it
			if (i % 2 == 0) {
				mask[pathRows.get(i)][pathCols.get(i)] = 1;
			} else {
				// unstar stared zeros
				mask[pathRows.get(i)][pathCols.get(i)] = 0;
			}
		}
		
		for (int r=0; r<cost.length; r++) {
			for (int c=0; c<cost[0].length; c++) {
				if (mask[r][c] == 2) {
					mask[r][c] = 0;
				}
			}
		}
		
		// uncover all the lines
		Arrays.fill(colCover, false);
		Arrays.fill(rowCover, false);
		step = 3;
	}
	
	private int[] coverOtherZeros() {
		boolean finished = false;
		int row, col;
		int[] pos;
		int[] pathStart = new int[2];
		
		Arrays.fill(pathStart, -1);
		
		while (! finished) {
			pos = findUncoveredZero();
			row = pos[0];
			col = pos[1];
			if (row == -1) {
				finished = true;
				step = 6;
			} else {
				// prime this uncovered zero
				mask[row][col] = 2;
				int starCol;
				if ((starCol = rowStar(row)) != -1) {
					rowCover[row] = true;
					colCover[starCol] = false;
				} else {
					finished = true;
					step = 5;
					pathStart[0] = row;
					pathStart[1] = col;
				}
			}
		}
		return pathStart;
	}
	
	private int rowPrime(int row) {
		for (int c=0; c<cost[0].length; c++) {
			if (mask[row][c] == 2) {
				return c;
			}
		}
		return -1;
	}
	
	private int colStar(int col) {
		for (int r=0; r<cost.length; r++) {
			if (mask[r][col] == 1) {
				return r;
			}
		}
		return -1;
	}
	
	private int rowStar(int row) {
		for (int c=0; c<cost[0].length; c++) {
			if (mask[row][c] == 1) {
				return c;
			}
		}
		return -1;
	}
	
	private int[] findUncoveredZero() {
		int[] pos = new int[]{-1, -1};
		
		for (int r=0; r<cost.length; r++) {
			for (int c=0; c<cost[0].length; c++) {
				if (Math.abs(cost[r][c]) <= EPSILON && ! rowCover[r] && ! colCover[c]) {
					pos[0] = r;
					pos[1] = c;
					return pos;
				}
			}
		}
		return pos;
	}
	
	private void coverStaredZeros() {
		int coverColCount = 0;
		
		for (int r=0; r<cost.length; r++) {
			for (int c=0; c<cost[0].length; c++) {
				if (mask[r][c] == 1) {
					colCover[c] = true;
				}
			}
		}
		
		for (int i=0; i<colCover.length; i++) {
			coverColCount += colCover[i] ? 1 : 0;
		}
		
		if (coverColCount >= k) {
			step = 7;
		} else {
			step = 4;
		}
	}
	
	private void starZeros() {
		for (int r=0; r<cost.length; r++) {
			for (int c=0; c<cost[0].length; c++) {
				if (Math.abs(cost[r][c]) <= EPSILON && ! rowCover[r] && ! colCover[c]) {
					mask[r][c] = 1;
					rowCover[r] = true;
					colCover[c] = true;
				}
			}
		}
		
		Arrays.fill(rowCover, false);
		Arrays.fill(colCover, false);
	}
	
	private void reduceRow() {
		for (int i=0; i<cost.length; i++) {
			double minVal = Double.MAX_VALUE;
			for (int j=0; j<cost[0].length; j++) {
				minVal = Math.min(minVal, cost[i][j]);
			}
			
			for (int j=0; j<cost[0].length; j++) {
				cost[i][j] -= minVal;
			}
		}
	}
/*	
	public static void main(String[] args) {
		double[][] input = new double[7][88];
		for (int i=0; i<input.length; i++) {
			for (int j=0; j<input[0].length; j++) {
				input[i][j] = i * j / (i + j == 0 ? 1 : i + j);
			}
		}
		
		Hungarian hun = new Hungarian();
		int[][] assignment = hun.computeAssignment(input);
		double sum = 0.0;
		for (int i=0; i<assignment.length; i++) {
			sum += input[assignment[i][0]][assignment[i][1]];
		}
		System.out.println(sum);
	}*/
}
