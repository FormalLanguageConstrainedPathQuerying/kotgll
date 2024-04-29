/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jit.graph;

public class RBTree {
    public final static int maxNodes = 70;      
    public final static int INSERT = 0;         
    public final static int DELETE = 1;         
    public final static int NOP = 2;
    public final static Node treeNull = new Node(); 

    private Node root;
    private int num_of_nodes;
    private int height;           

    private Node node;  
    private int action; 
    private int stage;  

    public RBTree() {
        root = treeNull;
        node = treeNull;
        num_of_nodes = 0;
        height = 0;
        action = NOP;
        stage = 0;
    }

    public Node getRoot() {
        return root;
    }

    public int getNodes() {
        return num_of_nodes;
    }

    public int getHeight() {
        return height;
    }


    public boolean RBInsert(int k) {
        if (action != NOP) {
            System.out.println("Only one operation can be done at a time.");
            return false;
        }

        if (num_of_nodes == maxNodes) {
            System.out.println("The maximum nodes allowed is already reached.");
            return false;
        }

        if (Search(k) == treeNull) {
            action = INSERT;
            node = new Node(k);
            node.setNode(Node.Left_son, treeNull);
            node.setNode(Node.Right_son, treeNull);
            node.setNode(Node.Parent, treeNull);
            stage = 1;
            while (stage != 0) {
                InsertStep();
                updateHeight();
            }
            action = NOP;
            return true;
        } else
            System.out.println("Insertion failed. This key already exist.");
        return false;
    }


    public boolean RBDelete(int k) {
        if (action != NOP) {
            System.out.println("Only one operation can be done at a time.");
            return false;
        }
        node = Search(k);
        if (node != treeNull) {
            action = DELETE;
            stage = 1;
            while (stage != 0) {
                DeleteStep();
                updateHeight();
            }
            action = NOP;
            return true;
        } else
            System.out.println("Deletion failed. This key doesn't exist.");
        return false;
    }

    private void InsertStep() {
        Node Pr, GrPr, Un;
        switch (stage) {
            case 1:
                Tree_Insert();
                break;
            case 2:
                Pr = node.getNode(Node.Parent);
                GrPr = Pr.getNode(Node.Parent);
                if (Pr == GrPr.getNode(Node.Left_son)) {
                    Un = GrPr.getNode(Node.Right_son);
                    if (Un.getColor() == Node.Red) {
                        stage = 3;
                    } else if (node == Pr.getNode(Node.Right_son)) {
                        node = Pr;
                        stage = 5;
                    } else {
                        stage = 6;
                    }
                } else {
                    Un = GrPr.getNode(Node.Left_son);
                    if (Un.getColor() == Node.Red) {
                        stage = 3;
                    } else if (node == Pr.getNode(Node.Left_son)) {
                        node = Pr;
                        stage = 5;
                    } else {
                        stage = 6;
                    }
                }
                break;
            case 3:
                Pr = node.getNode(Node.Parent);
                GrPr = Pr.getNode(Node.Parent);
                if (Pr == GrPr.getNode(Node.Left_son)) {
                    Un = GrPr.getNode(Node.Right_son);
                } else {
                    Un = GrPr.getNode(Node.Left_son);
                }
                node = GrPr;
                stage = 4;
                break;
            case 4:
                node.setColor(Node.Red);
                node.getNode(Node.Left_son).setColor(Node.Black);
                node.getNode(Node.Right_son).setColor(Node.Black);

                if ((node == root) ||
                        (node.getNode(Node.Parent).getColor() == Node.Black)) {
                    if (root.getColor() == Node.Red) {
                        stage = 9;
                    } else
                        stage = 0;
                } else {
                    stage = 2;
                    InsertStep();
                }
                break;
            case 5:
                Pr = node.getNode(Node.Parent);
                if (node == Pr.getNode(Node.Left_son)) {
                    Left_Rotate(node);
                } else {
                    Right_Rotate(node);
                }
                stage = 6;
                break;
            case 6:
                Pr = node.getNode(Node.Parent);
                GrPr = Pr.getNode(Node.Parent);

                stage = 7;
                break;
            case 7:
                Pr = node.getNode(Node.Parent);
                Pr.setColor(Node.Black);
                GrPr = Pr.getNode(Node.Parent);
                GrPr.setColor(Node.Red);

                stage = 8;
                break;
            case 8:
                Pr = node.getNode(Node.Parent);
                GrPr = Pr.getNode(Node.Parent);
                if (Pr == GrPr.getNode(Node.Left_son)) {
                    Right_Rotate(GrPr);
                } else {
                    Left_Rotate(GrPr);
                }
                if (root.getColor() == Node.Red) {
                    stage = 9;
                } else
                    stage = 0;
                break;
            case 9:
                stage = 10;
                break;
            case 10:
                root.setColor(Node.Black);
                stage = 0;
                break;
        }
    }

