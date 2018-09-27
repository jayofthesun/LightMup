
public class Edge {

  GamePiece fromNode;
  GamePiece toNode;
  int weight;

  Edge(GamePiece fromNode, GamePiece toNode, int weight) {
    this.fromNode = fromNode;
    this.toNode = toNode;
    this.weight = weight;
  }

  // connects this fromNode to this toNode
  void connectNodes() {
    this.fromNode.connectGP(this.toNode);
  }
}
