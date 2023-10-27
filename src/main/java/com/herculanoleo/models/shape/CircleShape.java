package com.herculanoleo.models.shape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record CircleShape(CartesianPoint a,
                          CartesianPoint b
) implements Shape {
    public List<CartesianPoint> cartesianPoints() {
        var points = new ArrayList<CartesianPoint>(2);
        points.add(a);
        points.add(b);
        return Collections.unmodifiableList(points);
    }

}
