package com.ads;

import java.util.*;

/**
 * Maintains a serialization graph to detect cycles with two consecutive RW edges.
 * This is essential for ensuring serializability in Snapshot Isolation.
 * Based on the FLOOS theorem: non-serializable executions under SI must contain
 * two consecutive RW (read-write anti-dependency) edges in a cycle.
 * @author Claude Code
 * @version 1.0 (Created: 2025-11-30)
 */
public class SerializationGraph {
    /**
     * Types of edges in the serialization graph:
     * - WW: Write-Write dependency (T1 writes x, T2 writes x, commit(T1) < start(T2))
     * - WR: Write-Read dependency (T1 writes x, T2 reads x, commit(T1) < start(T2))
     * - RW: Read-Write anti-dependency (T1 reads x, T2 writes x, start(T1) < commit(T2))
     */
    public enum EdgeType {
        WW,  // Write-Write
        WR,  // Write-Read
        RW   // Read-Write (anti-dependency)
    }

    // Adjacency list representation: fromTxn -> (toTxn -> Set<EdgeType>)
    private final Map<String, Map<String, Set<EdgeType>>> edges;

    /**
     * Constructor for SerializationGraph
     */
    public SerializationGraph() {
        this.edges = new HashMap<>();
    }

    /**
     * Add an edge to the serialization graph.
     * @param from Source transaction ID
     * @param to Destination transaction ID
     * @param type Type of dependency edge
     */
    public void addEdge(String from, String to, EdgeType type) {
        edges.computeIfAbsent(from, k -> new HashMap<>())
             .computeIfAbsent(to, k -> new HashSet<>())
             .add(type);
    }

    /**
     * Check if the graph contains a cycle with two consecutive RW edges.
     * This is the key criterion for detecting non-serializable executions under SSI.
     * @return true if such a cycle exists, false otherwise
     */
    public boolean hasCycleWithTwoConsecutiveRW() {
        Set<String> visited = new HashSet<>();
        Set<String> recStack = new HashSet<>();
        List<String> path = new ArrayList<>();

        // Try to find a cycle starting from each node
        for (String node : edges.keySet()) {
            if (detectCycle(node, visited, recStack, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * DFS-based cycle detection.
     * @param node Current node being explored
     * @param visited Set of all visited nodes
     * @param recStack Recursion stack for cycle detection
     * @param path Current path being explored
     * @return true if a cycle with consecutive RW edges is found
     */
    private boolean detectCycle(String node, Set<String> visited,
                               Set<String> recStack, List<String> path) {
        // If we've reached a node already in the recursion stack, we found a cycle
        if (recStack.contains(node)) {
            // Extract the cycle and check for consecutive RW edges
            int startIdx = path.indexOf(node);
            List<String> cycle = new ArrayList<>(path.subList(startIdx, path.size()));
            cycle.add(node); // Close the cycle
            return hasConsecutiveRW(cycle);
        }

        // If already visited and not in recursion stack, skip
        if (visited.contains(node)) {
            return false;
        }

        // Mark as visited and add to recursion stack
        visited.add(node);
        recStack.add(node);
        path.add(node);

        // Explore all neighbors
        Map<String, Set<EdgeType>> neighbors = edges.get(node);
        if (neighbors != null) {
            for (String neighbor : neighbors.keySet()) {
                if (detectCycle(neighbor, visited, recStack, path)) {
                    return true;
                }
            }
        }

        // Backtrack: remove from recursion stack
        recStack.remove(node);
        path.remove(path.size() - 1);
        return false;
    }

    /**
     * Check if a cycle contains two consecutive RW edges.
     * @param cycle List of transaction IDs forming a cycle
     * @return true if the cycle has two consecutive RW edges
     */
    private boolean hasConsecutiveRW(List<String> cycle) {
        if (cycle.size() < 3) {
            return false; // Need at least 3 transactions for consecutive RW edges
        }

        // Check each triple of consecutive transactions in the cycle
        for (int i = 0; i < cycle.size() - 2; i++) {
            String t1 = cycle.get(i);
            String t2 = cycle.get(i + 1);
            String t3 = cycle.get(i + 2);

            // Check if there are edges t1 -> t2 and t2 -> t3
            Map<String, Set<EdgeType>> t1Edges = edges.get(t1);
            Map<String, Set<EdgeType>> t2Edges = edges.get(t2);

            if (t1Edges != null && t2Edges != null) {
                Set<EdgeType> edge1 = t1Edges.get(t2);
                Set<EdgeType> edge2 = t2Edges.get(t3);

                // Check if both edges are RW edges
                if (edge1 != null && edge2 != null &&
                    edge1.contains(EdgeType.RW) && edge2.contains(EdgeType.RW)) {
                    return true;
                }
            }
        }

        // Also check the wrap-around case for cycles
        // Last two edges: (second-to-last -> last) and (last -> first)
        if (cycle.size() >= 3) {
            String tLast = cycle.get(cycle.size() - 2);
            String tFirst = cycle.get(cycle.size() - 1);
            String tSecond = cycle.get(0);

            Map<String, Set<EdgeType>> tLastEdges = edges.get(tLast);
            Map<String, Set<EdgeType>> tFirstEdges = edges.get(tFirst);

            if (tLastEdges != null && tFirstEdges != null) {
                Set<EdgeType> edge1 = tLastEdges.get(tFirst);
                Set<EdgeType> edge2 = tFirstEdges.get(tSecond);

                if (edge1 != null && edge2 != null &&
                    edge1.contains(EdgeType.RW) && edge2.contains(EdgeType.RW)) {
                    return true;
                }
            }

            // Also check (last-1 -> last-0) and (last-0 -> 1)
            String tSecondLast = cycle.get(cycle.size() - 1);
            String tZero = cycle.get(0);
            String tOne = cycle.get(1);

            Map<String, Set<EdgeType>> tSecondLastEdges = edges.get(tSecondLast);
            Map<String, Set<EdgeType>> tZeroEdges = edges.get(tZero);

            if (tSecondLastEdges != null && tZeroEdges != null) {
                Set<EdgeType> edge1 = tSecondLastEdges.get(tZero);
                Set<EdgeType> edge2 = tZeroEdges.get(tOne);

                if (edge1 != null && edge2 != null &&
                    edge1.contains(EdgeType.RW) && edge2.contains(EdgeType.RW)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Remove a transaction from the graph.
     * Removes all edges from and to this transaction.
     * @param txnId Transaction ID to remove
     */
    public void removeTransaction(String txnId) {
        // Remove all edges originating from this transaction
        edges.remove(txnId);

        // Remove all edges pointing to this transaction
        for (Map<String, Set<EdgeType>> neighbors : edges.values()) {
            neighbors.remove(txnId);
        }
    }
}
