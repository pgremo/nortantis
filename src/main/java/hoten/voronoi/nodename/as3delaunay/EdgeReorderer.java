package hoten.voronoi.nodename.as3delaunay;

import java.util.ArrayList;

public final class EdgeReorderer {

    private ArrayList<Edge> _edges;
    private ArrayList<LR> _edgeOrientations;

    public ArrayList<Edge> get_edges() {
        return _edges;
    }

    public ArrayList<LR> get_edgeOrientations() {
        return _edgeOrientations;
    }

    public EdgeReorderer(ArrayList<Edge> origEdges, Class<?> criterion) {
        if (criterion != Vertex.class && criterion != Site.class) {
            throw new Error("Edges: criterion must be Vertex or Site");
        }
        _edges = new ArrayList<Edge>();
        _edgeOrientations = new ArrayList<LR>();
        if (origEdges.size() > 0) {
            _edges = reorderEdges(origEdges, criterion);
        }
    }

    public void dispose() {
        _edges = null;
        _edgeOrientations = null;
    }

    private ArrayList<Edge> reorderEdges(ArrayList<Edge> origEdges, Class<?> criterion) {
        int i;
        int n = origEdges.size();
        Edge edge;
        // we're going to reorder the edges in order of traversal
        ArrayList<Boolean> done = new ArrayList<Boolean>(n);
        int nDone = 0;
        for (int k = 0; k < n; k++) {
            done.add( false);
        }
        ArrayList<Edge> newEdges = new ArrayList<Edge>();

        i = 0;
        edge = origEdges.get(i);
        newEdges.add(edge);
        _edgeOrientations.add(LR.LEFT);
        Coord firstPoint = (criterion == Vertex.class) ? edge.get_leftVertex() : edge.get_leftSite();
        Coord lastPoint = (criterion == Vertex.class) ? edge.get_rightVertex() : edge.get_rightSite();

        if (firstPoint == Vertex.VERTEX_AT_INFINITY || lastPoint == Vertex.VERTEX_AT_INFINITY) {
            return new ArrayList<Edge>();
        }

        done.set(i, true);
        ++nDone;

        while (nDone < n) {
            for (i = 1; i < n; ++i) {
                if (done.get(i)) {
                    continue;
                }
                edge = origEdges.get(i);
                Coord leftPoint = (criterion == Vertex.class) ? edge.get_leftVertex() : edge.get_leftSite();
                Coord rightPoint = (criterion == Vertex.class) ? edge.get_rightVertex() : edge.get_rightSite();
                if (leftPoint == Vertex.VERTEX_AT_INFINITY || rightPoint == Vertex.VERTEX_AT_INFINITY) {
                    return new ArrayList<Edge>();
                }
                if (leftPoint == lastPoint) {
                    lastPoint = rightPoint;
                    _edgeOrientations.add(LR.LEFT);
                    newEdges.add(edge);
                    done.set(i, true);
                } else if (rightPoint == firstPoint) {
                    firstPoint = leftPoint;
                    _edgeOrientations.add(0, LR.LEFT);
                    newEdges.add(0, edge);
                    done.set(i, true);
                } else if (leftPoint == firstPoint) {
                    firstPoint = rightPoint;
                    _edgeOrientations.add(0, LR.RIGHT);
                    newEdges.add(0, edge);

                    done.set(i, true);
                } else if (rightPoint == lastPoint) {
                    lastPoint = leftPoint;
                    _edgeOrientations.add(LR.RIGHT);
                    newEdges.add(edge);
                    done.set(i, true);
                }
                if (done.get(i)) {
                    ++nDone;
                }
            }
        }

        return newEdges;
    }
}