    public void DeleteStep() {
        Node Pr, Br;
        switch (stage) {
            case 1:
                Tree_Delete();
                break;
            case 2:
                Pr = node.getNode(Node.Parent);
                if (node == Pr.getNode(Node.Left_son)) {
                    Br = Pr.getNode(Node.Right_son);
                } else {
                    Br = Pr.getNode(Node.Left_son);
                }
                if (Br.getColor() == Node.Red) {
                    stage = 3;
                } else if ((Br.getNode(Node.Right_son).getColor() == Node.Black)
                        && (Br.getNode(Node.Left_son).getColor() == Node.Black)) {
                    stage = 5;
                    DeleteStep();
                } else {
                    stage = 7;
                    DeleteStep();
                }
                break;
            case 3:
                Pr = node.getNode(Node.Parent);
                if (node == Pr.getNode(Node.Left_son)) {
                    Br = Pr.getNode(Node.Right_son);
                } else {
                    Br = Pr.getNode(Node.Left_son);
                }
                Br.setColor(Node.Black);
                Pr.setColor(Node.Red);

                stage = 4;
                break;
            case 4:
                Pr = node.getNode(Node.Parent);
                if (node == Pr.getNode(Node.Left_son)) {
                    Left_Rotate(Pr);
                    Br = Pr.getNode(Node.Right_son);
                } else {
                    Right_Rotate(Pr);
                    Br = Pr.getNode(Node.Left_son);
                }
                if ((Br.getNode(Node.Right_son).getColor() == Node.Black)
                        && (Br.getNode(Node.Left_son).getColor() == Node.Black)) {
                    stage = 5;
                } else {
                    stage = 7;
                }

                break;
            case 5:
                Pr = node.getNode(Node.Parent);
                if (node == Pr.getNode(Node.Left_son)) {
                    Br = Pr.getNode(Node.Right_son);
                } else {
                    Br = Pr.getNode(Node.Left_son);
                }
                stage = 6;
                break;
            case 6:
                Pr = node.getNode(Node.Parent);
                if (node == Pr.getNode(Node.Left_son)) {
                    Br = Pr.getNode(Node.Right_son);
                } else {
                    Br = Pr.getNode(Node.Left_son);
                }
                Br.setColor(Node.Red);
                node = Pr;

                if ((node != root) && (node.getColor() == Node.Black)) {
                    stage = 2;
                } else if (node.getColor() == Node.Red) {
                    stage = 13;
                } else
                    stage = 0;
                break;
            case 7:
                Pr = node.getNode(Node.Parent);
                if (node == Pr.getNode(Node.Left_son)) {
                    Br = Pr.getNode(Node.Right_son);
                    if ((Br.getNode(Node.Right_son)).getColor() == Node.Black) {
                        stage = 8;
                    } else {
                        stage = 10;
                        DeleteStep();
                    }
                } else {
                    Br = Pr.getNode(Node.Left_son);
                    if ((Br.getNode(Node.Left_son)).getColor() == Node.Black) {
                        stage = 8;
                    } else {
                        stage = 10;
                        DeleteStep();
                    }
                }
                break;
            case 8:
                Pr = node.getNode(Node.Parent);
                if (node == Pr.getNode(Node.Left_son)) {
                    Br = Pr.getNode(Node.Right_son);
                    Br.getNode(Node.Left_son).setColor(Node.Black);
                } else {
                    Br = Pr.getNode(Node.Left_son);
                    Br.getNode(Node.Right_son).setColor(Node.Black);
                }
                Br.setColor(Node.Red);
                stage = 9;
                break;
            case 9:
                Pr = node.getNode(Node.Parent);
                if (node == Pr.getNode(Node.Left_son)) {
                    Br = Pr.getNode(Node.Right_son);
                    Right_Rotate(Br);
                } else {
                    Br = Pr.getNode(Node.Left_son);
                    Left_Rotate(Br);
                }

                stage = 10;
                break;
            case 10:
                Pr = node.getNode(Node.Parent);
                if (node == Pr.getNode(Node.Left_son)) {
                    Br = Pr.getNode(Node.Right_son);
                } else {
                    Br = Pr.getNode(Node.Left_son);
                }

                stage = 11;
                break;
            case 11:
                Pr = node.getNode(Node.Parent);
                if (node == Pr.getNode(Node.Left_son)) {
                    Br = Pr.getNode(Node.Right_son);
                    Br.getNode(Node.Right_son).setColor(Node.Black);
                } else {
                    Br = Pr.getNode(Node.Left_son);
                    Br.getNode(Node.Left_son).setColor(Node.Black);

                }
                if (Br.getColor() != Pr.getColor()) {
                    Br.setColor(Pr.getColor());
                }
                if (Pr.getColor() != Node.Black) {
                    Pr.setColor(Node.Black);
                }

                stage = 12;
                break;
            case 12:
                Pr = node.getNode(Node.Parent);
                if (node == Pr.getNode(Node.Left_son)) {
                    Left_Rotate(Pr);
                } else {
                    Right_Rotate(Pr);
                }
                node = root;
                if (node.getColor() == Node.Red) {
                    stage = 13;
                } else {
                    stage = 0;
                }
                break;
            case 13:
                stage = 14;
                break;
            case 14:
                node.setColor(Node.Black);
                stage = 0;
                break;
        }
    }

