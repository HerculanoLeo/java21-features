package com.herculanoleo.models.shape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record RectangleShape(CartesianPoint a,
                             CartesianPoint b,
                             CartesianPoint c,
                             CartesianPoint d
) implements Shape {
    public List<CartesianPoint> cartesianPoints() {
        var points = new ArrayList<CartesianPoint>(4);
        points.add(a);
        points.add(b);
        points.add(c);
        points.add(d);
        return Collections.unmodifiableList(points);
    }

}
