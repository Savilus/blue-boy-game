package org.savilusGame.ai;

import static org.savilusGame.GamePanel.TILE_SIZE;

import java.util.ArrayList;
import java.util.Objects;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.interactiveTile.InteractiveTile;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class PathFinder {

  final static int MAX_PATH_LENGTH = 100;
  final GamePanel gamePanel;
  Node[][] node;
  final ArrayList<Node> openList = new ArrayList<>();
  @Getter
  final ArrayList<Node> pathList = new ArrayList<>();
  Node startNode, goalNode, currentNode;
  boolean goalReached = false;
  int step = 0;

  public PathFinder(GamePanel gamePanel) {
    this.gamePanel = gamePanel;
    instantiateNodes();
  }

  public void instantiateNodes() {
    node = new Node[gamePanel.getMaxWorldCol()][gamePanel.getMaxWorldRow()];

    for (int col = 0; col < gamePanel.getMaxWorldCol(); col++) {
      for (int row = 0; row < gamePanel.getMaxWorldRow(); row++) {
        node[col][row] = new Node(col, row);
      }
    }
  }

  public void resetNodes() {
    for (int col = 0; col < gamePanel.getMaxWorldCol(); col++) {
      for (int row = 0; row < gamePanel.getMaxWorldRow(); row++) {
        // Reset open, checked and solid state
        node[col][row].open = false;
        node[col][row].checked = false;
        node[col][row].solid = false;
      }
    }

    // Reset other settings
    openList.clear();
    pathList.clear();
    goalReached = false;
    step = 0;
  }

  public void setNodes(int startCol, int startRow, int goalCol, int goalRow) {
    resetNodes();

    // Set start and goal node
    startNode = node[startCol][startRow];
    currentNode = startNode;
    goalNode = node[goalCol][goalRow];
    openList.add(currentNode);

    for (int col = 0; col < gamePanel.getMaxWorldCol(); col++) {
      for (int row = 0; row < gamePanel.getMaxWorldRow(); row++) {
        // SET SOLID NODE
        // CHECK TILES
        int tileNum = gamePanel.getTileManager().getGameMaps().get(gamePanel.getWorld().getCurrentMap())[col][row];
        if (gamePanel.getTileManager().getTile()[tileNum].collision()) {
          node[col][row].solid = true;
        }
        // CHECK INTERACTIVE TILES
        var interactiveTiles = gamePanel.getWorld().currentInteractiveTiles();
        if (Objects.nonNull(interactiveTiles)) {
          for (InteractiveTile interactiveTile : interactiveTiles) {
            if (Objects.nonNull(interactiveTile) && interactiveTile.isDestructible()) {
              int itCol = interactiveTile.getWorldX() / TILE_SIZE;
              int itRow = interactiveTile.getWorldY() / TILE_SIZE;
              if (itCol >= 0 && itCol < gamePanel.getMaxWorldCol() && itRow >= 0 && itRow < gamePanel.getMaxWorldRow()) {
                node[itCol][itRow].solid = true;
              }
            }
          }
        }
        // SET COST
        getCost(node[col][row]);
      }
    }
  }

  public boolean search() {
    while (!goalReached && step < MAX_PATH_LENGTH) {
      // Current node is processed
      currentNode.checked = true;
      openList.remove(currentNode);

      // Open neighboring nodes
      openNeighboringNodes(currentNode);

      // Find the best node in the open list
      Node bestNode = findBestNode();

      if (bestNode == null) {
        break; // If there is no best node, exit the loop
      }

      // Move to the best node
      currentNode = bestNode;

      // Check if goal is reached
      if (currentNode == goalNode) {
        goalReached = true;
        trackThePath();
        break; // Exit the loop once the goal is reached
      }

      step++;
    }

    return goalReached;
  }

  private void openNeighboringNodes(Node currentNode) {
    int col = currentNode.col;
    int row = currentNode.row;

    // OPEN THE UP NODE
    if (row - 1 >= 0) openNode(node[col][row - 1]);
    // OPEN THE LEFT NODE
    if (col - 1 >= 0) openNode(node[col - 1][row]);
    // OPEN THE DOWN NODE
    if (row + 1 < gamePanel.getMaxWorldRow()) openNode(node[col][row + 1]);
    // OPEN THE RIGHT NODE
    if (col + 1 < gamePanel.getMaxWorldCol()) openNode(node[col + 1][row]);
  }

  private Node findBestNode() {
    int bestNodeCost = Integer.MAX_VALUE;
    Node bestNode = null;

    for (Node n : openList) {
      if (n.fCost < bestNodeCost) {
        bestNodeCost = n.fCost;
        bestNode = n;
      } else if (n.fCost == bestNodeCost && n.gCost < bestNode.gCost) {
        bestNode = n;
      }
    }

    return bestNode;
  }


  private void trackThePath() {
    Node current = goalNode;

    while (current != startNode) {
      pathList.addFirst(current);
      current = current.parent;
    }
  }

  private void openNode(Node node) {
    if (!node.open && !node.checked && !node.solid) {
      node.open = true;
      node.parent = currentNode;
      openList.add(node);
    }
  }

  private void getCost(Node node) {
    // G cost
    int xDistance = Math.abs(node.col - startNode.col);
    int yDistance = Math.abs(node.row - startNode.row);
    node.gCost = xDistance + yDistance;
    // H cost
    xDistance = Math.abs(node.col - goalNode.col);
    yDistance = Math.abs(node.row - goalNode.row);
    node.hCost = xDistance + yDistance;
    // F cost
    node.fCost = node.gCost + node.hCost;
  }
}
