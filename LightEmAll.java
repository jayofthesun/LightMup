import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;

import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

public class LightEmAll extends World {

  // a list of columns of GamePieces,
  // i.e., represents the board in column-major order
  ArrayList<ArrayList<GamePiece>> board;
  // a list of all nodes
  ArrayList<GamePiece> nodes;
  // a list of edges of the minimum spanning tree
  ArrayList<Edge> mst;
  // the width and height of the board
  int width;
  int height;
  // the current location of the power station,
  // as well as its effective radius
  int powerRow;
  int powerCol;
  int radius;
  int moves;
  Random rand;

  LightEmAll(int width, int height, Random rand) {
    this.board = new ArrayList<ArrayList<GamePiece>>();
    this.nodes = new ArrayList<GamePiece>();
    this.mst = new ArrayList<Edge>();
    this.width = width;
    this.height = height;
    this.powerRow = 0;
    this.powerCol = 0;
    this.rand = rand;
    this.moves = 0;
    this.initGamePieces();
    // this.initFractalEdges(0, this.height - 1, 0, this.width - 1);
    this.initKruskalEdges();
    this.radius = this.findRadius();
    this.randomRotation();
  }

  LightEmAll(int width, int height) {
    this(width, height, new Random());
  }

  // initializes the game pieces
  void initGamePieces() {
    for (int i = 0; i < this.height; i++) {
      ArrayList<GamePiece> newRow = new ArrayList<GamePiece>();
      for (int j = 0; j < this.width; j++) {
        GamePiece newGP = new GamePiece(i, j, false, false, false, false, false);
        // Connect to the previous
        if (j > 0) {
          newGP.connect(newRow.get(j - 1));
        }
        // Connect to the top
        if (i > 0) {
          newGP.connect(this.board.get(i - 1).get(j));
        }
        // Connect to the diagonal left
        if (i > 0 && j > 0) {
          newGP.connect(this.board.get(i - 1).get(j - 1));
        }
        // Connect to the diagonal right
        if (i > 0 && j < this.width - 1) {
          newGP.connect(this.board.get(i - 1).get(j + 1));
        }
        newRow.add(newGP);
        this.nodes.add(newGP);
      }
      this.board.add(newRow);
    }
    GamePiece powerSource = this.board.get(0).get(0);
    powerSource.powerStation = true;
  }

  // initializes the edges (all horizontal and vertical down the middle)
  void initEdges() {
    // connects all left to right
    for (int i = 0; i < this.height; i++) {
      for (int j = 0; j < this.width; j++) {
        if (j > 0) {
          GamePiece left = this.board.get(i).get(j - 1);
          GamePiece right = this.board.get(i).get(j);
          Edge newEdge = left.connectLeftRight(right);
          this.mst.add(newEdge);
        }
      }
    }
    // connects all the top and bottom
    for (int k = 0; k < this.height; k++) {
      if (k > 0) {
        GamePiece top = this.board.get(k - 1).get(this.width / 2);
        GamePiece bottom = this.board.get(k).get(this.width / 2);
        Edge newEdge = top.connectTopBottom(bottom);
        this.mst.add(newEdge);
      }
    }
  }

  // initializes the edges into a fractal pattern
  void initFractalEdges(int startRow, int endRow, int startCol, int endCol) {

    int heightSub1 = endRow - startRow;
    int widthSub1 = endCol - startCol;

    if (heightSub1 == 1 && widthSub1 == 1) {
      GamePiece gp00 = this.board.get(startRow).get(startCol);
      GamePiece gp01 = this.board.get(startRow).get(startCol + 1);
      GamePiece gp10 = this.board.get(startRow + 1).get(startCol);
      GamePiece gp11 = this.board.get(startRow + 1).get(startCol + 1);

      this.mst.add(gp10.connectLeftRight(gp11));
      this.mst.add(gp00.connectTopBottom(gp10));
      this.mst.add(gp01.connectTopBottom(gp11));
    }
    else if (heightSub1 == 1 && widthSub1 == 0) {
      GamePiece gp00 = this.board.get(startRow).get(startCol);
      GamePiece gp10 = this.board.get(startRow + 1).get(startCol);
      this.mst.add(gp00.connectTopBottom(gp10));
    }
    else if (heightSub1 == 0 && widthSub1 == 1) {
      GamePiece gp00 = this.board.get(startRow).get(startCol);
      GamePiece gp01 = this.board.get(startRow).get(startCol + 1);
      this.mst.add(gp00.connectLeftRight(gp01));
    }
    // if the row is height = 2 and the row is long, split vertically
    else if (heightSub1 == 1 && widthSub1 > 1) {
      this.initFractalEdges(startRow, endRow, startCol, startCol + 1);
      this.initFractalEdges(startRow, endRow, startCol + 2, endCol);
      GamePiece gpLeft = this.board.get(endRow).get(startCol + 1);
      GamePiece gpRight = this.board.get(endRow).get(startCol + 2);
      this.mst.add(gpLeft.connectLeftRight(gpRight));
    }
    // if the col is width = 2 and the col is long, split horizontally
    else if (heightSub1 > 1 && widthSub1 == 1) {
      this.initFractalEdges(startRow, startRow + 1, startCol, endCol);
      this.initFractalEdges(startRow + 2, endRow, startCol, endCol);

      GamePiece gpTop = this.board.get(startRow + 1).get(startCol);
      GamePiece gpBottom = this.board.get(startRow + 2).get(startCol);
      this.mst.add(gpTop.connectTopBottom(gpBottom));
    }
    else if (heightSub1 > 0 && widthSub1 > 0) {
      int upperQuadsRowEnd = (startRow + endRow) / 2;
      int lowerQuadsRowStart = upperQuadsRowEnd + 1;
      int leftQuadsColEnd = (startCol + endCol) / 2;
      int rightQuadsColStart = leftQuadsColEnd + 1;

      this.initFractalEdges(startRow, upperQuadsRowEnd, startCol, leftQuadsColEnd);
      this.initFractalEdges(startRow, upperQuadsRowEnd, rightQuadsColStart, endCol);
      this.initFractalEdges(lowerQuadsRowStart, endRow, startCol, leftQuadsColEnd);
      this.initFractalEdges(lowerQuadsRowStart, endRow, rightQuadsColStart, endCol);

      // the connection of the top left and bottom left quadrants
      GamePiece gp1a = this.board.get(upperQuadsRowEnd).get(startCol);
      GamePiece gp1b = this.board.get(lowerQuadsRowStart).get(startCol);
      this.mst.add(gp1a.connectTopBottom(gp1b));

      // the connection of the top right and the bottom right quadrants
      GamePiece gp2a = this.board.get(upperQuadsRowEnd).get(endCol);
      GamePiece gp2b = this.board.get(lowerQuadsRowStart).get(endCol);
      this.mst.add(gp2a.connectTopBottom(gp2b));

      // the connection of the bottom left and right quadrants
      GamePiece gp3a = this.board.get(endRow).get(leftQuadsColEnd);
      GamePiece gp3b = this.board.get(endRow).get(rightQuadsColStart);
      this.mst.add(gp3a.connectLeftRight(gp3b));
    }
  }