    private void Tree_Insert() {
        Node n1, n2;
        n1 = root;
        n2 = treeNull;
        while (n1 != treeNull) {
            n2 = n1;
            if (node.getKey() < n1.getKey()) {
                n1 = n1.getNode(Node.Left_son);
            } else {
                n1 = n1.getNode(Node.Right_son);
            }
        }
        node.setNode(Node.Parent, n2);
        if (n2 == treeNull) {
            root = node;
        }
        else {
            if (node.getKey() < n2.getKey()) {
                n2.setNode(Node.Left_son, node);
            } else {
                n2.setNode(Node.Right_son, node);
            }
        }
        if ((node == root) ||
                (node.getNode(Node.Parent).getColor() == Node.Black)) {
            if (root.getColor() == Node.Red) {
                stage = 9;
            } else {
                stage = 0;
            }
        } else {
            stage = 2;
            InsertStep();
        }
        num_of_nodes++;   
    }

    private void Tree_Delete() {
        Node n1, n2, n3;
        if ((node.getNode(Node.Left_son) == treeNull) ||
                (node.getNode(Node.Right_son) == treeNull)) {
            n1 = node;
        } else {
            n1 = Tree_Successor(node);
        }

        if (n1.getNode(Node.Left_son) != treeNull) {
            n2 = n1.getNode(Node.Left_son);
        } else {
            n2 = n1.getNode(Node.Right_son);
        }

        n3 = n1.getNode(Node.Parent);
        n2.setNode(Node.Parent, n3);
        if (n3 == treeNull) {
            root = n2;
        } else if (n1 == n3.getNode(Node.Left_son)) {
            n3.setNode(Node.Left_son, n2);
        } else {
            n3.setNode(Node.Right_son, n2);
        }

        if (n1 != node) {
            node.setKey(n1.getKey());
        }

        node = n2;
        if (n1.getColor() == Node.Black) {
            if ((node != root) && (node.getColor() == Node.Black)) {
                stage = 2;
            } else if (node.getColor() == Node.Red) {
                stage = 13;
            } else {
                stage = 0;
            }
        } else {
            stage = 0;
        }
        num_of_nodes--;
    }

