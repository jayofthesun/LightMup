import java.util.ArrayList;
import java.util.Random;
import java.awt.Color;
import javalib.worldimages.*;

public class GamePiece {

  // in logical coordinates, with the origin
  // at the top-left corner of the screen
  int row;
  int col;
  // whether this GamePiece is connected to the
  // adjacent left, right, top, or bottom pieces
  boolean left;
  boolean right;
  boolean top;
  boolean bottom;
  // whether the power station is on this piece
  boolean powerStation;
  boolean isLit;
  ArrayList<GamePiece> neighbor;
  int distFromBFS;

  Random rand;

  GamePiece(int row, int col, boolean left, boolean right, boolean top, boolean bottom,
      boolean powerStation, Random rand) {
    this.row = row;
    this.col = col;
    this.left = left;
    this.right = right;
    this.top = top;
    this.bottom = bottom;
    this.powerStation = powerStation;
    this.isLit = false;
    this.neighbor = new ArrayList<GamePiece>();
    this.distFromBFS = 0;
    this.rand = rand;
  }

  GamePiece(int row, int col, boolean left, boolean right, boolean top, boolean bottom,
      boolean powerStation) {
    this(row, col, left, right, top, bottom, powerStation, new Random());
  }

  // connects this and the given gp
  public void connect(GamePiece gp) {
    this.neighbor.add(gp);
    gp.neighbor.add(this);
  }

  // returns if this GamePiece and the given GamePiece are connected
  public boolean connected(GamePiece other, int height, int width) {
    return this.neighbor.contains(other)
        && ((this.left && other.right && this.row == other.row && this.col == other.col + 1)
            || (this.right && other.left && this.row == other.row && this.col == other.col - 1)
            || (this.top && other.bottom && this.row == other.row + 1 && this.col == other.col)
            || (this.bottom && other.top && this.row == other.row - 1 && this.col == other.col));
  }

  // returns the GamePiece at the given row/col
  // should be passed row/col within range
  public GamePiece find(int row, int col) {
    for (GamePiece gp : this.neighbor) {
      if (gp.row == row && gp.col == col) {
        return gp;
      }
    }
    // dummy case
    throw new IndexOutOfBoundsException("this is not supposed to happen");
  }

  // rotates this game piece counterclockwise once
  void rotate() {
    boolean prevLeft = this.left;
    boolean prevRight = this.right;
    boolean prevTop = this.top;
    boolean prevBottom = this.bottom;

    this.left = prevBottom;
    this.top = prevLeft;
    this.right = prevTop;
    this.bottom = prevRight;
  }

  // connects this GamePiece to the left of the given GamePiece
  // returns the Edge with both GamePieces
  Edge connectLeftRight(GamePiece other) {
    this.right = true;
    other.left = true;
    return new Edge(this, other, 2);
  }

  // connects this GamePiece to the top of the given GamePiece
  // returns the Edge with both GamePieces
  Edge connectTopBottom(GamePiece other) {
    this.bottom = true;
    other.top = true;
    return new Edge(this, other, 2);
  }

  // connects this GamePiece and the given GamePiece based on their relative
  // locations
  void connectGP(GamePiece other) {
    if (this.col == other.col - 1) {
      this.connectLeftRight(other);
    }
    else if (this.row == other.row - 1) {
      this.connectTopBottom(other);
    }
  }

  // draws this GamePiece's image
  WorldImage draw(WorldImage scene, int radius) {
    WorldImage gamePiece = new RectangleImage(40, 40, "solid", Color.DARK_GRAY);

    Color wireColor;
    if (this.isLit) {
      if (this.distFromBFS > radius * 0.75) {
        wireColor = Color.red;
      }
      else if (this.distFromBFS > radius * 0.5) {
        wireColor = Color.orange;
      }
      else {
        wireColor = Color.yellow;
      }
    }
    else {
      wireColor = Color.gray;
    }

    if (this.left) {
      gamePiece = new OverlayOffsetImage(new RectangleImage(20, 2, "solid", wireColor), 10, 0,
          gamePiece);
    }
    if (this.right) {
      gamePiece = new OverlayOffsetImage(new RectangleImage(20, 2, "solid", wireColor), -10, 0,
          gamePiece);
    }
    if (this.top) {
      gamePiece = new OverlayOffsetImage(new RectangleImage(2, 20, "solid", wireColor), 0, 10,
          gamePiece);
    }
    if (this.bottom) {
      gamePiece = new OverlayOffsetImage(new RectangleImage(2, 20, "solid", wireColor), 0, -10,
          gamePiece);
    }
    if (this.powerStation) {
      gamePiece = new OverlayImage(new StarImage(15, 7, OutlineMode.SOLID, Color.CYAN), gamePiece);
    }

    gamePiece = new FrameImage(gamePiece);
    return new BesideImage(scene, gamePiece);
  }

}