  // initializes the minimum spanning tree with kruskal's algo
  void initKruskalEdges() {
    HeapSort<Edge> hs = new HeapSort<Edge>();
    this.mst = this.kruskalAlgo(hs.heapsort(this.allPossibleEdges(), new EdgeWeightComparator()));
  }

  // returns the minimum spanning tree with Kruskal's algorithm, using the given
  // edges
  ArrayList<Edge> kruskalAlgo(ArrayList<Edge> edges) {
    HashMap<GamePiece, GamePiece> representatives = this.initHashMap();
    ArrayList<Edge> edgesInTree = new ArrayList<Edge>();
    ArrayList<Edge> worklist = edges;
    // all edges in graph, sorted by edge weights (least to most);

    while (!this.hasOneTree(representatives) && !worklist.isEmpty()) {
      Edge next = worklist.get(0);
      if (this.find(representatives, next.toNode) == this.find(representatives, next.fromNode)) {
        // don't do anything
      }
      else {
        next.connectNodes();
        edgesInTree.add(next);
        this.union(representatives, this.find(representatives, next.toNode),
            this.find(representatives, next.fromNode));
      }
      worklist.remove(next);
    }
    return edgesInTree;
  }

  // converts the representative for all those represented by key2 into key1
  void union(HashMap<GamePiece, GamePiece> hm, GamePiece key1, GamePiece key2) {
    GamePiece link = hm.get(key2);
    hm.replace(key2, key1);

    if (key2 != link) {
      this.union(hm, key1, link);
    }
  }

  // finds the representative GamePiece for the given GamePiece
  GamePiece find(HashMap<GamePiece, GamePiece> hm, GamePiece key) {
    if (key == hm.get(key)) {
      return key;
    }
    else {
      return this.find(hm, hm.get(key));
    }
  }

  // returns if the given HashMap<GamePiece, GamePiece> has one tree/all one link
  boolean hasOneTree(HashMap<GamePiece, GamePiece> hm) {
    GamePiece rep = this.find(hm, this.board.get(0).get(0));

    for (GamePiece key : hm.keySet()) {
      if (rep != this.find(hm, key)) {
        return false;
      }
    }
    return true;
  }

  // returns a hashMap with this board's gamepieces
  HashMap<GamePiece, GamePiece> initHashMap() {
    HashMap<GamePiece, GamePiece> hm = new HashMap<GamePiece, GamePiece>();
    for (ArrayList<GamePiece> row : this.board) {
      for (GamePiece gp : row) {
        hm.put(gp, gp);
      }
    }
    return hm;
  }

  // returns a list of all possible edges
  ArrayList<Edge> allPossibleEdges() {
    ArrayList<Edge> edges = new ArrayList<Edge>();

    for (int i = 0; i < this.board.size(); i++) {
      for (int j = 0; j < this.board.get(0).size(); j++) {
        if (i > 0) {
          Edge newEdge = new Edge(this.board.get(i - 1).get(j), this.board.get(i).get(j),
              rand.nextInt(100));
          edges.add(newEdge);
        }
        if (j > 0) {
          Edge newEdge = new Edge(this.board.get(i).get(j - 1), this.board.get(i).get(j),
              rand.nextInt(100));
          edges.add(newEdge);
        }
      }
    }
    return edges;
  }

  // finds the radius of this board
  int findRadius() {
    GamePiece end1 = this.breadthFirstSearch(this.board.get(0).get(0));
    GamePiece end2 = this.breadthFirstSearch(end1);
    return (end2.distFromBFS / 2) + 1;
  }

  // uses breadth first search to find the farthest GamePiece from the given
  // GamePiece
  // also sets the variable distFromBFS/isLit of each cell
  GamePiece breadthFirstSearch(GamePiece gp) {
    ArrayList<GamePiece> seen = new ArrayList<GamePiece>();
    ArrayList<GamePiece> workList = new ArrayList<GamePiece>();

    for (GamePiece gamePieces : this.nodes) {
      gamePieces.distFromBFS = 0;
      gamePieces.isLit = false;
    }

    workList.add(gp);

    while (!workList.isEmpty()) {
      GamePiece next = workList.get(0);
      workList.remove(next);

      if (gp.powerStation && (next.distFromBFS <= this.radius)) {
        next.isLit = true;
      }

      if (!seen.contains(next)) {
        for (GamePiece neighbor : next.neighbor) {
          if (!seen.contains(neighbor) && next.connected(neighbor, this.height, this.width)) {
            neighbor.distFromBFS = next.distFromBFS + 1;
            workList.add(neighbor);
          }
        }
        seen.add(next);
      }
    }
    return seen.get(seen.size() - 1);
  }

  // randomly rotates the GamePieces in this board
  void randomRotation() {
    for (ArrayList<GamePiece> row : board) {
      for (GamePiece gp : row) {
        int numOfRotation = this.rand.nextInt(3);
        for (int i = 0; i <= numOfRotation; i += 1) {
          gp.rotate();
        }
      }
    }
  }

  // displays the world
  public WorldScene makeScene() {
    WorldImage boardImage = this.makeBoard();
    WorldScene ws = new WorldScene((int) boardImage.getWidth(), (int) boardImage.getHeight());
    ws.placeImageXY(boardImage, (int) (boardImage.getWidth() / 2),
        (int) (boardImage.getHeight() / 2));
    return ws;
  }