    private Node Tree_Successor(Node n) {
        Node n1;
        if (n.getNode(Node.Right_son) != treeNull) {
            n = n.getNode(Node.Right_son);
            while (n.getNode(Node.Left_son) != treeNull) {
                n = n.getNode(Node.Left_son);
            }
            return n;
        }
        n1 = n.getNode(Node.Parent);
        while ((n1 != treeNull) && (n == n1.getNode(Node.Right_son))) {
            n = n1;
            n1 = n1.getNode(Node.Parent);
        }
        return n1;
    }

    private void Left_Rotate(Node n1) {
        Node n2;

        n2 = n1.getNode(Node.Right_son);
        n1.setNode(Node.Right_son, n2.getNode(Node.Left_son));
        if (n2.getNode(Node.Left_son) != treeNull) {
            n2.getNode(Node.Left_son).setNode(Node.Parent, n1);
        }
        n2.setNode(Node.Parent, n1.getNode(Node.Parent));
        if (n1.getNode(Node.Parent) == treeNull) {
            root = n2;
        } else if (n1 == n1.getNode(Node.Parent).getNode(Node.Left_son)) {
            n1.getNode(Node.Parent).setNode(Node.Left_son, n2);
        } else {
            n1.getNode(Node.Parent).setNode(Node.Right_son, n2);
        }
        n2.setNode(Node.Left_son, n1);
        n1.setNode(Node.Parent, n2);
    }

    private void Right_Rotate(Node n1) {
        Node n2;

        n2 = n1.getNode(Node.Left_son);
        n1.setNode(Node.Left_son, n2.getNode(Node.Right_son));
        if (n2.getNode(Node.Right_son) != treeNull) {
            n2.getNode(Node.Right_son).setNode(Node.Parent, n1);
        }
        n2.setNode(Node.Parent, n1.getNode(Node.Parent));
        if (n1.getNode(Node.Parent) == treeNull) {
            root = n2;
        } else if (n1 == (n1.getNode(Node.Parent)).getNode(Node.Left_son)) {
            n1.getNode(Node.Parent).setNode(Node.Left_son, n2);
        } else {
            n1.getNode(Node.Parent).setNode(Node.Right_son, n2);
        }
        n2.setNode(Node.Right_son, n1);
        n1.setNode(Node.Parent, n2);
    }

    public Node Search(int key) {
        Node node;
        node = root;
        while ((node != treeNull) && (key != node.getKey())) {
            if (key < node.getKey()) {
                node = node.getNode(Node.Left_son);
            } else {
                node = node.getNode(Node.Right_son);
            }
        }
        return node;
    }

    private void updateHeight() {
        height = 0;
        if (root != treeNull) {
            findHeight(root, 1);
        }
    }

    private void findHeight(Node n, int curr) {
        if (height < curr) {
            height = curr;
        }
        if (n.getNode(Node.Left_son) != treeNull) {
            findHeight(n.getNode(Node.Left_son), curr + 1);
        }
        if (n.getNode(Node.Right_son) != treeNull) {
            findHeight(n.getNode(Node.Right_son), curr + 1);
        }
    }

}
