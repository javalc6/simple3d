/*
License Information, 2025 Livio (javalc6)

Feel free to modify, re-use this software, please give appropriate
credit by referencing this Github repository.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

IMPORTANT NOTICE
Note that this software is freeware and it is not designed, licensed or
intended for use in mission critical, life support and military purposes.
The use of this software is at the risk of the user. 

DO NOT USE THIS SOFTWARE IF YOU DON'T AGREE WITH STATED CONDITIONS.
*/
package simple3d;

import java.util.ArrayList;
import java.util.List;

/**
 * BSPNode is a node in the Binary Space Partitioning tree.
 * v1.0 12-12-2025: first release
 */
public class BSPNode {
	Polyface3D splitter;// The polygon defining the plane
	BSPNode front;      // Polygons in front of the splitter
	BSPNode back;       // Polygons behind the splitter

	// --- Hyperparameters for the Cost Function ---
	// Adjust these weights to prioritize tree balance or minimize polygon splits.
	private final static double WEIGHT_BALANCE = 1.0;
	private final static double WEIGHT_SPLIT = 10.0;
	private final static int MAX_CANDIDATES = 20;//sampling to speed-up selection of candidate. High value: slow but optimal, Low value: fast but sub-optimal

	/**
	 * Calculates the cost of using a specific polygon as a splitter.
	 */
	private static double calculateCost(Polyface3D candidateSplitter, List<Polyface3D> list) {
		int front = 0;
		int back = 0;
		int spanning = 0;

		for (Polyface3D poly : list) {
			if (poly.equals(candidateSplitter)) continue;//skip candidate splitter itself
			int classification = poly.classify(candidateSplitter);

			if (classification == Polyface3D.FRONT) {
				front++;
			} else if (classification == Polyface3D.BACK) {
				back++;
			} else if (classification == Polyface3D.SPANNING) {
				spanning++;
			} else { // COPLANAR
				// Coplanar polygons don't affect balance or splits in the cost calculation
			}
		}

		// The cost function: Balance penalty + Split penalty
		double balancePenalty = WEIGHT_BALANCE * Math.abs(front - back);
		double splitPenalty = WEIGHT_SPLIT * spanning;

		return balancePenalty + splitPenalty;
	}

	public int size() {
		return 1 + (front != null ? front.size() : 0) + (back != null ? back.size() : 0);
	}

	public int deepness() {
		return 1 + Math.max(front != null ? front.deepness() : 0, back != null ? back.deepness() : 0);
	}

	/**
	 * Builds the BSP tree recursively from a list of polygons.
	 */
	public static BSPNode build(List<Polyface3D> list) {
		if (list == null || list.isEmpty()) {
			return null;
		}

		// --- 1. Choose near optimal splitter ---
		Polyface3D splitter = null;
		double minCost = Double.MAX_VALUE;

		int numCandidates = Math.min(list.size(), MAX_CANDIDATES);
		int step = list.size() / numCandidates;//step = 1 --> optimal selection; step > 1 --> sub-optimal selection

		for (int i = 0; i < list.size(); i += step) {
			Polyface3D candidateSplitter = list.get(i);
			
			// Calculate the cost
			double currentCost = calculateCost(candidateSplitter, list);
			if (currentCost < minCost) {
				minCost = currentCost;
				splitter = candidateSplitter;
			}
		}			
		
		if (splitter == null) {
			return null; 
		}

		BSPNode node = new BSPNode();
		node.splitter = splitter;

		// --- 2. Partition the Remaining Polygons using the Chosen Splitter ---
		List<Polyface3D> frontList = new ArrayList<>();
		List<Polyface3D> backList = new ArrayList<>();
		
		for (Polyface3D poly : list) {
			if (poly.equals(splitter)) continue;//skip splitter itself

			int classification = poly.classify(splitter);

			if (classification == Polyface3D.FRONT) {
				frontList.add(poly);
			} else if (classification == Polyface3D.BACK) {
				backList.add(poly);
			} else if (classification == Polyface3D.COPLANAR) {
				// Add coplanar polygon to the side its normal points toward for consistent ordering
				if (poly.normal.dot(splitter.normal) > 0) {
					frontList.add(poly); 
				} else {
					backList.add(poly);
				}
			} else { // SPANNING - REQUIRES SPLIT
				poly.split(splitter, frontList, backList);
			}
		}

		// --- 3. Recursively build subtrees ---
		node.front = build(frontList);
		node.back = build(backList);

		return node;
	}

	/**
	 * Traverses the tree and adds polygons to the drawing list in back-to-front order.
	 * @param cameraPos The position of the camera.
	 * @param drawList The list to populate with sorted polygons.
	 */
	public void traverse(Vector3D cameraPos, List<Polyface3D> drawList) {
		if (splitter != null) {
		// Check camera position relative to the splitter plane.
			Vector3D cameraRay = cameraPos.subtract(splitter.vertices[0], new Vector3D(0, 0, 0)); // cameraRay = cameraPos - splitterPoint
			double distance = cameraRay.dot(splitter.normal);

			if (distance > 0) { // Camera is in front of the splitter -> Draw back, draw splitter, draw front
				if (back != null) back.traverse(cameraPos, drawList);
				drawList.add(splitter);
				if (front != null) front.traverse(cameraPos, drawList);
			} else { // Camera is behind the splitter -> Draw front, draw splitter, draw back
				if (front != null) front.traverse(cameraPos, drawList);
				drawList.add(splitter);
				if (back != null) back.traverse(cameraPos, drawList);
			}
		}
	}
}