  // creates the image of the board
  public WorldImage makeBoard() {
    WorldImage boardImage = new EmptyImage();
    for (ArrayList<GamePiece> row : this.board) {
      WorldImage rowImage = new EmptyImage();
      for (GamePiece gp : row) {
        rowImage = gp.draw(rowImage, this.radius);
      }
      boardImage = new AboveImage(boardImage, rowImage);
    }
    return boardImage;
  }

  // the last scene displayed at the end of the world
  public WorldScene lastScene(String msg) {
    WorldImage boardImage = this.makeBoard();
    boardImage = new OverlayImage(new TextImage(msg + " Moves: " + this.moves, 24, Color.WHITE),
        boardImage);
    WorldScene ws = new WorldScene((int) boardImage.getWidth(), (int) boardImage.getHeight());
    ws.placeImageXY(boardImage, (int) (boardImage.getWidth() / 2),
        (int) (boardImage.getHeight() / 2));
    return ws;
  }

  // Changes the world on the mouse click
  public void onMouseClicked(Posn pos, String button) {
    int yUpperBound = 40;
    for (ArrayList<GamePiece> row : this.board) {
      int xUpperBound = 40;

      // Finds if the y position is correct, then looks through the row
      if (pos.y < yUpperBound && pos.y > yUpperBound - 40) {

        for (GamePiece gp : row) {
          // Finds the cell based on pos.x
          if (pos.x < xUpperBound && pos.x > xUpperBound - 40) {
            this.clickedGamePiece(gp, button);
          }
          xUpperBound += 40;
        }
      }
      yUpperBound += 40;
    }
    this.checkWin();
  }

  // checks if the game is won
  public void checkWin() {
    // re-lights up the board
    GamePiece power = this.board.get(this.powerRow).get(this.powerCol);
    this.breadthFirstSearch(power);

    // Ends game if the board is all lit
    if (this.allLit()) {
      this.endOfWorld("Winner!");
    }
  }

  // checks if all the cells are lit
  public boolean allLit() {
    boolean lit = true;
    for (ArrayList<GamePiece> row : this.board) {
      for (GamePiece gp : row) {
        lit = lit && gp.isLit;
      }
    }
    return lit;
  }

  // changes the game piece based on button clicked
  public void clickedGamePiece(GamePiece gamePiece, String button) {
    if (button.equals("LeftButton")) {
      gamePiece.rotate();
      this.moves += 1;
    }
  }

  // changes the world based on a key event
  public void onKeyEvent(String key) {
    GamePiece oldPower = this.board.get(this.powerRow).get(this.powerCol);

    if (key.equals("up") && this.powerRow > 0 && oldPower
        .connected(this.board.get(this.powerRow - 1).get(this.powerCol), this.height, this.width)) {
      this.powerRow -= 1;
      this.moves += 1;
    }
    else if (key.equals("down") && this.powerRow < this.height - 1 && oldPower
        .connected(this.board.get(this.powerRow + 1).get(this.powerCol), this.height, this.width)) {
      this.powerRow += 1;
      this.moves += 1;
    }
    else if (key.equals("left") && this.powerCol > 0 && oldPower
        .connected(this.board.get(this.powerRow).get(this.powerCol - 1), this.height, this.width)) {
      this.powerCol -= 1;
      this.moves += 1;
    }
    else if (key.equals("right") && this.powerCol < this.width - 1 && oldPower
        .connected(this.board.get(this.powerRow).get(this.powerCol + 1), this.height, this.width)) {
      this.powerCol += 1;
      this.moves += 1;
    }

    oldPower.powerStation = false;
    GamePiece newPower = this.board.get(this.powerRow).get(this.powerCol);
    newPower.powerStation = true;

    this.checkWin();
  }
}

class EdgeWeightComparator implements Comparator<Edge> {

  // compares the two Edge weights
  // 0 - Edge weights are the same
  // > 0 - second Edge weight is larger
  // < 0 - first Edge weight is larger
  public int compare(Edge e1, Edge e2) {
    return e2.weight - e1.weight;
  }

}

class ExamplesLightEmAll {
  LightEmAll lea;
  LightEmAll lea1;
  LightEmAll lea2;
  LightEmAll lea3;
  LightEmAll lea4;
  GamePiece gp1;
  GamePiece gp2;
  GamePiece gp3;
  WorldImage scene;
  HeapSort<Integer> hp = new HeapSort<Integer>();
  ArrayList<Integer> arr;
  ArrayList<Integer> arr2;
  ArrayList<Integer> arr3;
  ArrayList<Integer> arr4;

  void initData() {
    lea = new LightEmAll(25, 25);
    lea1 = new LightEmAll(15, 15, new Random(5));
    lea2 = new LightEmAll(2, 2, new Random(10));
    lea3 = new LightEmAll(4, 4, new Random(2));
    lea4 = new LightEmAll(10, 10, new Random(2));
    gp1 = new GamePiece(0, 0, true, true, true, true, false);
    gp2 = new GamePiece(0, 0, true, false, false, false, false);
    scene = new EmptyImage();
    arr = new ArrayList<Integer>();
    arr2 = new ArrayList<Integer>();
    arr3 = new ArrayList<Integer>();
    arr4 = new ArrayList<Integer>();

    arr.add(1);
    arr.add(2);
    arr.add(3);

    arr2.add(3);
    arr2.add(2);
    arr2.add(1);

    arr3.add(10);
    arr3.add(3);
    arr3.add(11);
    arr3.add(6);
    arr3.add(7);
    arr3.add(24);

    arr4.add(24);
    arr4.add(11);
    arr4.add(10);
    arr4.add(7);
    arr4.add(6);
    arr4.add(3);
  }

  void testLightEmUp(Tester t) {
    this.initData();
    lea1.bigBang(600, 600);
  }

  void testKruskalAlgo(Tester t) {
    this.initData();
    HeapSort<Edge> hs = new HeapSort<Edge>();
    ArrayList<Edge> allEdges = lea2.allPossibleEdges();
    ArrayList<Edge> edgesSorted = hs.heapsort(allEdges, new EdgeWeightComparator());
    ArrayList<Edge> newMST = lea2.kruskalAlgo(edgesSorted);
    t.checkExpect(newMST.size(), 3);
    t.checkExpect(lea1.mst.size(), 224);
    t.checkExpect(lea.mst.size(), 624);
    t.checkExpect(lea3.mst.size(), 15);
  }

