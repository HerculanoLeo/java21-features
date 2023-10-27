package com.herculanoleo.models.shape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record TriangleShape(CartesianPoint a,
                            CartesianPoint b,
                            CartesianPoint c
) implements Shape {
    public List<CartesianPoint> cartesianPoints() {
        var points = new ArrayList<CartesianPoint>(3);
        points.add(a);
        points.add(b);
        points.add(c);
        return Collections.unmodifiableList(points);
    }

}
