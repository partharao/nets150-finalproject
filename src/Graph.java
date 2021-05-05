import java.util.*;

class Node
{
    List<Float> features;
    boolean visited;
    String id;

    Node(String id, List<Float> features)
    {
        this.features = features;
        this.visited = false;
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setVisited() {
        this.visited = true;
    }

    public boolean getVisited() {
        return this.visited;
    }

    public List<Float> getFeatures() {
        return this.features;
    }

    public static Double cosineSimilarity(List<Float> vectorA, List<Float> vectorB) {
        Double dotProduct = 0.0;
        Double normA = 0.0;
        Double normB = 0.0;
        for (int i = 0; i < vectorA.size(); i++) {
            dotProduct += (double)vectorA.get(i) * (double)vectorB.get(i);
            normA = normA + Math.pow((double)vectorA.get(i), 2);
            normB = normB + Math.pow((double)vectorB.get(i), 2);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public Double getWeight(Node dest) {
        List<Float> sfeatures = this.getFeatures();
        List<Float> dfeatures = dest.getFeatures();
        return cosineSimilarity(sfeatures, dfeatures);
    }
}

class Edge
{
    public Node src, dest;
    public Double weight;

    public Edge(Node src, Node dest, Double weight)
    {
        this.src = src;
        this.dest = dest;
        this.weight = weight;
    }

    public Double getWeight() {
        return this.weight;
    }

    public Node getDest() {
        return this.dest;
    }

    public Node getSource() {
        return this.src;
    }


}


class Graph
{
    String sourceId = "castle";
    private Map<Node, List<Edge>> adj = new HashMap<>();
    public Edge[][] adjMat;
    Map<String, Integer> mappings;
    int size;

    public Graph(Map<String, List<String>> inputEdges, Map<String, List<Float>> inputFeatures)
    {
        size = inputFeatures.keySet().size();
        adjMat = new Edge[size][size];

        mappings = new HashMap<String, Integer>();
        int index = 0;
        for (String id : inputFeatures.keySet()) {
            mappings.put(id, index);
            index++;
        }

        for (int i = 0; i < size; i++) {
            for (int j = 0; j <size; j++) {
                adjMat[i][j] = null;
            }
        }

        for (String song : inputFeatures.keySet()) {
            List<Float> sfeatures = inputFeatures.get(song);
            Node src = new Node(song, sfeatures);
            int row;
            row = mappings.get(src.getId());
            int col;
            for (String song1 : inputFeatures.keySet()) {
                if (song.equals(song1)) {
                    continue;
                }
                List<Float> dfeatures = inputFeatures.get(song1);
                Node dest = new Node(song1, dfeatures);
                Double weight = src.getWeight(dest);
                Edge e = new Edge(src, dest, weight);
                col = mappings.get(dest.getId());
                adjMat[row][col] = e;
            }


        }


    }



    public void printGraph() {

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                Edge temp = adjMat[i][j];
                if (temp == null) {
                    System.out.print("null");
                } else {
                    System.out.print(adjMat[i][j].getWeight() + ", ");
                }

            }
            System.out.println();
        }
    }

    public List<String> similarPath(String sourceId){
        Node curr = adjMat[mappings.get(sourceId)][0].getSource();
        int row;
        List<String> path = new ArrayList<String>();
        path.add(curr.getId());
        while(curr != null && (path.size() < 50)) {
//            System.out.println(curr.getId());
            row = mappings.get(curr.getId());
//            System.out.println(row);
            Double max = 0.0;
            Node newC = null;
            for (int j = 0; j < size; j++) {
                Edge temp = adjMat[row][j];
                if (temp == null) {
                    //System.out.print("null");
                } else if (!path.contains(temp.getDest().getId())){
                    if (temp.getWeight()>max) {
                        newC = temp.getDest();
                    }


                }

            }
            if (newC == null || curr.getId().equals(newC.getId())) {
                curr = null;
            } else {
                curr = newC;
                path.add(curr.getId());
            }

        }
//        System.out.println(mappings.entrySet());
        return path;

//    	for (Node n: adj.keySet()) {
//    		if (n.getId() == sourceId) {
//    			curr = n;
//    		}
//    	}
//    	path.add(curr.getId());
//    	while((curr != null) && (path.size() <= 50)) {
//    		curr.setVisited();
//    		List<Edge> neighbors = adj.get(curr);
//    		if (neighbors != null) {
//    			Double maxWeight = 0.0;
//        		Node maxDest = null;
//        		for (Edge e: neighbors) {
//        			if (e.getDest().getVisited() == true) {
//        				continue;
//        			}
//    				e.getDest().setVisted();
//        			if (e.getWeight()>maxWeight) {
//        				maxWeight = e.getWeight();
//        				maxDest = e.getDest();
//        			}
//        		}
//        		curr = maxDest;
//        		path.add(curr.getId());
//
//    		} else {
//    			curr = null;
//    		}
//    	}
//    	return path;

    }


}