  void testEdgeComparator(Tester t) {
    this.initData();
    EdgeWeightComparator ewc = new EdgeWeightComparator();
    t.checkExpect(ewc.compare(new Edge(gp1, gp2, 10), new Edge(gp1, gp2, 11)), 1);
    t.checkExpect(ewc.compare(new Edge(gp1, gp2, 12), new Edge(gp1, gp2, 1)), -11);
    t.checkExpect(ewc.compare(new Edge(gp1, gp2, 2), new Edge(gp1, gp2, 2)), 0);
  }

  void testConnectNodes(Tester t) {
    this.initData();
    GamePiece gp00 = lea2.board.get(0).get(0);
    GamePiece gp01 = lea2.board.get(0).get(1);
    GamePiece gp10 = lea2.board.get(1).get(0);
    Edge edg00 = new Edge(gp00, gp01, 10);
    Edge edg01 = new Edge(gp00, gp10, 10);
    t.checkExpect(gp00.right, false);
    t.checkExpect(gp01.left, false);
    t.checkExpect(gp00.bottom, false);
    t.checkExpect(gp10.top, false);
    edg00.connectNodes();
    edg01.connectNodes();
    t.checkExpect(gp00.right, true);
    t.checkExpect(gp01.left, true);
    t.checkExpect(gp00.top, true);
    t.checkExpect(gp10.bottom, true);
  }

  void testConnectGP(Tester t) {
    this.initData();
    GamePiece gp00 = lea2.board.get(0).get(0);
    GamePiece gp01 = lea2.board.get(0).get(1);
    GamePiece gp10 = lea2.board.get(1).get(0);
    t.checkExpect(gp00.right, false);
    t.checkExpect(gp01.left, false);
    gp00.connectGP(gp01);
    t.checkExpect(gp00.right, true);
    t.checkExpect(gp01.left, true);
    t.checkExpect(gp00.bottom, false);
    t.checkExpect(gp10.top, false);
    gp00.connectGP(gp10);
    t.checkExpect(gp00.top, true);
    t.checkExpect(gp10.bottom, true);
  }

  void testAllPossibleEdges(Tester t) {
    this.initData();
    GamePiece gp00 = lea2.board.get(0).get(0);
    GamePiece gp01 = lea2.board.get(0).get(1);
    GamePiece gp10 = lea2.board.get(1).get(0);
    GamePiece gp11 = lea2.board.get(1).get(1);
    t.checkExpect(lea2.allPossibleEdges().get(0), new Edge(gp00, gp01, 81));
    t.checkExpect(lea2.allPossibleEdges().get(1), new Edge(gp00, gp10, 8));
    t.checkExpect(lea2.allPossibleEdges().get(2), new Edge(gp01, gp11, 73));
    t.checkExpect(lea2.allPossibleEdges().get(3), new Edge(gp10, gp11, 8));
    t.checkExpect(lea2.allPossibleEdges().size(), 4);
  }

  void testUnion(Tester t) {
    this.initData();
    GamePiece gp00 = lea2.board.get(0).get(0);
    GamePiece gp01 = lea2.board.get(0).get(1);
    GamePiece gp10 = lea2.board.get(1).get(0);
    GamePiece gp11 = lea2.board.get(1).get(1);
    HashMap<GamePiece, GamePiece> hm1 = new HashMap<GamePiece, GamePiece>();
    hm1.put(gp00, gp00);
    hm1.put(gp01, gp01);
    hm1.put(gp10, gp10);
    hm1.put(gp11, gp11);

    t.checkExpect(hm1.get(gp00), gp00);
    t.checkExpect(hm1.get(gp01), gp01);
    t.checkExpect(hm1.get(gp10), gp10);
    t.checkExpect(hm1.get(gp11), gp11);
    lea2.union(hm1, gp00, gp01);
    t.checkExpect(hm1.get(gp00), gp00);
    t.checkExpect(hm1.get(gp01), gp00);
    t.checkExpect(hm1.get(gp10), gp10);
    t.checkExpect(hm1.get(gp11), gp11);

    this.initData();
    HashMap<GamePiece, GamePiece> hm2 = new HashMap<GamePiece, GamePiece>();
    hm2.put(gp00, gp01);
    hm2.put(gp01, gp10);
    hm2.put(gp10, gp11);
    hm2.put(gp11, gp11);

    hm2.put(gp1, gp2);
    hm2.put(gp2, gp2);

    t.checkExpect(hm2.get(gp1), gp2);
    t.checkExpect(hm2.get(gp2), gp2);
    lea2.union(hm2, gp11, gp1);
    t.checkExpect(hm2.get(gp1), gp11);
    t.checkExpect(hm2.get(gp2), gp11);
    lea2.union(hm2, gp11, gp1);
    t.checkExpect(hm2.get(gp1), gp11);
    t.checkExpect(hm2.get(gp2), gp11);
  }

  void testHasOneTree(Tester t) {
    this.initData();
    GamePiece gp00 = lea2.board.get(0).get(0);
    GamePiece gp01 = lea2.board.get(0).get(1);
    GamePiece gp10 = lea2.board.get(1).get(0);
    GamePiece gp11 = lea2.board.get(1).get(1);
    HashMap<GamePiece, GamePiece> hm = new HashMap<GamePiece, GamePiece>();
    t.checkExpect(lea2.hasOneTree(hm), true);
    hm.put(gp00, gp01);
    hm.put(gp01, gp10);
    hm.put(gp10, gp11);
    hm.put(gp11, gp11);
    t.checkExpect(lea2.hasOneTree(hm), true);
    t.checkExpect(lea2.hasOneTree(lea2.initHashMap()), false);
    hm.put(gp1, gp1);
    t.checkExpect(lea2.hasOneTree(hm), false);

    this.initData();
    HashMap<GamePiece, GamePiece> hm1 = new HashMap<GamePiece, GamePiece>();
    hm1.put(gp00, gp00);
    hm1.put(gp01, gp01);
    hm1.put(gp10, gp10);
    hm1.put(gp11, gp11);
    t.checkExpect(lea2.hasOneTree(hm1), false);

    this.initData();
    HashMap<GamePiece, GamePiece> hm2 = new HashMap<GamePiece, GamePiece>();
    hm1.put(gp00, gp00);
    t.checkExpect(lea2.hasOneTree(hm2), true);
  }

  void testFindLEA(Tester t) {
    this.initData();
    GamePiece gp00 = lea2.board.get(0).get(0);
    GamePiece gp01 = lea2.board.get(0).get(1);
    GamePiece gp10 = lea2.board.get(1).get(0);
    GamePiece gp11 = lea2.board.get(1).get(1);
    t.checkExpect(lea2.find(lea2.initHashMap(), gp00), gp00);
    t.checkExpect(lea2.find(lea2.initHashMap(), gp10), gp10);
    t.checkExpect(lea2.find(lea2.initHashMap(), gp01), gp01);
    t.checkExpect(lea2.find(lea2.initHashMap(), gp11), gp11);

    HashMap<GamePiece, GamePiece> hm = new HashMap<GamePiece, GamePiece>();
    hm.put(gp00, gp01);
    hm.put(gp01, gp10);
    hm.put(gp10, gp11);
    hm.put(gp11, gp11);
    t.checkExpect(lea2.find(hm, gp00), gp11);
    t.checkExpect(lea2.find(hm, gp01), gp11);
    t.checkExpect(lea2.find(hm, gp10), gp11);
    t.checkExpect(lea2.find(hm, gp11), gp11);
  }

  void testInitHashMap(Tester t) {
    this.initData();
    GamePiece gp00 = lea2.board.get(0).get(0);
    GamePiece gp01 = lea2.board.get(0).get(1);
    GamePiece gp10 = lea2.board.get(1).get(0);
    GamePiece gp11 = lea2.board.get(1).get(1);
    t.checkExpect(lea2.initHashMap().containsKey(gp00), true);
    t.checkExpect(lea2.initHashMap().get(gp00), gp00);
    t.checkExpect(lea2.initHashMap().containsKey(gp01), true);
    t.checkExpect(lea2.initHashMap().get(gp01), gp01);
    t.checkExpect(lea2.initHashMap().containsKey(gp10), true);
    t.checkExpect(lea2.initHashMap().get(gp10), gp10);
    t.checkExpect(lea2.initHashMap().containsKey(gp11), true);
    t.checkExpect(lea2.initHashMap().get(gp11), gp11);
    t.checkExpect(lea2.initHashMap().size(), 4);
  }

  void testIntialized(Tester t) {
    this.initData();
    GamePiece gp00 = lea2.board.get(0).get(0);
    GamePiece gp01 = lea2.board.get(0).get(1);
    GamePiece gp10 = lea2.board.get(1).get(0);
    GamePiece gp11 = lea2.board.get(1).get(1);

    t.checkExpect(gp00.neighbor.contains(gp01), true);
    t.checkExpect(gp00.neighbor.contains(gp10), true);
    t.checkExpect(gp00.neighbor.contains(gp11), true);
    t.checkExpect(gp00.neighbor.contains(gp00), false);

    t.checkExpect(gp01.neighbor.contains(gp00), true);
    t.checkExpect(gp01.neighbor.contains(gp10), true);
    t.checkExpect(gp01.neighbor.contains(gp11), true);
    t.checkExpect(gp01.neighbor.contains(gp01), false);

    t.checkExpect(gp10.neighbor.contains(gp01), true);
    t.checkExpect(gp10.neighbor.contains(gp00), true);
    t.checkExpect(gp10.neighbor.contains(gp11), true);
    t.checkExpect(gp10.neighbor.contains(gp10), false);

    t.checkExpect(gp11.neighbor.contains(gp01), true);
    t.checkExpect(gp11.neighbor.contains(gp10), true);
    t.checkExpect(gp11.neighbor.contains(gp00), true);
    t.checkExpect(gp11.neighbor.contains(gp11), false);

    lea2.initEdges();
    t.checkExpect(gp00.connected(gp01, lea2.height, lea2.width), true);
    t.checkExpect(gp00.connected(gp10, lea2.height, lea2.width), false);
    t.checkExpect(gp01.connected(gp11, lea2.height, lea2.width), true);

    lea2.initFractalEdges(0, lea2.height - 1, 0, lea2.width - 1);
    t.checkExpect(gp00.connected(gp10, lea2.height, lea2.width), true);
    t.checkExpect(gp10.connected(gp11, lea2.height, lea2.width), true);
    t.checkExpect(gp01.connected(gp11, lea2.height, lea2.width), true);
    t.checkExpect(gp00.connected(gp11, lea2.height, lea2.width), false);

    lea3.initFractalEdges(0, lea3.height - 1, 0, lea3.width - 1);
    GamePiece gp3_00 = lea3.board.get(0).get(0);
    GamePiece gp3_01 = lea3.board.get(0).get(1);
    GamePiece gp3_10 = lea3.board.get(1).get(0);
    GamePiece gp3_11 = lea3.board.get(1).get(1);

    GamePiece gp3_20 = lea3.board.get(2).get(0);
    GamePiece gp3_21 = lea3.board.get(2).get(1);
    GamePiece gp3_30 = lea3.board.get(3).get(0);
    GamePiece gp3_31 = lea3.board.get(3).get(1);

    GamePiece gp3_32 = lea3.board.get(3).get(2);
    GamePiece gp3_13 = lea3.board.get(1).get(3);
    GamePiece gp3_23 = lea3.board.get(2).get(3);

    t.checkExpect(gp3_00.connected(gp3_10, lea3.height, lea3.width), true);
    t.checkExpect(gp3_10.connected(gp3_11, lea3.height, lea3.width), true);
    t.checkExpect(gp3_01.connected(gp3_11, lea3.height, lea3.width), true);
    t.checkExpect(gp3_00.connected(gp3_11, lea3.height, lea3.width), false);
    t.checkExpect(gp3_20.connected(gp3_30, lea3.height, lea3.width), true);
    t.checkExpect(gp3_31.connected(gp3_21, lea3.height, lea3.width), true);
    t.checkExpect(gp3_20.connected(gp3_21, lea3.height, lea3.width), false);
    t.checkExpect(gp3_30.connected(gp3_31, lea3.height, lea3.width), true);

    t.checkExpect(gp3_13.connected(gp3_23, lea3.height, lea3.width), true);
    t.checkExpect(gp3_32.connected(gp3_31, lea3.height, lea3.width), true);
  }

  void testRandomRotate(Tester t) {
    this.initData();
    GamePiece gp00 = lea2.board.get(0).get(0);
    GamePiece gp01 = lea2.board.get(0).get(1);
    GamePiece gp10 = lea2.board.get(1).get(0);
    GamePiece gp11 = lea2.board.get(1).get(1);

    // not just rotated in the standard way
    t.checkExpect(gp00.left, true);
    t.checkExpect(gp00.bottom, false);
    t.checkExpect(gp00.right, false);
    t.checkExpect(gp00.top, true);

    t.checkExpect(gp01.left, false);
    t.checkExpect(gp01.top, false);
    t.checkExpect(gp01.bottom, false);
    t.checkExpect(gp01.right, true);

    t.checkExpect(gp10.left, true);
    t.checkExpect(gp10.top, false);
    t.checkExpect(gp10.bottom, true);
    t.checkExpect(gp10.right, false);

    t.checkExpect(gp11.left, false);
    t.checkExpect(gp11.top, false);
    t.checkExpect(gp11.bottom, false);
    t.checkExpect(gp11.right, true);
  }

  void testMakeScene(Tester t) {
    this.initData();
    WorldImage boardImage = lea2.makeBoard();
    WorldScene ws = new WorldScene((int) boardImage.getWidth(), (int) boardImage.getHeight());
    ws.placeImageXY(boardImage, (int) (boardImage.getWidth() / 2),
        (int) (boardImage.getHeight() / 2));
    t.checkExpect(lea2.makeScene(), ws);

    WorldImage boardImage2 = lea3.makeBoard();
    WorldScene ws2 = new WorldScene((int) boardImage2.getWidth(), (int) boardImage2.getHeight());
    ws2.placeImageXY(boardImage2, (int) (boardImage2.getWidth() / 2),
        (int) (boardImage2.getHeight() / 2));
    t.checkExpect(lea3.makeScene(), ws2);
  }

  void testMakeBoard(Tester t) {
    this.initData();

    WorldImage boardImage = new EmptyImage();
    for (ArrayList<GamePiece> row : lea2.board) {
      WorldImage rowImage = new EmptyImage();
      for (GamePiece gp : row) {
        rowImage = gp.draw(rowImage, lea2.radius);
      }
      boardImage = new AboveImage(boardImage, rowImage);
    }
    t.checkExpect(lea2.makeBoard(), boardImage);

    WorldImage boardImage2 = new EmptyImage();
    for (ArrayList<GamePiece> row : lea3.board) {
      WorldImage rowImage = new EmptyImage();
      for (GamePiece gp : row) {
        rowImage = gp.draw(rowImage, lea3.radius);
      }
      boardImage2 = new AboveImage(boardImage2, rowImage);
    }
    t.checkExpect(lea3.makeBoard(), boardImage2);
  }

  void testLastScene(Tester t) {
    this.initData();
    WorldImage boardImage = lea2.makeBoard();
    boardImage = new OverlayImage(new TextImage("Winner! Moves: " + lea2.moves, 24, Color.WHITE),
        boardImage);
    WorldScene ws = new WorldScene((int) boardImage.getWidth(), (int) boardImage.getHeight());
    ws.placeImageXY(boardImage, (int) (boardImage.getWidth() / 2),
        (int) (boardImage.getHeight() / 2));
    t.checkExpect(lea2.lastScene("Winner!"), ws);

    WorldImage boardImage2 = lea3.makeBoard();
    boardImage2 = new OverlayImage(new TextImage("Winner! Moves: " + lea3.moves, 24, Color.WHITE),
        boardImage2);
    WorldScene ws2 = new WorldScene((int) boardImage2.getWidth(), (int) boardImage2.getHeight());
    ws2.placeImageXY(boardImage2, (int) (boardImage2.getWidth() / 2),
        (int) (boardImage2.getHeight() / 2));
    t.checkExpect(lea3.lastScene("Winner!"), ws2);
  }

  void testClickedGamePiece(Tester t) {
    this.initData();
    GamePiece gp00 = lea2.board.get(0).get(0);
    t.checkExpect(gp00.left, true);
    t.checkExpect(gp00.bottom, false);
    t.checkExpect(gp00.right, false);
    t.checkExpect(gp00.top, true);
    t.checkExpect(lea2.moves, 0);
    lea2.clickedGamePiece(gp00, "LeftButton");
    t.checkExpect(gp00.left, false);
    t.checkExpect(gp00.bottom, false);
    t.checkExpect(gp00.right, true);
    t.checkExpect(gp00.top, true);
    t.checkExpect(lea2.moves, 1);

    GamePiece gp01 = lea2.board.get(0).get(1);
    t.checkExpect(gp01.left, false);
    t.checkExpect(gp01.top, false);
    t.checkExpect(gp01.bottom, false);
    t.checkExpect(gp01.right, true);
    t.checkExpect(lea2.moves, 1);
    lea2.clickedGamePiece(gp01, "RightButton");
    t.checkExpect(gp01.left, false);
    t.checkExpect(gp01.top, false);
    t.checkExpect(gp01.bottom, false);
    t.checkExpect(gp01.right, true);
    t.checkExpect(lea2.moves, 1);

    lea2.board.get(0).get(0).neighbor = new ArrayList<GamePiece>();
    lea2.board.get(0).get(0).distFromBFS = 0;
    t.checkExpect(lea2.moves, 1);
    t.checkExpect(lea2.board.get(0).get(0), new GamePiece(0, 0, false, true, true, false, true));
    lea2.clickedGamePiece(lea2.board.get(0).get(0), "LeftButton");
    t.checkExpect(lea2.moves, 2);
    t.checkExpect(lea2.board.get(0).get(0), new GamePiece(0, 0, false, true, false, true, true));
  }

  void testOnMouseClicked(Tester t) {
    this.initData();
    Posn gp01 = new Posn(50, 20);
    lea2.board.get(0).get(1).neighbor = new ArrayList<GamePiece>();
    lea2.board.get(0).get(1).distFromBFS = 0;
    t.checkExpect(lea2.moves, 0);
    t.checkExpect(lea2.board.get(0).get(1), new GamePiece(0, 1, false, true, false, false, false));
    lea2.onMouseClicked(gp01, "RightButton");
    t.checkExpect(lea2.moves, 0);
    t.checkExpect(lea2.board.get(0).get(1), new GamePiece(0, 1, false, true, false, false, false));
    lea2.onMouseClicked(gp01, "LeftButton");
    t.checkExpect(lea2.moves, 1);
    t.checkExpect(lea2.board.get(0).get(1), new GamePiece(0, 1, false, false, false, true, false));
  }

  void testRotate(Tester t) {
    this.initData();
    t.checkExpect(gp1, new GamePiece(0, 0, true, true, true, true, false));
    gp1.rotate();
    t.checkExpect(gp1, new GamePiece(0, 0, true, true, true, true, false));

    t.checkExpect(gp2, new GamePiece(0, 0, true, false, false, false, false));
    gp2.rotate();
    t.checkExpect(gp2, new GamePiece(0, 0, false, false, true, false, false));
  }

  void testConnectLeftright(Tester t) {
    this.initData();
    GamePiece ex1 = new GamePiece(2, 3, false, false, false, false, true);
    GamePiece ex2 = new GamePiece(5, 6, false, false, false, true, true);
    t.checkExpect(ex1.right, false);
    t.checkExpect(ex2.left, false);
    t.checkExpect(ex1.connectLeftRight(ex2), new Edge(ex1, ex2, 2));
    t.checkExpect(ex1.right, true);
    t.checkExpect(ex2.left, true);
    t.checkExpect(ex1.connectLeftRight(ex2), new Edge(ex1, ex2, 2));
    t.checkExpect(ex1.right, true);
    t.checkExpect(ex2.left, true);

    GamePiece ex3 = new GamePiece(10, 10, false, false, false, false, false);
    t.checkExpect(ex3.right, false);
    t.checkExpect(ex3.left, false);
    t.checkExpect(ex3.connectLeftRight(ex3), new Edge(ex3, ex3, 2));
    t.checkExpect(ex3.right, true);
    t.checkExpect(ex3.left, true);
  }

  void testConnectTopBottom(Tester t) {
    this.initData();
    GamePiece ex3 = new GamePiece(3, 10, true, false, false, false, false);
    GamePiece ex4 = new GamePiece(3, 11, false, true, false, false, true);
    t.checkExpect(ex3.bottom, false);
    t.checkExpect(ex4.top, false);
    t.checkExpect(ex3.connectTopBottom(ex4), new Edge(ex3, ex4, 2));
    t.checkExpect(ex3.bottom, true);
    t.checkExpect(ex4.top, true);
    t.checkExpect(ex3.connectTopBottom(ex4), new Edge(ex3, ex4, 2));
    t.checkExpect(ex3.bottom, true);
    t.checkExpect(ex4.top, true);

    GamePiece ex5 = new GamePiece(10, 10, false, false, false, false, false);
    t.checkExpect(ex5.top, false);
    t.checkExpect(ex5.bottom, false);
    t.checkExpect(ex5.connectTopBottom(ex5), new Edge(ex5, ex5, 2));
    t.checkExpect(ex5.top, true);
    t.checkExpect(ex5.bottom, true);
  }

  void testDraw(Tester t) {
    this.initData();
    GamePiece left = new GamePiece(1, 1, true, false, false, false, false);
    GamePiece right = new GamePiece(1, 1, false, true, false, false, false);
    GamePiece top = new GamePiece(1, 1, false, false, true, false, false);
    GamePiece bottom = new GamePiece(1, 1, false, false, false, true, false);
    WorldImage pt1 = new RectangleImage(40, 40, "solid", Color.DARK_GRAY);
    WorldImage pt2 = new RectangleImage(20, 2, "solid", Color.GRAY);
    WorldImage pt3 = new RectangleImage(2, 20, "solid", Color.GRAY);
    WorldImage pt4 = new RectangleImage(20, 2, "solid", Color.YELLOW);

    WorldImage pt5 = new RectangleImage(20, 2, "solid", Color.orange);
    WorldImage pt6 = new RectangleImage(20, 2, "solid", Color.red);

    // 5 is an arbitrary value for the radius
    t.checkExpect(left.draw(scene, 5),
        new BesideImage(new EmptyImage(), new FrameImage(new OverlayOffsetImage(pt2, 10, 0, pt1))));
    t.checkExpect(right.draw(scene, 5), new BesideImage(new EmptyImage(),
        new FrameImage(new OverlayOffsetImage(pt2, -10, 0, pt1))));
    t.checkExpect(top.draw(scene, 5),
        new BesideImage(new EmptyImage(), new FrameImage(new OverlayOffsetImage(pt3, 0, 10, pt1))));
    t.checkExpect(bottom.draw(scene, 5), new BesideImage(new EmptyImage(),
        new FrameImage(new OverlayOffsetImage(pt3, 0, -10, pt1))));
    left.isLit = true;
    t.checkExpect(left.draw(scene, 5),
        new BesideImage(new EmptyImage(), new FrameImage(new OverlayOffsetImage(pt4, 10, 0, pt1))));

    GamePiece gp = new GamePiece(0, 0, true, false, false, false, false);
    t.checkExpect(gp.draw(scene, 100),
        new BesideImage(new EmptyImage(), new FrameImage(new OverlayOffsetImage(pt2, 10, 0, pt1))));
    gp.isLit = true;
    gp.distFromBFS = 76;
    t.checkExpect(gp.draw(scene, 100),
        new BesideImage(new EmptyImage(), new FrameImage(new OverlayOffsetImage(pt6, 10, 0, pt1))));
    gp.distFromBFS = 75;
    t.checkExpect(gp.draw(scene, 100),
        new BesideImage(new EmptyImage(), new FrameImage(new OverlayOffsetImage(pt5, 10, 0, pt1))));
    gp.distFromBFS = 51;
    t.checkExpect(gp.draw(scene, 100),
        new BesideImage(new EmptyImage(), new FrameImage(new OverlayOffsetImage(pt5, 10, 0, pt1))));
    gp.distFromBFS = 10;
    t.checkExpect(gp.draw(scene, 100),
        new BesideImage(new EmptyImage(), new FrameImage(new OverlayOffsetImage(pt4, 10, 0, pt1))));
  }

  void testCheckWin(Tester t) {
    this.initData();
    lea2.checkWin();
    for (ArrayList<GamePiece> row : lea2.board) {
      for (GamePiece gp : row) {
        gp.isLit = true;
      }
    }
    t.checkExpect(lea2.allLit(), true);
    lea2.checkWin();
    t.checkExpect(lea2.board.get(lea2.powerRow).get(lea2.powerCol).distFromBFS, 0);
  }

  void testAllLit(Tester t) {
    this.initData();
    t.checkExpect(lea2.allLit(), false);
    for (ArrayList<GamePiece> row : lea2.board) {
      for (GamePiece gp : row) {
        gp.isLit = true;
      }
    }
    t.checkExpect(lea2.allLit(), true);
  }

  void testFind(Tester t) {
    this.initData();
    lea2.board.get(0).get(0).neighbor = new ArrayList<GamePiece>();
    lea2.board.get(0).get(0).neighbor.add(lea2.board.get(0).get(1));
    t.checkExpect(lea2.board.get(0).get(0).find(0, 1), lea2.board.get(0).get(1));
    lea2.board.get(0).get(0).neighbor = new ArrayList<GamePiece>();
    t.checkException(new IndexOutOfBoundsException("this is not supposed to happen"),
        lea2.board.get(0).get(0), "find", 0, 1);
  }

  void testConnect(Tester t) {
    this.initData();
    ArrayList<GamePiece> one = new ArrayList<GamePiece>();
    ArrayList<GamePiece> two = new ArrayList<GamePiece>();
    lea2.board.get(0).get(0).neighbor = new ArrayList<GamePiece>();
    lea2.board.get(0).get(1).neighbor = new ArrayList<GamePiece>();
    t.checkExpect(lea2.board.get(0).get(0).neighbor, new ArrayList<GamePiece>());
    t.checkExpect(lea2.board.get(0).get(1).neighbor, new ArrayList<GamePiece>());
    lea2.board.get(0).get(0).connect(lea2.board.get(0).get(1));
    one.add(lea2.board.get(0).get(1));
    t.checkExpect(lea2.board.get(0).get(0).neighbor, one);
    two.add(lea2.board.get(0).get(0));
    t.checkExpect(lea2.board.get(0).get(1).neighbor, two);

  }

  void testConnected(Tester t) {
    this.initData();
    GamePiece ex1 = new GamePiece(2, 3, false, false, false, false, true);
    GamePiece ex2 = new GamePiece(5, 6, false, false, false, true, true);
    GamePiece ex3 = new GamePiece(3, 10, false, true, false, false, false);
    GamePiece ex4 = new GamePiece(3, 11, true, false, false, false, true);
    GamePiece ex5 = new GamePiece(3, 10, false, false, false, true, false);
    GamePiece ex6 = new GamePiece(3, 11, false, true, true, false, true);
    ex3.neighbor.add(ex4);
    t.checkExpect(ex3.connected(ex4, 20, 20), true);
    t.checkExpect(ex1.connected(ex2, 20, 20), false);
    t.checkExpect(ex5.connected(ex6, 20, 20), false);
  }

  void testfindRadius(Tester t) {
    this.initData();
    t.checkExpect(lea1.findRadius(), 1);
    t.checkExpect(lea1.radius, 29);
    t.checkExpect(lea2.findRadius(), 1);
    t.checkExpect(lea2.radius, 2);
    t.checkExpect(lea3.findRadius(), 1);
    t.checkExpect(lea3.radius, 5);
  }

  void testbreadthFirstSearch(Tester t) {
    this.initData();
    GamePiece result2 = lea2.board.get(1).get(0);
    result2.distFromBFS = 4;
    GamePiece result3 = lea2.board.get(0).get(0);
    GamePiece result4 = lea2.board.get(1).get(0);
    GamePiece result5 = lea2.board.get(1).get(1);
    result5.distFromBFS = 3;
    result4.distFromBFS = 2;
    result3.distFromBFS = 3;
    result5.connect(result4);
    result4.connect(result3);
    result3.connect(result2);
    result3.neighbor.add(result4);
    t.checkExpect(lea1.breadthFirstSearch(lea2.board.get(1).get(0)), result2);
    lea2.board.get(1).get(0).neighbor = new ArrayList<GamePiece>();
    GamePiece result1 = new GamePiece(1, 0, true, false, false, true, false);
    result1.distFromBFS = 2;
    t.checkExpect(lea1.breadthFirstSearch(lea2.board.get(1).get(0)), result1);

  }

  void testHeapSort(Tester t) {
    this.initData();
    t.checkExpect(hp.heapsort(new ArrayList<Integer>(), new IntComparator()),
        new ArrayList<Integer>());
    t.checkExpect(hp.heapsort(arr, new IntComparator()), arr2);
    t.checkExpect(hp.heapsort(arr3, new IntComparator()), arr4);
  }

  void testRemoveMax(Tester t) {
    this.initData();
    // since the given list is not sorted, it's just returning the first element
    t.checkException(new IllegalArgumentException("Can't remove from nothing."), hp, "removeMax",
        new ArrayList<Integer>(), new IntComparator());
    t.checkExpect(hp.removeMax(arr, new IntComparator()), 1);
    t.checkExpect(hp.removeMax(arr, new IntComparator()), 3);
    t.checkExpect(hp.removeMax(arr, new IntComparator()), 2);
  }

  void testSwap(Tester t) {
    this.initData();
    ArrayList<Integer> a1 = new ArrayList<Integer>();
    hp.swap(a1, 10, 100);
    t.checkExpect(a1, new ArrayList<Integer>());

    a1.add(1);
    a1.add(2);
    hp.swap(a1, 1, 100);
    t.checkExpect(a1.get(0), 1);
    t.checkExpect(a1.get(1), 2);
    t.checkExpect(a1.size(), 2);

    hp.swap(a1, 0, 1);
    t.checkExpect(a1.get(0), 2);
    t.checkExpect(a1.get(1), 1);
    t.checkExpect(a1.size(), 2);

    a1.add(10);
    a1.add(200);
    a1.add(3);
    hp.swap(a1, 2, 0);
    t.checkExpect(a1.get(0), 10);
    t.checkExpect(a1.get(1), 1);
    t.checkExpect(a1.get(2), 2);
    t.checkExpect(a1.get(3), 200);
    t.checkExpect(a1.get(4), 3);
    t.checkExpect(a1.size(), 5);
  }

}
